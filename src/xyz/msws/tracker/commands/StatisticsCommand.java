package xyz.msws.tracker.commands;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.TimeParser;

public class StatisticsCommand extends AbstractCommand {
	private PlayerTrackerModule tracker;
	private long start = System.currentTimeMillis();

	public StatisticsCommand(Client client, String name) {
		super(client, name);
		tracker = client.getModule(PlayerTrackerModule.class);
		setAliases("stats");
		setDescription("Views bot, player, or server statistics");
		setUsage("<server/player>");
	}

	@Override
	public void execute(Message message, String[] args) {
		EmbedBuilder builder = new EmbedBuilder();
		if (args.length == 0) {
			builder.setTitle("Statistics");
			builder.setColor(Color.GREEN);
			builder.addField("Total Players", tracker.getPlayers().size() + "", true);
			builder.addField("Servers", tracker.getServers().size() + "", true);
			builder.addField("Uptime", TimeParser.getDurationDescription((System.currentTimeMillis() - start) / 1000),
					true);
			builder.addField("Ping", client.getJDA().getGatewayPing() + "", true);

			message.getChannel().sendMessage(builder.build()).queue();

			message.getChannel()
					.sendFile(new ByteArrayInputStream(String.join(", ", tracker.getPlayerNames()).getBytes()),
							"players.txt")
					.queue();
			return;
		}

		String name = String.join("", args), serverName = null;

		ServerData server;
		for (String n : tracker.getServerNames()) {
			if (n.equalsIgnoreCase(name)) {
				serverName = n;
				break;
			}
		}
		for (String n : tracker.getServerNames()) {
			if (n.replace(" ", "").toLowerCase().contains(name.toLowerCase())
					|| name.toLowerCase().contains(n.replace(" ", "").toLowerCase())) {
				serverName = n;
				break;
			}
		}
		final String fs = serverName;
		server = tracker.getServers().parallelStream().filter(s -> s.getName().equals(fs)).findFirst().orElse(null);

		if (serverName == null || server == null) {
			ServerPlayer player = null;
			for (ServerPlayer p : tracker.getPlayers())
				if (p.getRawName().replace(" ", "").equalsIgnoreCase(name))
					player = p;

			if (player == null)
				for (ServerPlayer p : tracker.getPlayers())
					if (p.getRawName().replace(" ", "").toLowerCase().contains(name.toLowerCase()))
						player = p;

			if (player == null) {
				message.getChannel().sendMessage(
						"Unknown server, available options: \n-" + String.join("\n- ", tracker.getServerNames()))
						.queue();
				return;
			}

			builder.setTitle(player.getRawName() + " Statistics");
			Map<String, Long> servers = new HashMap<>();

			long last = 0;
			for (String s : tracker.getServerNames()) {
				servers.put(s, player.getPlaytimeSince(0, s));
				List<Entry<Long, Long>> times = new ArrayList<>(
						player.getTimes().getOrDefault(s, new LinkedHashMap<>()).entrySet());
				if (times.isEmpty())
					continue;
				long t = times.get(times.size() - 1).getValue();
				if (t > last || t == -1) {
					last = t;
					serverName = s;
				}
			}
			List<Entry<String, Long>> sorted = new ArrayList<>(servers.entrySet());
			sorted.sort(new Comparator<Entry<String, Long>>() {
				@Override
				public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});
			builder.appendDescription("**Server Playtimes**\n");
			for (Entry<String, Long> entry : sorted) {
				builder.appendDescription(
						entry.getKey() + ": " + TimeParser.getDurationDescription(entry.getValue() / 1000) + "\n");
			}

			builder.addField("Last Online", serverName + " "
					+ (last == -1 ? "Now"
							: TimeParser.getDurationDescription((System.currentTimeMillis() - last) / 1000))
					+ " ago", true);

			message.getChannel().sendMessage(builder.build()).queue();
			return;
		}

		builder.setTitle(serverName + " Statistics ");
		builder.appendDescription("**Player Counts Within**:\n");

		for (long time : new long[] { 1000 * 60 * 60, 1000 * 60 * 60 * 24, 1000 * 60 * 60 * 24 * 3,
				1000 * 60 * 60 * 24 * 7 }) {
			long players = tracker.getPlayers().parallelStream()
					.filter(p -> p.getPlaytimeSince(System.currentTimeMillis() - time, fs) > 0).count();

			builder.appendDescription(TimeParser.getDurationDescription(time / 1000) + ": " + players + "\n");
		}
		long players = tracker.getPlayers().parallelStream().filter(p -> p.getPlaytimeSince(0, fs) > 0).count();
		builder.appendDescription("All Time: " + players + "\n");

		builder.appendDescription("\n**Map Rankings**\n");

		List<Entry<String, Set<Long>>> rank = new ArrayList<>();
		rank.addAll(server.getMaps().entrySet());
		rank.sort(new Comparator<Entry<String, Set<Long>>>() {
			@Override
			public int compare(Entry<String, Set<Long>> o1, Entry<String, Set<Long>> o2) {
				return o1.getValue().size() == o2.getValue().size() ? 0
						: o1.getValue().size() > o2.getValue().size() ? -1 : 1;
			}
		});

		for (int i = 0; i < rank.size() && i < 5; i++) {
			Entry<String, Set<Long>> entry = rank.get(i);
			builder.appendDescription(entry.getKey() + ": " + entry.getValue().size() + "\n");
		}

		message.getChannel().sendMessage(builder.build()).queue();
	}

}

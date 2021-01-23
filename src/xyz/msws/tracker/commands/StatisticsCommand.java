package xyz.msws.tracker.commands;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.TimeParser;

public class StatisticsCommand extends AbstractCommand {
	private PlayerTrackerModule tracker;
	private long start = System.currentTimeMillis();

	public StatisticsCommand(Client client, String name) {
		super(client, name);
		tracker = client.getModule(PlayerTrackerModule.class);
		setAliases("stats");
		setDescription("Views bot or server statistics");
		setUsage("<server>");
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
			message.getChannel()
					.sendMessage(
							"Unknown server, available options: \n-" + String.join("\n- ", tracker.getServerNames()))
					.queue();
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

		builder.appendDescription("**Map Rankings**\n");

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

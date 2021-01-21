package xyz.msws.tracker.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.data.pageable.PageableEmbed;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.MSG;
import xyz.msws.tracker.utils.TimeParser;

public class PlaytimeCommand extends AbstractCommand {

	private PlayerTrackerModule tracker;

	public PlaytimeCommand(Client client, String name) {
		super(client, name);
		this.tracker = client.getModule(PlayerTrackerModule.class);
		setAliases("pt");
	}

	@Override
	public void execute(Message message, String[] args) {
		String server = null;
		long from = 0, to = System.currentTimeMillis();

		for (String s : tracker.getServerNames()) {
			if (MSG.simplify(String.join("", args)).contains(MSG.simplify(s.replace(" ", "")))) {
				server = s;
				break;
			}
		}

		for (String s : args) {
			long time;
			try {
				time = Long.parseLong(s) * 1000;
			} catch (NumberFormatException e) {
				time = TimeParser.getDate(s);
			}

			if (time == 0 && server == null) {
				message.getChannel().sendMessage("Unknown argument not a server, duration, or date : " + s).queue();
				continue;
			}
			if (from == 0) {
				from = System.currentTimeMillis() - time;
			} else {
				to = System.currentTimeMillis() - time;
			}

		}

		String duration = "over all time";
		if (from != 0) {
			duration = ("from " + TimeParser.getDateDescription(from));
		}
		if (System.currentTimeMillis() - to > 1000) {
			duration += " to " + ((System.currentTimeMillis() - to < 1000) ? "now" : TimeParser.getDateDescription(to));
		}

		formatPlaytimes(getRankings(from, to, server), (server == null ? "All Servers" : server) + " times " + duration)
				.bindTo(message.getAuthor()).send(message.getTextChannel());
	}

	private PageableEmbed formatPlaytimes(List<Entry<ServerPlayer, Long>> ranked, String title) {
		return formatPlaytimes(ranked, title, 10);
	}

	private PageableEmbed formatPlaytimes(List<Entry<ServerPlayer, Long>> ranked, String title, int pageLines) {
		List<MessageEmbed> lines = new ArrayList<>();

		int maxPages = ranked.size() / pageLines + 1;

		while (!ranked.isEmpty()) {
			EmbedBuilder builder = new EmbedBuilder();

			for (int i = 0; i < pageLines && !ranked.isEmpty(); i++) {
				String name = ranked.get(0).getKey().getRawName();
				long time = ranked.get(0).getValue();
				ranked.remove(0);
				builder.appendDescription(String.format("%d. %s: %s\n", lines.size() * pageLines + i + 1, name,
						TimeParser.getDurationDescription(time / 1000)));
				builder.setFooter("Page " + (lines.size() + 1) + " of " + maxPages);
				builder.setTitle(title);
			}

			lines.add(builder.build());
		}

		return new PageableEmbed(client, lines);
	}

	private Map<ServerPlayer, Long> getPlaytimes(long start, long end, String server) {
		Map<ServerPlayer, Long> leaderboard = new LinkedHashMap<>();
		tracker.getPlayers().forEach(p -> leaderboard.put(p, p.getPlaytimeDuring(start, end, server)));
		return leaderboard;
	}

	private List<Entry<ServerPlayer, Long>> getRankings(long start, long end, String server,
			Comparator<? super Entry<ServerPlayer, Long>> comp) {
		List<Entry<ServerPlayer, Long>> ranked = new ArrayList<>();
		ranked.addAll(getPlaytimes(start, end, server).entrySet());
		ranked.sort(comp);
		return ranked;
	}

	private List<Entry<ServerPlayer, Long>> getRankings(long start, long end, String server) {
		return getRankings(start, end, server, new Comparator<Entry<ServerPlayer, Long>>() {
			@Override
			public int compare(Entry<ServerPlayer, Long> o1, Entry<ServerPlayer, Long> o2) {
				return o1.getValue() == o2.getValue() ? 0 : o1.getValue() > o2.getValue() ? -1 : 1;
			}
		});
	}

}

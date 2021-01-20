package xyz.msws.tracker.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.data.pageable.PageableEmbed;
import xyz.msws.tracker.module.PlayerTrackerModule;
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
		Map<ServerPlayer, Long> leaderboard = new LinkedHashMap<>();
		tracker.getPlayers().forEach(p -> leaderboard.put(p, p.getPlaytimeSince(0)));
		List<Entry<ServerPlayer, Long>> ranked = new ArrayList<>();
		ranked.addAll(leaderboard.entrySet());

		ranked.sort(new Comparator<Entry<ServerPlayer, Long>>() {

			@Override
			public int compare(Entry<ServerPlayer, Long> o1, Entry<ServerPlayer, Long> o2) {
				return o1.getValue() == o2.getValue() ? 0 : o1.getValue() > o2.getValue() ? -1 : 1;
			}
		});

		List<MessageEmbed> lines = new ArrayList<>();

		int pageLines = 10, maxPages = ranked.size() / pageLines + 1;

		while (!ranked.isEmpty()) {
			EmbedBuilder builder = new EmbedBuilder();

			for (int i = 0; i < pageLines && !ranked.isEmpty(); i++) {
				String name = ranked.get(0).getKey().getRawName();
				long time = ranked.get(0).getValue();
				ranked.remove(0);
				builder.appendDescription(String.format("%d. %s: %s\n", lines.size() * pageLines + i + 1, name,
						TimeParser.getDurationDescription(time / 1000)));
				builder.setFooter("Page " + (lines.size() + 1) + " of " + maxPages);
				builder.setTitle("All Servers Leaderboard");
			}

			lines.add(builder.build());
		}

		new PageableEmbed(client, lines).bindTo(message.getAuthor()).send(message.getTextChannel());
	}

}

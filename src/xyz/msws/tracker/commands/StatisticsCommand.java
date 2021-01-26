package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.TimeParser;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

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
            builder.setTitle("Bot Statistics");
            builder.setColor(Color.GREEN);

            String oldest = null, newest = null;
            long oldestLong = 0, newestLong = 0;
            for (ServerPlayer p : tracker.getPlayers()) {
                long pt = p.getPlaytimeSince(0);
                long joined = p.getFirstPlayed();

                if (pt > oldestLong) {
                    oldest = p.getRawName();
                    oldestLong = pt;
                }
                if (joined > newestLong) {
                    newest = p.getRawName();
                    newestLong = joined;
                }
            }

            builder.addField("Most Playtime",
                    oldest + " (" + TimeParser.getDurationDescription(oldestLong / 1000) + ")", true);
            builder.addField("Newest Player",
                    newest + " (Joined " + TimeParser.getDurationDescription(newestLong / 1000) + " ago)", true);

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
                message.getChannel().sendMessage("Unknown server / player, available options: \n-"
                        + String.join("\n- ", tracker.getServerNames())).queue();
                return;
            }

            builder.setColor(Color.BLUE);

            builder.setTitle(player.getRawName() + " Statistics");
            Map<String, Long> servers = new HashMap<>();
            for (String s : player.getTimes().keySet()) {
                servers.put(s, player.getTotalPlaytime(s));
            }

            long last = player.getLastPlayed(), first = player.getFirstPlayed();
            String lastName = player.getLastPlayedName(), firstName = player.getFirstPlayedName();

            List<Entry<String, Long>> sorted = new ArrayList<>(servers.entrySet());
            sorted.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            builder.appendDescription("**Server Playtimes**\n");
            for (Entry<String, Long> entry : sorted) {
                builder.appendDescription(
                        entry.getKey() + ": " + TimeParser.getDurationDescription(entry.getValue() / 1000) + "\n");
            }

            builder.addField(
                    "First Played On", firstName + " ("
                            + TimeParser.getDurationDescription((System.currentTimeMillis() - first) / 1000) + " ago)",
                    true);

            builder.addField("Last Online On",
                    lastName + " (" + (last == -1 ? "Now"
                            : TimeParser.getDurationDescription((System.currentTimeMillis() - last) / 1000) + " ago")
                            + ")",
                    true);

            message.getChannel().sendMessage(builder.build()).queue();
            return;
        }

        builder.setTitle(serverName + " Statistics ");
        builder.setColor(Color.RED);
        builder.appendDescription("**Player Counts Within**:\n");

        for (long time : new long[]{1000 * 60 * 60, 1000 * 60 * 60 * 24, 1000 * 60 * 60 * 24 * 3,
                1000 * 60 * 60 * 24 * 7}) {
            long players = tracker.getPlayers().parallelStream()
                    .filter(p -> p.getPlaytimeSince(System.currentTimeMillis() - time, fs) > 0).count();

            builder.appendDescription(TimeParser.getDurationDescription(time / 1000) + ": " + players + "\n");
        }
        long players = tracker.getPlayers().parallelStream().filter(p -> p.getPlaytimeSince(0, fs) > 0).count();
        builder.appendDescription("All Time: " + players + "\n");

        builder.appendDescription("\n**Map Rankings**\n");

        List<Entry<String, Set<Long>>> rank = new ArrayList<>();
        rank.addAll(server.getMaps().entrySet());
        rank.sort((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size()));

        for (int i = 0; i < rank.size() && i < 5; i++) {
            Entry<String, Set<Long>> entry = rank.get(i);
            builder.appendDescription(entry.getKey() + ": " + entry.getValue().size() + "\n");
        }

        message.getChannel().sendMessage(builder.build()).queue();
    }

}

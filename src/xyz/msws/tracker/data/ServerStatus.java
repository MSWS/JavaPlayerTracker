package xyz.msws.tracker.data;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.servers.SourceServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ServerStatus extends TimerTask {
    private final ServerData data;
    private final Client client;
    private final String botRegex = "((Pending Vote \\(Timeleft:\\s)|\\w+\\s\\(in Time:\\s|\\s\\()(\\d{1,2}:|)\\d{2}[:|.]\\d{2}\\)\\s?";
    private TextChannel channel;
    private Message msg;
    private List<String> players = new ArrayList<>();
    private String recents = "", sourceName = null;
    private Map<String, Object> map = new HashMap<>();
    private int ping = 0;
    private long lastUpdated;

    public ServerStatus(Client client, ServerData data) {
        this.client = client;
        this.data = data;
        client.getJDA().addEventListener(this);
    }

    @SubscribeEvent
    public void onDelete(MessageDeleteEvent e) {
        if (msg == null)
            return;
        if (e.getMessageIdLong() == msg.getIdLong())
            msg = null;
    }

    @Override
    public void run() {
        if (channel == null) {
            String chan = (client instanceof PlayerTracker) ? ((PlayerTracker) client).getConfig().getChannel(data)
                    : "player-logs";
            channel = client.getJDA().getTextChannelsByName(chan, true).get(0);
        }
        if (msg == null) {
            EmbedBuilder embed = new EmbedBuilder().setTitle(data.getName() + " Players");
            channel.sendMessage(embed.build()).queue(m -> {
                msg = m;
                sendUpdate();
            });
        }

        if (msg == null)
            return;
        sendUpdate();
    }

    private void sendUpdate() {
        EmbedBuilder embed = new EmbedBuilder().setTitle((sourceName == null ? data.getName() + " Players" : sourceName));
        StringBuilder desc = new StringBuilder();

        List<String> description = new ArrayList<>();
        if (!players.isEmpty()) {
            Collections.sort(players);
            for (String player : players) {
                if (player.isEmpty())
                    continue;
                if (desc.length() + player.length() > 65) {
                    description.add(desc.substring(0, desc.length() - 2));
                    desc = new StringBuilder();
                }
                desc.append(player).append(", ");
            }
            if (desc.length() > 2)
                description.add(desc.substring(0, desc.length() - 2));
        }

        embed.setDescription(players.isEmpty() ? "No players" : String.join("\n", description));
        embed.addField("Players", players.size() + "/" + (map.get("maxPlayers") == null ? "0" : map.get("maxPlayers")), true);
        embed.addField("Map", map.get("mapName") == null ? "Unknown" : map.get("mapName") + "", true);
//        embed.addField("Ping", ping + "", true);
        embed.setFooter((recents == null ? "" : recents + "\n") + data.getIp() + ":" + data.getPort());
        embed.setTimestamp(Instant.ofEpochMilli(lastUpdated));

        if (map.containsKey("maxPlayers")) {
            int max = Integer.parseInt(map.getOrDefault("maxPlayers", "-1") + "");
            float percent = (float) players.size() / (float) max;
            int r = (int) (percent * 255);
            int g = (int) ((Math.cos((map.get("mapName") + "").length()) + 1) * 255);
            int b = (int) ((Math.sin(ping) + 1) * 255);
            r = Math.min(Math.max(r, 0), 255);
            g = Math.min(Math.max(g, 0), 255);
            b = Math.min(Math.max(b, 0), 255);
            embed.setColor(new Color(r, g, b));
        }

        msg.editMessage(embed.build()).queue();
    }

    private String filterBot(String name) {
        return name.replaceAll(botRegex, "");
    }

    public void update(SourceServer server) {
        try {
            if (!new HashSet<>(players).equals(server.getPlayers().keySet()) && !players.isEmpty()) {
                recents = "";
                StringJoiner joined = new StringJoiner(", "), left = new StringJoiner(", ");
                for (String p : server.getPlayers().keySet()) {
                    String pn = filterBot(p);
                    if (!this.players.contains(pn))
                        joined.add(pn);
                }
                for (String p : this.players) {
                    String pn = filterBot(p);
                    if (!server.getPlayers().containsKey(pn))
                        left.add(pn);
                }
                if (joined.length() > 0)
                    recents += "[+] " + joined + (left.length() > 0 ? "\n" : "");
                if (left.length() > 0)
                    recents += "[-] " + left;
            }
            // Clone player list
            this.players = server.getPlayers().keySet().parallelStream().map(s -> s.replaceAll(botRegex, "")).collect(Collectors.toList());
            this.ping = server.getPing();
            map = server.getServerInfo();
            lastUpdated = System.currentTimeMillis();
            sourceName = server.getServerInfo().get("serverName") + "";
//            Logger.log("Info: " + server.getServerInfo().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.toList()));
        } catch (SteamCondenserException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}

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
import xyz.msws.tracker.utils.TimeParser;

import java.awt.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class ServerStatus extends TimerTask {
	private final ServerData data;
	private TextChannel channel;
	private Message msg;
	private Set<String> players = new HashSet<>();
	private String recents = "";
	private Map<String, Object> map = new HashMap<>();
	private final Client client;
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
		EmbedBuilder embed = new EmbedBuilder().setTitle(data.getName() + " Players");

		embed.setDescription(players.isEmpty() ? "No players" : String.join("\n", players));
		embed.addField("Players", players.size() + "/" + map.get("maxPlayers"), true);
		embed.addField("Map", map.get("mapName") + "", true);
		embed.addField("Ping", ping + "", true);
		embed.setFooter(recents + "\nLast Updated: " + TimeParser.getDateDescription(lastUpdated));

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

	public void update(SourceServer server) {
		try {
			if (!this.players.equals(server.getPlayers().keySet())) {
				recents = "";
				StringJoiner joined = new StringJoiner(", "), left = new StringJoiner(", ");
				for (String p : server.getPlayers().keySet())
					if (!this.players.contains(p))
						joined.add(p);
				for (String p : this.players) {
					if (!server.getPlayers().containsKey(p))
						left.add(p);
				}
				if (joined.length() > 0)
					recents += "[+] " + joined.toString() + (left.length() > 0 ? "\n" : "");
				if (left.length() > 0)
					recents += "[-] " + left.toString();
			}
			// Clone player list
			this.players = new HashSet<>(server.getPlayers().keySet());
			this.ping = server.getPing();
			map = server.getServerInfo();
			lastUpdated = System.currentTimeMillis();
		} catch (SteamCondenserException | TimeoutException e) {
			e.printStackTrace();
		}

	}

}

package xyz.msws.tracker.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;

public class ServerStatus extends TimerTask {
	private ServerData data;
	private TextChannel channel;
	private Message msg;
	private Set<String> players;
	private Map<String, Object> map = new HashMap<>();
	private Client client;

	public ServerStatus(Client client, ServerData data) {
		this.client = client;
		this.data = data;
	}

	@Override
	public void run() {
		if (channel == null)
			channel = client.getJDA().getTextChannelsByName("player-logs", true).get(0);
		if (msg == null) {
			EmbedBuilder embed = new EmbedBuilder().setTitle(data.getName() + " Players");
			embed.setDescription("Querying...");
			channel.sendMessage(embed.build()).queue(m -> msg = m);
		}
		if (msg == null)
			return;

		EmbedBuilder embed = new EmbedBuilder().setTitle(data.getName() + " Players");

		embed.setDescription(players.isEmpty() ? "No players" : String.join("\n", players));
		embed.addField("Players", players.size() + "/" + map.get("maxPlayers"), true);
		embed.addField("Map", map.get("mapName") + "", true);
		msg.editMessage(embed.build()).queue();
	}

	public void update(SourceServer server) {
		try {
			this.players = server.getPlayers().keySet();
			map = server.getServerInfo();
		} catch (SteamCondenserException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

package xyz.msws.tracker.module;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.data.ServerStatus;
import xyz.msws.tracker.utils.Logger;

public class PlayerTrackerModule extends Module {
	private Map<String, ServerPlayer> players = new ConcurrentHashMap<>();
	private Map<ServerData, ServerStatus> servers = new HashMap<>();

	public PlayerTrackerModule(Client client, List<ServerData> servers) {
		super(client);
		servers.forEach(s -> this.servers.put(s, new ServerStatus(client, s)));
	}

	private Timer timer;

	@Override
	public void load() {
		timer = new Timer();

		for (ServerData d : servers.keySet()) {
			String chan = (client instanceof PlayerTracker) ? ((PlayerTracker) client).getConfig().getChannel(d)
					: "player-logs";
			TextChannel channel = client.getJDA().getTextChannelsByName(chan, true).get(0);
			purge(channel);
		}

		servers.values().forEach(s -> timer.schedule(s, 0, 1000 * 30));

		Logger.logf("Loading all player files...");

		for (File f : PlayerTracker.PLAYER_FILE.listFiles()) {
			ServerPlayer sp = new ServerPlayer(f);
			if (sp.getRawName() == null)
				continue;
			players.put(sp.getRawName(), sp);
		}

		Logger.logf("Loaded %d files", players.size());
	}

	private void purge(TextChannel channel) {
		List<Message> messages = channel.getHistory().retrievePast(100).complete();
		if (messages.size() < 2) {
			messages.forEach(m -> m.delete().queue());
			return;
		}
		channel.deleteMessages(messages).queue();
	}

	@Override
	public void unload() {
		players = new HashMap<String, ServerPlayer>();
	}

	public void update(ServerData server, SourceServer data) {
		servers.get(server).update(data);
	}

	public ServerPlayer getPlayer(String name) {
		if (players.containsKey(name))
			return players.get(name);
		ServerPlayer result = new ServerPlayer(name);
		players.put(name, result);
		return result;
	}

	public Collection<ServerPlayer> getPlayers() {
		return players.values();
	}

	public Set<String> getPlayerNames() {
		return players.keySet();
	}

	public Collection<ServerData> getServers() {
		return servers.keySet();
	}

	public List<String> getServerNames() {
		return servers.keySet().stream().map(s -> s.getName()).collect(Collectors.toList());
	}

}

package xyz.msws.tracker.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;

public class PlayerTrackerModule extends Module {
	private Map<String, ServerPlayer> players = new HashMap<>();
	private Map<ServerData, ServerStatus> servers = new HashMap<>();

	public PlayerTrackerModule(Client client, List<ServerData> servers) {
		super(client);
		servers.forEach(s -> this.servers.put(s, new ServerStatus(client, s)));
	}

	private Timer timer;

	@Override
	public void load() {
		timer = new Timer();
		servers.values().forEach(s -> timer.schedule(s, 0, 1000));
	}

	@Override
	public void unload() {
		players.values().forEach(ServerPlayer::saveData);
		players = new HashMap<String, ServerPlayer>();
	}

	public void save() {
		players.values().forEach(ServerPlayer::saveData);
	}

	public void update(ServerData server, SourceServer data) {
		servers.get(server).update(data);
	}

	public ServerPlayer getPlayer(String name) {
		if (players.containsKey(name))
			return players.get(name);
		ServerPlayer result = new ServerPlayer(name);
		result.load();
		players.put(name, result);
		return result;
	}

}

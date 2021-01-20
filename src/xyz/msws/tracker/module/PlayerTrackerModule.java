package xyz.msws.tracker.module;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;
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

		System.out.println("Loading all player files...");

		for (File f : PlayerTracker.PLAYER_FILE.listFiles()) {
			ServerPlayer sp = new ServerPlayer(f);
			players.put(sp.getRawName(), sp);
		}

		System.out.printf("Loaded %d files", players.size());
	}

	@Override
	public void unload() {
		save();
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
		players.put(name, result);
		return result;
	}

	public Collection<ServerPlayer> getPlayers() {
		return players.values();
	}

}

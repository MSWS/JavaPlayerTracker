package xyz.msws.tracker.trackers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class CSGOTracker extends Tracker {

	private PlayerTrackerModule tracker;
	private Set<ServerPlayer> toSave = new HashSet<>();

	public CSGOTracker(Client client, ServerData server) {
		super(client, server);
		tracker = client.getModule(PlayerTrackerModule.class);

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					connection.updateServerInfo();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 10000, 1000 * 60 * 5);
	}

	private Set<String> oldPlayers = new HashSet<>();

	@Override
	public void run() {
		try {
			connection.updatePlayers();
			connection.updatePing();
			Set<String> unparsed = connection.getPlayers().keySet();
			Iterator<String> it = unparsed.iterator();
			while (it.hasNext()) {
				String s = it.next();
				ServerPlayer sp = tracker.getPlayer(s);
				toSave.add(sp);
				if (oldPlayers.contains(s)) {
					oldPlayers.remove(s);
					continue;
				}
				if (s.isEmpty() || s == null)
					continue;
				sp.logOn(server);
			}

			oldPlayers.forEach(s -> {
				ServerPlayer sp = tracker.getPlayer(s);
				toSave.add(sp);
				sp.logOn(server);
			});
			oldPlayers = connection.getPlayers().keySet();

			server.addMap(connection.getServerInfo().get("mapName") + "");
		} catch (SteamCondenserException | TimeoutException e) {
			e.printStackTrace();
		}

		tracker.update(server, connection);
	}

	@Override
	public void save() {
		toSave.forEach(ServerPlayer::saveData);
		toSave.clear();
	}
}

package xyz.msws.tracker.trackers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class CSGOTracker extends Tracker {

	private PlayerTrackerModule tracker;

	public CSGOTracker(Client client, ServerData server) {
		super(client, server);
		tracker = client.getModule(PlayerTrackerModule.class);
	}

	private Set<String> oldPlayers = new HashSet<>();

	@Override
	public void run() {
		try {
			connection.updatePlayers();
			connection.updatePing();
			connection.updateServerInfo();
			Set<String> unparsed = connection.getPlayers().keySet();
			Iterator<String> it = unparsed.iterator();
			while (it.hasNext()) {
				String s = it.next();
				if (oldPlayers.contains(s)) {
					oldPlayers.remove(s);
					continue;
				}
				System.out.printf("%s logged on to %s\n", s, server.getName());
				tracker.getPlayer(s).logOn(server);
			}

			oldPlayers.forEach(s -> {
				System.out.printf("%s logged off of %s\n", s, server.getName());
				tracker.getPlayer(s).logOff(server);
			});

			oldPlayers = connection.getPlayers().keySet();

			server.addMap(connection.getServerInfo().get("mapName") + "");
		} catch (SteamCondenserException | TimeoutException e) {
			e.printStackTrace();
		}

		tracker.update(server, connection);
	}
}

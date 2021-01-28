package xyz.msws.tracker.trackers;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class CSGOTracker extends Tracker {

    private final PlayerTrackerModule tracker;
    private final Set<ServerPlayer> toSave = new HashSet<>();

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
                    Logger.log(e);
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
            for (String s : unparsed) {
                if (s.trim().isEmpty())
                    continue;
                ServerPlayer sp = tracker.getPlayer(s);
                toSave.add(sp);
                if (oldPlayers.contains(s)) {
                    oldPlayers.remove(s);
                    continue;
                }

                sp.logOn(server);
            }

            oldPlayers.forEach(s -> {
                if (s.trim().isEmpty())
                    return;
                ServerPlayer sp = tracker.getPlayer(s);
                toSave.add(sp);
                sp.logOff(server);
            });
            oldPlayers = new HashSet<>(connection.getPlayers().keySet());

            server.addMap(connection.getServerInfo().get("mapName") + "");
            tracker.update(server, connection);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.log(e);
        }
    }

    @Override
    public void save() {
        toSave.forEach(ServerPlayer::saveData);
        toSave.clear();
    }
}

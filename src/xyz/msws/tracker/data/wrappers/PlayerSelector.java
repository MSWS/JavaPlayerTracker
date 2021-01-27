package xyz.msws.tracker.data.wrappers;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.MSG;

import java.util.ArrayList;

public class PlayerSelector extends Selector<ServerPlayer> {
    public PlayerSelector(Client client, String term) {
        super(new ArrayList<>());
        options.addAll(client.getModule(PlayerTrackerModule.class).getPlayers());
        this.filter(s -> {
            String ss = MSG.simplify(s.getRawName().replace(" ", ""));
            if (s.getRawName().equals(term) || ss.contains(term) || term.contains(ss))
                return true;
            ss = MSG.simplify(s.getName().replace(" ", ""));
            return (s.getRawName().equals(term) || ss.contains(term) || term.contains(ss));
        });
        this.sortLexi(term);
    }
}

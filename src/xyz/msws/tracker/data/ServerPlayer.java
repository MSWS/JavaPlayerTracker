package xyz.msws.tracker.data;

/**
 * Encapsulates the data of a player, primarily playtime
 *
 * @author Isaac
 */
public class ServerPlayer {

    private final String rawName;

    public ServerPlayer(String rawName) {
        this.rawName = rawName;
    }

    public String getRawName() {
        return rawName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ServerPlayer))
            return false;
        ServerPlayer sp = (ServerPlayer) obj;
        return sp.getRawName().equals(this.getRawName());
    }


    @Override
    public String toString() {
        return rawName;
    }
}

package xyz.msws.tracker.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates server data including server name, ip, port, and map history
 *
 * @author Isaac
 */
public class ServerData {
    private final Map<String, Set<Long>> maps = new HashMap<>();
    private final String name;
    private final String ip;
    private String sourceName;
    private int port;
    private String lastMap;

    /**
     * Alias for alternative constructor to add support to differentiate between ip
     * and port
     *
     * @param name
     * @param ip
     * @param port
     */
    public ServerData(String name, String ip, int port) {
        this(name, ip);
        this.port = port;
    }

    /**
     * Creates a ServerData instance give the server name and ip Manually loads the
     * data from the file if it exists
     *
     * @param name Name of the server
     * @param ip   ip of the server, can include a colon to separate ip and port
     */
    public ServerData(String name, String ip) {

        if (ip.contains(":")) {
            this.ip = ip.split(":")[0];
            port = Integer.parseInt(ip.split(":")[1]);
        } else {
            this.ip = ip;
            this.port = 27015;
        }

        this.name = name;
    }


    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return getIp() + ":" + getPort();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ServerData))
            return false;
        ServerData dat = (ServerData) obj;
        return dat.getIp().equals(this.getIp()) && dat.getName().equals(this.name) && dat.getPort() == this.port;
    }

}

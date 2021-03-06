package xyz.msws.tracker.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.utils.Logger;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Encapsulates server data including server name, ip, port, and map history
 *
 * @author Isaac
 */
public class ServerData {
    private final Map<String, Set<Long>> maps = new HashMap<>();
    private final File file;
    private String name, ip;
    private int port;
    private String lastMap;

    public ServerData(File file) {
        this.file = file;
    }

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
        this.file = new File(PlayerTracker.SERVER_FILE, name + ".txt");
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();

        if (ip.contains(":")) {
            this.ip = ip.split(":")[0];
            port = Integer.parseInt(ip.split(":")[1]);
        } else {
            this.ip = ip;
            this.port = 27015;
        }

        this.name = name;
        loadData();
    }

    /**
     * Loads the data from the file
     */
    private void loadData() {
        Logger.logf("Loading server data of %s", name);
        FileReader fread;
        if (!file.exists())
            return;
        try {
            fread = new FileReader(file);
            BufferedReader reader = new BufferedReader(fread);
            String data = reader.readLine();
            reader.close();
            JsonElement obj = JsonParser.parseString(data);
            if (!obj.isJsonObject()) {
                Logger.logf("Json data from file %s is invalid", file.getName());
                return;
            }

            JsonObject dat = obj.getAsJsonObject();
            name = dat.get("name").getAsString();
            ip = dat.get("ip").getAsString();
            port = dat.get("port").getAsInt();

            JsonElement mapData = dat.get("maps");
            if (!mapData.isJsonObject()) {
                Logger.logf("Unable to load mapdata from %s", file.getName());
                return;
            }

            JsonObject mapObj = mapData.getAsJsonObject();
            long big = 0;
            for (Entry<String, JsonElement> entry : mapObj.entrySet()) {
                Set<Long> times = new HashSet<>();
                if (!entry.getValue().isJsonArray()) {
                    Logger.logf("Unable to load map timings for map %s from %s",
                            entry.getKey(), file.getName());
                    continue;
                }
                for (JsonElement p : entry.getValue().getAsJsonArray()) {
                    if (!p.isJsonPrimitive()) {
                        Logger.logf("Timing report %s from map %s in file %s is malformed",
                                p.toString(), entry.getKey(), file.getName());
                        continue;
                    }
                    if (p.getAsLong() > big) {
                        lastMap = entry.getKey();
                        big = p.getAsLong();
                    }
                    times.add(p.getAsLong());
                }
                maps.put(entry.getKey(), times);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData() {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("ip", ip);
        data.addProperty("port", port);

        JsonObject maps = new JsonObject();

        for (Entry<String, Set<Long>> entry : this.maps.entrySet()) {
            JsonArray times = new JsonArray();
            entry.getValue().forEach(times::add);
            maps.add(entry.getKey(), times);
        }

        data.add("maps", maps);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(data.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void addMap(String mapName) {
        if (mapName.equals(lastMap))
            return;
        this.lastMap = mapName;
        Set<Long> values = maps.getOrDefault(mapName, new HashSet<>());
        values.add(System.currentTimeMillis());
        maps.put(mapName, values);
    }

    /**
     * Returns the best guess of what the server's map was at the time, may return null if no data
     *
     * @param time Epoch timestamp to get map at
     * @return Name of map, may be null
     */
    public String getMap(long time) {
        LinkedHashMap<Long, String> times = new LinkedHashMap<>();
        maps.forEach((s, longs) -> {
            longs.forEach(l -> times.put(l, s));
        });
        List<Entry<Long, String>> sorted = times.entrySet().stream().sorted(Entry.comparingByKey()).collect(Collectors.toList());
        String result = null;
        for (Entry<Long, String> entry : sorted) {
            if (entry.getKey() > time)
                return result;
            result = entry.getValue();
        }

        return result;
    }

    public Map<String, Set<Long>> getMaps() {
        return maps;
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

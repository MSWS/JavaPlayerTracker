package xyz.msws.tracker.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.utils.Logger;

/**
 * Encapsulates server data including server name, ip, port, and map history
 * 
 * @author Isaac
 *
 */
public class ServerData {
	private String name, ip;
	private int port;

	private Map<String, Set<Long>> maps = new HashMap<>();
	private File file;
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
	 * 
	 * @return
	 */
	private boolean loadData() {
		Logger.logf("Loading server data of %s", name);
		FileReader fread;
		if (!file.exists())
			return false;
		try {
			fread = new FileReader(file);
			BufferedReader reader = new BufferedReader(fread);
			String data = reader.readLine();
			reader.close();
			JsonElement obj = JsonParser.parseString(data);
			if (!obj.isJsonObject()) {
				Logger.logf("Json data from file %s is invalid", file.getName());
				return false;
			}

			JsonObject dat = obj.getAsJsonObject();
			name = dat.get("name").getAsString();
			ip = dat.get("ip").getAsString();
			port = dat.get("port").getAsInt();

			JsonElement mapData = dat.get("maps");
			if (!mapData.isJsonObject()) {
				Logger.logf("Unable to load mapdata from %s", file.getName());
				return true;
			}

			JsonObject mapObj = mapData.getAsJsonObject();
			long big = 0;
			for (Entry<String, JsonElement> entry : mapObj.entrySet()) {
				Set<Long> times = new HashSet<>();
				if (!entry.getValue().isJsonArray()) {
					Logger.logf("Unable to load map timings for map %s from %s", entry.getKey(), file.getName());
					continue;
				}
				for (JsonElement p : entry.getValue().getAsJsonArray()) {
					if (!p.isJsonPrimitive()) {
						Logger.logf("Timing report %s from map %s in file %s is malformed", p.toString(),
								entry.getKey(), file.getName());
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
			return false;
		}
		return true;
	}

	public void saveData() {
		Logger.logf("Saving server data of %s", name);
		JsonObject data = new JsonObject();
		data.addProperty("name", name);
		data.addProperty("ip", ip);
		data.addProperty("port", port);

		JsonObject maps = new JsonObject();

		for (Entry<String, Set<Long>> entry : this.maps.entrySet()) {
			JsonArray times = new JsonArray();
			entry.getValue().forEach(l -> times.add(l));
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

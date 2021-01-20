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

public class ServerData {
	private String name, ip;
	private int port;

	private Map<String, Set<Long>> maps = new HashMap<>();
	private File file;
	private String lastMap;

	public ServerData(File file) {
		this.file = file;
	}

	public ServerData(String name, String ip, int port) {
		this(name, ip);
		this.port = port;
	}

	public ServerData(String name, String ip) {
		this.file = new File("servers" + File.separator + name + ".txt");
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
	}

	public boolean loadData() {
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
				System.out.printf("Json data from file %s is invalid\n", file.getName());
				return false;
			}

			JsonObject dat = obj.getAsJsonObject();
			name = dat.get("name").getAsString();
			ip = dat.get("ip").getAsString();
			port = dat.get("port").getAsInt();

			JsonElement mapData = dat.get("maps");
			if (!mapData.isJsonObject()) {
				System.out.printf("Unable to load mapdata from %s\n", file.getName());
				return true;
			}

			JsonObject mapObj = mapData.getAsJsonObject();
			for (Entry<String, JsonElement> entry : mapObj.entrySet()) {
				Set<Long> times = new HashSet<>();
				if (!entry.getValue().isJsonArray()) {
					System.out.printf("Unable to load map timings for map %s from %s\n", entry.getKey(),
							file.getName());
					continue;
				}
				for (JsonElement p : entry.getValue().getAsJsonArray()) {
					if (!p.isJsonPrimitive()) {
						System.out.printf("Timing report %s from map %s in file %s is malformed\n", p.toString(),
								entry.getKey(), file.getName());
						continue;
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

package xyz.msws.tracker.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.msws.tracker.Client;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Support for specifying servers from a config file
 *
 * @author Isaac
 */
public class TrackerConfig {

    private final File file;
    private final Map<ServerData, String> servers = new HashMap<>();

    public TrackerConfig(File file) {
        Client.getLogger().info(
                String.format("Creating new tracker config from %s (%s)", file.getName(), file.getAbsolutePath()));
        this.file = file;
        FileReader fread;
        if (!file.exists())
            return;
        try {
            fread = new FileReader(file);
			BufferedReader reader = new BufferedReader(fread);
			String data = reader.readLine();
			reader.close();
			JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
			if (!obj.isJsonObject())
				return;
			JsonObject serverObj = obj.get("servers").getAsJsonObject();
			for (Entry<String, JsonElement> entry : serverObj.entrySet()) {
				if (!(entry.getValue().isJsonObject())) {
					Client.getLogger().info(String.format("%s is not a json object", entry.getKey()));
					continue;
				}
				JsonObject serverEntry = entry.getValue().getAsJsonObject();
				String ip = serverEntry.get("ip").getAsString();
				String channel = serverEntry.get("channel").getAsString();
				servers.put(new ServerData(entry.getKey(), ip), channel);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		Client.getLogger().info("Saving Tracker Config...");
		JsonObject data = new JsonObject();

		JsonObject map = new JsonObject();
		for (Entry<ServerData, String> entry : servers.entrySet()) {
			JsonObject s = new JsonObject();
			s.addProperty("ip", entry.getKey().getIp());
			s.addProperty("channel", entry.getValue());
			map.add(entry.getKey().getName(), s);
		}
		data.add("servers", map);

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(data.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getChannel(ServerData data) {
		return servers.getOrDefault(data, "unset");
	}

	public Map<ServerData, String> getServers() {
		return servers;
	}

	public void addServer(ServerData data, String channel) {
		servers.put(data, channel);
	}

	public void removeServer(ServerData data) {
		servers.remove(data);
	}

	public void clearServers() {
		servers.clear();
	}

}

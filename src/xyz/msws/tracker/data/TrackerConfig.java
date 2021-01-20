package xyz.msws.tracker.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TrackerConfig {

	private File file;
	private List<ServerData> servers = new ArrayList<>();
	private String channelName = "player-logs";

	public TrackerConfig(File file) {
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
				servers.add(new ServerData(entry.getKey(), entry.getValue().getAsString()));
			}

			channelName = obj.get("channelName").getAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		JsonObject data = new JsonObject();
		JsonObject servers = new JsonObject();
		this.servers.forEach(s -> servers.addProperty(s.getName(), s.toString()));
		data.add("servers", servers);
		data.addProperty("channelName", channelName);

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(data.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<ServerData> getServers() {
		return servers;
	}

	public String getChannelName() {
		return channelName;
	}

	public void addServer(ServerData data) {
		servers.add(data);
	}

	public void setServers(List<ServerData> data) {
		this.servers = data;
	}

	public void clearServers() {
		servers.clear();
	}

}

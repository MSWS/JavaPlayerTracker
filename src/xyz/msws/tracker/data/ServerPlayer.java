package xyz.msws.tracker.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.slugify.Slugify;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ServerPlayer {
	private final static Slugify slg = new Slugify();

	private String name, rawName;
	private File file;

	private Map<String, LinkedHashMap<Long, Long>> times = new HashMap<String, LinkedHashMap<Long, Long>>();

	public ServerPlayer(String rawName) {
		this.rawName = name;
		this.name = simplify(rawName);

		file = new File("players" + File.separator + name + ".txt");
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
	}

	public ServerPlayer(File file) {
		this.file = file;
	}

	public boolean load() {
		if (!file.exists())
			return false;
		FileReader fread;
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
			rawName = dat.get("name").getAsString();
			name = simplify(rawName);

			JsonElement timeData = dat.get("time");
			if (!timeData.isJsonObject()) {
				System.out.printf("Player data %s is malformed from file %s\n", timeData.toString(), file.getName());
				return false;
			}

			JsonObject timeObj = timeData.getAsJsonObject();

			for (Entry<String, JsonElement> entry : timeObj.entrySet()) {
				String server = entry.getKey();
				if (!entry.getValue().isJsonObject()) {
					System.out.printf("Skipping server data %s because it is malformed (%s)", server,
							entry.getValue().toString());
					continue;
				}
				LinkedHashMap<Long, Long> timeMap = new LinkedHashMap<Long, Long>();

				for (Entry<String, JsonElement> e : entry.getValue().getAsJsonObject().entrySet()) {
					timeMap.put(Long.parseLong(e.getKey()), e.getValue().getAsLong());
				}

				times.put(server, timeMap);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void saveData() {
		JsonObject data = new JsonObject();
		data.addProperty("name", name);

		JsonObject serverTimes = new JsonObject();
		for (Entry<String, LinkedHashMap<Long, Long>> entry : times.entrySet()) {
			JsonObject times = new JsonObject();

			// Check if the value is -1, in which case save the current time

			entry.getValue().entrySet().forEach(e -> times.addProperty(e.getKey().toString(),
					e.getValue() == -1 ? System.currentTimeMillis() + "" : e.getValue().toString()));
			serverTimes.add(entry.getKey(), times);
		}

		data.add("time", serverTimes);

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(data.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void logOn(ServerData server) {
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server.getName(), new LinkedHashMap<Long, Long>());
		times.put(System.currentTimeMillis(), -1L);
		this.times.put(server.getName(), times);
	}

	public void logOff(ServerData server) {
		if (this.times.get(server.getName()).isEmpty()) {
			System.out
					.println("[WARNING] Desynchronization of player tracking, attempted to logOff when not logged on");
			return;
		}
		List<Entry<Long, Long>> sessions = new ArrayList<>();
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server.getName(), new LinkedHashMap<Long, Long>());

		times.entrySet().forEach(e -> sessions.add(e));

		if (sessions.get(sessions.size() - 1).getValue() != -1) {
			System.out
					.println("[WARNING] Desynchronization of player tracking, attempted to logOff when not logged on");
			return;
		}

		times.put(sessions.get(sessions.size() - 1).getKey(), System.currentTimeMillis());
		this.times.put(server.getName(), times);
	}

	public String getName() {
		return name;
	}

	public String getRawName() {
		return rawName;
	}

	public static String simplify(String name) {
		return slg.slugify(name);
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

}

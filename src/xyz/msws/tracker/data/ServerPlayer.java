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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.msws.tracker.Logger;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.utils.MSG;

/**
 * Encaplusates the data of a player, primarily playtime
 * 
 * @author Isaac
 *
 */
public class ServerPlayer {

	private String name, rawName;
	private File file;

	private Map<String, LinkedHashMap<Long, Long>> times = new HashMap<String, LinkedHashMap<Long, Long>>();

	public ServerPlayer(String rawName) {
		this.rawName = rawName;
		this.name = MSG.simplify(rawName);

		file = new File(PlayerTracker.PLAYER_FILE, name + ".txt");
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
	}

	public ServerPlayer(File file) {
		this.file = file;
		load();
	}

	private boolean load() {
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
				Logger.logf("Json data from file %s is invalid", file.getName());
				return false;
			}

			JsonObject dat = obj.getAsJsonObject();
			rawName = dat.get("name").getAsString();
			name = MSG.simplify(rawName);

			JsonElement timeData = dat.get("time");
			if (!timeData.isJsonObject()) {
				Logger.logf("Player data %s is malformed from file %s", timeData.toString(), file.getName());
				return false;
			}

			JsonObject timeObj = timeData.getAsJsonObject();

			for (Entry<String, JsonElement> entry : timeObj.entrySet()) {
				String server = entry.getKey();
				if (!entry.getValue().isJsonObject()) {
					Logger.logf("Skipping server data %s because it is malformed (%s)", server,
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
		data.addProperty("name", rawName);
		if (rawName == null)
			Logger.logf("[WARNING] Saving %s's name as null", rawName);

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

	/**
	 * Marks the player as having logged on to a server
	 * 
	 * @param server
	 */
	public void logOn(ServerData server) {
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server.getName(), new LinkedHashMap<Long, Long>());
		times.put(System.currentTimeMillis(), -1L);
		this.times.put(server.getName(), times);
	}

	/**
	 * Marks the player as having logged off of the specified server
	 * 
	 * @param server
	 */
	public void logOff(ServerData server) {
		if (this.times.get(server.getName()).isEmpty()) {
			Logger.logf("[WARNING] Desynchronization of player tracking, attempted to logOff %s when not logged on",
					rawName);
			Logger.logf("Error type: empty");
			return;
		}
		List<Entry<Long, Long>> sessions = new ArrayList<>();
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server.getName(), new LinkedHashMap<Long, Long>());

		times.entrySet().forEach(e -> sessions.add(e));

		Entry<Long, Long> entry = sessions.get(sessions.size() - 1);

		if (entry.getValue() != -1) {
			Logger.logf("[WARNING] Desynchronization of player tracking, attempted to logOff %s when not logged on",
					rawName);
			Logger.logf("Error type: -1");
			Logger.logf("Actual value: " + entry.getValue() + " key: " + entry.getKey() + " (time ago: "
					+ (System.currentTimeMillis() - entry.getKey()) + ")");
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ServerPlayer))
			return false;
		ServerPlayer sp = (ServerPlayer) obj;
		return sp.getRawName().equals(this.getRawName());
	}

	public long getPlaytimeSince(long start) {
		return getPlaytimeSince(start, null);
	}

	public long getPlaytimeSince(long start, String server) {
		return getPlaytimeDuring(start, System.currentTimeMillis(), server);
	}

	public long getPlaytimeDuring(long start, long end) {
		return getPlaytimeDuring(start, end, null);
	}

	/**
	 * Returns the given playtime of the player between the start and end
	 * timestamps, if the player is currently on then that time is accounted for
	 * 
	 * @param start
	 * @param end
	 * @param server
	 * @return
	 */
	public long getPlaytimeDuring(long start, long end, String server) {
		long result = 0;
		if (server == null) {
			for (String k : this.times.keySet())
				result += getPlaytimeDuring(start, end, k);
			return result;
		}
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server, new LinkedHashMap<Long, Long>());
		for (Entry<Long, Long> entry : times.entrySet()) {
			if (entry.getKey() > end) {
				// We've gone past the max limit specified by end
				break;
			}
			result += (entry.getValue() == -1 ? System.currentTimeMillis() : entry.getValue()) - entry.getKey();
		}
		return result;
	}

}

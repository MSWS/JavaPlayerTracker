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
		load();
	}

	public ServerPlayer(File file) {
		this.file = file;
		load();
	}

	private boolean load() {
		if (!times.isEmpty()) {
			PlayerTracker.getLogger().warning("Attempted to load while data was already loaded");
		}
		if (!file.exists())
			return false;
		FileReader fread;
		try {
			fread = new FileReader(file);
			BufferedReader reader = new BufferedReader(fread);
			String data = reader.readLine();
			reader.close();
			if (data == null) {
				PlayerTracker.getLogger().severe(String.format("%s's data is null", file.getName()));
				return false;
			}
			JsonElement obj = JsonParser.parseString(data);
			if (!obj.isJsonObject()) {
				PlayerTracker.getLogger().severe(String.format("Json data from file %s is invalid", file.getName()));
				return false;
			}

			JsonObject dat = obj.getAsJsonObject();
			if (!dat.has("name")) {
				PlayerTracker.getLogger()
						.severe(String.format("Json data from file %s does not have name", file.getName()));
				return false;
			}
			if (dat.get("name").isJsonNull()) {
				PlayerTracker.getLogger()
						.warning(String.format("Json data from file %s has null name", file.getName()));
				return false;
			}

			rawName = dat.get("name").getAsString();
			name = MSG.simplify(rawName);

			JsonElement timeData = dat.get("time");
			if (!timeData.isJsonObject()) {
				PlayerTracker.getLogger().warning(
						String.format("Player data %s is malformed from file %s", timeData.toString(), file.getName()));
				return false;
			}

			JsonObject timeObj = timeData.getAsJsonObject();

			for (Entry<String, JsonElement> entry : timeObj.entrySet()) {
				String server = entry.getKey();
				if (!entry.getValue().isJsonObject()) {
					PlayerTracker.getLogger()
							.severe(String.format("Skipping server data %s because it is malformed (%s)", server,
									entry.getValue().toString()));
					continue;
				}
				LinkedHashMap<Long, Long> timeMap = new LinkedHashMap<Long, Long>();

				long last = 0;
				for (Entry<String, JsonElement> e : entry.getValue().getAsJsonObject().entrySet()) {
					long c = Long.parseLong(e.getKey());
					if (c < last) {
						PlayerTracker.getLogger()
								.warning(String.format("WARNING Loading data of %s and it is unordered", rawName));
					}
					timeMap.put(Long.parseLong(e.getKey()), e.getValue().getAsLong());
				}

				times.put(server, timeMap);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void delete() {
		file.delete();
		times = new HashMap<>();
	}

	public void saveData() {
		JsonObject data = new JsonObject();
		if (rawName == null)
			return;
		data.addProperty("name", rawName);

		JsonObject serverTimes = new JsonObject();
		for (Entry<String, LinkedHashMap<Long, Long>> entry : times.entrySet()) {
			JsonObject times = new JsonObject();
			boolean online = false;
			long last = 0;
			for (Entry<Long, Long> e : entry.getValue().entrySet()) {
				long end = e.getValue();
				if (end == -1) {
					if (online) {
						PlayerTracker.getLogger()
								.warning(String.format("More than 1 entry had a value of -1 for player " + rawName));
						PlayerTracker.getLogger().warning(String.format("%d: %d", e.getKey(), e.getValue()));
					}
					online = true;
					end = System.currentTimeMillis();
				}

				if (e.getKey() < last)
					PlayerTracker.getLogger().warning(String.format("Player data of %s is unordered", rawName));

				times.addProperty(e.getKey().toString(), end);
			}
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

	public Map<String, LinkedHashMap<Long, Long>> getTimes() {
		return times;
	}

	/**
	 * Marks the player as having logged on to a server
	 * 
	 * @param server
	 */
	public void logOn(ServerData server) {
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server.getName(), new LinkedHashMap<Long, Long>());
		if (!times.isEmpty()) {
			List<Entry<Long, Long>> sessions = new ArrayList<>();

			times.entrySet().forEach(e -> sessions.add(e));
//			sessions.sort(new Comparator<Entry<Long, Long>>() {
//				@Override
//				public int compare(Entry<Long, Long> o1, Entry<Long, Long> o2) {
//					return o1.getKey() == o2.getKey() ? 0 : o1.getKey() > o2.getKey() ? -1 : 1;
//				}
//			});
			times.clear();
			for (Entry<Long, Long> entry : sessions)
				times.put(entry.getKey(), entry.getValue());

			Entry<Long, Long> entry = sessions.get(sessions.size() - 1);

			if (entry.getValue() == -1) {
				// Player is already logged on
				return;
			}
		}

		times.put(System.currentTimeMillis(), -1L);
		this.times.put(server.getName(), times);
	}

	/**
	 * Marks the player as having logged off of the specified server
	 * 
	 * @param server
	 */
	public void logOff(ServerData server) {
		if (this.times.getOrDefault(server.getName(), new LinkedHashMap<>()).isEmpty()) {
			PlayerTracker.getLogger().warning(String.format(
					"Desynchronization of player tracking, attempted to logOff %s when not logged on", rawName + ""));
			PlayerTracker.getLogger().warning("Error type: empty");
			return;
		}
		List<Entry<Long, Long>> sessions = new ArrayList<>();
		LinkedHashMap<Long, Long> times = this.times.getOrDefault(server.getName(), new LinkedHashMap<Long, Long>());

		times.entrySet().forEach(e -> sessions.add(e));

		boolean online = false;
		for (Entry<Long, Long> entry : sessions) {
			if (entry.getValue() == -1) {
				if (online) {
					PlayerTracker.getLogger()
							.warning("Attempted to log off a player that should have already been logged off");
					PlayerTracker.getLogger().warning(String.format("%d: %d", entry.getKey(), entry.getValue()));
					entry.setValue(System.currentTimeMillis());
				}
				online = true;
			}
		}

		Entry<Long, Long> entry = sessions.get(sessions.size() - 1);

		if (entry.getValue() != -1) {
			PlayerTracker.getLogger()
					.warning(String.format(
							"[WARNING] Desynchronization of player tracking, attempted to logOff %s when not logged on",
							rawName));
			PlayerTracker.getLogger().warning("Error type: -1");
			PlayerTracker.getLogger().warning("Actual value: " + entry.getValue() + " key: " + entry.getKey()
					+ " (time ago: " + (System.currentTimeMillis() - entry.getKey()) + ")");
			PlayerTracker.getLogger().warning(String.format("0 index: %d", sessions.get(0).getValue()));
			for (Entry<Long, Long> v : sessions) {
				PlayerTracker.getLogger().warning(String.format("%d: %d", v.getKey(), v.getValue()));
			}
			sessions.forEach(
					(k) -> PlayerTracker.getLogger().warning(String.format("> %d: %d", k.getKey(), k.getValue())));
			return;
		}

		times.put(entry.getKey(), System.currentTimeMillis());
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

	/**
	 * Gets the server that the player first played on, null if they've never logged
	 * on
	 * 
	 * @return
	 */
	public String getFirstPlayedName() {
		String firstName = null;
		long first = Long.MAX_VALUE;
		for (String s : times.keySet()) {
			long firstPlayed = getFirstPlayed(s);
			if (firstPlayed < first) {
				firstName = s;
				first = firstPlayed;
			}
		}
		return firstName;
	}

	/**
	 * Gets when the player first played on the specified server, returns
	 * {@link Long#MAX_VALUE} if they've never logged on
	 * 
	 * @param server
	 * @return
	 */
	public long getFirstPlayed(String server) {
		LinkedHashMap<Long, Long> t = times.getOrDefault(server == null ? getFirstPlayedName() : server,
				new LinkedHashMap<>());
		if (t.isEmpty())
			return Long.MAX_VALUE;
		List<Entry<Long, Long>> ts = new ArrayList<>(t.entrySet());

		return ts.get(0).getKey();
	}

	/**
	 * Gets when the player first played, returns {@link Long#MAX_VALUE} if they've
	 * never logged on
	 * 
	 * @param server
	 * @return
	 */
	public long getFirstPlayed() {
		return getFirstPlayed(null);
	}

	/**
	 * Gets the server that the player last played on, null if they've never played
	 * 
	 * @return
	 */
	public String getLastPlayedName() {
		String lastName = null;
		long last = 0;
		for (String s : times.keySet()) {
			long lastPlayed = getLastPlayed(s);
			if (lastPlayed == -1) {
				return s;
			}
			if (lastPlayed > last) {
				lastName = s;
				last = lastPlayed;
			}
		}
		return lastName;

	}

	/**
	 * Gets when the player last played on the specified server
	 * 
	 * @param server
	 * @return
	 */
	public long getLastPlayed(String server) {
		LinkedHashMap<Long, Long> t = times.getOrDefault(server == null ? getFirstPlayedName() : server,
				new LinkedHashMap<>());
		if (t.isEmpty())
			return Long.MAX_VALUE;
		List<Entry<Long, Long>> ts = new ArrayList<>(t.entrySet());

		return ts.get(ts.size() - 1).getValue();
	}

	/**
	 * Gets when the player last played
	 * 
	 * @param server
	 * @return
	 */
	public long getLastPlayed() {
		return getLastPlayed(null);
	}

	public long getTotalPlaytime() {
		return getTotalPlaytime(null);
	}

	public long getTotalPlaytime(String server) {
		return getPlaytimeSince(0, server);
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
			long s = entry.getKey(), e = entry.getValue() == -1 ? System.currentTimeMillis() : entry.getValue();

			if (s > end) // We've gone past the max limit specified by end
				break;
			if (e < start)
				continue;

			if (s < start)
				s = start;
			if (e > end)
				e = end;

			result += e - s;
		}
		return result;
	}

}

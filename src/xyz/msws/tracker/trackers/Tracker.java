package xyz.msws.tracker.trackers;

import java.util.TimerTask;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;

public abstract class Tracker extends TimerTask {

	protected final ServerData server;
	protected SourceServer connection;
	protected final Client client;

	public Tracker(Client client, ServerData server) {
		this.server = server;
		this.client = client;
		try {
			this.connection = new SourceServer(server.getIp(), server.getPort());
		} catch (SteamCondenserException e) {
			e.printStackTrace();
		}
	}

	public ServerData getServer() {
		return server;
	}

	public SourceServer getConnection() {
		return connection;
	}

}

package xyz.msws.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import xyz.msws.tracker.commands.PlaytimeCommand;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.trackers.CSGOTracker;
import xyz.msws.tracker.trackers.Tracker;

/**
 * Tracks players on source servers
 * 
 * @author Isaac
 *
 */
public class PlayerTracker extends Client {

	private TrackerConfig config;
	public static final File PLAYER_FILE = new File("players"), SERVER_FILE = new File("servers");
	private IEventManager events;

	public PlayerTracker(String token) {
		super(token);
		this.config = new TrackerConfig(new File("config.txt"));
	}

	@Override
	public void start() {
		try {
			this.jda = JDABuilder.createDefault(token).build();

			events = new AnnotatedEventManager();
			jda.setEventManager(events);
			jda.addEventListener(commands);

			startTimers();
			jda.awaitReady();
			loadModules();

			commands.registerCommand(new PlaytimeCommand(this, "playtime"));

			jda.getPresence().setActivity(Activity.watching("CS:GO Servers"));
			MessageAction.setDefaultMentions(new ArrayList<>()); // Prevent the bot from messaging others (prevents
																	// abuse of @everyone, @here, etc.)
		} catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void startTimers() {
		Timer timer = new Timer();

		List<ServerData> data = config.getServers();

		modules.add(new PlayerTrackerModule(this, data));

		for (ServerData d : data) {
			Tracker t = new CSGOTracker(this, d);
			timer.schedule(t, 0, 1000 * 20);
		}

		// Save player data
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				getModule(PlayerTrackerModule.class).save();
				data.forEach(ServerData::saveData);
			}
		}, 10 * 1000, 1000 * 60 * 5);
	}

	public TrackerConfig getConfig() {
		return config;
	}

	@Override
	public String getName() {
		return "PlayerTracker";
	}

	@Override
	public String getPrefix() {
		return "?";
	}

}

package xyz.msws.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import xyz.msws.tracker.commands.DeletePlayerCommand;
import xyz.msws.tracker.commands.HelpCommand;
import xyz.msws.tracker.commands.LogsCommand;
import xyz.msws.tracker.commands.PlaytimeCommand;
import xyz.msws.tracker.commands.StatisticsCommand;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.trackers.CSGOTracker;
import xyz.msws.tracker.trackers.Tracker;
import xyz.msws.tracker.utils.Logger;

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
			Logger.log("Setting up events...");
			Enumeration<String> it = LogManager.getLogManager().getLoggerNames();

			while (it.hasMoreElements()) {
				String s = it.nextElement();
				java.util.logging.Logger log = java.util.logging.Logger.getLogger(s);
				log.setLevel(Level.SEVERE);
			}
			jda.setEventManager(events);
			jda.addEventListener(commands);

			Logger.log("Starting timers...");
			startTimers();
			jda.awaitReady();

			Logger.log("Registering commands");
			commands.registerCommand(new PlaytimeCommand(this, "playtime"));
			commands.registerCommand(new LogsCommand(this, "logs"));
			commands.registerCommand(new StatisticsCommand(this, "statistics"));
			commands.registerCommand(new HelpCommand(this, "help"));
			commands.registerCommand(new DeletePlayerCommand(this, "deleteplayer"));
			Logger.logf("Successfully registered %d command%s", commands.getCommands().size(),
					commands.getCommands().size() == 1 ? "" : "s");

			Logger.log("Loading modules...");
			loadModules();

			jda.getPresence().setActivity(Activity.watching("CS:GO Servers"));
			MessageAction.setDefaultMentions(new ArrayList<>()); // Prevent the bot from messaging others (prevents
																	// abuse of @everyone, @here, etc.)
		} catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void startTimers() {
		Timer timer = new Timer();

		List<ServerData> data = new ArrayList<>();
		List<Tracker> trackers = new ArrayList<>();
		data.addAll(config.getServers().keySet());

		modules.add(new PlayerTrackerModule(this, data));

		for (ServerData d : data) {
			Tracker t = new CSGOTracker(this, d);
			timer.schedule(t, 0, 1000 * 20);
			trackers.add(t);
		}

		// Save player data
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				data.forEach(ServerData::saveData);
				trackers.forEach(Tracker::save);
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

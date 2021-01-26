package xyz.msws.tracker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import xyz.msws.tracker.commands.AddServerCommand;
import xyz.msws.tracker.commands.DeletePlayerCommand;
import xyz.msws.tracker.commands.DeleteServerCommand;
import xyz.msws.tracker.commands.HelpCommand;
import xyz.msws.tracker.commands.LogsCommand;
import xyz.msws.tracker.commands.PlaytimeCommand;
import xyz.msws.tracker.commands.StatisticsCommand;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.trackers.CSGOTracker;
import xyz.msws.tracker.trackers.Tracker;
import xyz.msws.tracker.utils.MSG;

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
		logger = Logger.getLogger(PlayerTracker.class.getName());
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy h:m:s.S");

		logger.addHandler(new Handler() {
			@Override
			public void publish(LogRecord record) {
				logs.add(format.format(System.currentTimeMillis()) + " " + record.getLevel().toString() + " "
						+ record.getMessage());
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() throws SecurityException {
			}
		});
		logger.setLevel(Level.SEVERE);
	}

	@Override
	public void start() {
		try {
			this.jda = JDABuilder.createDefault(token).build();
			jda.awaitReady();
		} catch (LoginException | InterruptedException e) {
			Client.getLogger().severe(MSG.toString(e));
			e.printStackTrace();
		}
		jda.getPresence().setActivity(Activity.watching("CS:GO Servers"));

		events = new AnnotatedEventManager();

		logger.info("Setting up events...");
		jda.setEventManager(events);
		jda.addEventListener(commands);

		startTimers();

		registerCommands();

		logger.info("Loading modules...");
		loadModules();

		// Prevent the bot from messaging others (prevents abuse of @everyone, @here,
		// etc.)
		MessageAction.setDefaultMentions(new ArrayList<>());

	}

	private void registerCommands() {
		logger.info("Registering commands...");
		commands.registerCommand(new PlaytimeCommand(this, "playtime"));
		commands.registerCommand(new LogsCommand(this, "logs"));
		commands.registerCommand(new StatisticsCommand(this, "statistics"));
		commands.registerCommand(new HelpCommand(this, "help"));
		commands.registerCommand(new DeletePlayerCommand(this, "deleteplayer"));
		commands.registerCommand(new AddServerCommand(this, "addserver"));
		commands.registerCommand(new DeleteServerCommand(this, "deleteserver"));
		logger.info("Successfully registered " + commands.getCommands().size() + " commands");
	}

	private void startTimers() {
		logger.info("Starting timers...");
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

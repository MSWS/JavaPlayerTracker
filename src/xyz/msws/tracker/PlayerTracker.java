package xyz.msws.tracker;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import xyz.msws.tracker.commands.*;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.trackers.CSGOTracker;
import xyz.msws.tracker.trackers.Tracker;
import xyz.msws.tracker.utils.Logger;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Tracks players on source servers
 *
 * @author Isaac
 */
public class PlayerTracker extends Client {

    private final TrackerConfig config;

    public PlayerTracker(String token) {
        super(token);
        this.config = new TrackerConfig(new File("config.txt"));
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy h:m:s.S");

        Enumeration<String> it = LogManager.getLogManager().getLoggerNames();

        while (it.hasMoreElements()) {
            String s = it.nextElement();
            java.util.logging.Logger log = java.util.logging.Logger.getLogger(s);
            log.setLevel(Level.SEVERE);
        }
    }

    @Override
    public void start() {
        try {
            this.jda = JDABuilder.createDefault(token).build();
            jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            Logger.log(e);
            e.printStackTrace();
        }
        jda.getPresence().setActivity(Activity.watching("CS:GO Servers"));

        IEventManager events = new AnnotatedEventManager();

        Logger.log("Setting up events...");
        jda.setEventManager(events);
        jda.addEventListener(commands);

        startTimers();

        registerCommands();

        Logger.log("Loading modules...");
        loadModules();

        // Prevent the bot from messaging others (prevents abuse of @everyone, @here,
        // etc.)
        MessageAction.setDefaultMentions(new ArrayList<>());

    }

    private void registerCommands() {
        Logger.log("Registering commands...");
        commands.registerCommand(new LogsCommand(this, "logs"));
        commands.registerCommand(new StatisticsCommand(this, "statistics"));
        commands.registerCommand(new HelpCommand(this, "help"));
        commands.registerCommand(new AddServerCommand(this, "addserver"));
        commands.registerCommand(new DeleteServerCommand(this, "deleteserver"));
        Logger.log("Successfully registered " + commands.getCommands().size() + " commands");
    }

    private void startTimers() {
        Logger.log("Starting timers...");
        Timer timer = new Timer();

        List<ServerData> data = new ArrayList<>(config.getServers().keySet());

        modules.add(new PlayerTrackerModule(this, data));

        for (ServerData d : data) {
            Tracker t = new CSGOTracker(this, d);
            timer.schedule(t, 0, 1000 * 20);
        }
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

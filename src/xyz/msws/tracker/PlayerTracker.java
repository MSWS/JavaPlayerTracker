package xyz.msws.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.trackers.CSGOTracker;
import xyz.msws.tracker.trackers.Tracker;

public class PlayerTracker extends Client {

	public PlayerTracker(String token) {
		super(token);
	}

	@Override
	public void start() {
		try {
			this.jda = JDABuilder.createDefault(token).build();

			Timer timer = new Timer();

			List<ServerData> data = new ArrayList<ServerData>();
			data.add(new ServerData("eGO JB", "74.91.113.83"));
			data.add(new ServerData("eGO TTT", "74.91.113.113"));

			modules.add(new PlayerTrackerModule(this, data));

			for (ServerData d : data) {
				d.loadData();
				Tracker t = new CSGOTracker(this, d);
				timer.schedule(t, 0, 1000 * 20);
			}

			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					getModule(PlayerTrackerModule.class).save();
					data.forEach(ServerData::saveData);
				}
			}, 10 * 1000, 1000 * 60);

			jda.awaitReady();
			loadModules();

			jda.addEventListener(commands);
			jda.getPresence().setActivity(Activity.watching("CS:GO Servers"));
			MessageAction.setDefaultMentions(new ArrayList<>());

		} catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public String getName() {
		return "PlayerTracker";
	}

	@Override
	public String getPrefix() {
		return ".";
	}

}

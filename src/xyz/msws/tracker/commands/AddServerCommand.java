package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class AddServerCommand extends AbstractCommand {

	private TrackerConfig config;
	private PlayerTrackerModule tracker;

	public AddServerCommand(Client client, String name) {
		super(client, name);
		setAliases("as");
		setDescription("Adds a server to be tracked");
		setPermission(Permission.ADMINISTRATOR);
		setUsage("[server]:[port] [name] [channel]");

		if (client instanceof PlayerTracker) {
			config = ((PlayerTracker) client).getConfig();
		}
		tracker = client.getModule(PlayerTrackerModule.class);
	}

	@Override
	public void execute(Message message, String[] args) {
		if (args.length < 3) {
			message.getChannel().sendMessage("Invalid usage, please check proper format.").queue();
			return;
		}
		String server = args[0];
		String name = "";
		for (int i = 1; i < args.length - 1; i++)
			name += args[i] + " ";
		name = name.trim();
		String channel = args[2];

		if (message.getGuild().getTextChannelsByName(channel, true) == null) {
			message.getChannel().sendMessage("Unknown channel: " + channel).queue();
			return;
		}
		
		if(tracker.getServerNames().contains(name)) {
			message.getChannel().sendMessage("That server name is already taken.").queue();
			return;
		}

		ServerData data = new ServerData(name, server);

		config.addServer(data, channel);
		config.save();
		tracker.addServer(data);

		message.getChannel().sendMessage("Added server: " + name);
	}

}

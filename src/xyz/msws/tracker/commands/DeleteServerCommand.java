package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.data.wrappers.Confirmation;
import xyz.msws.tracker.data.wrappers.Selector;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class DeleteServerCommand extends AbstractCommand {
	private TrackerConfig config;
	private final PlayerTrackerModule tracker;

	public DeleteServerCommand(Client client, String name) {
		super(client, name);
		setPermission(Permission.ADMINISTRATOR);
		setAliases("ds", "delsrv");
		setUsage("[server]");
		if (client instanceof PlayerTracker) {
			config = ((PlayerTracker) client).getConfig();
		}
		tracker = client.getModule(PlayerTrackerModule.class);
	}

	@Override
	public void execute(Message message, String[] args) {
		if (args.length == 0) {
			message.getChannel().sendMessage("Please specify a server name").queue();
			return;
		}

		String n = String.join(" ", args);

		Selector<String> servers = new Selector<>(tracker.getServerNames());
		servers.filter(s -> s.toLowerCase().contains(n.toLowerCase()));
		servers.sortLexi(n);
		servers.setAction(new Callback<String>() {

			@Override
			public void execute(String call) {
				if (call == null) {
					message.getChannel().sendMessage("Unknown server.").queue();
					return;
				}
				Confirmation conf = new Confirmation(client, "Are you sure you want to delete the server data of " + n);
				conf.confirm(new Callback<GuildMessageReactionAddEvent>() {
					@Override
					public void execute(GuildMessageReactionAddEvent react) {
						tracker.deleteServer(call);
						config.removeServer(tracker.getServer(call));
						config.save();
						react.getChannel().sendMessage("Successfully deleted server data of " + call).queue();
						react.retrieveMessage().queue(Message::delete);
					}
				});
				conf.send(message);
			}
		});
		servers.send(client, message);
	}

}

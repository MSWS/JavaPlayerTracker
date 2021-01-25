package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.data.pageable.Pageable;
import xyz.msws.tracker.data.pageable.PageableText;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class DeleteServerCommand extends AbstractCommand {
	private TrackerConfig config;
	private PlayerTrackerModule tracker;

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

		if (!tracker.getServerNames().contains(n)) {
			message.getChannel().sendMessage("Unknown server.").queue();
			return;
		}

		Pageable<?> pager = new PageableText(client, "Are you sure you want to delete the server data of " + n);
		pager.addCallback("✅", confirm(n));
		pager.addCallback("❌", cancel());
		pager.bindTo(message.getAuthor());
		pager.send(message.getTextChannel());
	}

	private Callback<GuildMessageReactionAddEvent> confirm(String server) {
		return new Callback<GuildMessageReactionAddEvent>() {

			@Override
			public void execute(GuildMessageReactionAddEvent call) {
				tracker.deleteServer(server);
				config.removeServer(tracker.getServer(server));
				config.save();
				call.getChannel().sendMessage("Successfully deleted server data of " + server).queue();
				call.retrieveMessage().queue(m -> m.delete());
			}
		};
	}

	private Callback<GuildMessageReactionAddEvent> cancel() {
		return new Callback<GuildMessageReactionAddEvent>() {
			@Override
			public void execute(GuildMessageReactionAddEvent call) {
				call.retrieveMessage().queue(m -> m.delete());
			}
		};
	}

}

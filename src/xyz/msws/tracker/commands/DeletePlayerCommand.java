package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.data.pageable.Confirmation;
import xyz.msws.tracker.data.pageable.Selector;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.MSG;

public class DeletePlayerCommand extends AbstractCommand {
	private PlayerTrackerModule tracker;

	public DeletePlayerCommand(Client client, String name) {
		super(client, name);
		setAliases("dp");
		setPermission(Permission.ADMINISTRATOR);
		setDescription("Deletes specified player data");
		tracker = client.getModule(PlayerTrackerModule.class);
	}

	@Override
	public void execute(Message message, String[] args) {
		if (tracker == null) {
			message.getChannel().sendMessage("PlayerTracker is not enabled").queue();
			return;
		}
		if (args.length == 0) {
			message.getChannel().sendMessage("Please specify a player.").queue();
			return;
		}

		Callback<String> call = new Callback<String>() {
			@Override
			public void execute(String call) {
				if (call == null)
					return;

				Confirmation conf = new Confirmation(client,
						call.equals("all") ? "Do you really want to delete **ALL** player data?"
								: "Are you sure you want to delete **" + call + "**'s data?");
				conf.confirm(new Callback<GuildMessageReactionAddEvent>() {

					@Override
					public void execute(GuildMessageReactionAddEvent c) {
						c.retrieveMessage().queue(m -> m.delete().queue());
						if (call.equalsIgnoreCase("all")) {
							tracker.deleteAllData();
							c.getChannel().sendMessage("Successfully deleted all player data.").queue();
							return;
						}
						tracker.getPlayer(call).delete();
						tracker.deletePlayer(call);
						c.getChannel().sendMessage("Successfully deleted " + call + "'s data.").queue();
					}
				});
			}
		};

		if (args[0].equalsIgnoreCase("all")) {
			call.execute("all");
			return;
		}
		Selector<String> ps = new Selector<>(tracker.getPlayerNames());
		String term = String.join(" ", args);
		ps.filter(s -> {
			String ss = MSG.simplify(s.replace(" ", ""));
			return (s.equals(term) || ss.contains(term) || term.contains(ss));
		});
		ps.sortLexi(term);
		ps.setAction(call);
		ps.send(client, message);

	}
}

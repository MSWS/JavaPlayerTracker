package xyz.msws.tracker.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.data.pageable.Pageable;
import xyz.msws.tracker.data.pageable.PageableText;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.MSG;

public class DeletePlayerCommand extends AbstractCommand {

	public DeletePlayerCommand(Client client, String name) {
		super(client, name);
		setAliases("dp");
		setPermission(Permission.ADMINISTRATOR);
		tracker = client.getModule(PlayerTrackerModule.class);
	}

	private PlayerTrackerModule tracker;

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

		if (args[0].equalsIgnoreCase("all")) {
			delete(args[0], message);
			return;
		}

		String given = String.join(" ", args), simp = MSG.simplify(given.replace(" ", ""));
		List<String> results = new ArrayList<>();
		for (String s : tracker.getPlayerNames()) {
			String ss = MSG.simplify(s.replace(" ", ""));
			if (s.equals(given)) {
				delete(s, message);
				break;
			}
			if (ss.contains(simp) || simp.contains(ss))
				results.add(s);
		}
		if (results.size() == 1) {
			delete(results.get(0), message);
			return;
		}
		if (results.isEmpty()) {
			message.getChannel().sendMessage("No players matched your search").queue();
			return;
		}

		List<String> messages = new ArrayList<>();

		int size = 5;

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < results.size(); i++) {
			builder.append((i % size) + 1).append(": ").append(results.get(i)).append("\n");
			if ((i + 1) % size == 0) {
				messages.add(builder.toString());
				builder = new StringBuilder();
			}
		}
		if (builder.length() != 0)
			messages.add(builder.toString());

		messages.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});

		String[] nums = new String[] { "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣" };

		Pageable<?> pager = new PageableText(client, messages).bindTo(message.getAuthor());

		for (int i = 0; i < Math.min(nums.length, results.size()); i++) {
			final int fi = i;
			pager.addCallback(nums[i], new Callback<GuildMessageReactionAddEvent>() {
				@Override
				public void execute(GuildMessageReactionAddEvent call) {
					delete(results.get(fi + pager.getPage() * size), message);
				}
			});
		}
		pager.send(message.getTextChannel());
	}

	private void delete(String player, Message message) {
		Pageable<?> pager = new PageableText(client,
				player.equalsIgnoreCase("all") ? "Do you really want to delete **ALL** player data?"
						: "Are you sure you want to delete **" + player + "**'s data?").bindTo(message.getAuthor());
		pager.addCallback("✅ ", confirm(player));
		pager.addCallback("❌", cancel());
		pager.send(message.getTextChannel());
	}

	private Callback<GuildMessageReactionAddEvent> confirm(String player) {
		return new Callback<GuildMessageReactionAddEvent>() {
			@Override
			public void execute(GuildMessageReactionAddEvent call) {
				if (player.equalsIgnoreCase("all")) {
					tracker.deleteAllData();
					return;
				}
				tracker.getPlayer(player).delete();
				call.getChannel().sendMessage("Successfully deleted " + player + "'s data.");
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

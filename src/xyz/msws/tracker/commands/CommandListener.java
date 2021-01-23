package xyz.msws.tracker.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.utils.Logger;

public class CommandListener {

	private List<AbstractCommand> commands = new ArrayList<>();
	private Client client;

	public CommandListener(Client client) {
		this.client = client;
	}

	public boolean registerCommand(AbstractCommand command) {
		return commands.add(command);
	}

	public boolean unregisterCommand(AbstractCommand command) {
		return commands.remove(command);
	}

	public boolean isCommandRegistered(AbstractCommand command) {
		return commands.contains(command);
	}

	public AbstractCommand getCommand(String name) {
		return commands.stream().filter(cmd -> cmd.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	public List<AbstractCommand> getCommands() {
		return commands;
	}

	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().getIdLong() == client.getJDA().getSelfUser().getIdLong())
			return;
		Message message = event.getMessage();

		if (!message.getContentDisplay().toLowerCase().startsWith(client.getPrefix().toLowerCase())
				&& !message.getContentDisplay().startsWith("@" + client.getJDA().getSelfUser().getName()))
			return;

		String msg = message.getContentDisplay()
				.substring(message.getContentDisplay().toLowerCase().startsWith(client.getPrefix().toLowerCase())
						? client.getPrefix().length()
						: client.getJDA().getSelfUser().getName().length() + 2);

		AbstractCommand cmd = null;

		for (AbstractCommand c : commands) {
			if (c.getName().equalsIgnoreCase(msg.split(" ")[0])
					|| c.getAliases().contains(msg.split(" ")[0].toLowerCase())) {
				if (!c.checkPermission(message))
					break;
				cmd = c;
				break;
			}
		}
		if (cmd == null)
			return;

		boolean running = true;

		final AbstractCommand fCmd = cmd;
		Timer timer = new Timer();

		new Thread(() -> {
			Logger.logf("%s sent command: %s, executing...", event.getAuthor().getName(), message.getContentRaw());
			fCmd.execute(message, msg.contains(" ") ? msg.substring(msg.indexOf(" ") + 1).split(" ") : new String[0]);
			Logger.log("Finished command execution");
			timer.cancel();
		}).start();

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (!running) {
					this.cancel();
					return;
				}
				message.getTextChannel().sendTyping().queue();
			}
		}, 0, 8000);

	}
}

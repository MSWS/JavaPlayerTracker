package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.pageable.PageableEmbed;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HelpCommand extends AbstractCommand {

	public HelpCommand(Client client, String name) {
		super(client, name);
		setAliases("pleasehelpmeiamgoingtoexplode", "commands");
		setUsage("");
		setDescription("Views available commands and their syntaxes");
	}

	@Override
	public void execute(Message message, String[] args) {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Available Commands");
		builder.setColor(Color.YELLOW);

		List<MessageEmbed> embeds = new ArrayList<>();

		int i = 1;
		for (AbstractCommand cmd : client.getCommandListener().getCommands()) {
			if (cmd.getPermission() != null) {
				if (!cmd.checkPermission(message, false))
					continue;
				builder.appendDescription("(_" + cmd.getPermission().getName() + "_) **" + client.getPrefix()
						+ cmd.getName() + "** " + cmd.getUsage() + " - " + cmd.getDescription() + "\n");
				continue;
			}

			builder.appendDescription("**" + client.getPrefix() + cmd.getName() + "** " + cmd.getUsage() + " - "
					+ cmd.getDescription() + "\n");
			if (i % 6 == 0) {
				embeds.add(builder.build());
				builder = new EmbedBuilder();
				builder.setColor(Color.YELLOW);
			}
			i++;
		}
		if (builder.getDescriptionBuilder().length() > 0)
			embeds.add(builder.build());

		new PageableEmbed(client, embeds).bindTo(message.getAuthor()).send(message.getTextChannel());
	}

}

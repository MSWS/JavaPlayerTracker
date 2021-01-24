package xyz.msws.tracker.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.utils.Logger;

public class LogsCommand extends AbstractCommand {

	public LogsCommand(Client client, String name) {
		super(client, name);
	}

	@Override
	public void execute(Message message, String[] args) {
		File file = new File("output.log");
		List<String> out = new ArrayList<>();
		for (int i = Logger.getLogs().size() - 1; i >= 0; i--)
			out.add(Logger.getLogs().get(i));
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(String.join("\n", Logger.getLogs()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			message.getChannel().sendMessage("```\n" + String.join("\n", out) + "```").queue();
			message.getChannel().sendFile(file, file.getName()).queue();
		} catch (IllegalArgumentException e) {
			message.getChannel().sendMessage("Logs are too big...").queue();
		}
	}

}

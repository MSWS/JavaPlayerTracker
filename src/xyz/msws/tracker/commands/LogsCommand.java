package xyz.msws.tracker.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(String.join("\n", Logger.getLogs()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			message.getChannel().sendFile(file, file.getName()).queue();
		} catch (IllegalArgumentException e) {
			message.getChannel().sendMessage("Logs are too big...").queue();
		}
	}

}

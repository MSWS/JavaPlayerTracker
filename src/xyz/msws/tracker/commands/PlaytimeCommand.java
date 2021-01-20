package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class PlaytimeCommand extends AbstractCommand {

	public PlaytimeCommand(Client client, String name) {
		super(client, name);
	}

	
	
	@Override
	public void execute(Message message, String[] args) {
//		client.getModule(PlayerTrackerModule.class).getPlayer();
	}

}

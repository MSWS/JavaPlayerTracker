package xyz.msws.tracker.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;

/**
 * Represents a command that can be run, @see {@link CommandListener} for how
 * the execute method is run.
 * 
 * @author imodm
 *
 */
public abstract class AbstractCommand {

	protected Client client;
	protected String name;
	protected Permission perm;
	protected String usage = "<args>";
	protected String description = "";

	protected List<String> aliases = new ArrayList<>();

	public AbstractCommand(Client client, String name) {
		this.client = client;
		this.name = name;
	}

	public abstract void execute(Message message, String[] args);

	public boolean checkPermission(Message message) {
		return checkPermission(message, true);
	}

	public boolean checkPermission(Message message, boolean verbose) {
		if (perm == null)
			return true;
		if (!message.getMember().hasPermission(perm)) {
			if (verbose) {
				message.getChannel()
						.sendMessageFormat("The **%s** command requires the **%s** permission.", name, perm.getName())
						.queue();
			}
			return false;
		}
		return true;
	}

	public String getName() {
		return name;
	}

	public String getUsage() {
		return this.usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	public void setAliases(String... strings) {
		this.aliases = Arrays.asList(strings);
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getAliases() {
		return aliases;
	}

	public void setPermission(Permission permission) {
		this.perm = permission;
	}

	public Permission getPermission() {
		return perm;
	}

}

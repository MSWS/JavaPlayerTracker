package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.PlayerTracker;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.TrackerConfig;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.Logger;

public class AddServerCommand extends AbstractCommand {

    private final PlayerTrackerModule tracker;
    private TrackerConfig config;

    public AddServerCommand(Client client, String name) {
        super(client, name);
        setAliases("as");
        setDescription("Adds a server to be tracked");
        setPermission(Permission.ADMINISTRATOR);
        setUsage("[server]:[port] [name] [channel]");

        if (client instanceof PlayerTracker)
            config = ((PlayerTracker) client).getConfig();

        tracker = client.getModule(PlayerTrackerModule.class);
        if (tracker == null)
            client.getCommandListener().unregisterCommand(this);
    }

    @Override
    public void execute(Message message, String[] args) {
        if (args.length < 3) {
            message.getChannel().sendMessage("Invalid usage, please check proper format.").queue();
            return;
        }
        if (config == null) {
            message.getChannel().sendMessage("Tracker config is disabled.").queue();
            return;
        }
        String server = args[0];
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length - 1; i++)
            name.append(args[i]).append(" ");
        name = new StringBuilder(name.toString().trim());
        String channel = args[2];

        if (message.getGuild().getTextChannelsByName(channel, true).isEmpty()) {
            message.getChannel().sendMessage("Unknown channel: `" + channel + "`").queue();
            return;
        }

        if (tracker.getServerNames().contains(name.toString())) {
            message.getChannel().sendMessage("That server name is already taken.").queue();
            return;
        }

        ServerData data = new ServerData(name.toString(), server);

        config.addServer(data, channel);
        config.save();
        tracker.addServer(data);

        Logger.log(message.getAuthor().getAsTag() + " added a server");

        message.getChannel()
                .sendMessage("Successfully added the server " + name + "\nIP: " + server + "\nChannel: " + channel)
                .queue();
    }

}

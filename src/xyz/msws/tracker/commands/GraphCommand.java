package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.graph.GlobalGraph;
import xyz.msws.tracker.module.PlayerTrackerModule;

import java.io.File;

public class GraphCommand extends AbstractCommand {
    private final PlayerTrackerModule tracker;

    public GraphCommand(Client client, String name) {
        super(client, name);
        setAliases("g");
        setUsage("[server/player]");
        setDescription("Graphs playtime");
        tracker = client.getModule(PlayerTrackerModule.class);
    }

    @Override
    public void execute(Message message, String[] args) {
        sendServerStats(message, String.join(" ", args));
    }

    private boolean sendServerStats(Message message, String name) {
        ServerData server = tracker.findServer(name);
        if (server == null)
            return false;

        File file = new GlobalGraph(tracker).generate();
        if (file == null || !file.exists()) {
            message.getChannel().sendMessage("Unable to create graph").queue();
            return true;
        }

        message.getChannel().sendFile(file, "graph.png").queue();
        return true;
    }
}

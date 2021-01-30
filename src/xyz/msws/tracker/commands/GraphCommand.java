package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.graph.GlobalGraph;
import xyz.msws.tracker.data.graph.ServerGraph;
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
        if (args.length == 0) {
            File file = new GlobalGraph(tracker).generate();
            if (file == null || !file.exists()) {
                message.getChannel().sendMessage("Unable to create graph").queue();
                return;
            }

            message.getChannel().sendFile(file, "graph.png").queue();
            return;
        }
        String name = String.join(" ", args);

        ServerData server = tracker.findServer(name);
        if (server == null) {
            message.getChannel().sendMessage("Unknown server.").queue();
            return;
        }

        File file = new ServerGraph(tracker, server).generate();
        message.getChannel().sendFile(file, "graph.png").queue();
    }
}

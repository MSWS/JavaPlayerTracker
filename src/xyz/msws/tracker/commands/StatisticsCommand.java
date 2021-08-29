package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.TimeParser;

import java.awt.*;

public class StatisticsCommand extends AbstractCommand {
    private final PlayerTrackerModule tracker;
    private final long start = System.currentTimeMillis();

    public StatisticsCommand(Client client, String name) {
        super(client, name);
        tracker = client.getModule(PlayerTrackerModule.class);
        setAliases("stats");
        setDescription("Views bot, player, or server statistics");
        setUsage("<server/player>");
    }

    @Override
    public void execute(Message message, String[] args) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Bot Statistics");
        builder.setColor(Color.GREEN);

        builder.addField("Servers", tracker.getServers().size() + "", true);
        builder.addField("Uptime", TimeParser.getDurationDescription((System.currentTimeMillis() - start) / 1000),
                true);
        builder.addField("Ping", client.getJDA().getGatewayPing() + "", true);

        message.getChannel().sendMessage(builder.build()).queue();
    }
}

package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.data.Callback;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.data.wrappers.Confirmation;
import xyz.msws.tracker.data.wrappers.PlayerSelector;
import xyz.msws.tracker.module.PlayerTrackerModule;

public class DeletePlayerCommand extends AbstractCommand {
    private final PlayerTrackerModule tracker;

    public DeletePlayerCommand(Client client, String name) {
        super(client, name);
        setAliases("dp");
        setPermission(Permission.ADMINISTRATOR);
        setDescription("Deletes specified player data");
        tracker = client.getModule(PlayerTrackerModule.class);
    }

    @Override
    public void execute(Message message, String[] args) {
        if (tracker == null) {
            message.getChannel().sendMessage("PlayerTracker is not enabled").queue();
            return;
        }
        if (args.length == 0) {
            message.getChannel().sendMessage("Please specify a player.").queue();
            return;
        }

        if (args[0].equalsIgnoreCase("all")) {
            Confirmation conf = new Confirmation(client, "Are you sure you want to delete **ALL** player data?");
            conf.confirm(new Callback<GuildMessageReactionAddEvent>() {
                @Override
                public void execute(GuildMessageReactionAddEvent c) {
                    tracker.deleteAllData();
                    c.getChannel().sendMessage("Successfully deleted all player data.").queue();
                }
            });
            conf.send(message);
            return;
        }

        Callback<ServerPlayer> call = new Callback<ServerPlayer>() {
            @Override
            public void execute(ServerPlayer call) {
                if (call == null)
                    return;

                Confirmation conf = new Confirmation(client, "Are you sure you want to delete **" + call.getRawName() + "**'s data?");
                conf.confirm(new Callback<GuildMessageReactionAddEvent>() {
                    @Override
                    public void execute(GuildMessageReactionAddEvent c) {
                        c.retrieveMessage().queue(m -> m.delete().queue());
                        call.delete();
                        tracker.deletePlayer(call.getRawName());
                        c.getChannel().sendMessage("Successfully deleted " + call + "'s data.").queue();
                    }
                });
                conf.send(message);
            }
        };
        String term = String.join(" ", args);
        PlayerSelector ps = new PlayerSelector(client, term);
        ps.setAction(call);
        ps.send(client, message);
    }
}

package xyz.msws.tracker.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import xyz.msws.tracker.Client;
import xyz.msws.tracker.utils.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CommandListener {

    private final List<AbstractCommand> commands = new ArrayList<>();
    private final Client client;

    public CommandListener(Client client) {
        this.client = client;
    }

    public void registerCommand(AbstractCommand command) {
        commands.add(command);
    }

    public void unregisterCommand(AbstractCommand command) {
        commands.remove(command);
    }

    public List<AbstractCommand> getCommands() {
        return commands;
    }

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == client.getJDA().getSelfUser().getIdLong())
            return;
        Message message = event.getMessage();

        if (!message.getContentDisplay().toLowerCase().startsWith(client.getPrefix().toLowerCase())
                && !message.getContentDisplay().startsWith("@" + client.getJDA().getSelfUser().getName()))
            return;

        String msg = message.getContentDisplay()
                .substring(message.getContentDisplay().toLowerCase().startsWith(client.getPrefix().toLowerCase())
                        ? client.getPrefix().length()
                        : client.getJDA().getSelfUser().getName().length() + 2);

        AbstractCommand cmd = null;

        for (AbstractCommand c : commands) {
            if (c.getName().equalsIgnoreCase(msg.split(" ")[0])
                    || c.getAliases().contains(msg.split(" ")[0].toLowerCase())) {
                if (!c.checkPermission(message))
                    break;
                cmd = c;
                break;
            }
        }
        if (cmd == null)
            return;

        final AbstractCommand fCmd = cmd;
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            final long start = System.currentTimeMillis();

            @Override
            public void run() {
                if (System.currentTimeMillis() - start > 60 * 1000 * 3) {
                    Logger.log(
                            message.getContentRaw() + " started a command that hasn't finished within 3 minutes.");
                    this.cancel();
                    return;
                }
                message.getTextChannel().sendTyping().queue();
            }
        }, 0, 8000);

        new Thread(() -> {
            long start = System.currentTimeMillis();
            Logger.logf("%s sent command: '%s', executing...", event.getAuthor().getName(),
                    message.getContentRaw());
            try {
                fCmd.execute(message,
                        msg.contains(" ") ? msg.substring(msg.indexOf(" ") + 1).split(" ") : new String[0]);

                Logger.log("Successfully finished execution, took " + (System.currentTimeMillis() - start) + "ms");
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                message.getChannel()
                        .sendMessage("An error occured while running that command:\n``` " + sw.toString() + "```")
                        .queue();
                timer.cancel();
                Logger.log("An error occured while executing command:\n``` " + sw.toString() + "```");
            }
            timer.cancel();
        }).start();

    }
}

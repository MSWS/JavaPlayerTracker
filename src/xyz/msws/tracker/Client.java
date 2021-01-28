package xyz.msws.tracker;

import net.dv8tion.jda.api.JDA;
import xyz.msws.tracker.commands.CommandListener;
import xyz.msws.tracker.module.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a discord client
 *
 * @author imodm
 */
public abstract class Client {
    protected String token;
    protected JDA jda;
    protected CommandListener commands;
    protected List<Module> modules = new ArrayList<>();
    protected List<String> logs = new ArrayList<>();

    public Client(String token) {
        this.token = token;
        commands = new CommandListener(this);
    }

    public void loadModules() {
        modules.forEach(Module::load);
    }

    public <T extends Module> T getModule(Class<T> c) {
        for (Module m : modules) {
            if (m.getClass().isAssignableFrom(c))
                return c.cast(m);
        }
        return null;
    }

    public abstract void start();

    public JDA getJDA() {
        return jda;
    }

    public CommandListener getCommandListener() {
        return commands;
    }

    /**
     * Returns the client's name
     *
     * @return The bot's name
     */
    public abstract String getName();

    /**
     * Returns the client's prefix
     */
    public abstract String getPrefix();

}

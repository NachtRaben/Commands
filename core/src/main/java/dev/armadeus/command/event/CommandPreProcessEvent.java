package dev.armadeus.command.event;

import dev.armadeus.command.command.Command;
import dev.armadeus.command.CommandSender;

import java.util.Map;

/**
 * Event used to determine if commands should be ran.
 */
public class CommandPreProcessEvent {

    private CommandSender sender;
    private Command command;
    private Map<String, String> args;
    private Map<String, String> flags;
    private boolean cancelled = false;

    /**
     * Instantiates a new Command Pre-Process event.
     *
     * @param sender  the sender
     * @param command the command
     * @param args    the args
     * @param flags   the flags
     */
    public CommandPreProcessEvent(CommandSender sender, Command command, Map<String, String> args, Map<String, String> flags) {
        this.sender = sender;
        this.command = command;
        this.args = args;
        this.flags = flags;
    }

    /**
     * Gets sender.
     *
     * @return the sender
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * Gets command.
     *
     * @return the command
     */
    public Command getCommand() {
        return command;
    }

    /**
     * Gets args.
     *
     * @return the args
     */
    public Map<String, String> getArgs() {
        return args;
    }

    /**
     * Gets flags.
     *
     * @return the flags
     */
    public Map<String, String> getFlags() {
        return flags;
    }

    /**
     * Is cancelled boolean.
     *
     * @return the boolean
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets cancelled.
     */
    public void setCancelled() {
        cancelled = true;
    }
}

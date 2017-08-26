package com.nachtraben.orangeslice.event;

import com.nachtraben.orangeslice.CommandResult;
import com.nachtraben.orangeslice.CommandSender;
import com.nachtraben.orangeslice.command.Command;

import java.util.Map;

public class CommandPostProcessEvent {

    private CommandSender sender;
    private Command command;
    private Map<String, String> args;
    private Map<String, String> flags;
    private CommandResult result;
    private Throwable throwable;

    /**
     * Instantiates a new Command Pre-Process event.
     *
     * @param sender  the sender
     * @param command the command
     * @param args    the args
     * @param flags   the flags
     */
    public CommandPostProcessEvent(CommandSender sender, Command command, Map<String, String> args, Map<String, String> flags, CommandResult result) {
        this(sender, command, args, flags, result, null);
    }

    public CommandPostProcessEvent(CommandSender sender, Command command, Map<String, String> args, Map<String, String> flags, CommandResult result, Throwable throwable) {
        this.sender = sender;
        this.command = command;
        this.args = args;
        this.flags = flags;
        this.result = result;
        this.throwable = throwable;
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

    public CommandResult getResult() {
        return result;
    }

    public Throwable getException() {
        return throwable;
    }
}

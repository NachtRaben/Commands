package dev.armadeus.command.event;

import dev.armadeus.command.CommandSender;
import dev.armadeus.command.command.Command;

public class CommandExceptionEvent {

    private CommandSender sender;
    private Command command;
    private Throwable exception;

    public CommandExceptionEvent(CommandSender sender, Command command, Exception exception) {
        this.sender = sender;
        this.command = command;
        this.exception = exception;
    }

    public CommandSender getSender() {
        return sender;
    }

    public Command getCommand() {
        return command;
    }

    public Throwable getException() {
        return exception;
    }
}

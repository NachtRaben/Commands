package dev.armadeus.command.command;

public abstract class SubCommand extends Command {

    public SubCommand(String name, String format, String description) {
        super(name, format, description);
    }

    public void init() {}

}

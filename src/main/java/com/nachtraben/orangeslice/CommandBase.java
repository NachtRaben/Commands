package com.nachtraben.orangeslice;

import com.nachtraben.orangeslice.command.AnnotatedCommand;
import com.nachtraben.orangeslice.command.Cmd;
import com.nachtraben.orangeslice.command.CmdAttribute;
import com.nachtraben.orangeslice.command.Command;
import com.nachtraben.orangeslice.event.CommandEventListener;
import com.nachtraben.orangeslice.event.CommandExceptionEvent;
import com.nachtraben.orangeslice.event.CommandPreProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by NachtRaben on 2/4/2017.
 */
public class CommandBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBase.class);

    private Map<String, List<Command>> commands;
    private ExecutorService executor;

    private List<CommandEventListener> eventListeners;

    /**
     * Instantiates a new Command base.
     */
    public CommandBase() {
        commands = new HashMap<>();
        eventListeners = new ArrayList<>();
        ThreadGroup group = new ThreadGroup("Command Threads");
        executor = Executors.newCachedThreadPool(r -> new Thread(group, r, "CommandThread-" + group.activeCount()));
    }

    /**
     * Register commands.
     *
     * @param object the object
     */
    public void registerCommands(Object object) {
        if (object instanceof Command) {
            Command command = (Command) object;
            command.setCommandBase(this);
            List<Command> commands = this.commands.computeIfAbsent(command.getName(), list -> new ArrayList<>());
            // TODO: Command overlapping.
            commands.add(command);
            for (String alias : command.getAliases()) {
                List<Command> aliasedCommands = this.commands.computeIfAbsent(alias, list -> new ArrayList<>());
                aliasedCommands.add(command);
            }
            LOGGER.info("Added command, " + command.toString());
        }

        // Process any annotated commands
        for (Method method : object.getClass().getMethods()) {
            if (method.isAnnotationPresent(Cmd.class)) {
                Cmd cmd = method.getAnnotation(Cmd.class);
                AnnotatedCommand command = new AnnotatedCommand(cmd, object, method);
                if (method.isAnnotationPresent(CmdAttribute.class)) {
                    for(CmdAttribute attrib : method.getAnnotationsByType(CmdAttribute.class)) {
                        command.setAttribute(attrib.name(), attrib.value());
                    }
                }
                command.setCommandBase(this);
                List<Command> commands = this.commands.computeIfAbsent(command.getName(), list -> new ArrayList<>());
                // TODO: Command overlapping.
                commands.add(command);
                for (String alias : command.getAliases()) {
                    List<Command> aliasedCommands = this.commands.computeIfAbsent(alias, list -> new ArrayList<>());
                    aliasedCommands.add(command);
                }
                LOGGER.info("Added command, " + command.toString());
            }
        }
    }

    /**
     * Execute future.
     *
     * @param sender    the sender
     * @param command   the command
     * @param arguments the arguments
     *
     * @return the future
     */
    public Future<CommandResult> execute(final CommandSender sender, final String command, final String[] arguments) {
        return executor.submit(() -> {
            Map<String, String> mappedFlags = new HashMap<>();
            ArrayList<String> processedArgs = new ArrayList<>();

            filterArgsAndFlags(arguments, mappedFlags, processedArgs);

            String[] args = new String[processedArgs.size()];
            processedArgs.toArray(args);
            Command canidate = getCommandMatch(sender, command, args);

            if (canidate != null) {
                //TODO: Flag validation
                Map<String, String> mappedArguments = canidate.processArgs(args);
                CommandPreProcessEvent event = new CommandPreProcessEvent(sender, canidate, mappedArguments, mappedFlags);
                eventListeners.forEach(el -> el.onCommandPreProcess(event));
                if(!event.isCancelled()) {
                    try {
                        canidate.run(sender, mappedArguments, mappedFlags);
                        return CommandResult.SUCCESS;
                    } catch (Exception e) {
                        LOGGER.error("An error occurred while processing one of the commands.", e);
                        CommandExceptionEvent exceptionEvent = new CommandExceptionEvent(sender, canidate, e);
                        eventListeners.forEach(el -> el.onCommandException(exceptionEvent));
                        return CommandResult.EXCEPTION;
                    }
                } else {
                    return CommandResult.CANCELLED;
                }
            } else {
                return CommandResult.UNKNOWN_COMMAND;
            }
        });
    }

    private void filterArgsAndFlags(String[] arguments, Map<String, String> flags, ArrayList<String> processedArgs) {
        for (String s : arguments) {
            if (Command.flagsRegex.matcher(s).find()) {
                for (char c : s.substring(1).toCharArray()) {
                    flags.put(String.valueOf(c), null);
                }
            } else if (Command.flagRegex.matcher(s).find()) {
                flags.put(s.substring(2), null);
            } else if (Command.flagWithValue.matcher(s).find()) {
                flags.put(s.substring(2, s.indexOf("=")), s.substring(s.indexOf("=") + 1));
            } else {
                processedArgs.add(s);
            }
        }
    }

    private Command getCommandMatch(CommandSender sender, String command, String[] arguments) {
        List<Command> canidates = commands.get(command);
        if (canidates != null) {
            for (Command canidate : canidates) {
                if (canidate.getPattern().matcher(arrayToString(arguments)).find()) {
                    return canidate;
                }
            }
        }
        return null;
    }

    /**
     * Register event listener.
     *
     * @param handler the handler
     */
    public void registerEventListener(CommandEventListener handler) {
        eventListeners.add(handler);
    }

    /**
     * Unregister event listner.
     *
     * @param listener the listener
     */
    public void unregisterEventListner(CommandEventListener listener) {
        eventListeners.remove(listener);
    }

    private String arrayToString(String[] args) {
        if (args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            sb.append(s).append(" ");
        }
        sb.replace(sb.length() - 1, sb.length(), "");
        return sb.toString();
    }

    /**
     * Remove command.
     *
     * @param command the command
     */
    public void removeCommand(Command command) {
        commands.remove(command.getName());
        for(String alias : command.getAliases()) {
            if(commands.containsKey(alias)) {
                commands.get(alias).remove(command);
            }
        }
    }

    /**
     * Gets commands.
     *
     * @return the commands
     */
    public Map<String, List<Command>> getCommands() {
        return new HashMap<>(commands);
    }

    public void updateAliases(Command command, List<String> old, List<String> newaliases) {
        for (String alias : old) {
            List<Command> aliasedCommands = this.commands.computeIfAbsent(alias, list -> new ArrayList<>());
            aliasedCommands.remove(command);
            if(aliasedCommands.isEmpty())
                this.commands.remove(alias);
        }
        for(String alias : newaliases) {
            List<Command> aliasedCommands = this.commands.computeIfAbsent(alias, list -> new ArrayList<>());
            aliasedCommands.add(command);
        }
    }
}

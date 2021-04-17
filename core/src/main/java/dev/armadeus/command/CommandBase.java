package dev.armadeus.command;

import dev.armadeus.command.command.AnnotatedCommand;
import dev.armadeus.command.command.Cmd;
import dev.armadeus.command.command.CmdAttribute;
import dev.armadeus.command.command.Command;
import dev.armadeus.command.command.CommandTree;
import dev.armadeus.command.event.CommandEventListener;
import dev.armadeus.command.event.CommandExceptionEvent;
import dev.armadeus.command.event.CommandPostProcessEvent;
import dev.armadeus.command.event.CommandPreProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by NachtRaben on 2/4/2017.
 */
public class CommandBase {

    private static final Logger log = LoggerFactory.getLogger(CommandBase.class);

    private Map<String, List<Command>> commands;
    private Map<String, List<Command>> aliases;
    private ExecutorService executor;
    private boolean processFlags = true;
    public boolean debug = false;

    private List<CommandEventListener> eventListeners;

    /**
     * Instantiates a new Command base.
     */
    public CommandBase() {
        commands = new HashMap<>();
        aliases = new HashMap<>();
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

            Command overlap = checkForOverlaps(command);
            if (overlap != null) {
                log.error(String.format("Found an overlapping command. %s{%s} overlaps with previously registered command %s{%s}.", command.getName(), command.getFormat(), overlap.getName(), overlap.getFormat()));
                return;
            }

            command.setCommandBase(this);
            List<Command> commands = this.commands.computeIfAbsent(command.getName(), list -> new ArrayList<>());

            commands.add(command);
            for (String alias : command.getAliases()) {
                List<Command> aliases = this.aliases.computeIfAbsent(alias, list -> new ArrayList<>());
                // TODO: Warn about overlapping aliases.
                if (aliases.contains(command))
                    continue;

                aliases.add(command);
            }
            if (debug)
                log.info("Added command, " + command.toString());
        } else if (object instanceof CommandTree) {
            CommandTree tree = (CommandTree) object;
            if (debug)
                log.info("Registering CommandTree: " + tree.getClass().getSimpleName());
            tree.registerChildren(this);
        }

        // Process any annotated commands
        for (Method method : object.getClass().getMethods()) {
            if (method.isAnnotationPresent(Cmd.class)) {
                Cmd cmd = method.getAnnotation(Cmd.class);
                AnnotatedCommand command = new AnnotatedCommand(cmd, object, method);
                if (method.isAnnotationPresent(CmdAttribute.class)) {
                    for (CmdAttribute attrib : method.getAnnotationsByType(CmdAttribute.class)) {
                        command.setAttribute(attrib.name(), attrib.value());
                    }
                }

                Command overlap = checkForOverlaps(command);
                if (overlap != null) {
                    log.error(String.format("Found an overlapping command. %s{%s} overlaps with previously registered command %s{%s}.", command.getName(), command.getFormat(), overlap.getName(), overlap.getFormat()));
                    continue;
                }

                command.setCommandBase(this);
                List<Command> commands = this.commands.computeIfAbsent(command.getName(), list -> new ArrayList<>());
                // TODO: Command overlapping.

                commands.add(command);
                for (String alias : command.getAliases()) {
                    List<Command> aliases = this.aliases.computeIfAbsent(alias, list -> new ArrayList<>());
                    aliases.add(command);
                }
                if (debug)
                    log.info("Added command, " + command.toString());
            }
        }
    }

    private Command checkForOverlaps(Command c) {
        List<Command.CommandArg> cargs = c.getCmdArgs();
        for (List<Command> commands : commands.values()) {
            for (Command comm : commands) {
                if (!c.getName().equalsIgnoreCase(comm.getName()))
                    continue;

                boolean matches = true;
                List<Command.CommandArg> commargs = comm.getCmdArgs();
                if (cargs.size() == commargs.size()) {
                    for (int i = 0; i < cargs.size(); i++) {
                        Command.CommandArg carg = cargs.get(i);
                        Command.CommandArg commarg = commargs.get(i);
                        if (i == cargs.size() - 1) {
                            if (!carg.isRest() && !carg.isRequired() && !commarg.isRequired() && !commarg.isRest())
                                matches = false;
                        } else if (!carg.equals(commarg)) {
                            matches = false;
                            break;
                        }
                    }
                } else {
                    List<Command.CommandArg> shorter = cargs.size() < commargs.size() ? cargs : commargs;
                    List<Command.CommandArg> longer = shorter.equals(cargs) ? commargs : cargs;

                    if (shorter.isEmpty() && longer.size() >= 1 && longer.get(0).isRequired()) {
                        matches = false;
                    } else {
                        for (int i = 0; i < shorter.size(); i++) {
                            Command.CommandArg shortest = shorter.get(i);
                            Command.CommandArg longest = longer.get(i);
                            Command.CommandArg longest2 = longer.get(i + 1);
                            if (i == shorter.size() - 1) {
                                matches = (shortest.isRequired() && longest.isRequired() && !longest2.isRequired()) || shortest.isRest();
                            } else if (!shortest.equals(longest)) {
                                matches = false;
                                break;
                            }
                        }
                    }
                }
                if (matches)
                    return comm;
            }
        }
        return null;
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
                for (Map.Entry<String, String> flags : mappedFlags.entrySet()) {
                    if (canidate.getFlags().stream().noneMatch(flag -> flag.contains(flags.getKey()))) {
                        CommandPostProcessEvent cpp = new CommandPostProcessEvent(sender, canidate, mappedFlags, mappedFlags, CommandResult.INVALID_FLAGS, new IllegalArgumentException("{ " + flags.getKey() + " } is not a valid flag for the command."));
                        eventListeners.forEach(el -> el.onCommandPostProcess(cpp));
                        return CommandResult.INVALID_FLAGS;
                    }
                }
                Map<String, String> mappedArguments = canidate.processArgs(args);
                CommandPreProcessEvent event = new CommandPreProcessEvent(sender, canidate, mappedArguments, mappedFlags);
                eventListeners.forEach(el -> el.onCommandPreProcess(event));
                if (!event.isCancelled()) {
                    try {
                        canidate.run(sender, mappedArguments, mappedFlags);
                        CommandPostProcessEvent cpp = new CommandPostProcessEvent(sender, canidate, mappedArguments, mappedFlags, CommandResult.SUCCESS);
                        eventListeners.forEach(el -> el.onCommandPostProcess(cpp));
                        return CommandResult.SUCCESS;
                    } catch (Exception e) {
                        CommandExceptionEvent exceptionEvent = new CommandExceptionEvent(sender, canidate, e);
                        eventListeners.forEach(el -> el.onCommandException(exceptionEvent));
                        CommandPostProcessEvent cpp = new CommandPostProcessEvent(sender, canidate, mappedArguments, mappedFlags, CommandResult.EXCEPTION, e);
                        eventListeners.forEach(el -> el.onCommandPostProcess(cpp));
                        return CommandResult.EXCEPTION;
                    }
                } else {
                    CommandPostProcessEvent cpp = new CommandPostProcessEvent(sender, canidate, mappedArguments, mappedFlags, CommandResult.CANCELLED);
                    eventListeners.forEach(el -> el.onCommandPostProcess(cpp));
                    return CommandResult.CANCELLED;
                }
            } else {
                CommandPostProcessEvent cpp = new CommandPostProcessEvent(sender, null, null, null, CommandResult.UNKNOWN_COMMAND);
                eventListeners.forEach(el -> el.onCommandPostProcess(cpp));
                return CommandResult.UNKNOWN_COMMAND;
            }
        });
    }

    private void filterArgsAndFlags(String[] arguments, Map<String, String> flags, ArrayList<String> processedArgs) {
        // TODO: Filter args/flags with "" as a single value
        for (String s : arguments) {
            if (processFlags && Command.flagsRegex.matcher(s).find()) {
                for (char c : s.substring(1).toCharArray()) {
                    flags.put(String.valueOf(c), null);
                }
            } else if (processFlags && Command.flagRegex.matcher(s).find()) {
                flags.put(s.substring(2), null);
            } else if (processFlags && Command.flagWithValue.matcher(s).find()) {
                flags.put(s.substring(2, s.indexOf("=")), s.substring(s.indexOf("=") + 1));
            } else {
                // Process flag escape
                if (s.startsWith("\\-"))
                    processedArgs.add(s.replace("\\", ""));
                else
                    processedArgs.add(s);
            }
        }
    }

    private Command getCommandMatch(CommandSender sender, String command, String[] arguments) {
        List<Command> canidates = getCommand(command);
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
        List<Command> commands = this.commands.get(command.getName());
        if (commands != null)
            commands.remove(command);
        for (String alias : command.getAliases()) {
            List<Command> aliased = this.aliases.get(alias);
            if (aliased != null)
                aliased.remove(command);
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

    public Map<String, List<Command>> getAliases() {
        return new HashMap<>(aliases);
    }

    public List<Command> getCommand(String command) {
        List<Command> commands = this.commands.get(command);
        if (commands == null)
            commands = aliases.get(command);
        return commands;
    }

    public boolean isProcessFlags() {
        return processFlags;
    }

    public void setProcessFlags(boolean b) {
        processFlags = b;
    }

    public void updateAliases(Command command, List<String> old, List<String> newaliases) {
        for (String alias : old) {
            List<Command> commands = this.aliases.get(alias);
            if (commands != null)
                commands.remove(command);
        }
        for (String alias : newaliases) {
            List<Command> commands = this.aliases.computeIfAbsent(alias, list -> new ArrayList<>());
            commands.add(command);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Failed to safely shutdown command executor.", e);
        }
    }

}

package com.nachtraben.orangeslice.command;

import com.nachtraben.orangeslice.CommandBase;
import com.nachtraben.orangeslice.CommandCreationException;
import com.nachtraben.orangeslice.CommandSender;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by NachtRaben on 3/7/2017.
 */
public abstract class Command {

    private final String name;
    private final String format;
    private final String description;

    private List<String> aliases;
    private List<String> flags;
    private Map<String, Object> attributes;

    private List<CommandArg> commandArgs;
    private List<CommandFlag> commandFlags;

    private Pattern pattern;

    private CommandBase commandBase;

    public static final String requiredRegex = "\\S+";
    public static final String optionalRegex = "(\\s+\\S+)?";
    public static final String firstOptionalRegex = "(\\S+)?";
    public static final String restRegex = "(?!\\s*$).+";
    public static final String optionalRestRegex = "(\\s+" + restRegex + ")?";
    public static final String firstOptionalRestRegex = "(" + restRegex + ")?";

    /**
     * The constant flagsRegex. Used to process flags like "-f".
     */
    public static final Pattern flagsRegex = Pattern.compile("^-\\w+$", 0);
    /**
     * The constant flagRegex. Used to process flags like "--force".
     */
    public static final Pattern flagRegex = Pattern.compile("^--\\w+$");
    /**
     * The constant flagWithValue. Used to process flags like "--name=ted".
     */
    public static final Pattern flagWithValue = Pattern.compile("^--\\w+=\\S+$", 0);

    /**
     * Instantiates a new Command.
     *
     * @param name        the name
     * @param format      the format
     * @param description the description
     */
    public Command(String name, String format, String description) {
        this.name = name;
        this.format = format;
        this.description = description;

        aliases = new ArrayList<>();
        flags = new ArrayList<>();
        attributes = new HashMap<>();

        commandArgs = new ArrayList<>();
        commandFlags = new ArrayList<>();
        validateFormat();
        buildPattern();
        buildFlags();
    }

    private void validateFormat() throws CommandCreationException {
        String[] tokens = format.split(" ");
        tokens = Arrays.stream(tokens).filter(s -> (!s.isEmpty())).toArray(String[]::new);
        if (tokens.length > 0 && tokens[0].equals(name)) {
            tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
        }
        if (tokens.length > 0) {
            for (int i = 0; i < tokens.length; i++) {
                String arg = tokens[i];
                if (arg.charAt(0) == '<' && arg.charAt(arg.length() - 1) == '>') {
                    commandArgs.add(i, new CommandArg(arg.substring(1, arg.length() - 1), true, true, false));
                } else if (arg.charAt(0) == '[' && arg.charAt(arg.length() - 1) == ']') {
                    commandArgs.add(i, new CommandArg(arg.substring(1, arg.length() - 1), true, false, false));
                    if (i != tokens.length - 1) {
                        throw new CommandCreationException(this, "[] statements can only be at the end of the format.");
                    }
                } else if (arg.charAt(0) == '{' && arg.charAt(arg.length() - 1) == '}') {
                    commandArgs.add(i, new CommandArg(arg.substring(1, arg.length() - 1), true, true, true));
                    if (i != tokens.length - 1) {
                        throw new CommandCreationException(this, "{} statements can only be at the end of the format.");
                    }
                } else if (arg.charAt(0) == '(' && arg.charAt(arg.length() - 1) == ')') {
                    commandArgs.add(i, new CommandArg(arg.substring(1, arg.length() - 1), true, false, true));
                    if (i != tokens.length - 1) {
                        throw new CommandCreationException(this, "() statements can only be at the end of the format.");
                    }
                } else {
                    commandArgs.add(i, new CommandArg(arg, false, true, false));
                }
            }
        }
    }

    private void buildPattern() {
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        if (commandArgs.size() >= 1) {
            for (int i = 0; i < commandArgs.size(); i++) {
                CommandArg arg = commandArgs.get(i);
                if (!arg.isRequired) {
                    if (i == 0) {
                        if (arg.isRest) {
                            sb.append(firstOptionalRestRegex);
                        } else {
                            sb.append(firstOptionalRegex);
                        }
                    } else if (arg.isRest) {
                        sb.append(optionalRestRegex);
                    } else {
                        sb.append(optionalRegex); // Process [] tag
                    }
                } else {
                    if (i > 0) {
                        sb.append("\\s+"); // Add space if not first tag
                    }
                    if (arg.isDynamic) {
                        if (arg.isRest) {
                            sb.append(restRegex); // Process {} tag
                        } else {
                            sb.append(requiredRegex); // Process <> tag
                        }
                    } else {
                        sb.append(arg.name);
                    }
                }
            }
        }
        sb.append("$");
        pattern = Pattern.compile(sb.toString(), 0);
    }

    private void buildFlags() {
        commandFlags = new ArrayList<>();
        for (String s : flags) {
            if (flagsRegex.matcher(s).find()) {
                for (char c : s.substring(1).toCharArray()) {
                    commandFlags.add(new CommandFlag(String.valueOf(c), false));
                }
            } else if (flagRegex.matcher(s).find()) {
                commandFlags.add(new CommandFlag(s.substring(2), false));
            } else if (flagWithValue.matcher(s).find()) {
                commandFlags.add(new CommandFlag(s.substring(2, s.indexOf("=")), true));
            }
        }
    }

    /**
     * Process args array into keymap of command arg and value.
     *
     * @param args the args
     *
     * @return the map
     */
    public Map<String, String> processArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            CommandArg cmdarg = commandArgs.get(i);
            if (cmdarg.isDynamic) {
                if (cmdarg.isRest) {
                    result.put(cmdarg.name.toLowerCase(), arrayToString(Arrays.copyOfRange(args, i, args.length)));
                    return result;
                } else {
                    result.put(cmdarg.name.toLowerCase(), args[i]);
                }
            }
        }
        return result;
    }

    private String arrayToString(String[] args) {
        if (args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            sb.append(s).append(" ");
        }
        sb.replace(sb.length() - 1, sb.length(), "");
        return sb.toString();
    }

    /**
     * Run.
     *
     * @param sender the sender
     * @param args   the args
     * @param flags  the flags
     */
    public abstract void run(CommandSender sender, Map<String, String> args, Map<String, String> flags);

    /**
     * Help string string.
     *
     * @return the string
     */
    public String helpString() {
        return "**Name**: " + name + "\t**Usage**: " + name + " " + format + "\t**Description**: " + description;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" +
                "name=" + name + ", " +
                "format=" + format + ", " +
                "description=" + description + ", " +
                "aliases=" + aliases + ", " +
                "flags=" + flags + ", " +
                "attributes=" + attributes +
                ")";
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets format.
     *
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets aliases.
     *
     * @return the aliases
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Sets aliases.
     *
     * @param aliases the aliases
     */
    protected void setAliases(List<String> aliases) {
        List<String> old = this.aliases;
        this.aliases = aliases;
        if (commandBase != null)
            commandBase.updateAliases(this, old, aliases);
    }

    /**
     * Gets flags.
     *
     * @return the flags
     */
    public List<String> getFlags() {
        return flags;
    }

    /**
     * Sets flags.
     *
     * @param flags the flags
     */
    protected void setFlags(List<String> flags) throws CommandCreationException {
        this.flags = flags;
        buildFlags();
    }

    /**
     * Gets attributes.
     *
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public <T> T getAttribute(String key, Class<T> tClass) {
        Object o = attributes.get(key);
        if (o != null) {
            return tClass.cast(o);
        }
        return null;
    }

    /**
     * Sets attributes.
     *
     * @param attributes the attributes
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }


    /**
     * Sets an attribute.
     *
     * @param identifier the identifier
     * @param value      the value
     */
    public void setAttribute(String identifier, Object value) {
        this.attributes.put(identifier, value);
    }

    /**
     * Gets pattern.
     *
     * @return the pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    public CommandBase getCommandBase() {
        return commandBase;
    }

    public void setCommandBase(CommandBase commandBase) {
        this.commandBase = commandBase;
    }

    /**
     * The type Command arg.
     */
    class CommandArg {
        /**
         * The Name.
         */
        String name = "";
        /**
         * The Is dynamic.
         */
        boolean isDynamic = true;
        /**
         * The Is required.
         */
        boolean isRequired = false;
        /**
         * The Is rest.
         */
        boolean isRest = false;

        /**
         * Instantiates a new Command arg.
         *
         * @param name       the name
         * @param isDynamic  the is dynamic
         * @param isRequired the is required
         * @param isRest     the is rest
         */
        CommandArg(String name, boolean isDynamic, boolean isRequired, boolean isRest) {
            this.name = name;
            this.isDynamic = isDynamic;
            this.isRequired = isRequired;
            this.isRest = isRest;
        }
    }

    /**
     * The type Command flag.
     */
    class CommandFlag {
        /**
         * The Name.
         */
        String name = "";
        /**
         * The Needs value.
         */
        boolean needsValue = false;

        /**
         * Instantiates a new Command flag.
         *
         * @param name       the name
         * @param needsValue the needs value
         */
        CommandFlag(String name, boolean needsValue) {
            this.name = name;
            this.needsValue = needsValue;
        }
    }
}

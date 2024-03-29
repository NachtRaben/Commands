package dev.armadeus.command.command;

import dev.armadeus.command.CommandCreationException;
import dev.armadeus.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by NachtRaben on 3/7/2017.
 */
public class AnnotatedCommand extends Command {

    private Cmd cmd;
    private Object methodHolder;
    private Method method;

    int size = 0;

    public AnnotatedCommand(Cmd cmd, Object methodHolder, Method method) throws CommandCreationException {
        super(cmd.name(), cmd.format(), cmd.description());
        this.cmd = cmd;
        this.methodHolder = methodHolder;
        this.method = method;

        setAliases(Arrays.asList(cmd.aliases()));
        setFlags(Arrays.asList(cmd.flags()));
        validateMethod();
    }

    private void validateMethod() throws CommandCreationException {
        Parameter[] parameters = method.getParameters();

        switch (parameters.length) {
            case 4: {
                size = 4;
                if(!Map.class.isAssignableFrom(parameters[3].getType())) {
                    throw new CommandCreationException(this, "Parameter[4] was not assignable from Map<String, String>.");
                }
                if (parameters[3].getParameterizedType() == null || !parameters[3].getParameterizedType().getTypeName().equals("java.util.Map<java.lang.String, java.lang.String>")) {
                    throw new CommandCreationException(this, "Parameter[2] was not assignable from Map<String, String>.");
                }
            }
            case 3: {
                if(size < 3) {
                    size = 3;
                }
                if (!Map.class.isAssignableFrom(parameters[2].getType())) {
                    throw new CommandCreationException(this, "Parameter[2] was not assignable from Map<String, String>.");
                }
                if (parameters[2].getParameterizedType() == null || !parameters[2].getParameterizedType().getTypeName().equals("java.util.Map<java.lang.String, java.lang.String>")) {
                    throw new CommandCreationException(this, "Parameter[2] was not assignable from Map<String, String>.");
                }
            }
            case 2: {
                if (size < 2) {
                    size = 2;
                }
                if (!Map.class.isAssignableFrom(parameters[1].getType())) {
                    throw new CommandCreationException(this, "Parameter[1] was not assignable from Map<String, String>.");
                }
                if (parameters[1].getParameterizedType() == null || !parameters[1].getParameterizedType().getTypeName().equals("java.util.Map<java.lang.String, java.lang.String>")) {
                    throw new CommandCreationException(this, "Parameter[1] was not assignable from Map<String, String>.");
                }
            }
            case 1: {
                if (size < 1) {
                    size = 1;
                }
                if (!parameters[0].getType().equals(CommandSender.class)) {
                    throw new CommandCreationException(this, "Parameter[0] was not CommandSender.class.");
                }
                break;
            }
            default: {
                throw new CommandCreationException(this, "Invalid number of parameters");
            }
        }
    }

    @Override
    public void run(CommandSender sender, Map<String, String> args, Map<String, String> flags) {
        try {
            switch (size) {
                case 4: {
                    method.invoke(methodHolder, sender, args, flags, getAttributes());
                    break;
                }
                case 3: {
                    method.invoke(methodHolder, sender, args, flags);
                    break;
                }
                case 2: {
                    method.invoke(methodHolder, sender, args);
                    break;
                }
                case 1: {
                    method.invoke(methodHolder, sender);
                    break;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}

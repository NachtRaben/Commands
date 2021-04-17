package dev.armadeus.command.event;

/**
 * Created by NachtRaben on 3/9/2017.
 */
public interface CommandEventListener {
    void onCommandPreProcess(CommandPreProcessEvent event);

    void onCommandPostProcess(CommandPostProcessEvent event);

    void onCommandException(CommandExceptionEvent exceptionEvent);
}

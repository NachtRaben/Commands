package com.nachtraben.orangeslice.event;

/**
 * Created by NachtRaben on 3/9/2017.
 */
public interface CommandEventListener {
    void onCommandPreProcess(CommandPreProcessEvent event);

    void onCommandException(CommandExceptionEvent exceptionEvent);
}

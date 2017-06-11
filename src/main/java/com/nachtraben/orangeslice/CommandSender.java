package com.nachtraben.orangeslice;

import java.util.concurrent.Future;

/**
 * Created by NachtDesk on 8/30/2016.
 */
public interface CommandSender {

    void sendMessage(String message);
    String getName();
    Future<CommandEvent> runCommand(String command, String[] args);
}

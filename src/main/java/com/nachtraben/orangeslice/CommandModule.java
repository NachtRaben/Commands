package com.nachtraben.orangeslice;

import java.util.Map;

public interface CommandModule {

    boolean run(Command command, CommandSender sender, Map<String, String> mapargs, Map<String, String> flags);

}

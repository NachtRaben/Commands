package com.nachtraben.orangeslice.command;

import com.nachtraben.orangeslice.CommandBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class CommandTree {

    private List<SubCommand> children = new ArrayList<>();

    public void registerChildren(CommandBase base) {
        for(SubCommand c : children) {
            c.init();
            base.registerCommands(c);
        }
    }

    public void unregisterChildren() {
        Iterator<SubCommand> it = children.iterator();
        Command child;
        while(it.hasNext()) {
            child = it.next();
            child.getCommandBase().removeCommand(child);
            it.remove();
        }
    }

    protected List<SubCommand> getChildren() {
        return children;
    }

}

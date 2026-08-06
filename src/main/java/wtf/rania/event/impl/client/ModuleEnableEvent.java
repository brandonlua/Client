package wtf.rania.event.impl.client;

import wtf.rania.event.Event;
import wtf.rania.client.modules.Module;

public class ModuleEnableEvent extends Event {
    public Module module;

    public ModuleEnableEvent(Module module) {
        this.module = module;
    }
}

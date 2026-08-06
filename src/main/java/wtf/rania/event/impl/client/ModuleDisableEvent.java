package wtf.rania.event.impl.client;

import wtf.rania.event.Event;
import wtf.rania.client.modules.Module;

public class ModuleDisableEvent extends Event {
    public Module module;

    public ModuleDisableEvent(Module module) {
        this.module = module;
    }
}

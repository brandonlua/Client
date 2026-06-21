package wtf.fentanyl.event.impl.client;

import wtf.fentanyl.event.Event;
import wtf.fentanyl.client.modules.Module;

public class ModuleDisableEvent extends Event {
    public Module module;

    public ModuleDisableEvent(Module module) {
        this.module = module;
    }
}

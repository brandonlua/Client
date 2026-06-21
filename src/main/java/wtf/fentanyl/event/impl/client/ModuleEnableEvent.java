package wtf.fentanyl.event.impl.client;

import wtf.fentanyl.event.Event;
import wtf.fentanyl.client.modules.Module;

public class ModuleEnableEvent extends Event {
    public Module module;

    public ModuleEnableEvent(Module module) {
        this.module = module;
    }
}

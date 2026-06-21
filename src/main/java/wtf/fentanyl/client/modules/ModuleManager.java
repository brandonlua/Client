package wtf.fentanyl.client.modules;

import lombok.Getter;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Getter
public class ModuleManager {
    private final HashMap<Class<? extends Module>, Module> modules;

    public ModuleManager() {
        this.modules = new HashMap<>();
        register();
    }

    public Module getModule(Class<? extends Module> module) {
        return modules.get(module);
    }

    public Module getModule(String name) {
        for(Module module : modules.values()) {
            if(module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public List<Module> getModules() {
        return new ArrayList<>(modules.values());
    }

    public List<Module> getModules(Category category) {
        List<Module> categoryModules = new ArrayList<>();
        for(Module module : modules.values()) {
            if(module.getCategory() == category) {
                categoryModules.add(module);
            }
        }
        return categoryModules;
    }

    public void register() {
        final Reflections refl = new Reflections("wtf.fentanyl.client.modules.impl");
        final Set<Class<? extends Module>> classes = refl.getSubTypesOf(Module.class);

        for(Class<? extends Module> c : classes) {
            try {
                final Module module = c.newInstance();
                modules.put(c, module);
            } catch(InstantiationException | IllegalAccessException e) {}
        }
    }

    public void unregister(Module... module) {
        for(Module mod : module) {
            modules.remove(mod.getClass());
        }
    }
}
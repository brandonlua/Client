package wtf.rania.client.modules.impl.movement;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;

@ModuleInfo(name = "KeepSprint", category = Category.MOVEMENT, enabled = true
)
public class KeepSprintModule extends Module {
    @Override
    public void onUpdate() {
        mc.gameSettings.keyBindSprint.setPressed(true);
    }
}
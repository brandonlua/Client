package wtf.fentanyl.client.modules.impl.movement;

import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;

@ModuleInfo(name = "KeepSprint", category = Category.MOVEMENT, enabled = true
)
public class KeepSprint extends Module {
    @Override
    public void onUpdate() {
        mc.gameSettings.keyBindSprint.setPressed(true);
    }
}
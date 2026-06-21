package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;

@ModuleInfo(name = "FullBright", category = Category.RENDER
)
public class FullBright extends Module {
    private float previousGamma;

    @Override
    public void onEnabled() {
        previousGamma = mc.gameSettings.gammaSetting;
        mc.gameSettings.gammaSetting = 100.0F;
    }

    @Override
    public void onDisabled() {
        mc.gameSettings.gammaSetting = previousGamma;
    }
}
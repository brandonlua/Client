package wtf.rania.client.modules.impl.render;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;

@ModuleInfo(name = "FullBright", category = Category.RENDER
)
public class FullBrightModule extends Module {
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
package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.SliderValue;

@ModuleInfo(name = "MotionCamera", category = Category.RENDER)
public class MotionCamera extends Module {

    public final SliderValue interpolation = new SliderValue("Motion Interpolation", 0.15f, 0.05f, 0.5f, 0.05f, this);

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.SliderValue;

@ModuleInfo(name = "PostProcessing", category = Category.RENDER, enabled = true)
public class PostProcessing extends Module {

    public final BoolValue blur = new BoolValue("Blur", true, this);
    public final SliderValue blurRadius = new SliderValue("Blur Interpolation", 10F, 1F, 20F, this, blur::get);

    public final BoolValue shadow = new BoolValue("Shadow", false, this);
    public final SliderValue shadowRadius = new SliderValue("Shadow Interpolation", 8F, 1F, 20F, this, shadow::get);

    public final BoolValue bloom = new BoolValue("Bloom", true, this);
    public final SliderValue bloomRadius = new SliderValue("Bloom Interpolation", 3F, 1F, 10F, this, bloom::get);
    public final SliderValue bloomOffset = new SliderValue("Bloom Offset", 1F, 1F, 10F, this, bloom::get);

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
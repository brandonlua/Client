package wtf.rania.client.modules.impl.render;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.SliderValue;

@ModuleInfo(name = "PostProcessing", category = Category.RENDER)
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
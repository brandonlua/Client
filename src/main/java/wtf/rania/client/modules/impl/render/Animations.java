package wtf.rania.client.modules.impl.render;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import lombok.Getter;

@ModuleInfo(name = "Animations", category = Category.RENDER)
@Getter
public class Animations extends Module {

    private final BoolValue old = new BoolValue("Old", false, this);
    private final ModeValue type = new ModeValue("Block Anim", new String[]{"Swank", "Swing", "Swang", "Swong", "Swaing", "Punch", "Virtue", "Push", "Stella", "Styles", "Slide", "Interia", "Ethereal", "1.7", "Sigma", "Exhibition", "Old Exhibition", "Smooth", "Moon", "Leaked", "Astolfo", "Small"}, "1.7", this, () -> !old.get());
    private final BoolValue blockWhenSwing = new BoolValue("Block When Swing", false, this);
    private final ModeValue hit = new ModeValue("Hit", new String[]{"Vanilla", "Smooth"}, "Vanilla", this, () -> !old.get());
    private final SliderValue slowdown = new SliderValue("Slow Down", 0, -5, 15, 1, this);
    private final SliderValue downscaleFactor = new SliderValue("Scale", 0f, 0.0f, 0.5f, .1f, this);
    private final BoolValue rotating = new BoolValue("Rotating", false, this, () -> !old.get());
    private final SliderValue x = new SliderValue("Item-X", 0.0F, -1.0F, 1.0F, .05f, this);
    private final SliderValue y = new SliderValue("Item-Y", 0.0F, -1.0F, 1.0F, .05f, this);
    private final SliderValue z = new SliderValue("Item-Z", 0.0F, -1.0F, 1.0F, .05f, this);
    private final SliderValue bx = new SliderValue("Block-X", 0.0F, -1.0F, 1.0F, .05f, this);
    private final SliderValue by = new SliderValue("Block-Y", 0.0F, -1.0F, 1.0F, .05f, this);
    private final SliderValue bz = new SliderValue("Block-Z", 0.0F, -1.0F, 1.0F, .05f, this);
    private final BoolValue walking = new BoolValue("Funny", false, this);
    private final BoolValue swingWhileUsingItem = new BoolValue("Swing While Using Item", false, this);

}

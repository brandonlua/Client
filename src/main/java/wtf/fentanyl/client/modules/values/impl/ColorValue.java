package wtf.fentanyl.client.modules.values.impl;

import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.values.Value;
import lombok.Getter;
import lombok.Setter;

import java.awt.Color;
import java.util.function.Supplier;

@Getter
@Setter
public class ColorValue extends Value {
    private float hue = 0;
    private float saturation = 1;
    private float brightness = 1;
    private float alpha = 1;
    private boolean rainbow = false;

    public ColorValue(String name, Color color, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        set(color);
    }

    public ColorValue(String name, Color color, Module module) {
        super(name, module, () -> true, false);
        set(color);
    }

    public Color get() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        Color base = new Color(rgb);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), (int)(alpha * 255));
    }

    public void set(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = color.getAlpha() / 255.0f;
    }
}
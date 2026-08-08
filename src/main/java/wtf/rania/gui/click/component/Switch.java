package wtf.rania.gui.click.component;

import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.utility.render.RenderUtil;

import java.awt.Color;

public class Switch {

    public static final float HEIGHT = 13f;

    private final BoolValue setting;

    public Switch(BoolValue setting) {
        this.setting = setting;
    }

    public void draw(float x, float y, float width, CFontRenderer font, Color theme) {
        RenderUtil.drawRect(x, y, width, HEIGHT, GuiTheme.BG_DARK);
        font.drawString(setting.getName(), x + 4, y + 2, GuiTheme.TEXT);
        boolean on = setting.get();
        String label = on ? "ON" : "OFF";
        float labelWidth = font.getStringWidth(label);
        font.drawString(label, x + width - labelWidth - 4, y + 2, Color.WHITE.getRGB());
    }

    public void click(float x, float y, float width, int mouseX, int mouseY) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            setting.toggle();
        }
    }

    public BoolValue getSetting() {
        return setting;
    }
}
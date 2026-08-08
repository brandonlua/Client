package wtf.rania.gui.click.component;

import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.utility.render.RenderUtil;

import java.awt.Color;

public class Slider {

    public static final float HEIGHT = 13f;

    private final SliderValue setting;
    private boolean dragging;
    private float lastX;
    private float lastWidth;

    public Slider(SliderValue setting) {
        this.setting = setting;
    }

    private String formatValue(float value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    public void draw(float x, float y, float width, CFontRenderer font, Color theme) {
        lastX = x;
        lastWidth = width;

        RenderUtil.drawRect(x, y, width, HEIGHT, GuiTheme.BG_DARK);

        float min = setting.getMin();
        float max = setting.getMax();
        float value = setting.get();
        float percent = max - min == 0 ? 0 : (value - min) / (max - min);
        percent = Math.max(0f, Math.min(1f, percent));

        if (percent > 0f) {
            RenderUtil.drawRect(x + 1, y + 1, (width - 2) * percent, HEIGHT - 2, theme.getRGB());
        }

        font.drawString(setting.getName(), x + 4, y + 3, GuiTheme.TEXT);
        String display = formatValue(value);
        float displayWidth = font.getStringWidth(display);
        font.drawString(display, x + width - displayWidth - 4, y + 3, GuiTheme.TEXT_SECONDARY);
    }

    public void mouseClick(float mouseX, float mouseY, float x, float y, float width) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            dragging = true;
            lastX = x;
            lastWidth = width;
            update(mouseX);
        }
    }

    public void mouseDrag(float mouseX) {
        if (dragging) {
            update(mouseX);
        }
    }

    public void mouseRelease() {
        dragging = false;
    }

    private void update(float mouseX) {
        float percent = (mouseX - lastX) / lastWidth;
        percent = Math.max(0f, Math.min(1f, percent));
        float min = setting.getMin();
        float max = setting.getMax();
        setting.setValue(min + (max - min) * percent);
    }

    public SliderValue getSetting() {
        return setting;
    }
}
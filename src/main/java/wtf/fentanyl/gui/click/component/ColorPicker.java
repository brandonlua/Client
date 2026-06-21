package wtf.fentanyl.gui.click.component;

import wtf.fentanyl.client.modules.values.impl.ColorValue;
import wtf.fentanyl.util.render.RenderUtil;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class ColorPicker {

    private ColorValue expandedColorValue = null;
    private ColorValue editingColor = null;
    private boolean editingHue = false;
    private boolean editingSat = false;

    public void render(float x, float y, float width, float height, ColorValue colorValue, int mouseX, int mouseY) {
        float hueW = 8;
        float hueX = x + width - hueW;
        float satW = width - hueW - 3;

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < satW; px++) {
                float s = px / satW;
                float b = 1.0f - (py / height);
                Color c = Color.getHSBColor(colorValue.getHue(), s, b);
                RenderUtil.drawRect(x + px, y + py, 1, 1, c);
            }
        }

        for (int i = 0; i < height; i++) {
            float h = i / height;
            Color hc = Color.getHSBColor(h, 1.0f, 1.0f);
            RenderUtil.drawRect(hueX, y + i, hueW, 1, hc);
        }

        if (editingColor == colorValue && editingSat && Mouse.isButtonDown(0)) {
            float s = Math.max(0, Math.min((mouseX - x) / satW, 1));
            float b = Math.max(0, Math.min(1.0f - ((mouseY - y) / height), 1));
            colorValue.setSaturation(s);
            colorValue.setBrightness(b);
        }
        if (editingColor == colorValue && editingHue && Mouse.isButtonDown(0)) {
            float h = Math.max(0, Math.min((mouseY - y) / height, 1));
            colorValue.setHue(h);
        }
    }

    public void handleClick(float x, float y, float width, float height, ColorValue colorValue, int mouseX, int mouseY) {
        float hueW = 8;
        float hueX = x + width - hueW;
        float satW = width - hueW - 3;

        if (isHovered(mouseX, mouseY, x, y, satW, height)) {
            editingColor = colorValue;
            editingSat = true;
            editingHue = false;
        } else if (isHovered(mouseX, mouseY, hueX, y, hueW, height)) {
            editingColor = colorValue;
            editingHue = true;
            editingSat = false;
        }
    }

    public void toggle(ColorValue colorValue) {
        if (expandedColorValue == colorValue) {
            expandedColorValue = null;
        } else {
            expandedColorValue = colorValue;
        }
    }

    public boolean isExpanded(ColorValue colorValue) {
        return expandedColorValue == colorValue;
    }

    public void reset() {
        editingColor = null;
        editingHue = false;
        editingSat = false;
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
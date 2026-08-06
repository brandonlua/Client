package wtf.rania.gui.click.component;

import wtf.rania.client.modules.values.impl.ColorValue;
import wtf.rania.util.render.RenderUtil;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class ColorPicker {

    private ColorValue expandedColorValue = null;
    private ColorValue editingColor = null;
    private boolean editingHue = false;
    private boolean editingSat = false;
    private boolean editingAlpha = false;

    public void render(float x, float y, float width, float height, ColorValue colorValue, int mouseX, int mouseY) {
        float barW = 8;
        float gap = 3;
        float alphaX = x + width - barW;
        float hueX = alphaX - barW - gap;
        float satW = hueX - x - gap;

        for (int py = 0; py < (int) height; py++) {
            for (int px = 0; px < (int) satW; px++) {
                float s = px / satW;
                float b = 1.0f - (py / height);
                Color c = Color.getHSBColor(colorValue.getHue(), s, b);
                RenderUtil.drawRect(x + px, y + py, 1, 1, c);
            }
        }

        for (int i = 0; i < (int) height; i++) {
            float h = i / height;
            Color hc = Color.getHSBColor(h, 1.0f, 1.0f);
            RenderUtil.drawRect(hueX, y + i, barW, 1, hc);
        }

        Color baseColor = colorValue.get();
        for (int i = 0; i < (int) height; i++) {
            float a = 1.0f - (i / height);
            Color ac = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (a * 255));
            RenderUtil.drawRect(alphaX, y + i, barW, 1, new Color(40, 40, 40));
            RenderUtil.drawRect(alphaX, y + i, barW, 1, ac);
        }

        RenderUtil.drawRoundedRect(x, y, satW, height, 2, new Color(0, 0, 0, 0));
        RenderUtil.drawRoundedRect(hueX, y, barW, height, 2, new Color(0, 0, 0, 0));
        RenderUtil.drawRoundedRect(alphaX, y, barW, height, 2, new Color(0, 0, 0, 0));
        RenderUtil.drawRoundedRect(x - 1, y - 1, satW + 2, height + 2, 2, new Color(255, 255, 255, 15));
        RenderUtil.drawRoundedRect(hueX - 1, y - 1, barW + 2, height + 2, 2, new Color(255, 255, 255, 15));
        RenderUtil.drawRoundedRect(alphaX - 1, y - 1, barW + 2, height + 2, 2, new Color(255, 255, 255, 15));

        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float dotX = x + hsb[1] * satW;
        float dotY = y + (1.0f - hsb[2]) * height;
        RenderUtil.drawRoundedRect(dotX - 3, dotY - 3, 6, 6, 3, new Color(255, 255, 255, 200));
        RenderUtil.drawRoundedRect(dotX - 2, dotY - 2, 4, 4, 2, baseColor);

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
        if (editingColor == colorValue && editingAlpha && Mouse.isButtonDown(0)) {
            float a = Math.max(0, Math.min(1.0f - ((mouseY - y) / height), 1));
            colorValue.setAlpha(a);
        }
    }

    public void handleClick(float x, float y, float width, float height, ColorValue colorValue, int mouseX, int mouseY) {
        float barW = 8;
        float gap = 3;
        float alphaX = x + width - barW;
        float hueX = alphaX - barW - gap;
        float satW = hueX - x - gap;

        if (isHovered(mouseX, mouseY, x, y, satW, height)) {
            editingColor = colorValue;
            editingSat = true;
            editingHue = false;
            editingAlpha = false;
        } else if (isHovered(mouseX, mouseY, hueX, y, barW, height)) {
            editingColor = colorValue;
            editingHue = true;
            editingSat = false;
            editingAlpha = false;
        } else if (isHovered(mouseX, mouseY, alphaX, y, barW, height)) {
            editingColor = colorValue;
            editingAlpha = true;
            editingHue = false;
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
        editingAlpha = false;
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
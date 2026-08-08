package wtf.rania.gui.components;

import wtf.rania.client.font.CFontRenderer;
import wtf.rania.utility.render.RenderUtil;

import java.awt.*;

public class ButtonComponent {
    public String text;
    public float x, y, width, height;
    public CFontRenderer font;
    private boolean onlyText = false;

    public ButtonComponent(String text, float x, float y, float width, float height, CFontRenderer font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = font;
    }

    public ButtonComponent(String text, float x, float y, CFontRenderer font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = font.getStringWidth(text);
        this.height = font.getHeight();
        this.font = font;
        onlyText = true;
    }

    public void draw(float mouseX, float mouseY) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (!onlyText) {
            RenderUtil.drawRect(x, y, width, height, hover ? new Color(40, 40, 40) : new Color(35, 35, 35));
            font.drawString(text, x + 3.5f, y + height / 2 - 2, Color.WHITE.getRGB());
        } else {
            font.drawString(text, x, y - 2, hover ? Color.LIGHT_GRAY.getRGB() : Color.WHITE.getRGB());
        }
    }

    public boolean click(float mouseX, float mouseY, int mouseButton) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (hover && mouseButton == 0) {
            return true;
        }
        return false;
    }
}
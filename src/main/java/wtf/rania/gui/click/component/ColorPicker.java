package wtf.rania.gui.click.component;

import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.values.impl.ColorValue;
import wtf.rania.utility.render.RenderUtil;

import java.awt.Color;

public class ColorPicker {

    public static float height = 13f;
    static float extraH = 60f;
    static float svSize = 50f;
    static float barW = 10f;
    static float spd = 14f;
    static float offset = 15f;

    ColorValue val;
    boolean open;
    float prog = 0f;
    long last = System.currentTimeMillis();

    boolean dragSv;
    boolean dragHue;
    boolean dragBr;

    public ColorPicker(ColorValue val) {
        this.val = val;
    }

    public float getHeight() {
        return height + extraH * ease(prog);
    }

    void tick() {
        long now = System.currentTimeMillis();
        float dt = (now - last) / 1000f;
        if (dt > 0.016f) dt = 0.016f;
        last = now;

        float t = 0f;
        if (open) t = 1f;

        if (prog < t) {
            prog += dt * spd;
            if (prog > t) prog = t;
        } else if (prog > t) {
            prog -= dt * spd;
            if (prog < t) prog = t;
        }
    }

    float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public void draw(float x, float y, float w, CFontRenderer font) {
        tick();
        float e = ease(prog);

        RenderUtil.drawRect(x, y, w, height, GuiTheme.BG_DARK);
        font.drawString(val.getName(), x + 4, y + 2, GuiTheme.TEXT);

        Color col = val.get();
        float sw = 8;
        String hex = String.format("#%06X", col.getRGB() & 0xFFFFFF);
        float hw = font.getStringWidth(hex);
        float swX = x + w - sw - 4;
        float hexX = swX - hw - 4;

        font.drawString(hex, hexX, y + 2, GuiTheme.TEXT_SECONDARY);
        RenderUtil.drawRect(swX, y + (height - sw) / 2f, sw, sw, col.getRGB());

        if (e < 0.02f) {
            return;
        }

        float exh = extraH * e;
        RenderUtil.drawRect(x, y + height, w, exh, GuiTheme.BG_DARK);

        float by = y + height + 4;
        float bx = x + offset;

        float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
        float hue = hsb[0];

        int hueCol = Color.HSBtoRGB(hue, 1f, 1f);
        RenderUtil.drawHorizontalGradientSideways(bx, by, svSize, svSize, 0xFFFFFFFF, hueCol | 0xFF000000);
        RenderUtil.drawGradientRect(bx, by, svSize, svSize, 0x00000000, 0xFF000000);

        float hueX = bx + svSize + 6;
        int[] cols = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        float segH = svSize / 6f;
        for (int i = 0; i < 6; i++) {
            RenderUtil.drawGradientRect(hueX, by + i * segH, barW, segH, cols[i], cols[i + 1]);
        }

        float brX = hueX + barW + 6;
        RenderUtil.drawGradientRect(brX, by, barW, svSize, 0xFFFFFFFF, 0xFF000000);
    }

    public void mouseClick(float mx, float my, float x, float y, float w) {
        if (mx >= x && mx <= x + w && my >= y && my <= y + height) {
            open = !open;
            if (!open) {
                dragSv = false;
                dragHue = false;
                dragBr = false;
            }
            return;
        }

        if (!open) return;

        float by = y + height + 4;
        float bx = x + offset;
        float hueX = bx + svSize + 6;
        float brX = hueX + barW + 6;

        if (mx >= bx && mx <= bx + svSize && my >= by && my <= by + svSize) {
            dragSv = true;
            setSv(mx, my, bx, by);
        } else if (mx >= hueX && mx <= hueX + barW && my >= by && my <= by + svSize) {
            dragHue = true;
            setHue(my, by);
        } else if (mx >= brX && mx <= brX + barW && my >= by && my <= by + svSize) {
            dragBr = true;
            setBr(my, by);
        }
    }

    public void mouseDrag(float mx, float my, float x, float y) {
        if (!open) return;

        float by = y + height + 4;
        float bx = x + offset;

        if (dragSv) setSv(mx, my, bx, by);
        if (dragHue) setHue(my, by);
        if (dragBr) setBr(my, by);
    }

    public void mouseRelease() {
        dragSv = false;
        dragHue = false;
        dragBr = false;
    }

    void setSv(float mx, float my, float bx, float by) {
        float sat = clamp((mx - bx) / svSize);
        float bright = 1f - clamp((my - by) / svSize);
        val.setSaturation(sat);
        val.setBrightness(bright);
    }

    void setHue(float my, float by) {
        float hue = clamp((my - by) / svSize);
        val.setHue(hue);
    }

    void setBr(float my, float by) {
        float bright = 1f - clamp((my - by) / svSize);
        val.setBrightness(bright);
    }

    float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public ColorValue getSetting() {
        return val;
    }
}
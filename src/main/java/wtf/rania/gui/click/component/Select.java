package wtf.rania.gui.click.component;

import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.utility.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.IntBuffer;

public class Select {

    public static final float HEIGHT = 13f;
    private static final float OPTION_HEIGHT = 12f;
    private static final float SHIFT_AMOUNT = 3f;
    private static final float SPEED = 14f;

    private final ModeValue setting;
    private boolean expanded;
    private float openProgress = 0f;
    private long lastFrame = System.currentTimeMillis();
    private final float[] optionHover;

    private boolean previousScissorEnabled;
    private final int[] previousScissor = new int[4];

    public Select(ModeValue setting) {
        this.setting = setting;
        this.optionHover = new float[setting.getModes().length];
    }

    private float ease(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private int lerpColor(int from, int to, float t) {
        float a1 = (from >>> 24) / 255f, r1 = (from >> 16 & 255) / 255f, g1 = (from >> 8 & 255) / 255f, b1 = (from & 255) / 255f;
        float a2 = (to >>> 24) / 255f, r2 = (to >> 16 & 255) / 255f, g2 = (to >> 8 & 255) / 255f, b2 = (to & 255) / 255f;
        int a = (int) ((a1 + (a2 - a1) * t) * 255f);
        int r = (int) ((r1 + (r2 - r1) * t) * 255f);
        int g = (int) ((g1 + (g2 - g1) * t) * 255f);
        int b = (int) ((b1 + (b2 - b1) * t) * 255f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void update(float mouseX, float mouseY, float x, float y, float width) {
        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastFrame) / 1000f, 0.016f);
        lastFrame = now;

        float target = expanded ? 1f : 0f;
        if (openProgress < target) {
            openProgress = Math.min(target, openProgress + delta * SPEED);
        } else if (openProgress > target) {
            openProgress = Math.max(target, openProgress - delta * SPEED);
        }

        String[] modes = setting.getModes();
        float eased = ease(openProgress);
        float oy = y + HEIGHT;

        for (int i = 0; i < modes.length; i++) {
            boolean hovering = eased > 0.05f && mouseX >= x && mouseX <= x + width && mouseY >= oy && mouseY <= oy + OPTION_HEIGHT;
            float itemTarget = hovering ? 1f : 0f;
            if (optionHover[i] < itemTarget) {
                optionHover[i] = Math.min(itemTarget, optionHover[i] + delta * 12f);
            } else if (optionHover[i] > itemTarget) {
                optionHover[i] = Math.max(itemTarget, optionHover[i] - delta * 12f);
            }
            oy += OPTION_HEIGHT;
        }
    }

    public float getHeight() {
        return HEIGHT + setting.getModes().length * OPTION_HEIGHT * ease(openProgress);
    }

    private void beginScissor(float sx, float sy, float sw, float sh) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        float scale = res.getScaleFactor();

        int scissorX = (int) (sx * scale);
        int scissorY = (int) (mc.displayHeight - (sy + sh) * scale);
        int scissorWidth = (int) Math.max(sw * scale, 1);
        int scissorHeight = (int) Math.max(sh * scale + 2, 1);

        previousScissorEnabled = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
        if (previousScissorEnabled) {
            IntBuffer box = BufferUtils.createIntBuffer(16);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            previousScissor[0] = box.get(0);
            previousScissor[1] = box.get(1);
            previousScissor[2] = box.get(2);
            previousScissor[3] = box.get(3);

            int x1 = Math.max(scissorX, previousScissor[0]);
            int y1 = Math.max(scissorY, previousScissor[1]);
            int x2 = Math.min(scissorX + scissorWidth, previousScissor[0] + previousScissor[2]);
            int y2 = Math.min(scissorY + scissorHeight, previousScissor[1] + previousScissor[3]);

            scissorX = x1;
            scissorY = y1;
            scissorWidth = Math.max(x2 - x1, 0);
            scissorHeight = Math.max(y2 - y1, 0);
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void endScissor() {
        if (previousScissorEnabled) {
            GL11.glScissor(previousScissor[0], previousScissor[1], previousScissor[2], previousScissor[3]);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    public void draw(float x, float y, float width, CFontRenderer font, Color theme) {
        float eased = ease(openProgress);

        RenderUtil.drawRect(x, y, width, HEIGHT, GuiTheme.BG_DARK);
        font.drawString(setting.getName(), x + 4, y + 3, GuiTheme.TEXT);

        String value = setting.get();
        float valueWidth = font.getStringWidth(value);
        font.drawString(value, x + width - valueWidth - 4, y + 3, GuiTheme.TEXT_SECONDARY);

        String[] modes = setting.getModes();
        float listHeight = modes.length * OPTION_HEIGHT * eased;

        if (listHeight < 0.5f) {
            return;
        }

        beginScissor(x, y + HEIGHT, width, listHeight);

        float oy = y + HEIGHT;
        String current = setting.get();

        for (int i = 0; i < modes.length; i++) {
            String option = modes[i];
            boolean selected = option.equals(current);
            float hoverEased = ease(optionHover[i]);

            int textColor = lerpColor(0xFF888888, 0xFFFFFFFF, hoverEased);
            float shiftX = SHIFT_AMOUNT * hoverEased;

            RenderUtil.drawRect(x, oy, width, OPTION_HEIGHT, GuiTheme.BG);
            RenderUtil.drawBorder(x, oy, width, OPTION_HEIGHT, 1f, GuiTheme.OUTLINE);
            font.drawString(option, x + 4 + shiftX, oy + 2, selected ? 0xFFFFFFFF : textColor);
            oy += OPTION_HEIGHT;
        }

        endScissor();
    }

    public void mouseClick(float mouseX, float mouseY, float x, float y, float width, int button) {
        if (button != 0) {
            return;
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            expanded = !expanded;
            return;
        }

        float eased = ease(openProgress);
        String[] modes = setting.getModes();
        float listHeight = modes.length * OPTION_HEIGHT * eased;

        if (listHeight < 0.5f) {
            return;
        }

        if (mouseX < x || mouseX > x + width || mouseY < y + HEIGHT || mouseY > y + HEIGHT + listHeight) {
            return;
        }

        int index = (int) ((mouseY - (y + HEIGHT)) / OPTION_HEIGHT);
        if (index >= 0 && index < modes.length) {
            setting.set(modes[index]);
            expanded = false;
        }
    }

    public ModeValue getSetting() {
        return setting;
    }
}
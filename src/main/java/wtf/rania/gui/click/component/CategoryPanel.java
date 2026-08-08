package wtf.rania.gui.click.component;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.utility.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {

    public static final float WIDTH = 115f;
    public static final float HEADER = 14f;

    public float x;
    public float y;
    public boolean moving;
    private float moveOffX;
    private float moveOffY;
    private boolean expanded = true;
    private float openProgress = 0f;
    private long lastFrame = System.currentTimeMillis();

    private final Category category;
    private final List<ModuleButton> moduleButtons = new ArrayList<>();

    public CategoryPanel(Category category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        if (Client.INSTANCE != null && Client.INSTANCE.getModuleManager() != null) {
            for (Module module : Client.INSTANCE.getModuleManager().getModules(category)) {
                if (module != null) {
                    moduleButtons.add(new ModuleButton(module));
                }
            }
        }
    }

    public Category getCategory() {
        return category;
    }

    public List<ModuleButton> getModuleButtons() {
        return moduleButtons;
    }

    private void updateAnimation() {
        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastFrame) / 1000f, 0.016f);
        lastFrame = now;

        float target = expanded ? 1f : 0f;
        float speed = 13f;

        if (openProgress < target) {
            openProgress = Math.min(target, openProgress + delta * speed);
        } else if (openProgress > target) {
            openProgress = Math.max(target, openProgress - delta * speed);
        }
    }

    private float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public void draw(int mouseX, int mouseY, CFontRenderer font) {
        updateAnimation();
        float eased = ease(openProgress);

        float contentHeight = 0f;
        float updateOffset = y + HEADER;
        for (ModuleButton button : moduleButtons) {
            button.update(mouseX, mouseY, x, updateOffset, WIDTH);
            contentHeight += button.getHeight();
            updateOffset += button.getHeight();
        }

        RenderUtil.drawRect(x, y, WIDTH, HEADER, GuiTheme.BG_DARK);
        font.drawString(format(category), x + 4, y + 3, GuiTheme.TEXT);

        float visibleHeight = contentHeight * eased;
        if (visibleHeight > 0.5f) {
            beginScissor(x, y + HEADER, WIDTH, visibleHeight);
            float offset = y + HEADER;
            for (ModuleButton button : moduleButtons) {
                button.draw(x, offset, WIDTH, font, mouseX, mouseY);
                offset += button.getHeight();
            }
            endScissor();
            RenderUtil.drawBorder(x, y, WIDTH, HEADER + visibleHeight, 1f, 0xFF000000);
        }
    }

    private void beginScissor(float sx, float sy, float sw, float sh) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        float scale = res.getScaleFactor();

        int scissorX = (int) (sx * scale);
        int scissorY = (int) (mc.displayHeight - (sy + sh) * scale);
        int scissorWidth = (int) Math.max(sw * scale, 1);
        int scissorHeight = (int) Math.max(sh * scale + 2, 1);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void mouseClick(float mouseX, float mouseY, int mouseButton) {
        if (mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER) {
            if (mouseButton == 0) {
                moving = true;
                moveOffX = mouseX - x;
                moveOffY = mouseY - y;
            } else if (mouseButton == 1) {
                expanded = !expanded;
            }
            return;
        }
        if (!expanded) {
            return;
        }

        float offset = y + HEADER;
        for (ModuleButton moduleButton : moduleButtons) {
            if (moduleButton.click(mouseX, mouseY, x, offset, WIDTH, mouseButton)) {
                return;
            }
            offset += moduleButton.getHeight();
        }
    }

    public void mouseDrag(float mouseX, float mouseY) {
        if (moving) {
            x = mouseX - moveOffX;
            y = mouseY - moveOffY;
        }
        if (!expanded) {
            return;
        }

        float offset = y + HEADER;
        for (ModuleButton moduleButton : moduleButtons) {
            moduleButton.mouseDrag(mouseX, mouseY, x, offset, WIDTH);
            offset += moduleButton.getHeight();
        }
    }

    public void mouseRelease() {
        moving = false;
        for (ModuleButton moduleButton : moduleButtons) {
            moduleButton.mouseRelease();
        }
    }

    public static String format(Category category) {
        String[] split = category.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String s : split) {
            builder.append(Character.toUpperCase(s.charAt(0)))
                    .append(s.substring(1))
                    .append(" ");
        }
        return builder.toString().trim();
    }
}
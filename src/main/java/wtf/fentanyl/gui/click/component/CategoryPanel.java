package wtf.fentanyl.gui.click.component;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.impl.render.HUD;
import wtf.fentanyl.client.modules.impl.render.PostProcessing;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.modules.values.impl.TextValue;
import wtf.fentanyl.util.render.RenderUtil;
import wtf.fentanyl.util.render.shaders.impl.Blur;
import wtf.fentanyl.util.render.shaders.impl.Shadow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryPanel {

    private Category category;
    private float x;
    private float y;
    private float width = 130;
    private float headerHeight = 16;
    private float scroll = 0;
    private List<ModuleButton> moduleButtons = new ArrayList<>();
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);

    private static final float RADIUS = 2f;

    private static final Map<Category, ResourceLocation> ICONS = new HashMap<>();

    static {
        ICONS.put(Category.PLAYER, new ResourceLocation("minecraft", "client/icon/Player.png"));
        ICONS.put(Category.WORLD, new ResourceLocation("minecraft", "client/icon/World.png"));
        ICONS.put(Category.RENDER, new ResourceLocation("minecraft", "client/icon/Visual.png"));
        ICONS.put(Category.MOVEMENT, new ResourceLocation("minecraft", "client/icon/Movement.png"));
        ICONS.put(Category.COMBAT, new ResourceLocation("minecraft", "client/icon/Combat.png"));
        ICONS.put(Category.MISC, new ResourceLocation("minecraft", "client/icon/misc.png"));
    }

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

    private CFontRenderer getFont(CFontRenderer fallback) {
        if (Client.INSTANCE == null || Client.INSTANCE.getModuleManager() == null) {
            return fallback;
        }
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        return hud != null && hud.fr != null ? hud.fr : fallback;
    }

    public void render(int mouseX, int mouseY, CFontRenderer font, Color themeColor, Module listeningModule, ModeValue expandedMode, ColorPicker colorPicker, TextValue editingText) {
        CFontRenderer currentFont = getFont(font);
        if (currentFont == null) {
            // Without a font there is nothing safe to draw; skip this frame for the panel.
            return;
        }
        if (themeColor == null) {
            themeColor = Color.WHITE;
        }

        PostProcessing postProcessing = Client.INSTANCE != null && Client.INSTANCE.getModuleManager() != null
                ? (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing")
                : null;

        boolean ppOn = postProcessing != null && postProcessing.isToggled();
        boolean blurOn = ppOn && postProcessing.blur.get();
        boolean shadowOn = ppOn && postProcessing.shadow.get();

        float totalContentHeight = calculateContentHeight(expandedMode, colorPicker);
        float maxPanelHeight = 300;
        float displayHeight = Math.min(totalContentHeight + 2, maxPanelHeight);
        float totalHeight = headerHeight + displayHeight;

        Color bg = new Color(28, 28, 28, blurOn ? 95 : 240);

        if (shadowOn) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, new Color(0, 0, 0, 255));
            shadowFramebuffer.unbindFramebuffer();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.0f);
            GlStateManager.enableBlend();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
            GlStateManager.disableBlend();
        }

        if (blurOn) {
            Blur.startBlur();
            RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, bg);
            Blur.endBlur(postProcessing.blurRadius.get(), 1);
        }

        RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, bg);

        RenderUtil.drawRect(x + 1, y + headerHeight - 0.5f, width - 2, 0.5f, new Color(255, 255, 255, 15));

        ResourceLocation icon = ICONS.get(category);
        if (icon != null) {
            RenderUtil.drawImage(icon, x + 3, y + 2, 12, 12);
        }

        String categoryName = category.name().charAt(0) + category.name().substring(1).toLowerCase();
        currentFont.drawString(categoryName, x + 18, y + 4, new Color(200, 200, 200).getRGB());

        float maxScroll = Math.max(0, totalContentHeight - maxPanelHeight + 2);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scale = sr.getScaleFactor();

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(
                (int) (x * scale),
                (int) ((Minecraft.getMinecraft().displayHeight / scale - (y + headerHeight + displayHeight)) * scale),
                (int) (width * scale),
                (int) (displayHeight * scale)
        );

        float moduleY = y + headerHeight - scroll;

        for (ModuleButton button : moduleButtons) {
            button.updateAnimation();
            button.render(x, moduleY, width, mouseX, mouseY, currentFont, themeColor, listeningModule, expandedMode, colorPicker, editingText);
            moduleY += 13;
            moduleY += button.renderSettings(x, moduleY, width, mouseX, mouseY, currentFont, themeColor, listeningModule, expandedMode, colorPicker, editingText);
        }

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, new Color(255, 255, 255, 10));
    }

    public void handleClick(int mouseX, int mouseY, int mouseButton, Module[] listeningModule, ModeValue[] expandedMode, ColorPicker colorPicker, TextValue[] editingText) {
        float moduleY = y + headerHeight - scroll;
        for (ModuleButton button : moduleButtons) {
            button.handleClick(x, moduleY, width, mouseX, mouseY, mouseButton, listeningModule, expandedMode, colorPicker, editingText);
            moduleY += button.calculateHeight(expandedMode[0], colorPicker);
        }
    }

    public void scroll(float amount) {
        scroll += amount;
    }

    public boolean isHoveringPanel(int mouseX, int mouseY) {
        return isHovered(mouseX, mouseY, x, y + headerHeight, width, 300);
    }

    public boolean isHoveringHeader(int mouseX, int mouseY) {
        return isHovered(mouseX, mouseY, x, y, width, headerHeight);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public Category getCategory() { return category; }

    private float calculateContentHeight(ModeValue expandedMode, ColorPicker colorPicker) {
        float totalHeight = 0;
        for (ModuleButton button : moduleButtons) {
            totalHeight += button.calculateHeight(expandedMode, colorPicker);
        }
        return totalHeight;
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
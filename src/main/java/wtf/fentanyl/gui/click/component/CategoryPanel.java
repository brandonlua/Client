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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {

    private Category category;
    private float x;
    private float y;
    private float width = 130;
    private float headerHeight = 13;
    private float scroll = 0;
    private List<ModuleButton> moduleButtons = new ArrayList<>();
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);

    private static final float RADIUS = 3f;

    public CategoryPanel(Category category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module module : Client.INSTANCE.getModuleManager().getModules(category)) {
            moduleButtons.add(new ModuleButton(module));
        }
    }

    private CFontRenderer getFont(CFontRenderer fallback) {
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        return hud != null && hud.fr != null ? hud.fr : fallback;
    }

    public void render(int mouseX, int mouseY, CFontRenderer font, Color themeColor, Module listeningModule, ModeValue expandedMode, ColorPicker colorPicker, TextValue editingText) {
        CFontRenderer currentFont = getFont(font);

        PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        boolean ppOn = postProcessing != null && postProcessing.isToggled();
        boolean blurOn = ppOn && postProcessing.blur.get();
        boolean shadowOn = ppOn && postProcessing.shadow.get();

        float totalContentHeight = calculateContentHeight(expandedMode, colorPicker);
        float maxPanelHeight = 300;
        float displayHeight = Math.min(totalContentHeight + 2, maxPanelHeight);
        float totalHeight = headerHeight + displayHeight;

        Color bgColor = new Color(30, 30, 30, blurOn ? 100 : 245);

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
            RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, bgColor);
            Blur.endBlur(postProcessing.blurRadius.get(), 1);
        }

        RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, bgColor);

        RenderUtil.drawRect(x, y + headerHeight - 1, width, 1, new Color(255, 255, 255, 18));

        String categoryName = category.name();
        categoryName = categoryName.charAt(0) + categoryName.substring(1).toLowerCase();
        currentFont.drawString(categoryName, x + 5, y + 3, new Color(210, 210, 210).getRGB());

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

        RenderUtil.drawRoundedRect(x, y, width, totalHeight, RADIUS, new Color(255, 255, 255, 12));
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
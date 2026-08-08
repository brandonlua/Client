package wtf.rania.client.widget;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.impl.render.PostProcessing;
import wtf.rania.utility.render.RenderUtil;
import wtf.rania.utility.render.shaders.impl.Blur;
import wtf.rania.utility.render.shaders.impl.Shadow;
import wtf.rania.utility.render.shaders.impl.Bloom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class SessionInfoWidget {

    public int killed = 0;
    public int won = 0;

    private long sessionStart;
    private CFontRenderer font;
    private CFontRenderer fontBold;
    private Minecraft mc;

    private float width = 140;
    private float height = 80;

    private float dragX = -1;
    private float dragY = -1;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;
    private boolean dragging = false;

    public float getDragX() {
        return dragX;
    }

    public void setDragX(float x) {
        this.dragX = x;
    }

    public float getDragY() {
        return dragY;
    }

    public void setDragY(float y) {
        this.dragY = y;
    }

    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);
    private Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public SessionInfoWidget() {
        this.mc = Minecraft.getMinecraft();
        sessionStart = System.currentTimeMillis();
        font = new CFontRenderer(new Font("Arial", Font.PLAIN, 16), true, true);
        fontBold = new CFontRenderer(new Font("Arial", Font.BOLD, 18), true, true);
    }

    public void render(int themeColor) {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        if (dragX == -1 && dragY == -1) {
            dragX = 4;
            dragY = 17;
        }

        float x = dragX;
        float y = dragY;

        int mouseX = Mouse.getX() * screenWidth / mc.displayWidth;
        int mouseY = screenHeight - Mouse.getY() * screenHeight / mc.displayHeight - 1;

        if (mc.currentScreen != null) {
            if (Mouse.isButtonDown(0)) {
                if (!dragging && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    dragging = true;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                }
                if (dragging) {
                    dragX = mouseX - dragOffsetX;
                    dragY = mouseY - dragOffsetY;
                    x = dragX;
                    y = dragY;
                }
            } else {
                dragging = false;
            }
        }

        PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.bloom.get()) {
            stencilFramebuffer = RenderUtil.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);
            RenderUtil.resetColor();
            renderContent(x, y, themeColor, false);
            RenderUtil.resetColor();
            stencilFramebuffer.unbindFramebuffer();

            Bloom.renderBlur(stencilFramebuffer.framebufferTexture, (int) postProcessing.bloomRadius.get(), (int) postProcessing.bloomOffset.get());
        }

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            renderContent(x, y, themeColor, false);
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
        }

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.blur.get()) {
            Blur.startBlur();
            GlStateManager.pushMatrix();
            renderContent(x, y, themeColor, false);
            GlStateManager.popMatrix();
            Blur.endBlur(postProcessing.blurRadius.get(), 1);
        }

        renderContent(x, y, themeColor, dragging);
    }

    private void renderContent(float x, float y, int themeColor, boolean showOutline) {
        RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
        RenderUtil.drawRect(x, y, width, 1, themeColor);

        if (showOutline) {
            RenderUtil.drawRect(x - 1, y - 1, width + 2, 1, Color.WHITE.getRGB());
            RenderUtil.drawRect(x - 1, y + height, width + 2, 1, Color.WHITE.getRGB());
            RenderUtil.drawRect(x - 1, y, 1, height, Color.WHITE.getRGB());
            RenderUtil.drawRect(x + width, y, 1, height, Color.WHITE.getRGB());
        }

        float left = x + 2;
        float right = x + width - 2;
        float textY = y + 4;

        fontBold.drawStringWithShadow("Current Session", left, textY, -1);

        textY += 14;
        font.drawStringWithShadow("Play Time:", left, textY, -1);
        font.drawStringWithShadow(getSessionTime(), right - font.getStringWidth(getSessionTime()), textY, -1);

        textY += 12;
        font.drawStringWithShadow("Games Won:", left, textY, -1);
        font.drawStringWithShadow(won + "", right - font.getStringWidth(won + ""), textY, -1);

        textY += 12;
        font.drawStringWithShadow("Players Killed:", left, textY, -1);
        font.drawStringWithShadow(killed + "", right - font.getStringWidth(killed + ""), textY, -1);
    }

    private String getSessionTime() {
        long elapsed = System.currentTimeMillis() - sessionStart;
        long seconds = (elapsed / 1000) % 60;
        long minutes = (elapsed / (1000 * 60)) % 60;
        long hours = (elapsed / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
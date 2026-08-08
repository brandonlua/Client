package wtf.rania.gui.notification;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.client.modules.impl.render.PostProcessing;
import wtf.rania.utility.animations.Animation;
import wtf.rania.utility.animations.Translate;
import wtf.rania.utility.animations.impl.EaseOutSine;
import wtf.rania.utility.math.TimerUtil;
import wtf.rania.utility.render.RenderUtil;
import wtf.rania.utility.render.shaders.impl.Blur;
import wtf.rania.utility.render.shaders.impl.Shadow;
import wtf.rania.utility.render.shaders.impl.Bloom;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import java.awt.*;

@Getter
public class Notification {

    private final NotificationType notificationType;
    private final String title, description;
    private final float time;
    private final TimerUtil TimerUtil;
    private final Animation animation;
    private final Translate translate;
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);
    private Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public float x;
    public float y;
    private boolean dragging = false;
    private float dragX, dragY;

    public Notification(NotificationType type, String title, String description) {
        this(type, title, description, Client.INSTANCE.getNotificationManager().getToggleTime());
    }

    public Notification(NotificationType type, String title, String description, float time) {
        this.title = title;
        this.description = description;
        this.time = (long) (time * 1000);
        TimerUtil = new TimerUtil();
        this.notificationType = type;
        this.animation = new EaseOutSine(250, 1);
        final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        this.x = sr.getScaledWidth() - (float) this.getWidth();
        this.y = sr.getScaledHeight() - (float) getHeight();
        this.translate = new Translate(x, y);
    }

    public void renderShaders(float x, float y, float width, float height, boolean showOutline) {
        PostProcessing penis = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (penis != null && penis.isToggled() && penis.bloom.get()) {
            stencilFramebuffer = RenderUtil.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);
            RenderUtil.resetColor();
            RenderUtil.drawRoundedRect(x, y, width, height, 4, Color.WHITE);
            RenderUtil.resetColor();
            stencilFramebuffer.unbindFramebuffer();

            Bloom.renderBlur(stencilFramebuffer.framebufferTexture, (int) penis.bloomRadius.get(), (int) penis.bloomOffset.get());
        }

        if (penis != null && penis.isToggled() && penis.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            RenderUtil.drawRoundedRect(x, y, width, height, 4, Color.WHITE);
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) penis.shadowRadius.get(), 1);
        }

        if (penis != null && penis.isToggled() && penis.blur.get()) {
            Blur.startBlur();
            GlStateManager.pushMatrix();
            RenderUtil.drawRoundedRect(x, y, width, height, 4, Color.WHITE);
            GlStateManager.popMatrix();
            Blur.endBlur(penis.blurRadius.get(), 1);
        }

        if (showOutline) {
            RenderUtil.drawRect(x - 1, y - 1, width + 2, 1, Color.WHITE.getRGB());
            RenderUtil.drawRect(x - 1, y + height, width + 2, 1, Color.WHITE.getRGB());
            RenderUtil.drawRect(x - 1, y - 1, 1, height + 2, Color.WHITE.getRGB());
            RenderUtil.drawRect(x + width, y - 1, 1, height + 2, Color.WHITE.getRGB());
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
            translate.setX(x);
            translate.setY(y);
        }
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + getWidth() && mouseY >= y && mouseY <= y + getHeight();
    }

    public boolean isDragging() {
        return dragging;
    }

    public double getWidth() {
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        if (hud == null || hud.fr == null) return 100;

        CFontRenderer fr = hud.fr;
        return Math.max(100.0f, Math.max(fr.getStringWidth(getTitle()), fr.getStringWidth(getDescription())) + 70);
    }

    public double getHeight() {
        return 30;
    }
}
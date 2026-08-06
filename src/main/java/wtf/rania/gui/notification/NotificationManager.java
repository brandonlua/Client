package wtf.rania.gui.notification;

import wtf.rania.Client;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.client.modules.impl.render.Notification;
import wtf.rania.client.modules.impl.render.PostProcessing;
import wtf.rania.util.animations.Animation;
import wtf.rania.util.animations.Direction;
import wtf.rania.util.animations.Translate;
import wtf.rania.util.render.RenderUtil;
import wtf.rania.util.render.shaders.impl.Blur;
import wtf.rania.util.render.shaders.impl.Shadow;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@Getter
public class NotificationManager {
    private final Deque<wtf.rania.gui.notification.Notification> notifications = new ConcurrentLinkedDeque<>();

    @Setter
    private float toggleTime = 2;

    private final Minecraft mc = Minecraft.getMinecraft();
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);

    private final ResourceLocation okayIcon = new ResourceLocation("client/img/noti/okay.png");
    private final ResourceLocation warningIcon = new ResourceLocation("client/img/noti/flag.png");
    private final ResourceLocation spotifyIcon = new ResourceLocation("client/img/noti/spotify.png");
    private final ResourceLocation infoIcon = new ResourceLocation("client/img/noti/info.png");
    private final ResourceLocation errorIcon = new ResourceLocation("client/img/noti/error.png");
    private final ResourceLocation notifyIcon = new ResourceLocation("client/img/noti/notify.png");

    public void post(NotificationType type, String title, String description) {
        post(new wtf.rania.gui.notification.Notification(type, title, description));
    }

    public void post(NotificationType type, String title, String description, float time) {
        post(new wtf.rania.gui.notification.Notification(type, title, description, time));
    }

    public void post(NotificationType type, String title) {
        post(new wtf.rania.gui.notification.Notification(type, title, title));
    }

    private void post(wtf.rania.gui.notification.Notification notification) {
        notifications.add(notification);
    }

    public void moduleToggled(Module module, boolean enabled) {
        if (enabled) {
            post(NotificationType.OKAY, module.getName(), "Enabled");
        } else {
            post(NotificationType.INFO, module.getName(), "Disabled");
        }
    }

    public void flagged(Module module) {
        post(NotificationType.WARNING, "Warning", "You've been flagged by the 20000$ anticheat");
        if (module != null && module.isToggled()) {
            module.toggle();
            post(NotificationType.ERROR, module.getName() + " Disabled", "Disabled due to config issues");
        }
    }

    public void publish(ScaledResolution sr, boolean shader) {
        Notification notifModule = (Notification) Client.INSTANCE.getModuleManager().getModule("Notification");
        if (notifModule == null || !notifModule.isToggled()) return;

        PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            renderNotifications(sr, true);
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
        }

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.blur.get()) {
            Blur.startBlur();
            GlStateManager.pushMatrix();
            renderNotifications(sr, true);
            GlStateManager.popMatrix();
            Blur.endBlur(postProcessing.blurRadius.get(), 1);
        }

        renderNotifications(sr, false);
    }

    private ResourceLocation getIconForType(NotificationType type) {
        switch (type) {
            case OKAY:
                return okayIcon;
            case WARNING:
                return warningIcon;
            case INFO:
                return errorIcon;
            case ERROR:
                return errorIcon;
            default:
                return notifyIcon;
        }
    }

    private void renderNotifications(ScaledResolution sr, boolean shader) {
        float yOffset = 0;
        for (wtf.rania.gui.notification.Notification notification : getNotifications()) {
            float width = (float) notification.getWidth();
            float height = (float) notification.getHeight();

            Animation animation = notification.getAnimation();
            animation.setDirection(notification.getTimerUtil().hasTimeElapsed((long) notification.getTime()) ? Direction.BACKWARDS : Direction.FORWARDS);

            if (notification.getAnimation().finished(Direction.BACKWARDS)) {
                getNotifications().remove(notification);
                continue;
            }

            if (!animation.finished(Direction.BACKWARDS)) {
                HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");

                Translate translate = notification.getTranslate();
                int scaledHeight = sr.getScaledHeight();
                int scaledWidth = sr.getScaledWidth();
                float y = scaledHeight - (mc.currentScreen instanceof GuiChat ? 45 : 31) - yOffset;

                if (!notification.getTimerUtil().hasTimeElapsed(notification.getTime())) {
                    translate.translate((scaledWidth - width), y);
                } else {
                    translate.translate(scaledWidth, y);
                }

                RenderUtil.drawRect((float) translate.getX(), (float) translate.getY(), width, height, shader ? Color.WHITE.getRGB() : new Color(0, 0, 0, 185).getRGB());

                if (!shader) {
                    float percentage = Math.min((notification.getTimerUtil().getTime() / notification.getTime()), 1);
                    RenderUtil.drawRect((float) (translate.getX() + (width * percentage)), (float) (translate.getY() + height - 1), width - (width * percentage), 1, notification.getNotificationType().getColor().getRGB());

                    ResourceLocation icon = getIconForType(notification.getNotificationType());
                    GlStateManager.pushMatrix();
                    GlStateManager.enableBlend();
                    if (notification.getNotificationType() == NotificationType.ERROR || notification.getNotificationType() == NotificationType.INFO) {
                        GlStateManager.color(1, 0, 0, 1);
                    } else {
                        GlStateManager.color(1, 1, 1, 1);
                    }
                    mc.getTextureManager().bindTexture(icon);
                    Gui.drawModalRectWithCustomSizedTexture((int) (translate.getX() + 5), (int) (translate.getY() + 7), 0, 0, 16, 16, 16, 16);
                    GlStateManager.color(1, 1, 1, 1);
                    GlStateManager.disableBlend();
                    GlStateManager.popMatrix();

                    hud.fr.drawString(notification.getTitle(), (float) (translate.getX() + 25), (float) (translate.getY() + 4.5), -1);
                    hud.fr.drawString(notification.getDescription(), (float) (translate.getX() + 25), (float) (translate.getY() + 15.5), -1);
                }

                yOffset += height + 5;
            }
        }
    }
}
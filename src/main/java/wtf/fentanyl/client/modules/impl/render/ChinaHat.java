package wtf.fentanyl.client.modules.impl.render;

import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.event.impl.Event3D;

import java.awt.*;

@ModuleInfo(name = "ChinaHat", category = Category.RENDER)
public class ChinaHat extends Module {

    @Subscribe
    private final Listener<Event3D> event3DListener = new Listener<>(event -> {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float partialTicks = event.getPartialTicks();

        double renderPosX = mc.getRenderManager().renderPosX,
                renderPosY = mc.getRenderManager().renderPosY,
                renderPosZ = mc.getRenderManager().renderPosZ;

        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != mc.thePlayer) continue;
            if (mc.gameSettings.thirdPersonView == 0 || player.isDead || player.isInvisible()) continue;

            GL11.glPushMatrix();

            double posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - renderPosX,
                    posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - renderPosY,
                    posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - renderPosZ;

            AxisAlignedBB bb = player.getEntityBoundingBox();
            double height = bb.maxY - bb.minY + 0.02;
            double radius = bb.maxX - bb.minX;

            float yaw = interpolate(player.prevRotationYaw, player.rotationYaw, partialTicks);
            float pitch = interpolate(player.prevRotationPitch, player.rotationPitch, partialTicks);

            GL11.glTranslated(posX, posY + height, posZ);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glRotated(yaw, 0, -1, 0);
            GL11.glRotated(pitch / 3.0, 1, 0, 0);
            GL11.glTranslated(0, 0, pitch / 270.0);
            GL11.glLineWidth(2);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i <= 180; i++) {
                applyColor(getHatColor(i * 4, 0.5f));
                GL11.glVertex3d(
                        -Math.sin(i * MathHelper.PI2 / 90) * radius,
                        -(player.isSneaking() ? 0.2 : 0) - 0.002,
                        Math.cos(i * MathHelper.PI2 / 90) * radius
                );
            }
            GL11.glEnd();

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            applyColor(getHatColor(4, 0.7f));
            GL11.glVertex3d(0, 0.3 - (player.isSneaking() ? 0.23 : 0), 0);
            for (int i = 0; i <= 180; i++) {
                applyColor(getHatColor(i * 4, 0.2f));
                GL11.glVertex3d(
                        -Math.sin(i * MathHelper.PI2 / 90) * radius,
                        -(player.isSneaking() ? 0.23f : 0),
                        Math.cos(i * MathHelper.PI2 / 90) * radius
                );
            }
            GL11.glVertex3d(0, 0.3 - (player.isSneaking() ? 0.23 : 0), 0);
            GL11.glEnd();

            GL11.glPopMatrix();
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glShadeModel(GL11.GL_FLAT);
    });

    private float interpolate(float prev, float current, float ticks) {
        return prev + (current - prev) * ticks;
    }

    private void applyColor(Color color) {
        GL11.glColor4f(
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                color.getAlpha() / 255f
        );
    }

    private Color getHatColor(int index, float alpha) {
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        Color base = (hud != null) ? hud.theme.get() : new Color(255, 50, 50);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float hue = (hsb[0] + (index % 360) / 360f) % 1f;
        Color cycled = Color.getHSBColor(hue, hsb[1], hsb[2]);
        return new Color(cycled.getRed(), cycled.getGreen(), cycled.getBlue(), (int)(alpha * 255));
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
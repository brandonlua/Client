package wtf.rania.client.modules.impl.render;
import org.lwjgl.opengl.GL11;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.Event2D;
import wtf.rania.utility.render.RenderUtil;
import wtf.rania.utility.render.shaders.impl.Blur;
import wtf.rania.utility.render.shaders.impl.Shadow;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.awt.*;

@ModuleInfo(name = "Radar", category = Category.RENDER)
public class RadarModule extends Module {

    private ModeValue mode = new ModeValue("Mode", new String[]{"Default", "Exhi", "Astolfo"}, "Default", this);
    private SliderValue size = new SliderValue("Size", 100F, 50F, 200F, this);

    private float dragX = -1;
    private float dragY = -1;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;
    private boolean dragging = false;

    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);

    @Subscribe
    private Listener<Event2D> event2DListener;

    public RadarModule() {
        event2DListener = new Listener<>(e -> {
            if (mc.thePlayer == null || mc.theWorld == null) return;

            ScaledResolution sr = new ScaledResolution(mc);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();

            if (dragX == -1 && dragY == -1) {
                dragX = screenWidth - size.get() - 10;
                dragY = screenHeight - size.get() - 10;
            }

            float x = dragX;
            float y = dragY;
            float width = size.get();
            float height = size.get();

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

            if (dragging) {
                RenderUtil.drawRect(x - 1, y - 1, width + 2, 1, Color.WHITE.getRGB());
                RenderUtil.drawRect(x - 1, y + height, width + 2, 1, Color.WHITE.getRGB());
                RenderUtil.drawRect(x - 1, y, 1, height, Color.WHITE.getRGB());
                RenderUtil.drawRect(x + width, y, 1, height, Color.WHITE.getRGB());
            }

            switch (mode.get()) {
                case "Default":
                    renderDefault(x, y, width, height);
                    break;
                case "Exhi":
                    renderExhi(x, y, width, height);
                    break;
                case "Astolfo":
                    renderAstolfo(x, y, width, height);
                    break;
            }
        });
    }

    private void renderDefault(float x, float y, float width, float height) {
        GL11.glPushMatrix();

        float cx = x + (width / 2f);
        float cy = y + (height / 2f);

        PostProcessing pp = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (pp != null && pp.isToggled() && pp.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            RenderUtil.drawBorderedRect(x, y, width, height, 1, 0xFF444444, 0xFF222222);
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) pp.shadowRadius.get(), 1);
        }

        if (pp != null && pp.isToggled() && pp.blur.get()) {
            Blur.startBlur();
            GlStateManager.pushMatrix();
            RenderUtil.drawBorderedRect(x, y, width, height, 1, 0xFF444444, 0xFF222222);
            GlStateManager.popMatrix();
            Blur.endBlur(pp.blurRadius.get(), 1);
        }

        RenderUtil.drawBorderedRect(x, y, width, height, 1, 0xFF444444, 0xFF222222);
        RenderUtil.drawRect(x + (width / 2f) - 0.5f, y, 1, height, 0xFF444444);
        RenderUtil.drawRect(x, y + (height / 2f) - 0.5f, width, 1, 0xFF444444);
        RenderUtil.drawRect(cx - 1, cy - 1, 2, 2, 0xFFFFFF00);

        int maxDist = (int) (size.get() / 2);
        for (Entity entity : mc.theWorld.loadedEntityList) {
            float partialTicks = 1.0F;

            double dx = interpolate(entity.prevPosX, entity.posX, partialTicks)
                    - interpolate(mc.thePlayer.prevPosX, mc.thePlayer.posX, partialTicks);

            double dz = interpolate(entity.prevPosZ, entity.posZ, partialTicks)
                    - interpolate(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, partialTicks);

            if ((dx * dx + dz * dz) <= (maxDist * maxDist)) {
                float dist = MathHelper.sqrt_double(dx * dx + dz * dz);
                double[] vector = getLookVector((float)(getYaw(entity) - interpolate(mc.thePlayer.prevRotationYaw, mc.thePlayer.rotationYaw, partialTicks)));

                if (entity instanceof EntityMob) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(248, 178, 0).getRGB());
                } else if (entity instanceof EntityPlayer) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(248, 0, 0).getRGB());
                } else if (entity instanceof EntityAnimal || entity instanceof EntitySquid || entity instanceof EntityVillager || entity instanceof EntityGolem) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(0, 252, 103).getRGB());
                }
            }
        }
        GL11.glPopMatrix();
    }

    private void renderExhi(float x, float y, float width, float height) {
        GL11.glPushMatrix();

        float cx = x + (width / 2f);
        float cy = y + (height / 2f);

        PostProcessing pp = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (pp != null && pp.isToggled() && pp.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            RenderUtil.drawExhiRect(x, y - 1, width, height, 1);
            RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) pp.shadowRadius.get(), 1);
        }

        if (pp != null && pp.isToggled() && pp.blur.get()) {
            Blur.startBlur();
            GlStateManager.pushMatrix();
            RenderUtil.drawExhiRect(x, y - 1, width, height, 1);
            RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
            GlStateManager.popMatrix();
            Blur.endBlur(pp.blurRadius.get(), 1);
        }

        RenderUtil.drawExhiRect(x, y - 1, width, height, 1);
        RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
        RenderUtil.drawRect(x + (width / 2f) - 0.5f, y, 1, height, 0xFF444444);
        RenderUtil.drawRect(x, y + (height / 2f) - 0.5f, width, 1, 0xFF444444);
        RenderUtil.drawRect(cx - 1, cy - 1, 2, 2, 0xFFFFFF00);

        float h = (System.currentTimeMillis() % 30000L) / 30000.0f * 255;
        float h2 = (h + 85f) % 255f;
        float h3 = (h + 170f) % 255f;

        RenderUtil.drawGradientRect(x, y - 1, width / 2, 0.5f, true, Color.getHSBColor(h / 255f, 0.8f, 1.0f).getRGB(), Color.getHSBColor(h2 / 255f, 0.8f, 1.0f).getRGB());
        RenderUtil.drawGradientRect(x + width / 2, y - 1, width / 2, 0.5f, true, Color.getHSBColor(h2 / 255f, 0.8f, 1.0f).getRGB(), Color.getHSBColor(h3 / 255f, 0.8f, 1.0f).getRGB());

        int maxDist = (int) (size.get() / 2);
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;

            float partialTicks = 1.0F;

            double dx = interpolate(entity.prevPosX, entity.posX, partialTicks)
                    - interpolate(mc.thePlayer.prevPosX, mc.thePlayer.posX, partialTicks);

            double dz = interpolate(entity.prevPosZ, entity.posZ, partialTicks)
                    - interpolate(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, partialTicks);

            if ((dx * dx + dz * dz) <= (maxDist * maxDist)) {
                float dist = MathHelper.sqrt_double(dx * dx + dz * dz);
                double[] vector = getLookVector((float)(getYaw(entity) - interpolate(mc.thePlayer.prevRotationYaw, mc.thePlayer.rotationYaw, partialTicks)));

                if (entity instanceof EntityMob) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(255, 95, 34).getRGB());
                } else if (entity instanceof EntityPlayer) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(248, 0, 0).getRGB());
                } else if (entity instanceof EntityAnimal || entity instanceof EntitySquid || entity instanceof EntityVillager || entity instanceof EntityGolem) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(0, 252, 103).getRGB());
                }
            }
        }
        GL11.glPopMatrix();
    }

    private void renderAstolfo(float x, float y, float width, float height) {
        GL11.glPushMatrix();

        float cx = x + (width / 2f);
        float cy = y + (height / 2f);

        HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        int themeColor = hudModule != null ? hudModule.theme.get().getRGB() : Color.WHITE.getRGB();

        PostProcessing pp = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (pp != null && pp.isToggled() && pp.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) pp.shadowRadius.get(), 1);
        }

        if (pp != null && pp.isToggled() && pp.blur.get()) {
            Blur.startBlur();
            GlStateManager.pushMatrix();
            RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
            GlStateManager.popMatrix();
            Blur.endBlur(pp.blurRadius.get(), 1);
        }

        RenderUtil.drawRect(x, y, width, height, new Color(0, 0, 0, 140).getRGB());
        RenderUtil.drawRect(x + (width / 2f) - 0.5f, y, 1f, height, new Color(180, 180, 180).getRGB());
        RenderUtil.drawRect(x, y + (height / 2f) - 0.5f, width, 1f, new Color(180, 180, 180).getRGB());
        RenderUtil.drawRect(x, y - 1, width, 1, themeColor);
        RenderUtil.drawRect(cx - 1, cy - 1, 2, 2, new Color(0, 255, 0).getRGB());

        int maxDist = (int) (size.get() / 2);
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;

            float partialTicks = 1.0F;

            double dx = interpolate(entity.prevPosX, entity.posX, partialTicks)
                    - interpolate(mc.thePlayer.prevPosX, mc.thePlayer.posX, partialTicks);

            double dz = interpolate(entity.prevPosZ, entity.posZ, partialTicks)
                    - interpolate(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, partialTicks);

            if ((dx * dx + dz * dz) <= (maxDist * maxDist)) {
                float dist = MathHelper.sqrt_double(dx * dx + dz * dz);
                double[] vector = getLookVector((float)(getYaw(entity) - interpolate(mc.thePlayer.prevRotationYaw, mc.thePlayer.rotationYaw, partialTicks)));

                if (entity instanceof EntityPlayer) {
                    RenderUtil.drawRect(cx - 1 - ((float) vector[0] * dist), cy - 1 - ((float) vector[1] * dist), 2, 2, new Color(255, 0, 0).getRGB());
                }
            }
        }
        GL11.glPopMatrix();
    }

    private double[] getLookVector(float yaw) {
        yaw *= MathHelper.deg2Rad;
        return new double[]{
                -MathHelper.sin(yaw),
                MathHelper.cos(yaw)
        };
    }

    private double interpolate(double prev, double current, float partialTicks) {
        return prev + (current - prev) * partialTicks;
    }

    private float getYaw(Entity entity) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        return (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }
}
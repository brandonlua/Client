package wtf.rania.client.widget;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.client.modules.impl.render.PostProcessing;
import wtf.rania.util.render.RenderUtil;
import wtf.rania.util.render.shaders.impl.Blur;
import wtf.rania.util.render.shaders.impl.Shadow;
import wtf.rania.util.render.shaders.impl.Bloom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TargetHUDWidget {

    private final Minecraft mc = Minecraft.getMinecraft();
    private EntityLivingBase target;
    private EntityLivingBase lastTarget;
    private float healthAnimation;
    private float displayHp;
    private ResourceLocation skin;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");
    private String mode = "Akrien";
    private Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);

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

    public String getMode() {
        return mode;
    }
    private float dragX = -1;
    private float dragY = -1;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;
    private boolean dragging = false;

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void render() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        target = getTarget();

        if (target == null && mc.currentScreen != null) {
            target = mc.thePlayer;
        }

        if (target == null) return;

        if (target != lastTarget) {
            onTargetChange(target);
            lastTarget = target;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        float w = getWidth();
        float h = getHeight();

        if (dragX == -1 && dragY == -1) {
            dragX = screenWidth / 2f + 50;
            dragY = screenHeight / 2f + 50;
        }

        float x = dragX;
        float y = dragY;

        int mouseX = Mouse.getX() * screenWidth / mc.displayWidth;
        int mouseY = screenHeight - Mouse.getY() * screenHeight / mc.displayHeight - 1;

        if (mc.currentScreen != null) {
            if (Mouse.isButtonDown(0)) {
                if (!dragging && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
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

        animateHealth();

        PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            if (mode.equals("Akrien")) {
                RenderUtil.drawRect(x, y + 2, w, h, new Color(0, 0, 0, 255).getRGB());
            } else if (mode.equals("Adjust")) {
                Gui.drawRect((int)x, (int)y, (int)(x + w), (int)(y + h), new Color(0, 0, 0, 255).getRGB());
            } else if (mode.equals("Modern")) {
                RenderUtil.drawRoundedRect(x, y, w, h, 8, new Color(0, 0, 0, 255));
            }
            shadowFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
        }

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.bloom.get()) {
            stencilFramebuffer = RenderUtil.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);
            RenderUtil.resetColor();
            if (mode.equals("Akrien")) {
                renderAkrien(x, y, w, h);
            } else if (mode.equals("Adjust")) {
                renderAdjust(x, y, w, h);
            } else if (mode.equals("Modern")) {
                renderModern(x, y, w, h);
            }
            RenderUtil.resetColor();
            stencilFramebuffer.unbindFramebuffer();
            Bloom.renderBlur(stencilFramebuffer.framebufferTexture, (int) postProcessing.bloomRadius.get(), (int) postProcessing.bloomOffset.get());
        }

        if (postProcessing != null && postProcessing.isToggled() && postProcessing.blur.get()) {
            Blur.startBlur();
            if (mode.equals("Akrien")) {
                RenderUtil.drawRect(x, y + 2, w, h, new Color(0, 0, 0, 150).getRGB());
            } else if (mode.equals("Adjust")) {
                Gui.drawRect((int)x, (int)y, (int)(x + w), (int)(y + h), new Color(0, 0, 0, 150).getRGB());
            } else if (mode.equals("Modern")) {
                RenderUtil.drawRoundedRect(x, y, w, h, 8, new Color(0, 0, 0, 100));
            }
            Blur.endBlur(postProcessing.blurRadius.get(), 1);
        }

        if (mode.equals("Akrien")) {
            renderAkrien(x, y, w, h);
        } else if (mode.equals("Adjust")) {
            renderAdjust(x, y, w, h);
        } else if (mode.equals("Modern")) {
            renderModern(x, y, w, h);
        }

        if (dragging) {
            if (mode.equals("Akrien")) {
                RenderUtil.drawRect(x - 1, y + 1, w + 2, 1, Color.WHITE.getRGB());
                RenderUtil.drawRect(x - 1, y + h + 2, w + 2, 1, Color.WHITE.getRGB());
                RenderUtil.drawRect(x - 1, y + 2, 1, h, Color.WHITE.getRGB());
                RenderUtil.drawRect(x + w, y + 2, 1, h, Color.WHITE.getRGB());
            } else if (mode.equals("Adjust")) {
                RenderUtil.drawRect(x - 1, y - 1, w + 2, 1, Color.WHITE.getRGB());
                RenderUtil.drawRect(x - 1, y + h, w + 2, 1, Color.WHITE.getRGB());
                RenderUtil.drawRect(x - 1, y, 1, h, Color.WHITE.getRGB());
                RenderUtil.drawRect(x + w, y, 1, h, Color.WHITE.getRGB());
            }
        }
    }

    private EntityLivingBase getTarget() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
                return (EntityLivingBase) mc.objectMouseOver.entityHit;
            }
        }
        return null;
    }

    private void onTargetChange(EntityLivingBase ent) {
        displayHp = ent.getHealth();
        if (ent instanceof EntityPlayer) {
            skin = getPlayerSkin((EntityPlayer) ent);
        }
    }

    private void animateHealth() {
        if (target == null) return;

        if (mode.equals("Akrien")) {
            float targetHealth = getWidth() * MathHelper.clamp_float(target.getHealth() / target.getMaxHealth(), 0, 1);
            healthAnimation += (targetHealth - healthAnimation) * 0.15f;
        } else if (mode.equals("Adjust")) {
            float hp = target.getHealth();
            if (displayHp > hp) {
                displayHp -= (displayHp - hp) * 0.05f;
                if (displayHp - hp < 0.1f) displayHp = hp;
            } else {
                displayHp = hp;
            }

            float padding = 2;
            float width = getWidth();
            float healthPercentage = target.getHealth() / target.getMaxHealth();
            float healthWidth = (width - padding * 2) * healthPercentage;
            healthAnimation += (healthWidth - healthAnimation) * 0.25f;
        } else if (mode.equals("Modern")) {
            float healthPercentage = target.getHealth() / target.getMaxHealth();
            float space = (getWidth() - 51) / 100;
            float targetHealthWidth = (100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1);
            healthAnimation += (targetHealthWidth - healthAnimation) * 0.25f;
        }
    }

    private float getWidth() {
        if (target == null) return 0;

        if (mode.equals("Akrien")) {
            HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            CFontRenderer font = hudModule != null ? hudModule.fr : null;
            if (font != null) {
                return 114 + ((35 + font.getStringWidth(target.getName())) / 25f);
            }
            return 114 + ((35 + mc.fontRendererObj.getStringWidth(target.getName())) / 25f);
        } else if (mode.equals("Modern")) {
            HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            CFontRenderer font = hudModule != null ? hudModule.fr : null;
            if (font != null) {
                return 35 + font.getStringWidth(target.getName()) + 20;
            }
            return 35 + mc.fontRendererObj.getStringWidth(target.getName()) + 20;
        } else {
            HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");

            String sheesh = decimalFormat.format(Math.abs(mc.thePlayer.getHealth() - target.getHealth()));
            String healthDiff = mc.thePlayer.getHealth() < target.getHealth() ? "-" + sheesh : "+" + sheesh;

            float nameWidth;
            float healthDiffWidth;

            if (hudModule != null) {
                nameWidth = hudModule.fr.getStringWidth(target.getName());
                healthDiffWidth = hudModule.fr.getStringWidth(healthDiff);
            } else {
                nameWidth = mc.fontRendererObj.getStringWidth(target.getName());
                healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiff);
            }

            float nameAndDiffWidth = nameWidth + healthDiffWidth + 40;

            List<ItemStack> items = target instanceof EntityPlayer ? getItems((EntityPlayer) target) : new ArrayList<>();
            float itemWidth = items.size() * 16;
            return Math.max(100, Math.max(nameAndDiffWidth, itemWidth + 40));
        }
    }

    private float getHeight() {
        if (mode.equals("Akrien")) {
            return 39.5f;
        } else if (mode.equals("Modern")) {
            return 38f;
        } else {
            return 35;
        }
    }

    private void renderAkrien(float x, float y, float width, float height) {
        float space = width - 2;
        RenderUtil.drawRect(x, y + 2, width, height, new Color(0, 0, 0, 150).getRGB());
        RenderUtil.drawBorderedRect(x + 1, y + 34.5f, space, 2.5f, 0.74f, new Color(0, 0, 0, 100).getRGB(), new Color(0, 0, 0, 100).getRGB());
        RenderUtil.drawBorderedRect(x + 1, y + 38.5f, space, 2.5f, 0.74f, new Color(0, 0, 0, 100).getRGB(), new Color(0, 0, 0, 100).getRGB());
        float healthWidth = space * (target.getHealth() / target.getMaxHealth());
        RenderUtil.drawGradientRect(x + 1, y + 34.5f, healthWidth, 2.5f, true, new Color(40, 145, 90).getRGB(), new Color(170, 255, 220).getRGB());
        if (target.getTotalArmorValue() > 0) {
            RenderUtil.drawGradientRect(x + 1, y + 38.5f, target.getTotalArmorValue() * 5.75f, 2.5f, true, new Color(40, 110, 160).getRGB(), new Color(100, 225, 255).getRGB());
        }
        String text = String.format("%.1f", target.getHealth());
        String text2 = String.format("%.1f", mc.thePlayer.getDistanceToEntity(target));

        HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        if (hudModule != null) {
            CFontRenderer font = hudModule.fr;
            font.drawStringWithShadow("Health: " + text, x + 32.5f, y + 16f + 2, -1);
            font.drawStringWithShadow("Distance: " + text2 + "m", x + 32.5f, y + 24.5f + 2, -1);
            font.drawStringWithShadow(target.getName(), x + 32.5f, y + 3 + 2, -1);
        } else {
            mc.fontRendererObj.drawStringWithShadow("Health: " + text, x + 32.5f, y + 16f + 2, -1);
            mc.fontRendererObj.drawStringWithShadow("Distance: " + text2 + "m", x + 32.5f, y + 24.5f + 2, -1);
            mc.fontRendererObj.drawStringWithShadow(target.getName(), x + 32.5f, y + 3 + 2, -1);
        }

        if (target instanceof EntityPlayer) {
            RenderUtil.renderPlayer2D((EntityPlayer) target, x + 1, y + 3, 30, 0, -1);
        }
    }

    private void renderAdjust(float x, float y, float width, float height) {
        float padding = 2;

        HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        CFontRenderer font = hudModule != null ? hudModule.fr : null;

        String sheesh = decimalFormat.format(Math.abs(mc.thePlayer.getHealth() - target.getHealth()));
        String healthDiff = mc.thePlayer.getHealth() < target.getHealth() ? "-" + sheesh : "+" + sheesh;

        int themeColor = hudModule != null ? hudModule.theme.get().getRGB() : Color.WHITE.getRGB();
        Color darkerColorObj = new Color(themeColor);
        int darkerColor = new Color(
                Math.max((int)(darkerColorObj.getRed() * 0.7f), 0),
                Math.max((int)(darkerColorObj.getGreen() * 0.7f), 0),
                Math.max((int)(darkerColorObj.getBlue() * 0.7f), 0),
                darkerColorObj.getAlpha()
        ).getRGB();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        Gui.drawRect(0, 0, (int)width, (int)height, new Color(0, 0, 0, 150).getRGB());
        Gui.drawRect((int)padding, (int)(height - 5), (int)(padding + width - padding * 2), (int)(height - 1), darkerColor);
        Gui.drawRect((int)padding, (int)(height - 5), (int)(padding + healthAnimation), (int)(height - 1), themeColor);

        if (skin != null) {
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);
            mc.getTextureManager().bindTexture(skin);
            Gui.drawScaledCustomSizeModalRect((int)padding, (int)padding, 8, 8, 8, 8, (int)(28 - padding), (int)(28 - padding), 64, 64);
            Gui.drawScaledCustomSizeModalRect((int)padding, (int)padding, 40, 8, 8, 8, (int)(28 - padding), (int)(28 - padding), 64, 64);
            GlStateManager.disableBlend();
        }

        if (font != null) {
            font.drawStringWithShadow(target.getName(), padding + 30, 3 + padding, Color.WHITE.getRGB());
            font.drawStringWithShadow(healthDiff, width - padding - font.getStringWidth(healthDiff), 3 + padding, Color.WHITE.getRGB());
        } else {
            mc.fontRendererObj.drawStringWithShadow(target.getName(), padding + 30, 3 + padding, Color.WHITE.getRGB());
            mc.fontRendererObj.drawStringWithShadow(healthDiff, width - padding - mc.fontRendererObj.getStringWidth(healthDiff), 3 + padding, Color.WHITE.getRGB());
        }

        if (target instanceof EntityPlayer) {
            float i = 30 + padding;
            List<ItemStack> items = getItems((EntityPlayer) target);

            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            for (ItemStack stack : items) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int)i, (int)(10 + padding));
                i += 16;
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

    private void renderModern(float x, float y, float width, float height) {
        float hurtTime = (target.hurtTime == 0 ? 0 : target.hurtTime - RenderUtil.getTimer().renderPartialTicks) * 0.5f;
        float space = (width - 45) / 100;
        float healthPercentage = (target.getHealth() / target.getMaxHealth()) * 100;
        String healthText = String.format("%.1f%%", healthPercentage);

        HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");

        GlStateManager.pushMatrix();
        RenderUtil.drawRoundedRect(x, y, width, height, 8, new Color(0, 0, 0, 100));

        RenderUtil.drawRoundedRect(x + 40, y + 21f, (100 * space), 8, 4, new Color(0, 0, 0, 255));

        RenderUtil.drawRoundedRect(x + 40, y + 21f, healthAnimation, 8f, 4, new Color(255, 0, 0));
        GlStateManager.popMatrix();

        if (target instanceof EntityPlayer) {
            Color hurtColor = new Color(255, (int)(255 * (1 - hurtTime / 7)), (int)(255 * (1 - hurtTime / 7)));
            RenderUtil.renderPlayer2D((EntityPlayer) target, x + 2.5f + (hurtTime) / 2, y + 2.5f + (hurtTime) / 2, 32 - hurtTime, 15, hurtColor.getRGB());
        }

        if (hudModule != null) {
            CFontRenderer font = hudModule.fr;
            font.drawStringWithShadow(target.getName(), x + 45F, y + 8.5f, -1);
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.85f, 0.85f, 0.85f);
            float healthTextWidth = font.getStringWidth(healthText) * 0.85f;
            font.drawStringWithShadow(healthText, (x + 40 + (100 * space) / 2 - healthTextWidth / 2) / 0.85f, (y + 22f) / 0.85f, -1);
            GlStateManager.popMatrix();
        } else {
            mc.fontRendererObj.drawStringWithShadow(target.getName(), x + 45F, y + 8.5f, -1);
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.85f, 0.85f, 0.85f);
            float healthTextWidth = mc.fontRendererObj.getStringWidth(healthText) * 0.85f;
            mc.fontRendererObj.drawStringWithShadow(healthText, (x + 40 + (100 * space) / 2 - healthTextWidth / 2) / 0.85f, (y + 22f) / 0.85f, -1);
            GlStateManager.popMatrix();
        }
    }

    private List<ItemStack> getItems(EntityPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        if (player.getHeldItem() != null) {
            items.add(player.getHeldItem());
        }
        for (int index = 3; index >= 0; index--) {
            ItemStack stack = player.inventory.armorInventory[index];
            if (stack != null) {
                items.add(stack);
            }
        }
        return items;
    }

    private ResourceLocation getPlayerSkin(EntityPlayer ent) {
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(ent.getUniqueID());
        if (info != null) {
            return info.getLocationSkin();
        }
        return null;
    }
}
package wtf.rania.utility.render;
import org.lwjgl.opengl.GL11;
import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.utility.misc.StencilUtil;
import wtf.rania.utility.render.shaders.impl.RoundedShader;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import java.util.List;
import net.minecraft.item.*;
import net.minecraft.util.*;
import wtf.rania.utility.mc.GLUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture;
import static net.minecraft.util.MathHelper.PI;
import static org.lwjgl.opengl.GL11.*;

public class RenderUtil {

    private static Minecraft mc;
    private static Frustum cameraFrustum;
    private static IntBuffer viewportBuffer;
    private static FloatBuffer modelViewBuffer;
    private static FloatBuffer projectionBuffer;
    private static FloatBuffer vectorBuffer;
    private static final Frustum FRUSTUM = new Frustum();

    public static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
    public static RoundedShader roundedShader = new RoundedShader("roundedRect");
    public static RoundedShader roundedOutlineShader = new RoundedShader("roundRectOutline");

    static {
        RenderUtil.mc = Minecraft.getMinecraft();
        RenderUtil.cameraFrustum = new Frustum();
        RenderUtil.viewportBuffer = GLAllocation.createDirectIntBuffer(16);
        RenderUtil.modelViewBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.projectionBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.vectorBuffer = GLAllocation.createDirectFloatBuffer(4);
    }

    public enum ArrowDirection {
        UP, DOWN, LEFT, RIGHT
    }

    public static boolean isBBInFrustum(EntityLivingBase entity) {
        return isBBInFrustum(entity.getEntityBoundingBox());
    }

    public static boolean isBBInFrustum(AxisAlignedBB aabb) {
        FRUSTUM.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        return FRUSTUM.isBoundingBoxInFrustum(aabb);
    }

    private static boolean isInViewFrustum(AxisAlignedBB bb) {
        Entity current = mc.getRenderViewEntity();
        FRUSTUM.setPosition(current.posX, current.posY, current.posZ);
        return FRUSTUM.isBoundingBoxInFrustum(bb);
    }

    public static boolean isInViewFrustum(Entity entity) {
        return isInViewFrustum(entity.getEntityBoundingBox()) || entity.ignoreFrustumCheck;
    }

    public static void drawRect(float left, float top, float width, float height, Color color) {
        drawRect(left,top,width,height,color.getRGB());
    }

    public static void drawRect(float left, float top, float width, float height, int color) {
        float right = left + width, bottom = top + height;
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }

        Gui.drawRect((int) left, (int) top, (int) right, (int) bottom, color);
    }

    public static void bindTexture(int texture) {
        GlStateManager.bindTexture(texture);
    }

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, RoundedShader roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(),
                (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    public static void drawImage(ResourceLocation image, float x, float y, int width, int height) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDepthMask(false);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(image);
        drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, width, height, width, height);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    public static void customRotatedObject2D(float oXpos, float oYpos, float oWidth, float oHeight, double rotate) {
        GlStateManager.translate(oXpos + oWidth / 2, oYpos + oHeight / 2, 0);
        GL11.glRotated(rotate, 0.0, 0.0, 1.0);
        GlStateManager.translate(-oXpos - oWidth / 2, -oYpos - oHeight / 2, 0);
    }

    public static void setupOrientationMatrix(double x, double y, double z) {
        GlStateManager.translate(x - mc.getRenderManager().viewerPosX, y - mc.getRenderManager().viewerPosY, z - mc.getRenderManager().viewerPosZ);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, false, color);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (0 * .01));
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        RoundedShader.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        resetColor();
    }

    public static void drawGradientText(String text, float x, float y, int startColor, int endColor, boolean shadow, CFontRenderer font) {
        if (font == null) return;

        float textWidth = font.getStringWidth(text);

        if (shadow) {
            int shadowStart = ColorUtil.dropShadow(startColor);
            int shadowEnd = ColorUtil.dropShadow(endColor);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                float charX = x + font.getStringWidth(text.substring(0, i));
                float progress = charX / textWidth;
                int color = interpolateColor(shadowStart, shadowEnd, progress);
                font.drawString(String.valueOf(c), charX + 1, y + 1, color);
            }
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float charX = x + font.getStringWidth(text.substring(0, i));
            float progress = charX / textWidth;
            int color = interpolateColor(startColor, endColor, progress);
            font.drawString(String.valueOf(c), charX, y, color);
        }
    }

    public static void drawGradientText(String text, float x, float y, int startColor, int endColor, CFontRenderer font) {
        drawGradientText(text, x, y, startColor, endColor, false, font);
    }

    public static void drawGradientRect(float x, float y, float width, float height, int startColor, int endColor) {
        float right = x + width;
        float bottom = y + height;

        Gui.drawRect((int)x, (int)y, (int)right, (int)bottom, startColor);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        float startR = (startColor >> 16 & 255) / 255.0F;
        float startG = (startColor >> 8 & 255) / 255.0F;
        float startB = (startColor & 255) / 255.0F;
        float startA = (startColor >> 24 & 255) / 255.0F;

        float endR = (endColor >> 16 & 255) / 255.0F;
        float endG = (endColor >> 8 & 255) / 255.0F;
        float endB = (endColor & 255) / 255.0F;
        float endA = (endColor >> 24 & 255) / 255.0F;

        worldrenderer.pos(right, y, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x, y, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x, bottom, 0.0D).color(endR, endG, endB, endA).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).color(endR, endG, endB, endA).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawGradientRectSideways(float x, float y, float width, float height, int startColor, int endColor) {
        float right = x + width;
        float bottom = y + height;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        float startR = (startColor >> 16 & 255) / 255.0F;
        float startG = (startColor >> 8 & 255) / 255.0F;
        float startB = (startColor & 255) / 255.0F;
        float startA = (startColor >> 24 & 255) / 255.0F;

        float endR = (endColor >> 16 & 255) / 255.0F;
        float endG = (endColor >> 8 & 255) / 255.0F;
        float endB = (endColor & 255) / 255.0F;
        float endA = (endColor >> 24 & 255) / 255.0F;

        worldrenderer.pos(right, y, 0.0D).color(endR, endG, endB, endA).endVertex();
        worldrenderer.pos(x, y, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x, bottom, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).color(endR, endG, endB, endA).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static int interpolateColor(int color1, int color2, float progress) {
        progress = Math.max(0, Math.min(1, progress));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);
        int a = (int)(a1 + (a2 - a1) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void setupCameraTransform(float partialTicks, int pass) {
        try {
            java.lang.reflect.Method m = mc.entityRenderer.getClass().getDeclaredMethod("setupCameraTransform", float.class, int.class);
            m.setAccessible(true);
            m.invoke(mc.entityRenderer, partialTicks, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
        }
        return framebuffer;
    }

    public static void enableRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GL11.glDisable(GL_LINE_SMOOTH);
    }

    public static void disableRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glEnable(GL_LINE_SMOOTH);
        resetColor();
    }

    public static void setColor(int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, a);
    }

    public static void resetColor() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static Timer getTimer() {
        try {
            Field timerField = Minecraft.class.getDeclaredField("field_71428_T");
            timerField.setAccessible(true);
            return (Timer) timerField.get(mc);
        } catch (Exception e) {
            try {
                Field timerField = Minecraft.class.getDeclaredField("timer");
                timerField.setAccessible(true);
                return (Timer) timerField.get(mc);
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static double getRenderPosX() {
        return mc.getRenderManager().viewerPosX;
    }

    public static double getRenderPosY() {
        return mc.getRenderManager().viewerPosY;
    }

    public static double getRenderPosZ() {
        return mc.getRenderManager().viewerPosZ;
    }

    public static void drawLine3D(Vec3 start, double endX, double endY, double endZ, float red, float green, float blue, float alpha, float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.color(red, green, blue, alpha);
        boolean bl = RenderUtil.mc.gameSettings.viewBobbing;
        RenderUtil.mc.gameSettings.viewBobbing = false;
        setupCameraTransform(getTimer().renderPartialTicks, 2);
        RenderUtil.mc.gameSettings.viewBobbing = bl;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
        GL11.glVertex3d(endX - getRenderPosX(), endY - getRenderPosY(), endZ - getRenderPosZ());
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawExhiRect(float x, float y, float x2, float y2, float alpha) {
        x2 = x + x2;
        y2 = y + y2;
        Gui.drawRect((int)(x - 3.5F), (int)(y - 3.5F), (int)(x2 + 3.5F), (int)(y2 + 3.5F), new Color(0, 0, 0, (int)alpha).getRGB());
        Gui.drawRect((int)(x - 3F), (int)(y - 3F), (int)(x2 + 3F), (int)(y2 + 3F), new Color(50F / 255F, 50F / 255F, 50F / 255F, alpha / 255F).getRGB());
        Gui.drawRect((int)(x - 2.5F), (int)(y - 2.5F), (int)(x2 + 2.5F), (int)(y2 + 2.5F), new Color(26F / 255F, 26F / 255F, 26F / 255F, alpha / 255F).getRGB());
        Gui.drawRect((int)(x - 0.5F), (int)(y - 0.5F), (int)(x2 + 0.5F), (int)(y2 + 0.5F), new Color(50F / 255F, 50F / 255F, 50F / 255F, alpha / 255F).getRGB());
        Gui.drawRect((int)x, (int)y, (int)x2, (int)y2, new Color(18F / 255F, 18 / 255F, 18F / 255F, alpha / 255F).getRGB());
    }

    public static void drawGradientRect(final double left, final double top, double right, double bottom, final boolean sideways, final int startColor, final int endColor) {
        right = left + right;
        bottom = top + bottom;
        GL11.glDisable(3553);
        GLUtils.startBlend();
        GL11.glShadeModel(7425);
        GL11.glBegin(7);
        color(startColor);
        if (sideways) {
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(left, bottom);
            color(endColor);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(right, top);
        } else {
            GL11.glVertex2d(left, top);
            color(endColor);
            GL11.glVertex2d(left, bottom);
            GL11.glVertex2d(right, bottom);
            color(startColor);
            GL11.glVertex2d(right, top);
        }
        GL11.glEnd();
        GL11.glDisable(3042);
        GL11.glShadeModel(7424);
        GLUtils.endBlend();
        GL11.glEnable(3553);
    }

    public static void drawBorder(float x, float y, float width, float height, final float outlineThickness, int outlineColor) {
        glEnable(GL_LINE_SMOOTH);
        color(outlineColor);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();

        glLineWidth(outlineThickness);
        float cornerValue = (float) (outlineThickness * .19);

        glBegin(GL_LINES);
        glVertex2d(x, y - cornerValue);
        glVertex2d(x, y + height + cornerValue);
        glVertex2d(x + width, y + height + cornerValue);
        glVertex2d(x + width, y - cornerValue);
        glVertex2d(x, y);
        glVertex2d(x + width, y);
        glVertex2d(x, y + height);
        glVertex2d(x + width, y + height);
        glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        glDisable(GL_LINE_SMOOTH);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float x1 = x + width, // @off
                y1 = y + height;
        final float f = (color >> 24 & 0xFF) / 255.0F,
                f1 = (color >> 16 & 0xFF) / 255.0F,
                f2 = (color >> 8 & 0xFF) / 255.0F,
                f3 = (color & 0xFF) / 255.0F; // @on
        GL11.glPushAttrib(0);
        GL11.glScaled(0.5, 0.5, 0.5);

        x *= 2;
        y *= 2;
        x1 *= 2;
        y1 *= 2;

        glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(f1, f2, f3, f);
        GlStateManager.enableBlend();
        glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glBegin(GL11.GL_POLYGON);
        final double v = PI / 180;

        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2d(x + radius + MathHelper.sin((float) (i * v)) * (radius * -1), y + radius + MathHelper.cos((float) (i * v)) * (radius * -1));
        }

        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2d(x + radius + MathHelper.sin((float) (i * v)) * (radius * -1), y1 - radius + MathHelper.cos((float) (i * v)) * (radius * -1));
        }

        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2d(x1 - radius + MathHelper.sin((float) (i * v)) * radius, y1 - radius + MathHelper.cos((float) (i * v)) * radius);
        }

        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2d(x1 - radius + MathHelper.sin((float) (i * v)) * radius, y + radius + MathHelper.cos((float) (i * v)) * radius);
        }

        GL11.glEnd();

        glEnable(GL11.GL_TEXTURE_2D);
        glDisable(GL11.GL_LINE_SMOOTH);
        glEnable(GL11.GL_TEXTURE_2D);

        GL11.glScaled(2, 2, 2);

        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
    }

    public static void renderPlayer2D(EntityLivingBase abstractClientPlayer, float x, float y, float size, float radius, int color) {
        if (abstractClientPlayer instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer) abstractClientPlayer;
            StencilUtil.initStencilToWrite();
            RenderUtil.drawRoundedRect(x, y, size, size, radius, -1);
            StencilUtil.readStencilBuffer(1);
            RenderUtil.color(color);
            GLUtils.startBlend();
            mc.getTextureManager().bindTexture(player.getLocationSkin());
            Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8.0f, 8.0f, 8, 8, (int) size, (int) size, 64.0F, 64.0F);
            GLUtils.endBlend();
            StencilUtil.uninitStencilBuffer();
        }
    }


    public static boolean hovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public static void start() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void stop() {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        resetColor();
    }

    public static void renderItemStack(ItemStack stack, double x, double y, float scale) {
        renderItemStack(stack, x, y, scale, false);
    }

    public static void renderItemStack(ItemStack stack, double x, double y, float scale, boolean enchantedText) {
        renderItemStack(stack, x, y, scale, enchantedText, scale);
    }

    public static void renderItemStack(ItemStack stack, double x, double y, float scale, boolean enchantedText, float textScale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, x);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        //mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, 0, 0);
        if (enchantedText)
            renderEnchantText(stack, 0, 0, textScale);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void renderItemStack(EntityPlayer target, float x, float y, float scale, boolean enchantedText, float textScale,boolean bg,boolean info) {
        List<ItemStack> items = new ArrayList<>();
        if (target.getHeldItem() != null) {
            items.add(target.getHeldItem());
        }
        for (int index = 3; index >= 0; index--) {
            ItemStack stack = target.inventory.armorInventory[index];
            if (stack != null) {
                items.add(stack);
            }
        }
        float i = x;

        for (ItemStack stack : items) {
            if (bg)
                RenderUtil.drawRect(i, y, 16 * scale, 16 * scale, new Color(0, 0, 0, 150).getRGB());
            if (info) {
                final int damage = stack.getMaxDamage() - stack.getItemDamage();

                HUD hudModule = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
                if (hudModule != null) {
                    CFontRenderer font = hudModule.fr;
                    font.drawStringWithShadow(damage + "", i + (16 * scale) / 2 - font.getStringWidth(damage + "") / 2, (y + 16 + 2) * scale, -1);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(damage + "", i + (16 * scale) / 2 - mc.fontRendererObj.getStringWidth(damage + "") / 2, (y + 16 + 2) * scale, -1);
                }
            }
        }
    }

    public static void renderItemStack(EntityPlayer target, float x, float y, float scale,boolean bg,boolean info) {
        renderItemStack(target,x,y,scale,false,0,bg,info);
    }

    public static void renderItemStack(EntityPlayer target, float x, float y, float scale, float textScale) {
        renderItemStack(target,x,y,scale,true,textScale,false,false);
    }

    public static void renderItemStack(EntityPlayer target, float x, float y, float scale) {
        renderItemStack(target,x,y,scale,scale);
    }

    public static void renderEnchantText(ItemStack stack, double x, double y, float scale) {
        int unBreakingLevel;
        RenderHelper.disableStandardItemLighting();
        double height = y;
        if (stack.getItem() instanceof ItemArmor) {
            int protectionLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
            int unBreakingLevel2 = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            int thornLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack);
            if (protectionLevel > 0) {
                drawEnchantTag("P" + getColor(protectionLevel) + protectionLevel, x, height, scale);
                height += 8 * scale;
            }
            if (unBreakingLevel2 > 0) {
                drawEnchantTag("U" + getColor(unBreakingLevel2) + unBreakingLevel2, x, height, scale);
                height += 8 * scale;
            }
            if (thornLevel > 0) {
                drawEnchantTag("T" + getColor(thornLevel) + thornLevel, x, height, scale);
                height += 8 * scale;
            }
        }
        if (stack.getItem() instanceof ItemBow) {
            int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            int punchLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
            int flameLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack);
            unBreakingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (powerLevel > 0) {
                drawEnchantTag("Pow" + getColor(powerLevel) + powerLevel, x, height, scale);
                height += 8 * scale;
            }
            if (punchLevel > 0) {
                drawEnchantTag("Pun" + getColor(punchLevel) + punchLevel, x, height, scale);
                height += 8 * scale;
            }
            if (flameLevel > 0) {
                drawEnchantTag("F" + getColor(flameLevel) + flameLevel, x, height, scale);
                height += 8 * scale;
            }
            if (unBreakingLevel > 0) {
                drawEnchantTag("U" + getColor(unBreakingLevel) + unBreakingLevel, x, height, scale);
                height += 8 * scale;
            }
        }
        if (stack.getItem() instanceof ItemSword) {
            int sharpnessLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
            int knockBackLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
            int fireAspectLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
            unBreakingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (sharpnessLevel > 0) {
                drawEnchantTag("S" + getColor(sharpnessLevel) + sharpnessLevel, x, height, scale);
                height += 8 * scale;
            }
            if (knockBackLevel > 0) {
                drawEnchantTag("K" + getColor(knockBackLevel) + knockBackLevel, x, height, scale);
                height += 8 * scale;
            }
            if (fireAspectLevel > 0) {
                drawEnchantTag("F" + getColor(fireAspectLevel) + fireAspectLevel, x, height, scale);
                height += 8 * scale;
            }
            if (unBreakingLevel > 0) {
                drawEnchantTag("U" + getColor(unBreakingLevel) + unBreakingLevel, x, height, scale);
                height += 8 * scale;
            }
        }
        if (stack.getRarity() == EnumRarity.EPIC) {
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GL11.glTranslated(x, y, x);
            GL11.glScaled(scale, scale, scale);
            mc.fontRendererObj.drawOutlinedString("God", (float) (x), (float) height, 1.0f, new Color(255, 255, 0).getRGB(), new Color(100, 100, 0, 140).getRGB());
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    private static void drawEnchantTag(String text, double x, double y, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GL11.glTranslated(x, y, x);
        GL11.glScaled(scale, scale, scale);
        mc.fontRendererObj.drawOutlinedString(text, (float) 0, (float) 0, 1.0f, -1, new Color(0, 0, 0, 140).getRGB());
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private static String getColor(final int n) {
        if (n != 1) {
            if (n == 2) {
                return "§a";
            }
            if (n == 3) {
                return "§3";
            }
            if (n == 4) {
                return "§4";
            }
            if (n >= 5) {
                return "§e";
            }
        }
        return "§f";
    }

    public static String stripColor(final String input) {
        return COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static void drawFilledBox(AxisAlignedBB box) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        renderer.pos(box.minX, box.minY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.minY, box.maxZ).endVertex();

        renderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.minZ).endVertex();

        renderer.pos(box.minX, box.minY, box.minZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.minZ).endVertex();

        renderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.maxZ).endVertex();

        renderer.pos(box.minX, box.minY, box.minZ).endVertex();
        renderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.minZ).endVertex();

        renderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.maxZ).endVertex();

        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawCircle(double x, double y, double radius, int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        GL11.glEnable(GL11.GL_BLEND);
        glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        for (int i = 0; i <= 360; i++) {
            glVertex2d(x + Math.sin(i * Math.PI / 180) * radius, y + Math.cos(i * Math.PI / 180) * radius);
        }
        GL11.glEnd();
        glDisable(GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        glDisable(GL11.GL_BLEND);
    }

    public static void drawBorderedRect(float x, float y, float width, float height, final float outlineThickness, int rectColor, int outlineColor) {
        drawRect(x,y,width,height,rectColor);
        drawBorder(x,y,width,height,outlineThickness,outlineColor);
    }

    public static void drawOutlinedBoundingBox(final AxisAlignedBB a) {
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(final AxisAlignedBB a) {
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.minY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.pos((float)a.minX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.maxZ).endVertex();
        worldrenderer.pos((float)a.maxX, (float)a.maxY, (float)a.minZ).endVertex();
        worldrenderer.endVertex();
        tessellator.draw();
    }

    public static void renderBoundingBox(AxisAlignedBB aabb, Color color, int alpha) {
        AxisAlignedBB bb = aabb;
        GlStateManager.pushMatrix();
        GLUtils.setup2DRendering();
        GLUtils.enableCaps(GL_BLEND, GL_POINT_SMOOTH, GL_POLYGON_SMOOTH, GL_LINE_SMOOTH);

        glLineWidth(5);
        float actualAlpha = .3f * alpha;
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, actualAlpha);
        color(color.getRGB(), actualAlpha);
        RenderGlobal.drawOutlinedBoundingBox(bb, color.getRed(), color.getGreen(), color.getBlue(), alpha);

        GLUtils.disableCaps();
        GLUtils.end2DRendering();

        GlStateManager.popMatrix();
    }

    public static void drawBlockESP(final BlockPos blockPos, final float red, final float green, final float blue, final float alpha, final float lineAlpha, final float lineWidth) {
        GlStateManager.color(red, green, blue, alpha);
        final float x = (float)(blockPos.getX() - mc.getRenderManager().viewerPosX);
        final float y = (float)(blockPos.getY() - mc.getRenderManager().viewerPosY);
        final float z = (float)(blockPos.getZ() - mc.getRenderManager().viewerPosZ);
        final Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        drawBoundingBox(new AxisAlignedBB(x, y, z, x + block.getBlockBoundsMaxX(), y + block.getBlockBoundsMaxY(), z + block.getBlockBoundsMaxZ()));
        if (lineWidth > 0.0f) {
            GL11.glLineWidth(lineWidth);
            GlStateManager.color(red, green, blue, lineAlpha);
            drawOutlinedBoundingBox(new AxisAlignedBB(x, y, z, x + block.getBlockBoundsMaxX(), y + block.getBlockBoundsMaxY(), z + block.getBlockBoundsMaxZ()));
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }

    public static void color(int color, float alpha) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, alpha);
    }


    public static void color(int color) {
        color(color, (float) (color >> 24 & 255) / 255.0F);
    }


    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static void scissor(final double x, final double y, final double width, final double height) {
        int scaleFactor = 1;
        while (scaleFactor < 2 && mc.displayWidth / (scaleFactor + 1) >= 320 && mc.displayHeight / (scaleFactor + 1) >= 240) {
            ++scaleFactor;
        }
        GL11.glScissor((int) (x * scaleFactor),
                (int) (Minecraft.getMinecraft().displayHeight - (y + height) * scaleFactor),
                (int) (width * scaleFactor), (int) (height * scaleFactor));
    }

    public static void startScissor(float x, float y, float width, float height) {

        GL11.glEnable(GL11.GL_SCISSOR_TEST);


        Minecraft mc = Minecraft.getMinecraft();
        int scaleFactor = 1;
        try {
            scaleFactor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        } catch (Exception ignored) {}

        int scissorX = (int) (x * scaleFactor);
        int scissorY = (int) (mc.displayHeight - (y + height) * scaleFactor);
        int scissorWidth = (int) (width * scaleFactor);
        int scissorHeight = (int) (height * scaleFactor);

        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void endScissor() {
        glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount),
                interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return applyOpacity(resultColor, interpolateInt(color1.getAlpha(), color2.getAlpha(), amount) / 255f);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static float interpolate(float old,
                                    float now,
                                    float partialTicks) {

        return old + (now - old) * partialTicks;
    }

    public static Vector4d projectToScreen(EntityPlayer entity, double scaleFactor) {
        net.minecraft.util.Timer timer = getTimer();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks;

        x -= getRenderPosX();
        y -= getRenderPosY();
        z -= getRenderPosZ();

        AxisAlignedBB bb = entity.getEntityBoundingBox().expand(0.1, 0.1, 0.1).offset(
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - entity.posX,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - entity.posY,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - entity.posZ
        );

        Vector4d result = null;

        double[][] corners = {
                {bb.minX, bb.minY, bb.minZ}, {bb.minX, bb.maxY, bb.minZ},
                {bb.maxX, bb.minY, bb.minZ}, {bb.maxX, bb.maxY, bb.minZ},
                {bb.minX, bb.minY, bb.maxZ}, {bb.minX, bb.maxY, bb.maxZ},
                {bb.maxX, bb.minY, bb.maxZ}, {bb.maxX, bb.maxY, bb.maxZ}
        };

        for (double[] corner : corners) {
            GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
            GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);

            if (GLU.gluProject(
                    (float) (corner[0] - getRenderPosX()),
                    (float) (corner[1] - getRenderPosY()),
                    (float) (corner[2] - getRenderPosZ()),
                    modelViewBuffer, projectionBuffer, viewportBuffer, vectorBuffer
            )) {
                double screenX = vectorBuffer.get(0) / scaleFactor;
                double screenY = ((float) Display.getHeight() - vectorBuffer.get(1)) / scaleFactor;
                double screenZ = vectorBuffer.get(2);

                if (screenZ >= 0.0 && screenZ < 1.0) {
                    if (result == null) {
                        result = new Vector4d(screenX, screenY, screenX, screenY);
                    }
                    result.x = Math.min(screenX, result.x);
                    result.y = Math.min(screenY, result.y);
                    result.z = Math.max(screenX, result.z);
                    result.w = Math.max(screenY, result.w);
                }
            }
        }

        return result;
    }

    public static class Vector4d {
        public double x, y, z, w;

        public Vector4d(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;

        }
    }
    public static void drawHorizontalGradientSideways(double x, double y, double width, double height, int leftColor, int rightColor) {
        drawGradientRect(x,y,width,height,true,leftColor,rightColor);
    }
    public static void drawVerticalGradientSideways(double x, double y, double width, double height, int topColor, int bottomColor) {
        drawGradientRect(x,y,width,height,false,topColor,bottomColor);
    }
}
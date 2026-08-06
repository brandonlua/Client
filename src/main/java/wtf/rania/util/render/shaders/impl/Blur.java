package wtf.rania.util.render.shaders.impl;
import org.lwjgl.opengl.GL11;

import wtf.rania.util.InstanceAccess;
import wtf.rania.util.math.MathUtil;
import wtf.rania.util.misc.StencilUtil;
import wtf.rania.util.render.RenderUtil;
import wtf.rania.util.render.shaders.ShaderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class Blur implements InstanceAccess {

    private static final ShaderUtil gaussianBlur = new ShaderUtil("gaussianBlur");
    private static Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static void setupUniforms(float dir1, float dir2, float radius) {
        gaussianBlur.setUniformi("textureIn", 0);
        gaussianBlur.setUniformf("texelSize", 1.0F / (float) mc.displayWidth, 1.0F / (float) mc.displayHeight);
        gaussianBlur.setUniformf("direction", dir1, dir2);
        gaussianBlur.setUniformf("radius", radius);

        final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);
        for (int i = 0; i <= radius; i++) {
            weightBuffer.put(MathUtil.calculateGaussianValue(i, radius / 2));
        }

        weightBuffer.rewind();
        GL20.glUniform1fv(gaussianBlur.getUniform("weights"), weightBuffer);
    }

    public static void startBlur(){
        StencilUtil.initStencilToWrite();
    }
    public static void endBlur(float radius, float compression) {
        StencilUtil.readStencilBuffer(1);

        framebuffer = RenderUtil.createFrameBuffer(framebuffer);

        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(compression, 0, radius);

        RenderUtil.bindTexture(mc.getFramebuffer().framebufferTexture);
        ShaderUtil.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.unload();

        mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(0, compression, radius);

        RenderUtil.bindTexture(framebuffer.framebufferTexture);
        ShaderUtil.drawQuads();
        gaussianBlur.unload();

        StencilUtil.uninitStencilBuffer();
        RenderUtil.resetColor();
        GlStateManager.bindTexture(0);

    }

    public static void renderBlur(float radius) {
        mc.getFramebuffer().bindFramebuffer(true);

        GlStateManager.enableBlend();
        GL11.glDisable(2929);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        update(framebuffer);
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        gaussianBlur.init();
        setupUniforms(1.0f, 0.0f, radius);
        RenderUtil.bindTexture(mc.getFramebuffer().framebufferTexture);
        ShaderUtil.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.unload();
        mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(0.0f, 1.0f, radius);
        RenderUtil.bindTexture(framebuffer.framebufferTexture);
        ShaderUtil.drawQuads();
        gaussianBlur.unload();
        GlStateManager.resetColor();
        GlStateManager.bindTexture(0);
        GL11.glEnable(2929);
    }

    public static void update(Framebuffer framebuffer) {
        if (framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            framebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
        }
    }
}
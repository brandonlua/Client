package wtf.rania.client.modules.impl.render;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ColorValue;
import wtf.rania.util.render.RenderUtil;
import net.minecraft.client.renderer.OpenGlHelper;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPolygonOffset;

@ModuleInfo(name = "Chams", category = Category.RENDER)
public class Chams extends Module {

    public final BoolValue occludedFlatProperty = new BoolValue("Occluded Flat", true, this);
    public final BoolValue visibleFlatProperty = new BoolValue("Visible Flat", true, this);
    public final BoolValue textureOccludedProperty = new BoolValue("Tex Occluded", false, this);
    public final BoolValue textureVisibleProperty = new BoolValue("Tex Visible", false, this);
    public final ColorValue visibleColorProperty = new ColorValue("V-Color", Color.RED, this);
    public final ColorValue occludedColorProperty = new ColorValue("O-Color", Color.GREEN, this);

    public static void preRenderOccluded(boolean disableTexture, int occludedColor, boolean occludedFlat) {
        if (disableTexture)
            glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        if (occludedFlat)
            glDisable(GL_LIGHTING);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(0.0F, -1000000.0F);
        // Must pass the real lightmap texture unit enum (GL_TEXTURE1 / 33985), not a raw
        // "1". A bare 1 is an invalid texture-unit argument to glMultiTexCoord2f and makes
        // the GPU driver crash with an access violation when entities are rendered.
        // Guard against an uninitialized unit (multitexture not yet ready) for the same reason.
        if (OpenGlHelper.lightmapTexUnit != 0) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        }
        glDepthMask(false);
        RenderUtil.color(occludedColor);
    }

    public static void preRenderVisible(boolean disableTexture, boolean enableTexture, int visibleColor, boolean visibleFlat, boolean occludedFlat) {
        if (enableTexture)
            glEnable(GL_TEXTURE_2D);
        else if (disableTexture)
            glDisable(GL_TEXTURE_2D);

        glDepthMask(true);
        if (occludedFlat && !visibleFlat)
            glEnable(GL_LIGHTING);
        else if (!occludedFlat && visibleFlat)
            glDisable(GL_LIGHTING);

        RenderUtil.color(visibleColor);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    public static void postRender(boolean enableTexture, boolean visibleFlat) {
        if (visibleFlat)
            glEnable(GL_LIGHTING);
        if (enableTexture)
            glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }
}
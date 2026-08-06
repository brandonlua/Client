package wtf.rania.util.render.shaders.impl;

import wtf.rania.util.InstanceAccess;
import wtf.rania.util.render.shaders.ShaderUtil;
import net.minecraft.client.gui.ScaledResolution;


public class MainMenu implements InstanceAccess {
    private static final ShaderUtil mainmenu = new ShaderUtil("mainmenu");

    public static void draw(long initTime) {
        ScaledResolution sr = new ScaledResolution(mc);
        mainmenu.init();
        mainmenu.setUniformf("TIME", (float) (System.currentTimeMillis() - initTime) / 1000);
        mainmenu.setUniformf("RESOLUTION", (float) ((double) sr.getScaledWidth() * sr.getScaleFactor()), (float) ((double) sr.getScaledHeight() * sr.getScaleFactor()));
        ShaderUtil.drawFixedQuads();
        mainmenu.unload();
    }
}
package wtf.rania.utility.render.shaders.impl;

import wtf.rania.Client;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.utility.InstanceAccess;
import wtf.rania.utility.render.shaders.ShaderUtil;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;

public class MainMenu implements InstanceAccess {
    private static final ShaderUtil mainmenu = new ShaderUtil("mainmenu");

    public static void draw(long initTime) {
        ScaledResolution sr = new ScaledResolution(mc);
        mainmenu.init();
        mainmenu.setUniformf("TIME", (float) (System.currentTimeMillis() - initTime) / 1000);
        mainmenu.setUniformf("RESOLUTION", (float) ((double) sr.getScaledWidth() * sr.getScaleFactor()), (float) ((double) sr.getScaledHeight() * sr.getScaleFactor()));

        Color theme = new Color(63, 213, 255);
        if (Client.INSTANCE != null && Client.INSTANCE.getModuleManager() != null) {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule(HUD.class);
            if (hud != null && hud.theme.get() != null) {
                theme = hud.theme.get();
            }
        }

        float r = theme.getRed() / 255F;
        float g = theme.getGreen() / 255F;
        float b = theme.getBlue() / 255F;

        mainmenu.setUniformf("THEME_COLOR", r, g, b);
        mainmenu.setUniformf("THEME_COLOR_DARK", r * 0.25F, g * 0.25F, b * 0.25F);

        ShaderUtil.drawFixedQuads();
        mainmenu.unload();
    }
}
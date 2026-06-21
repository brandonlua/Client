package wtf.fentanyl.client.widget;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.util.animations.Animation;
import wtf.fentanyl.util.animations.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArrayListWidget {

    private Minecraft mc = Minecraft.getMinecraft();

    public void render(CFontRenderer fr, int width, int height, String position, boolean suffix, String outlineMode, float bgAlpha, int themeColor) {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : Client.INSTANCE.getModuleManager().getModules()) {
            if (module.isHidden()) continue;

            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isToggled() ? Direction.FORWARDS : Direction.BACKWARDS);

            if (!module.isToggled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;

            if (!module.getName().equals("HUD") && !module.getName().equals("JumpCircles") &&
                    !module.getName().equals("ClickGui") && !module.getName().equals("TargetHUD") &&
                    !module.getName().equals("MotionBlur") && !module.getName().equals("MotionCamera") &&
                    !module.getName().equals("TabGUI") && !module.getName().equals("NoFire")) {
                enabledModules.add(module);
            }
        }

        float arrayListY = 3;
        float arrayListX = 2;
        boolean isRight = false;
        boolean isBottom = false;

        switch (position) {
            case "Top Left":
                arrayListX = 2;
                arrayListY = 3;
                enabledModules.sort(Comparator.comparingDouble(m -> {
                    String name = m.getName();
                    if (suffix && m.getSuffix() != null && !m.getSuffix().isEmpty()) {
                        name += " " + m.getSuffix();
                    }
                    return -fr.getStringWidth(name);
                }));
                break;
            case "Top Right":
                isRight = true;
                arrayListY = 3;
                enabledModules.sort(Comparator.comparingDouble(m -> {
                    String name = m.getName();
                    if (suffix && m.getSuffix() != null && !m.getSuffix().isEmpty()) {
                        name += " " + m.getSuffix();
                    }
                    return -fr.getStringWidth(name);
                }));
                break;
            case "Bottom Left":
                isBottom = true;
                arrayListX = 2;
                arrayListY = height - (enabledModules.size() * 10) - 2;
                enabledModules.sort(Comparator.comparingDouble(m -> {
                    String name = m.getName();
                    if (suffix && m.getSuffix() != null && !m.getSuffix().isEmpty()) {
                        name += " " + m.getSuffix();
                    }
                    return fr.getStringWidth(name);
                }));
                break;
            case "Bottom Right":
                isRight = true;
                isBottom = true;
                arrayListY = height - (enabledModules.size() * 10) - 2;
                enabledModules.sort(Comparator.comparingDouble(m -> {
                    String name = m.getName();
                    if (suffix && m.getSuffix() != null && !m.getSuffix().isEmpty()) {
                        name += " " + m.getSuffix();
                    }
                    return fr.getStringWidth(name);
                }));
                break;
        }

        float offset = 0;
        Module targetModule = null;
        float targetWidth = isBottom ? Float.MAX_VALUE : 0;

        for (Module module : enabledModules) {
            String moduleName = module.getName();
            String suffixText = "";
            if (suffix && module.getSuffix() != null && !module.getSuffix().isEmpty()) {
                suffixText = " " + module.getSuffix();
            }
            float moduleWidth = fr.getStringWidth(moduleName + suffixText);

            if (isBottom) {
                if (moduleWidth < targetWidth) {
                    targetWidth = moduleWidth;
                    targetModule = module;
                }
            } else {
                if (moduleWidth > targetWidth) {
                    targetWidth = moduleWidth;
                    targetModule = module;
                }
            }
        }

        float lastWidth = 0;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            String moduleName = module.getName();
            String suffixText = "";
            if (suffix && module.getSuffix() != null && !module.getSuffix().isEmpty()) {
                suffixText = " " + module.getSuffix();
            }
            float moduleWidth = fr.getStringWidth(moduleName + suffixText);

            float animationValue = (float) module.getAnimation().getOutput();

            float moduleX = isRight ? width - moduleWidth - 3 : arrayListX;
            float moduleY = arrayListY + offset;

            int bgColor = new Color(0, 0, 0, (int) (bgAlpha * animationValue)).getRGB();
            int textColor = new Color(
                    (themeColor >> 16) & 0xFF,
                    (themeColor >> 8) & 0xFF,
                    themeColor & 0xFF,
                    (int) (255 * animationValue)
            ).getRGB();

            mc.ingameGUI.drawRect((int) moduleX - 2, (int) moduleY - 1,
                    (int) (moduleX + moduleWidth + 2), (int) (moduleY + 9), bgColor);

            if (outlineMode.equals("Left")) {
                Gui.drawRect((int) moduleX - 2, (int) moduleY - 1, (int) moduleX - 1,
                        (int) (moduleY + 9), textColor);
            }

            if (outlineMode.equals("Right")) {
                Gui.drawRect((int) (moduleX + moduleWidth + 1), (int) moduleY - 1,
                        (int) (moduleX + moduleWidth + 2), (int) (moduleY + 9), textColor);
            }

            if (outlineMode.equals("Top") && module.equals(targetModule)) {
                Gui.drawRect((int) moduleX - 2, (int) moduleY - 2,
                        (int) (moduleX + moduleWidth + 2), (int) moduleY - 1, textColor);
            }

            fr.drawStringWithShadow(moduleName, moduleX, moduleY, textColor);
            if (!suffixText.isEmpty()) {
                float nameWidth = fr.getStringWidth(moduleName);
                int suffixColor = new Color(160, 160, 160, (int) (255 * animationValue)).getRGB();
                fr.drawStringWithShadow(suffixText, moduleX + nameWidth, moduleY, suffixColor);
            }

            offset += (float) (animationValue * 10);
            lastWidth = moduleWidth;
        }
    }
}
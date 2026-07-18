package wtf.fentanyl.gui.click.component;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.impl.render.HUD;
import wtf.fentanyl.client.modules.impl.render.PostProcessing;
import wtf.fentanyl.client.modules.values.Value;
import wtf.fentanyl.client.modules.values.impl.*;
import wtf.fentanyl.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class ModuleButton {

    private static final ResourceLocation moreIcon = new ResourceLocation("/client/icon/more.png");
    public static final float ROW_HEIGHT = 18f;
    private static final float ROW_GAP = 3f;
    private static final float ICON_SIZE = 12f;
    private static final Color PANEL_BG_TONE = new Color(28, 28, 28);

    private Module module;
    private float animation = 0;

    public ModuleButton(Module module) {
        this.module = module;
        this.animation = module.isExpanded() ? 1.0f : 0.0f;
    }

    public void updateAnimation() {
        float target = module.isExpanded() ? 1.0f : 0.0f;
        if (animation != target) {
            float speed = 0.15f;
            animation += (target - animation) * speed;
            if (Math.abs(animation - target) < 0.01f) {
                animation = target;
            }
        }
    }

    public float getAnimation() {
        return animation;
    }

    private HUD getHud() {
        if (Client.INSTANCE == null || Client.INSTANCE.getModuleManager() == null) {
            return null;
        }
        return (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
    }

    private CFontRenderer getFont() {
        HUD hud = getHud();
        return hud != null ? hud.fr : null;
    }

    private Color getThemeColor() {
        HUD hud = getHud();
        if (hud != null && hud.theme != null && hud.theme.get() != null) {
            return hud.theme.get();
        }
        return new Color(255, 50, 50);
    }

    private boolean isBlurOn() {
        if (Client.INSTANCE == null || Client.INSTANCE.getModuleManager() == null) {
            return false;
        }
        PostProcessing pp = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");
        return pp != null && pp.isToggled() && pp.blur.get();
    }

    public void render(float x, float y, float width, int mouseX, int mouseY, CFontRenderer font, Color themeColor, Module listeningModule, ModeValue expandedMode, ColorPicker colorPicker, TextValue editingText) {
        CFontRenderer currentFont = getFont();
        if (currentFont == null) currentFont = font;
        Color currentTheme = getThemeColor();

        boolean blurOn = isBlurOn();
        boolean hoverMod = isHovered(mouseX, mouseY, x + 1.5f, y, width - 3, ROW_HEIGHT);

        Color modBg;
        if (blurOn) {
            if (module.isToggled()) {
                modBg = new Color(currentTheme.getRed(), currentTheme.getGreen(), currentTheme.getBlue(), hoverMod ? 60 : 50);
            } else {
                modBg = new Color(PANEL_BG_TONE.getRed(), PANEL_BG_TONE.getGreen(), PANEL_BG_TONE.getBlue(), hoverMod ? 120 : 95);
            }
        } else {
            if (module.isToggled()) {
                modBg = hoverMod ? new Color(50, 50, 50) : new Color(45, 45, 45);
            } else {
                modBg = hoverMod ? new Color(34, 34, 34) : PANEL_BG_TONE;
            }
        }

        RenderUtil.drawRect(x + 1.5f, y, width - 3, ROW_HEIGHT, modBg);

        currentFont.drawString(module.getName(), x + 5, y + 5,
                module.isToggled() ? Color.WHITE.getRGB() : new Color(180, 180, 180).getRGB());

        if (!module.getValues().isEmpty()) {
            float iconX = x + width - 6 - ICON_SIZE;
            float iconY = y + (ROW_HEIGHT - ICON_SIZE) / 2f;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, module.isToggled() ? 1f : 0.6f);
            Minecraft.getMinecraft().getTextureManager().bindTexture(moreIcon);
            Gui.drawModalRectWithCustomSizedTexture((int) iconX, (int) iconY, 0, 0, (int) ICON_SIZE, (int) ICON_SIZE, (int) ICON_SIZE, (int) ICON_SIZE);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    public float renderSettings(float x, float y, float width, int mouseX, int mouseY, CFontRenderer font, Color themeColor, Module listeningModule, ModeValue expandedMode, ColorPicker colorPicker, TextValue editingText) {
        CFontRenderer currentFont = getFont();
        if (currentFont == null) currentFont = font;
        Color currentTheme = getThemeColor();

        boolean blurOn = isBlurOn();
        float offsetY = 0;

        if (animation > 0) {
            currentFont.drawString("Keybind", x + 5, y + offsetY + 3, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

            String keyName = Keyboard.getKeyName(module.getKey());
            if (listeningModule != null && listeningModule.equals(module)) {
                keyName = "...";
            }
            float keyX = x + width - 5;
            RenderUtil.drawRoundedRect(keyX - currentFont.getStringWidth(keyName) - 2, y + offsetY + 2.5f, currentFont.getStringWidth(keyName) + 4, 8 * animation, 2, new Color(45, 45, 45, (int) (255 * animation)));
            currentFont.drawString(keyName, keyX - currentFont.getStringWidth(keyName), y + offsetY + 3, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

            offsetY += 13 * animation;
            offsetY += 3 * animation;

            if (!module.getValues().isEmpty()) {
                for (Value value : module.getValues()) {
                    if (!value.canDisplay()) continue;

                    boolean hoverSetting = isHovered(mouseX, mouseY, x + 1.5f, y + offsetY, width - 3, 13);

                    if (value instanceof BoolValue) {
                        offsetY += renderBoolValue((BoolValue) value, x, y + offsetY, width, currentFont, currentTheme, hoverSetting, blurOn);
                    } else if (value instanceof SliderValue) {
                        offsetY += renderSliderValue((SliderValue) value, x, y + offsetY, width, mouseX, mouseY, currentFont, hoverSetting, blurOn);
                    } else if (value instanceof ModeValue) {
                        offsetY += renderModeValue((ModeValue) value, x, y + offsetY, width, currentFont, expandedMode, hoverSetting, blurOn);
                    } else if (value instanceof ColorValue) {
                        offsetY += renderColorValue((ColorValue) value, x, y + offsetY, width, mouseX, mouseY, currentFont, colorPicker, hoverSetting, blurOn);
                    } else if (value instanceof TextValue) {
                        offsetY += renderTextValue((TextValue) value, x, y + offsetY, width, currentFont, editingText, hoverSetting, blurOn);
                    }
                }
            }
        }

        return offsetY;
    }

    private float renderBoolValue(BoolValue boolValue, float x, float y, float width, CFontRenderer font, Color themeColor, boolean hover, boolean blurOn) {
        Color bg;
        if (blurOn) {
            bg = new Color(35, 35, 35, (int) (100 * animation));
        } else {
            bg = new Color(hover ? 40 : 35, hover ? 40 : 35, hover ? 40 : 35, (int) (255 * animation));
        }

        RenderUtil.drawRect(x + 1.5f, y, width - 3, 13 * animation, bg);
        font.drawString(boolValue.getName(), x + 5, y + 4, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

        float boxX = x + width - 14;
        float boxY = y + 3f;
        float boxW = 11;
        float boxH = 7;

        RenderUtil.drawRoundedRect(boxX, boxY, boxW, boxH * animation, 2, new Color(50, 50, 50, (int) (255 * animation)));

        if (boolValue.get()) {
            RenderUtil.drawRoundedRect(boxX + 1, boxY + 1, boxW - 2, (boxH - 2) * animation, 1,
                    new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int) (255 * animation)));
        }

        return 13 * animation;
    }

    private float renderSliderValue(SliderValue sliderValue, float x, float y, float width, int mouseX, int mouseY, CFontRenderer font, boolean hover, boolean blurOn) {
        float sliderVal = sliderValue.get();
        float min = sliderValue.getMin();
        float max = sliderValue.getMax();
        float percent = Math.min(Math.max((sliderVal - min) / (max - min), 0), 1);

        Color bg;
        if (blurOn) {
            bg = new Color(35, 35, 35, (int) (100 * animation));
        } else {
            bg = new Color(hover ? 40 : 35, hover ? 40 : 35, hover ? 40 : 35, (int) (255 * animation));
        }

        RenderUtil.drawRect(x + 1.5f, y, width - 3, 18 * animation, bg);

        float sliderX = x + 8;
        float sliderY = y + (18 * animation) / 2.0f - 3;
        float sliderWidth = width - 16;
        float sliderHeight = 6;

        RenderUtil.drawRoundedRect(sliderX, sliderY, sliderWidth, sliderHeight * animation, 2, new Color(45, 45, 45, (int) (255 * animation)));

        Color currentTheme = getThemeColor();
        if (percent > 0) {
            RenderUtil.drawRoundedRect(sliderX, sliderY, sliderWidth * percent, sliderHeight * animation, 2,
                    new Color(currentTheme.getRed(), currentTheme.getGreen(), currentTheme.getBlue(), (int) (255 * animation)));
        }

        font.drawString(sliderValue.getName(), x + 9, y + 5, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

        String formattedValue = String.valueOf(sliderVal);
        font.drawString(formattedValue, x + width - 8 - font.getStringWidth(formattedValue), y + 5,
                new Color(180, 180, 180, (int) (255 * animation)).getRGB());

        if (isHovered(mouseX, mouseY, sliderX, sliderY, sliderWidth, sliderHeight * animation) && Mouse.isButtonDown(0)) {
            double normalizedX = (mouseX - sliderX) / sliderWidth;
            normalizedX = Math.min(Math.max(normalizedX, 0), 1);
            double newValue = min + normalizedX * (max - min);
            newValue = Math.round(newValue * 100.0) / 100.0;
            newValue = Math.min(Math.max(newValue, min), max);
            sliderValue.setValue((float) newValue);
        }

        return 18 * animation;
    }

    private float renderModeValue(ModeValue modeValue, float x, float y, float width, CFontRenderer font, ModeValue expandedMode, boolean hover, boolean blurOn) {
        Color bg;
        if (blurOn) {
            bg = new Color(35, 35, 35, (int) (100 * animation));
        } else {
            bg = new Color(hover ? 40 : 35, hover ? 40 : 35, hover ? 40 : 35, (int) (255 * animation));
        }

        RenderUtil.drawRect(x + 1.5f, y, width - 3, 13 * animation, bg);
        font.drawString(modeValue.getName(), x + 5, y + 4, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

        String currentMode = modeValue.get();
        font.drawString(currentMode, x + width - 5 - font.getStringWidth(currentMode), y + 4, new Color(180, 180, 180, (int) (255 * animation)).getRGB());

        float offsetY = 13 * animation;

        if (expandedMode == modeValue) {
            for (String mode : modeValue.getModes()) {
                boolean isSelected = mode.equals(currentMode);
                Color modeBg = blurOn ? new Color(32, 32, 32, 100) : new Color(32, 32, 32);
                RenderUtil.drawRect(x + 1.5f, y + offsetY, width - 3, 13, modeBg);
                int textColor = isSelected ? new Color(220, 220, 220).getRGB() : new Color(110, 110, 110).getRGB();
                font.drawString(mode, x + 8, y + offsetY + 4, textColor);
                offsetY += 13;
            }
        }

        return offsetY;
    }

    private float renderColorValue(ColorValue colorValue, float x, float y, float width, int mouseX, int mouseY, CFontRenderer font, ColorPicker colorPicker, boolean hover, boolean blurOn) {
        float colorBoxHeight = 13;
        if (colorPicker != null && colorPicker.isExpanded(colorValue)) {
            colorBoxHeight = 82;
        }

        Color bg;
        if (blurOn) {
            bg = new Color(35, 35, 35, (int) (100 * animation));
        } else {
            bg = new Color(hover ? 40 : 35, hover ? 40 : 35, hover ? 40 : 35, (int) (255 * animation));
        }

        RenderUtil.drawRect(x + 1.5f, y, width - 3, colorBoxHeight * animation, bg);
        font.drawString(colorValue.getName(), x + 5, y + 4, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

        Color currentColor = colorValue.get();
        float colorBoxX = x + width - 22;
        float colorBoxY = y + 2.5f;
        float colorBoxW = 18;
        float colorBoxH = 8;

        RenderUtil.drawRoundedRect(colorBoxX, colorBoxY, colorBoxW, colorBoxH * animation, 2, new Color(50, 50, 50, (int) (255 * animation)));
        RenderUtil.drawRoundedRect(colorBoxX + 1, colorBoxY + 1, colorBoxW - 2, (colorBoxH - 2) * animation, 2, currentColor);

        if (colorPicker != null && colorPicker.isExpanded(colorValue)) {
            colorPicker.render(x + 5, y + 15, width - 10, 45, colorValue, mouseX, mouseY);

            float buttonY = y + 67;
            float buttonWidth = (width - 13) / 2;

            boolean hoverCopy = isHovered(mouseX, mouseY, x + 5, buttonY, buttonWidth, 11);
            boolean hoverPaste = isHovered(mouseX, mouseY, x + 8 + buttonWidth, buttonY, buttonWidth, 11);

            RenderUtil.drawRoundedRect(x + 5, buttonY, buttonWidth, 11, 2, hoverCopy ? new Color(55, 55, 55) : new Color(45, 45, 45));
            RenderUtil.drawRoundedRect(x + 8 + buttonWidth, buttonY, buttonWidth, 11, 2, hoverPaste ? new Color(55, 55, 55) : new Color(45, 45, 45));

            font.drawString("Copy", x + 5 + (buttonWidth - font.getStringWidth("Copy")) / 2, buttonY + 1.5f, new Color(200, 200, 200).getRGB());
            font.drawString("Paste", x + 8 + buttonWidth + (buttonWidth - font.getStringWidth("Paste")) / 2, buttonY + 1.5f, new Color(200, 200, 200).getRGB());
        }

        return colorBoxHeight * animation;
    }

    private float renderTextValue(TextValue textValue, float x, float y, float width, CFontRenderer font, TextValue editingText, boolean hover, boolean blurOn) {
        Color bg;
        if (blurOn) {
            bg = new Color(35, 35, 35, (int) (100 * animation));
        } else {
            bg = new Color(hover ? 40 : 35, hover ? 40 : 35, hover ? 40 : 35, (int) (255 * animation));
        }

        RenderUtil.drawRect(x + 1.5f, y, width - 3, 13 * animation, bg);
        font.drawString(textValue.getName(), x + 5, y + 4, new Color(255, 255, 255, (int) (255 * animation)).getRGB());

        String displayText = textValue.get();
        if (displayText.length() > 15) {
            displayText = displayText.substring(0, 15);
            textValue.setText(displayText);
        }
        if (displayText.isEmpty()) displayText = "Empty";
        if (editingText == textValue) displayText = textValue.get() + "_";

        font.drawString(displayText, x + width - 5 - font.getStringWidth(displayText), y + 3, new Color(180, 180, 180, (int) (255 * animation)).getRGB());

        return 13 * animation;
    }

    public void handleClick(float x, float y, float width, int mouseX, int mouseY, int mouseButton, Module[] listeningModule, ModeValue[] expandedMode, ColorPicker colorPicker, TextValue[] editingText) {
        boolean hoverMod = isHovered(mouseX, mouseY, x + 1.5f, y + 1, width - 3, ROW_HEIGHT);

        if (hoverMod && mouseButton == 0) {
            module.toggle();
        } else if (hoverMod && mouseButton == 1) {
            boolean wasExpanded = module.isExpanded();
            module.setExpanded(!module.isExpanded());
            if (wasExpanded && editingText[0] != null) {
                editingText[0] = null;
            }
        }

        float offsetY = ROW_HEIGHT;

        if (animation > 0) {
            if (isHovered(mouseX, mouseY, x + 1.5f, y + offsetY, width - 3, 8 * animation)) {
                if (listeningModule[0] != null && listeningModule[0].equals(module))
                    listeningModule[0] = null;
                else
                    listeningModule[0] = module;
            }

            offsetY += 13 * animation;
            offsetY += 3 * animation;

            if (!module.getValues().isEmpty()) {
                for (Value value : module.getValues()) {
                    if (!value.canDisplay()) continue;

                    if (value instanceof BoolValue) {
                        BoolValue boolValue = (BoolValue) value;
                        float boxX = x + width - 14;
                        if (isHovered(mouseX, mouseY, boxX, y + offsetY + 3f, 11, 7 * animation) && mouseButton == 0) {
                            boolValue.toggle();
                        }
                        offsetY += 13 * animation;
                    } else if (value instanceof SliderValue) {
                        offsetY += 18 * animation;
                    } else if (value instanceof ModeValue) {
                        ModeValue modeValue = (ModeValue) value;
                        if (isHovered(mouseX, mouseY, x + 1.5f, y + offsetY, width - 3, 13 * animation)) {
                            if (expandedMode[0] == modeValue) expandedMode[0] = null;
                            else expandedMode[0] = modeValue;
                        }
                        offsetY += 13 * animation;
                        if (expandedMode[0] == modeValue) {
                            for (String mode : modeValue.getModes()) {
                                if (isHovered(mouseX, mouseY, x + 1.5f, y + offsetY, width - 3, 13)) {
                                    modeValue.set(mode);
                                    expandedMode[0] = null;
                                }
                                offsetY += 13;
                            }
                        }
                    } else if (value instanceof ColorValue) {
                        ColorValue colorValue = (ColorValue) value;
                        float colorBoxX = x + width - 22;
                        float colorBoxY = y + offsetY + 2.5f;
                        float colorBoxW = 18;
                        float colorBoxH = 8;

                        if (isHovered(mouseX, mouseY, colorBoxX, colorBoxY, colorBoxW, colorBoxH * animation)) {
                            colorPicker.toggle(colorValue);
                        }

                        float colorBoxHeight = 13;
                        if (colorPicker.isExpanded(colorValue)) {
                            colorBoxHeight = 82;
                            colorPicker.handleClick(x + 5, y + offsetY + 15, width - 10, 45, colorValue, mouseX, mouseY);
                        }

                        offsetY += colorBoxHeight * animation;
                    } else if (value instanceof TextValue) {
                        TextValue textValue = (TextValue) value;
                        if (isHovered(mouseX, mouseY, x + 1.5f, y + offsetY, width - 3, 13 * animation)) {
                            if (editingText[0] == textValue) editingText[0] = null;
                            else editingText[0] = textValue;
                        }
                        offsetY += 13 * animation;
                    }
                }
            }
        }
    }

    public float calculateHeight(ModeValue expandedMode, ColorPicker colorPicker) {
        float height = ROW_HEIGHT;

        if (animation > 0) {
            float expandedHeight = 13 + ROW_GAP;

            if (!module.getValues().isEmpty()) {
                for (Value value : module.getValues()) {
                    if (!value.canDisplay()) continue;

                    if (value instanceof BoolValue) {
                        expandedHeight += 13;
                    } else if (value instanceof SliderValue) {
                        expandedHeight += 18;
                    } else if (value instanceof ModeValue) {
                        ModeValue modeValue = (ModeValue) value;
                        expandedHeight += 13;
                        if (expandedMode == modeValue) expandedHeight += modeValue.getModes().length * 13;
                    } else if (value instanceof ColorValue) {
                        ColorValue colorValue = (ColorValue) value;
                        expandedHeight += (colorPicker != null && colorPicker.isExpanded(colorValue)) ? 82 : 13;
                    } else if (value instanceof TextValue) {
                        expandedHeight += 13;
                    }
                }
            }

            height += expandedHeight * animation;
        }

        return height;
    }

    public Module getModule() {
        return module;
    }

    private boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}

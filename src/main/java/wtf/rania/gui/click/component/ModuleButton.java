package wtf.rania.gui.click.component;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.client.modules.values.Value;
import wtf.rania.client.modules.values.impl.*;
import wtf.rania.utility.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class ModuleButton {

    public static final float HEIGHT = 13f;

    private final Module module;
    private final Keybind keybind;
    private final Map<BoolValue, Switch> switches = new HashMap<>();
    private final Map<SliderValue, Slider> sliders = new HashMap<>();
    private final Map<ModeValue, Select> selects = new HashMap<>();
    private final Map<ColorValue, ColorPicker> pickers = new HashMap<>();

    private boolean expanded;
    private float openProgress = 0f;
    private long lastFrame = System.currentTimeMillis();
    private TextValue editingText = null;

    public ModuleButton(Module module) {
        this.module = module;
        this.keybind = new Keybind(module);
        for (Value value : module.getValues()) {
            if (value instanceof BoolValue) {
                switches.put((BoolValue) value, new Switch((BoolValue) value));
            } else if (value instanceof SliderValue) {
                sliders.put((SliderValue) value, new Slider((SliderValue) value));
            } else if (value instanceof ModeValue) {
                selects.put((ModeValue) value, new Select((ModeValue) value));
            } else if (value instanceof ColorValue) {
                pickers.put((ColorValue) value, new ColorPicker((ColorValue) value));
            }
        }
    }

    private Color getThemeColor() {
        if (Client.INSTANCE == null || Client.INSTANCE.getModuleManager() == null) {
            return new Color(63, 213, 255);
        }
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        if (hud != null && hud.theme != null && hud.theme.get() != null) {
            return hud.theme.get();
        }
        return new Color(63, 213, 255);
    }

    private float heightFor(Value value) {
        if (value instanceof BoolValue) return Switch.HEIGHT;
        if (value instanceof SliderValue) return Slider.HEIGHT;
        if (value instanceof ModeValue) {
            Select select = selects.get(value);
            return select != null ? select.getHeight() : Select.HEIGHT;
        }
        if (value instanceof ColorValue) {
            ColorPicker picker = pickers.get(value);
            return picker != null ? picker.getHeight() : ColorPicker.height;
        }
        if (value instanceof TextValue) return 13f;
        return 0f;
    }

    public void update(float mouseX, float mouseY, float x, float y, float width) {
        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastFrame) / 1000f, 0.016f);
        lastFrame = now;

        float target = expanded ? 1f : 0f;
        float speed = 14f;
        if (openProgress < target) {
            openProgress = Math.min(target, openProgress + delta * speed);
        } else if (openProgress > target) {
            openProgress = Math.max(target, openProgress - delta * speed);
        }

        float sy = y + HEIGHT + Keybind.HEIGHT;
        for (Value value : module.getValues()) {
            if (!value.canDisplay()) continue;
            if (value instanceof ModeValue) {
                Select select = selects.get(value);
                if (select != null) select.update(mouseX, mouseY, x, sy, width);
            }
            sy += heightFor(value);
        }
    }

    public void draw(float x, float y, float width, CFontRenderer font, int mouseX, int mouseY) {
        float eased = ease(openProgress);
        boolean hovering = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT;
        Color theme = getThemeColor();
        boolean on = module.isToggled();
        int bg = on ? theme.getRGB() : (hovering ? GuiTheme.BG_DARK : GuiTheme.BG);

        RenderUtil.drawRect(x, y, width, HEIGHT, bg);
        font.drawString(module.getName(), x + 4, y + 3, GuiTheme.TEXT);

        float settingsHeight = Keybind.HEIGHT;
        for (Value value : module.getValues()) {
            if (!value.canDisplay()) continue;
            settingsHeight += heightFor(value);
        }

        float visibleHeight = settingsHeight * eased;
        if (visibleHeight < 0.5f) {
            return;
        }

        beginScissor(x, y + HEIGHT, width, visibleHeight);
        float sy = y + HEIGHT;

        keybind.draw(x, sy, width, font);
        sy += Keybind.HEIGHT;

        for (Value value : module.getValues()) {
            if (!value.canDisplay()) continue;

            if (value instanceof BoolValue) {
                Switch sw = switches.get(value);
                sw.draw(x, sy, width, font, theme);
                sy += Switch.HEIGHT;
            } else if (value instanceof SliderValue) {
                Slider slider = sliders.get(value);
                slider.draw(x, sy, width, font, theme);
                sy += Slider.HEIGHT;
            } else if (value instanceof ModeValue) {
                Select select = selects.get(value);
                select.draw(x, sy, width, font, theme);
                sy += select.getHeight();
            } else if (value instanceof ColorValue) {
                ColorPicker picker = pickers.get(value);
                picker.draw(x, sy, width, font);
                sy += picker.getHeight();
            } else if (value instanceof TextValue) {
                sy += drawTextValue((TextValue) value, x, sy, width, font);
            }
        }

        endScissor();
    }

    private float drawTextValue(TextValue textValue, float x, float y, float width, CFontRenderer font) {
        RenderUtil.drawRect(x, y, width, 13, GuiTheme.BG_DARK);
        font.drawString(textValue.getName(), x + 4, y + 2, GuiTheme.TEXT);

        String displayText = textValue.get();
        if (displayText.length() > 15) {
            displayText = displayText.substring(0, 15);
            textValue.setText(displayText);
        }
        if (displayText.isEmpty()) displayText = "Empty";
        if (editingText == textValue) displayText = textValue.get() + "_";

        font.drawString(displayText, x + width - 4 - font.getStringWidth(displayText), y + 2, GuiTheme.TEXT_SECONDARY);
        return 13f;
    }

    private float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private void beginScissor(float sx, float sy, float sw, float sh) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        float scale = res.getScaleFactor();

        int scissorX = (int) (sx * scale);
        int scissorY = (int) (mc.displayHeight - (sy + sh) * scale);
        int scissorWidth = (int) Math.max(sw * scale, 1);
        int scissorHeight = (int) Math.max(sh * scale + 2, 1);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public boolean click(float mouseX, float mouseY, float x, float y, float width, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                expanded = !expanded;
            }
            return true;
        }
        if (!expanded) {
            return false;
        }

        float sy = y + HEIGHT;

        if (mouseX >= x && mouseX <= x + width && mouseY >= sy && mouseY <= sy + Keybind.HEIGHT) {
            keybind.mouseClick(mouseX, mouseY, x, sy, width);
            return true;
        }
        sy += Keybind.HEIGHT;

        for (Value value : module.getValues()) {
            if (!value.canDisplay()) continue;

            if (value instanceof BoolValue) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= sy && mouseY <= sy + Switch.HEIGHT) {
                    switches.get(value).click(x, sy, width, (int) mouseX, (int) mouseY);
                    return true;
                }
                sy += Switch.HEIGHT;
            } else if (value instanceof SliderValue) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= sy && mouseY <= sy + Slider.HEIGHT) {
                    sliders.get(value).mouseClick(mouseX, mouseY, x, sy, width);
                    return true;
                }
                sy += Slider.HEIGHT;
            } else if (value instanceof ModeValue) {
                Select select = selects.get(value);
                float selectHeight = select.getHeight();
                if (mouseX >= x && mouseX <= x + width && mouseY >= sy && mouseY <= sy + selectHeight) {
                    select.mouseClick(mouseX, mouseY, x, sy, width, button);
                    return true;
                }
                sy += selectHeight;
            } else if (value instanceof ColorValue) {
                ColorPicker picker = pickers.get(value);
                float pickerHeight = picker.getHeight();
                if (mouseX >= x && mouseX <= x + width && mouseY >= sy && mouseY <= sy + pickerHeight) {
                    picker.mouseClick(mouseX, mouseY, x, sy, width);
                    return true;
                }
                sy += pickerHeight;
            } else if (value instanceof TextValue) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= sy && mouseY <= sy + 13f) {
                    TextValue tv = (TextValue) value;
                    editingText = (editingText == tv) ? null : tv;
                    return true;
                }
                sy += 13f;
            }
        }
        return false;
    }

    public void mouseDrag(float mouseX, float mouseY, float x, float y, float width) {
        if (!expanded) {
            return;
        }
        float sy = y + HEIGHT + Keybind.HEIGHT;

        for (Value value : module.getValues()) {
            if (!value.canDisplay()) continue;

            if (value instanceof SliderValue) {
                Slider slider = sliders.get(value);
                if (slider != null) slider.mouseDrag(mouseX);
                sy += Slider.HEIGHT;
            } else if (value instanceof ColorValue) {
                ColorPicker picker = pickers.get(value);
                if (picker != null) picker.mouseDrag(mouseX, mouseY, x, sy);
                sy += picker != null ? picker.getHeight() : ColorPicker.height;
            } else if (value instanceof BoolValue) {
                sy += Switch.HEIGHT;
            } else if (value instanceof ModeValue) {
                Select select = selects.get(value);
                sy += select != null ? select.getHeight() : Select.HEIGHT;
            } else if (value instanceof TextValue) {
                sy += 13f;
            }
        }
    }

    public void mouseRelease() {
        for (Slider slider : sliders.values()) slider.mouseRelease();
        for (ColorPicker picker : pickers.values()) picker.mouseRelease();
    }

    public boolean onKey(char typedChar, int keyCode) {
        if (editingText != null) {
            if (keyCode == 14) {
                String currentText = editingText.getText();
                if (currentText.length() > 0) {
                    editingText.setText(currentText.substring(0, currentText.length() - 1));
                }
            } else if (keyCode == 28 || keyCode == 1) {
                editingText = null;
            } else if (editingText.isOnlyNumber()) {
                if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-') {
                    editingText.setText(editingText.getText() + typedChar);
                }
            } else if (typedChar >= 32 && typedChar < 127) {
                editingText.setText(editingText.getText() + typedChar);
            }
            return true;
        }
        return keybind.onKey(keyCode, true);
    }

    public boolean isCapturingInput() {
        return editingText != null || keybind.isListening();
    }

    public float getHeight() {
        float settingsHeight = Keybind.HEIGHT;
        for (Value value : module.getValues()) {
            if (!value.canDisplay()) continue;
            settingsHeight += heightFor(value);
        }
        return HEIGHT + settingsHeight * ease(openProgress);
    }

    public Module getModule() {
        return module;
    }

    public boolean isExpanded() {
        return expanded;
    }
}
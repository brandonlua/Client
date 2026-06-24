package wtf.fentanyl.gui.click;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.values.Value;
import wtf.fentanyl.client.modules.values.impl.ColorValue;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.modules.values.impl.TextValue;
import wtf.fentanyl.client.modules.impl.render.HUD;
import wtf.fentanyl.gui.click.component.CategoryPanel;
import wtf.fentanyl.gui.click.component.ColorPicker;
import wtf.fentanyl.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DropdownGUI extends GuiScreen {

    private CFontRenderer font;
    private String currentFont = null;

    private boolean dragging = false;
    private float dragOffsetX = 0, dragOffsetY = 0;
    private CategoryPanel draggedPanel = null;
    private Module listeningModule = null;

    private Map<Category, CategoryPanel> panels = new HashMap<>();

    private ColorPicker colorPicker = new ColorPicker();
    private ModeValue expandedMode = null;
    private TextValue editingText = null;

    private Color cachedThemeColor = Color.WHITE;

    public DropdownGUI() {
        float xOffset = 5;
        for (Category category : Category.values()) {
            try {
                panels.put(category, new CategoryPanel(category, xOffset, 5));
            } catch (Exception ignored) {
                // A single failing panel must never prevent the GUI from opening.
            }
            xOffset += 130 + 5;
        }

        String fontName = resolveFontName();
        this.font = buildFont(fontName);
        this.currentFont = fontName;
    }

    private String resolveFontName() {
        Module hud = Client.INSTANCE != null && Client.INSTANCE.getModuleManager() != null
                ? Client.INSTANCE.getModuleManager().getModule(HUD.class)
                : null;
        if (hud != null) {
            Value fontValue = hud.getValues().stream()
                    .filter(v -> v.getName().equals("Font"))
                    .findFirst()
                    .orElse(null);
            if (fontValue instanceof ModeValue) {
                String name = ((ModeValue) fontValue).get();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }
        return "Arial";
    }

    private CFontRenderer buildFont(String fontName) {
        try {
            return new CFontRenderer(new Font(fontName, Font.PLAIN, 18), true, true);
        } catch (Exception e) {
            // Guarantee a usable font so rendering can never NPE on a null renderer.
            return new CFontRenderer(new Font(Font.SANS_SERIF, Font.PLAIN, 18), true, true);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        updateThemeColor();
    }

    private void updateThemeColor() {
        if (Client.INSTANCE == null || Client.INSTANCE.getModuleManager() == null) {
            return;
        }
        Module hudModule = Client.INSTANCE.getModuleManager().getModule("HUD");
        if (hudModule != null) {
            Value themeValue = hudModule.getValues().stream()
                    .filter(v -> v.getName().equals("Theme"))
                    .findFirst()
                    .orElse(null);
            if (themeValue instanceof ColorValue) {
                Color color = ((ColorValue) themeValue).get();
                if (color != null) {
                    cachedThemeColor = color;
                }
            }
        }
    }

    private void updateFont() {
        String fontName = resolveFontName();

        if (!fontName.equals(currentFont) || font == null) {
            this.font = buildFont(fontName);
            currentFont = fontName;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Make sure the font and theme are valid before rendering anything.
        if (font == null) {
            updateFont();
        }
        if (cachedThemeColor == null) {
            cachedThemeColor = Color.WHITE;
        }

        RenderUtil.drawRect(0, 0, width, height, new Color(0, 0, 0, 150));

        for (CategoryPanel panel : panels.values()) {
            if (panel == null) {
                continue;
            }
            if (dragging && draggedPanel == panel) {
                panel.setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
            }

            try {
                panel.render(mouseX, mouseY, font, cachedThemeColor, listeningModule, expandedMode, colorPicker, editingText);
            } catch (Exception e) {
                // Never let a render-time error crash the game while the GUI is open.
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int wheel = Mouse.getEventDWheel();

        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -15 : 15;

            for (CategoryPanel panel : panels.values()) {
                if (panel.isHoveringPanel(mouseX, mouseY)) {
                    panel.scroll(scrollAmount);
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (CategoryPanel panel : panels.values()) {
            if (panel.isHoveringHeader(mouseX, mouseY) && mouseButton == 0) {
                dragging = true;
                draggedPanel = panel;
                dragOffsetX = mouseX - panel.getX();
                dragOffsetY = mouseY - panel.getY();
            }

            Module[] listeningRef = {listeningModule};
            ModeValue[] expandedRef = {expandedMode};
            TextValue[] editingRef = {editingText};

            panel.handleClick(mouseX, mouseY, mouseButton, listeningRef, expandedRef, colorPicker, editingRef);

            listeningModule = listeningRef[0];
            expandedMode = expandedRef[0];
            editingText = editingRef[0];
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = false;
        draggedPanel = null;
        colorPicker.reset();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (editingText != null) {
            if (keyCode == 14) {
                String currentText = editingText.getText();
                if (currentText.length() > 0) {
                    editingText.setText(currentText.substring(0, currentText.length() - 1));
                }
            } else if (keyCode == 28) {
                editingText = null;
            } else {
                if (editingText.isOnlyNumber()) {
                    if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-') {
                        editingText.setText(editingText.getText() + typedChar);
                    }
                } else {
                    if (typedChar >= 32 && typedChar < 127) {
                        editingText.setText(editingText.getText() + typedChar);
                    }
                }
            }
            return;
        }

        if (listeningModule != null) {
            if (keyCode == 14) {
                listeningModule.key = 0;
            } else {
                String keyName = Keyboard.getKeyName(keyCode);
                if (keyName != null && !keyName.isEmpty()) {
                    listeningModule.key = keyCode;
                }
            }
            listeningModule = null;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
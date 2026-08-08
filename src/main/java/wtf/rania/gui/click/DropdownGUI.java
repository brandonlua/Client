package wtf.rania.gui.click;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.client.modules.values.Value;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.gui.click.component.CategoryPanel;
import wtf.rania.gui.click.component.ModuleButton;
import net.minecraft.client.gui.GuiScreen;

import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DropdownGUI extends GuiScreen {

    private static DropdownGUI openInstance;

    private final List<CategoryPanel> panels = new ArrayList<>();
    private CFontRenderer font;
    private String currentFont = null;

    public DropdownGUI() {
        int i = 0;
        for (Category category : Category.values()) {
            panels.add(new CategoryPanel(category, 20 + i * (CategoryPanel.WIDTH + 10), 20));
            i++;
        }

        String fontName = resolveFontName();
        this.font = buildFont(fontName);
        this.currentFont = fontName;
    }

    private String resolveFontName() {
        HUD hud = Client.INSTANCE != null && Client.INSTANCE.getModuleManager() != null
                ? (HUD) Client.INSTANCE.getModuleManager().getModule(HUD.class)
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
            return new CFontRenderer(new Font(fontName, Font.PLAIN, 14), true, true);
        } catch (Exception e) {
            return new CFontRenderer(new Font(Font.SANS_SERIF, Font.PLAIN, 14), true, true);
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
    public void initGui() {
        super.initGui();
        openInstance = this;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (openInstance == this) {
            openInstance = null;
        }
    }

    public static DropdownGUI getOpenInstance() {
        return openInstance;
    }

    public boolean isCapturingKeybind() {
        for (CategoryPanel panel : panels) {
            for (ModuleButton button : panel.getModuleButtons()) {
                if (button.isCapturingInput()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (font == null) {
            updateFont();
        }
        for (CategoryPanel panel : panels) {
            panel.mouseDrag(mouseX, mouseY);
            panel.draw(mouseX, mouseY, font);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        for (CategoryPanel panel : panels) {
            panel.mouseClick(mouseX, mouseY, button);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
        for (CategoryPanel panel : panels) {
            panel.mouseDrag(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        for (CategoryPanel panel : panels) {
            panel.mouseRelease();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (CategoryPanel panel : panels) {
            for (ModuleButton button : panel.getModuleButtons()) {
                if (button.isCapturingInput()) {
                    button.onKey(typedChar, keyCode);
                    return;
                }
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
package wtf.rania.gui.click.component;

import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.Module;
import wtf.rania.utility.render.RenderUtil;
import org.lwjgl.input.Keyboard;

public class Keybind {

    public static final float HEIGHT = 12f;

    private final Module module;
    private boolean listening = false;

    public Keybind(Module module) {
        this.module = module;
    }

    public void draw(float x, float y, float width, CFontRenderer font) {
        RenderUtil.drawRect(x, y, width, HEIGHT, GuiTheme.BG_DARK);
        String display = listening ? ".." : Keyboard.getKeyName(module.getKey());
        font.drawString("Keybind: " + display, x + 4, y + 2, GuiTheme.TEXT_SECONDARY);
    }

    public void mouseClick(float mouseX, float mouseY, float x, float y, float width) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            listening = !listening;
        }
    }

    public boolean onKey(int key, boolean pressed) {
        if (!listening || !pressed) {
            return false;
        }

        if (key == Keyboard.KEY_ESCAPE) {
            listening = false;
            return true;
        }

        if (key == Keyboard.KEY_BACK) {
            module.key = 0;
            listening = false;
            return true;
        }

        module.key = key;
        listening = false;
        return true;
    }

    public boolean isListening() {
        return listening;
    }
}
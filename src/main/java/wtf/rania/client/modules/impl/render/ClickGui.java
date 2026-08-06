package wtf.rania.client.modules.impl.render;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.gui.click.DropdownGUI;
import org.lwjgl.input.Keyboard;

@ModuleInfo(name = "ClickGui", category = Category.RENDER)
public class ClickGui extends Module {

    public ClickGui() {
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            mc.displayGuiScreen(new DropdownGUI());
        }
        setEnabled(false);
    }
}

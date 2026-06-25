package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.gui.click.DropdownGUI;
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

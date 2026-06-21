package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.event.impl.EventKey;
import wtf.fentanyl.gui.click.DropdownGUI;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import org.lwjgl.input.Keyboard;

@ModuleInfo(name = "ClickGui", category = Category.RENDER)
public class ClickGui extends Module {

    public ClickGui() {
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Subscribe
    private final Listener<EventKey> eventkeyListener = new Listener<>(e -> {
        if (e.getKey() == getKey()) {
            mc.displayGuiScreen(new DropdownGUI());
        }
    });

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
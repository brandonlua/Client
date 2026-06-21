package wtf.fentanyl.client.modules.impl.movement;

import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.event.impl.UpdateEvent;
import wtf.fentanyl.event.impl.game.player.MoveInputEvent;
import wtf.fentanyl.util.player.MovementUtil;

import static wtf.fentanyl.util.InstanceAccess.mc;

@ModuleInfo(name = "InvMove", category = Category.MOVEMENT)
public class InvMove extends wtf.fentanyl.client.modules.Module {
    private final BoolValue cancelInventory = new BoolValue("NoInv", false, this);
    private final BoolValue cancelChest = new BoolValue("No Chest", false, this);
    private final KeyBinding[] keyBindings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindJump};

    public void onDisable() {
        for (KeyBinding keyBinding : this.keyBindings) {
            KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
        }
    }

    @Subscribe
    private void onUpdate(UpdateEvent event) {
        if (!(mc.currentScreen instanceof GuiChat) && !(mc.currentScreen instanceof GuiIngameMenu)) {
            if (cancelInventory.get() && (mc.currentScreen instanceof GuiContainer))
                return;

            if (cancelChest.get() && mc.currentScreen instanceof GuiChest)
                return;

            for (KeyBinding keyBinding : this.keyBindings) {
                KeyBinding.setKeyBindState(keyBinding.getKeyCode(), GameSettings.isKeyDown(keyBinding));
            }

        }
    }
}
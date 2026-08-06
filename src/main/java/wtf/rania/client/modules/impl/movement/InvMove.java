package wtf.rania.client.modules.impl.movement;

import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.event.impl.UpdateEvent;
import wtf.rania.event.impl.game.player.MoveInputEvent;
import wtf.rania.util.player.MovementUtil;

import static wtf.rania.util.InstanceAccess.mc;

@ModuleInfo(name = "InvMove", category = Category.MOVEMENT)
public class InvMove extends wtf.rania.client.modules.Module {
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
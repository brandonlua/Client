package wtf.rania.client.modules.impl.movement;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.event.impl.game.player.SlowdownEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@ModuleInfo(name = "NoSlow", category = Category.MOVEMENT, key = 0, enabled = false)
public class NoSlowModule extends Module {

    public ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "NCP"}, "Vanilla", this);
    public BoolValue sword = new BoolValue("Sword", true, this);
    public BoolValue food = new BoolValue("Food", true, this);
    public BoolValue bow = new BoolValue("Bow", true, this);

    @Subscribe
    private final Listener<SlowdownEvent> slowdownListener = new Listener<>(event -> {
        if (mc.thePlayer.getHeldItem() == null) return;

        boolean shouldCancel = false;

        if (sword.get() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            shouldCancel = true;
        }

        if (food.get() && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
            shouldCancel = true;
        }

        if (bow.get() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            shouldCancel = true;
        }

        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) {
            shouldCancel = true;
        }

        if (shouldCancel) {
            event.setForward(1.0f);
            event.setStrafe(1.0f);
            event.setAllowedSprinting(true);

            if (mode.get().equals("NCP") && mc.thePlayer.isBlocking()) {
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        EnumFacing.DOWN
                ));
            }
        }
    });

    @Override
    public String getSuffix() {
        return mode.get();
    }
}
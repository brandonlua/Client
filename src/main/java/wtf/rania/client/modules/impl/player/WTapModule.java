package wtf.rania.client.modules.impl.player;

import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C02PacketUseEntity;
import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.PacketEvent;
import wtf.rania.event.impl.game.player.TickEvent;

import java.lang.reflect.Field;

@ModuleInfo(name = "WTap", category = Category.MOVEMENT)
public class WTapModule extends Module {

    private final ModeValue mode = new ModeValue("Mode", new String[]{"single", "multi"}, "single", this);
    private final SliderValue delay = new SliderValue("Delay", 2F, 0F, 10F, 1F, this, () -> mode.is("multi"));
    private final SliderValue comboTimeout = new SliderValue("Timeout", 1000F, 200F, 5000F, 100F, this);

    private int tapTick;
    private boolean tapped;
    private long lastAttack;

    @Subscribe
    private final Listener<PacketEvent> packetListener = new Listener<>(event -> {
        if (event.isOutgoing() && event.getPacket() instanceof C02PacketUseEntity use
                && use.getAction() == C02PacketUseEntity.Action.ATTACK) {
            lastAttack = System.currentTimeMillis();
        }
    });

    @Subscribe
    private final Listener<TickEvent> tickListener = new Listener<>(event -> {
        if (mc.thePlayer == null) return;

        if (!inCombo()) {
            tapTick = 0;
            tapped = false;
            return;
        }

        if (!mc.thePlayer.isSprinting()) {
            tapTick = 0;
            tapped = false;
            return;
        }

        if (!isMovingForward()) {
            tapTick = 0;
            tapped = false;
            return;
        }

        if (mode.is("single")) {
            if (!tapped) {
                setKeyBindPressed(mc.gameSettings.keyBindForward, false);
                tapped = true;
            } else {
                setKeyBindPressed(mc.gameSettings.keyBindForward, true);
                tapped = false;
            }
        } else {
            tapTick++;
            if (tapTick >= (int) delay.get()) {
                setKeyBindPressed(mc.gameSettings.keyBindForward, false);
                tapTick = 0;
            } else {
                setKeyBindPressed(mc.gameSettings.keyBindForward, true);
            }
        }
    });

    private boolean inCombo() {
        return System.currentTimeMillis() - lastAttack <= (long) comboTimeout.get();
    }

    private boolean isMovingForward() {
        return mc.gameSettings.keyBindForward.isKeyDown() && mc.thePlayer.moveForward != 0;
    }

    private void setKeyBindPressed(KeyBinding keyBinding, boolean pressed) {
        try {
            Field field = KeyBinding.class.getDeclaredField("pressed");
            field.setAccessible(true);
            field.setBoolean(keyBinding, pressed);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
        tapTick = 0;
        tapped = false;
        lastAttack = 0;
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        setKeyBindPressed(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindForward.isKeyDown());
    }
}
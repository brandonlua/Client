package wtf.rania.client.modules.impl.player;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.UpdateEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;

import java.lang.reflect.Field;

@ModuleInfo(name = "FastPlace", description = "Makes you use items faster", category = Category.PLAYER
)
public class FastPlace extends Module {

    private final SliderValue rightDelay = new SliderValue("RMB delay", 0F, 0F, 1F, this);

    @Subscribe
    private Listener<UpdateEvent> updateListener;

    public FastPlace() {
        updateListener = new Listener<>(event -> {
            setRightClickDelayTimer((int) rightDelay.get());
        });
    }

    private void setRightClickDelayTimer(int value) {
        try {
            Field field = mc.getClass().getDeclaredField("rightClickDelayTimer");
            field.setAccessible(true);
            field.setInt(mc, value);
        } catch (Exception e) {
        }
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        setRightClickDelayTimer(4);
    }
}

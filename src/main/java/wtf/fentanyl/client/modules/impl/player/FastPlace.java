package wtf.fentanyl.client.modules.impl.player;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.UpdateEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;

import java.lang.reflect.Field;

@ModuleInfo(name = "FastPlace", description = "Makes you use items faster", category = Category.PLAYER
)
public class FastPlace extends Module {

    private final SliderValue rightDelay = new SliderValue("RMB delay", 0F, 0F, 1F, this);
    private final SliderValue leftDelay = new SliderValue("LMB delay", 0F, 0F, 1F, this);

    @Subscribe
    private Listener<UpdateEvent> updateListener;

    public FastPlace() {
        updateListener = new Listener<>(event -> {
            setRightClickDelayTimer((int) rightDelay.get());
            setLeftClickCounter((int) leftDelay.get());
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

    private void setLeftClickCounter(int value) {
        try {
            Field field = mc.getClass().getDeclaredField("leftClickCounter");
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
        setLeftClickCounter(10);
    }
}
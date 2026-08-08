package wtf.rania.client.modules.impl.combat;

import me.zero.alpine.listener.Subscribe;
import org.lwjgl.input.Mouse;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.UpdateEvent;
import wtf.rania.utility.math.TimerUtil;

import java.lang.reflect.Field;

@ModuleInfo(name = "AutoClicker", category = Category.COMBAT)
public class AutoClickerModule extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Toggle", "Hold"}, "Hold", this);
    public final SliderValue cps = new SliderValue("CPS", 12F, 1F, 20F, 0.1F, this);

    private final TimerUtil timer = new TimerUtil();

    @Override
    public void onDisabled() {
        timer.reset();
    }

    @Subscribe
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return;
        }

        boolean active = mode.is("Toggle") || Mouse.isButtonDown(0);
        if (!active) {
            timer.reset();
            return;
        }

        long delay = Math.max(1L, Math.round(1000.0 / cps.get()));
        if (!timer.hasTimeElapsed(delay)) {
            return;
        }

        clickOnce();

        timer.reset();
    }

    private void clickOnce() {
        resetLeftClickCounter();
        mc.clickMouse();
        resetLeftClickCounter();
    }

    private void resetLeftClickCounter() {
        try {
            Field field = mc.getClass().getDeclaredField("leftClickCounter");
            field.setAccessible(true);
            field.setInt(mc, 0);
        } catch (Exception ignored) {
        }
    }
}

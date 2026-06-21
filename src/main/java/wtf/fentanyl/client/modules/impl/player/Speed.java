package wtf.fentanyl.client.modules.impl.player;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.UpdateEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.settings.KeyBinding;

import java.lang.reflect.Field;
import java.util.Timer;

@ModuleInfo(name = "Speed", description = "makes you go faster", category = Category.MOVEMENT
)
public class Speed extends Module {

    private final SliderValue multiplier = new SliderValue("Multiplier", 1.0F, 0.0F, 10.0F, 0.1F, this);
    private final SliderValue friction = new SliderValue("Friction", 1.0F, 0.0F, 10.0F, 0.1F, this);
    private final SliderValue strafe = new SliderValue("Strafe", 0F, 0F, 100F, 1F, this);

    @Subscribe
    private Listener<UpdateEvent> updateListener;

    public Speed() {
        updateListener = new Listener<>(event -> {
            if (canBoost()) {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = 0.42;
                    setSpeed(getJumpMotion() * multiplier.get(), getMoveYaw());
                } else {
                    if (friction.get() != 1.0) {
                        mc.thePlayer.motionX *= friction.get();
                        mc.thePlayer.motionZ *= friction.get();
                    }
                    if (strafe.get() > 0) {
                        double speed = getSpeed();
                        setSpeed(speed * (100 - strafe.get()) / 100.0, getDirectionYaw());
                        addSpeed(speed * strafe.get() / 100.0, getMoveYaw());
                        setSpeed(speed);
                    }
                }
                setKeyBindPressed(mc.gameSettings.keyBindJump, false);
            }
        });
    }

    private boolean canBoost() {
        return isForwardPressed()
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isInLava();
    }

    private boolean isForwardPressed() {
        return mc.gameSettings.keyBindForward.isKeyDown();
    }

    private double getSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    private void setSpeed(double speed) {
        double yaw = getMoveYaw();
        mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }

    private void setSpeed(double speed, float yaw) {
        mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }

    private void addSpeed(double speed, float yaw) {
        mc.thePlayer.motionX += -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ += Math.cos(Math.toRadians(yaw)) * speed;
    }

    private float getMoveYaw() {
        float yaw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.moveForward < 0.0F) {
            yaw += 180.0F;
        }
        float forward = 1.0F;
        if (mc.thePlayer.moveForward < 0.0F) {
            forward = -0.5F;
        } else if (mc.thePlayer.moveForward > 0.0F) {
            forward = 0.5F;
        }
        if (mc.thePlayer.moveStrafing > 0.0F) {
            yaw -= 90.0F * forward;
        }
        if (mc.thePlayer.moveStrafing < 0.0F) {
            yaw += 90.0F * forward;
        }
        return yaw;
    }

    private float getDirectionYaw() {
        float rotationYaw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }
        float forward = 1.0F;
        if (mc.thePlayer.moveForward < 0.0F) {
            forward = -0.5F;
        } else if (mc.thePlayer.moveForward > 0.0F) {
            forward = 0.5F;
        }
        if (mc.thePlayer.moveStrafing > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }
        if (mc.thePlayer.moveStrafing < 0.0F) {
            rotationYaw += 90.0F * forward;
        }
        return rotationYaw;
    }

    private double getJumpMotion() {
        return 0.2873;
    }

    private void setKeyBindPressed(KeyBinding keyBinding, boolean pressed) {
        try {
            Field field = KeyBinding.class.getDeclaredField("pressed");
            field.setAccessible(true);
            field.setBoolean(keyBinding, pressed);
        } catch (Exception e) {
        }
    }

    private void setTimerSpeed(float speed) {
        try {
            Field field = mc.getClass().getDeclaredField("timer");
            field.setAccessible(true);
            Timer timer = (Timer) field.get(mc);
            Field speedField = Timer.class.getDeclaredField("timerSpeed");
            speedField.setAccessible(true);
            speedField.setFloat(timer, speed);
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
        setKeyBindPressed(mc.gameSettings.keyBindSprint, false);
        setKeyBindPressed(mc.gameSettings.keyBindJump, false);
        setTimerSpeed(1.0F);
    }
}
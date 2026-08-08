package wtf.rania.client.modules.impl.combat;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.PacketEvent;
import wtf.rania.event.impl.UpdateEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "Velocity" ,category = Category.COMBAT)
public class VelocityModule extends Module {

    private ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Jump", "Legit"}, "Vanilla", this);
    private SliderValue chance = new SliderValue("Chance", 100F, 0F, 100F, this);
    private SliderValue horizontal = new SliderValue("Horizontal", 0F, 0F, 100F, this);
    private SliderValue vertical = new SliderValue("Vertical", 100F, 0F, 100F, this);
    private SliderValue delayTicks = new SliderValue("Delay Ticks", 3F, 1F, 20F, this, () -> mode.is("Delay"));
    private SliderValue delayChance = new SliderValue("Delay Chance", 100F, 0F, 100F, this, () -> mode.is("Delay"));

    private int chanceCounter = 0;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean shouldJump = false;
    private int jumpCooldown = 0;

    @Subscribe
    private final Listener<PacketEvent.Receive> packetEventListener = new Listener<>(event -> {
        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            handleVelocityPacket(event, (S12PacketEntityVelocity) event.getPacket());
        } else if (event.getPacket() instanceof S27PacketExplosion) {
            handleExplosionPacket(event, (S27PacketExplosion) event.getPacket());
        } else if (event.getPacket() instanceof S19PacketEntityStatus) {
            handleEntityStatusPacket((S19PacketEntityStatus) event.getPacket());
        }
    });

    @Subscribe
    private final Listener<UpdateEvent> updateListener = new Listener<>(event -> {
        if (jumpFlag && mc.thePlayer.onGround && mc.thePlayer.isSprinting()) {
            mc.thePlayer.jump();
            jumpFlag = false;
        }

        if (mode.is("Legit")) {
            int hurtTime = mc.thePlayer.hurtTime;

            if (hurtTime >= 8) {
                if (jumpCooldown <= 0) {
                    shouldJump = true;
                    jumpCooldown = 2;
                }
            } else if (hurtTime <= 1) {
                shouldJump = false;
                jumpCooldown = 0;
            }

            if (shouldJump && mc.thePlayer.onGround && jumpCooldown <= 0) {
                mc.thePlayer.jump();
                shouldJump = false;
            }

            if (jumpCooldown > 0) {
                jumpCooldown--;
            }
        }
    });

    private void handleVelocityPacket(PacketEvent.Receive event, S12PacketEntityVelocity packet) {
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

        chanceCounter = (chanceCounter % 100) + (int) chance.get();
        if (chanceCounter < 100) {
            return;
        }

        if ((mode.is("Jump") || mode.is("Delay")) && packet.getMotionY() > 0) {
            jumpFlag = true;
        }

        modifyPacket(packet, horizontal.get(), vertical.get());
    }

    private void handleExplosionPacket(PacketEvent.Receive event, S27PacketExplosion packet) {
        if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {

        }
    }

    private void handleEntityStatusPacket(S19PacketEntityStatus packet) {
        Entity entity = packet.getEntity(mc.theWorld);
        if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
            allowNext = false;
        }
    }

    private void modifyPacket(S12PacketEntityVelocity packet, float horizPercent, float vertPercent) {
        if (horizPercent > 0) {
            packet.motionX = (int) (packet.getMotionX() * horizPercent / 100.0);
            packet.motionZ = (int) (packet.getMotionZ() * horizPercent / 100.0);
        } else {
            packet.motionX = 0;
            packet.motionZ = 0;
        }

        if (vertPercent > 0) {
            packet.motionY = (int) (packet.getMotionY() * vertPercent / 100.0);
        } else {
            packet.motionY = 0;
        }
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        allowNext = true;
        shouldJump = false;
        jumpCooldown = 0;
        jumpFlag = false;
    }
}
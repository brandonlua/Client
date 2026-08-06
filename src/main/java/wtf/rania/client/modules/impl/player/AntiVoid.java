package wtf.rania.client.modules.impl.player;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;

@ModuleInfo(name = "AntiVoid", description = "Prevents falling into the void", category = Category.PLAYER, key = 0, enabled = false)
public class AntiVoid extends Module {

    public ModeValue mode = new ModeValue("Mode", new String[]{"Flag", "Collision flag", "Blink", "Bounce"}, "Flag", this);
    public BoolValue stopHorizontalMove = new BoolValue("Stop horizontal move", false, this, () -> mode.is("Blink"));
    public SliderValue bounceMotion = new SliderValue("Bounce motion", 1.5f, 0.4f, 3f, this, () -> mode.is("Bounce"));
    public SliderValue minFallDist = new SliderValue("Min fall dist", 3.5f, 2f, 10f, this);

    private PlayerInfo lastSafePos;
    private BlockPos collisionBlock;
    private boolean blinking;
    private boolean receivedLagback;

    @Override
    public void onEnabled() {
        collisionBlock = null;

        if (mc.thePlayer != null) {
            lastSafePos = new PlayerInfo(
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                    mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ,
                    mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
                    mc.thePlayer.onGround, mc.thePlayer.fallDistance,
                    mc.thePlayer.inventory.currentItem
            );
        }
    }

    @Override
    public void onDisabled() {
        receivedLagback = false;
        if (collisionBlock != null) {
            mc.theWorld.setBlockToAir(collisionBlock);
            collisionBlock = null;
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 10) {
            collisionBlock = null;
            return;
        }

        switch (mode.get()) {
            case "Bounce":
                if (shouldSetback() && mc.thePlayer.motionY < -0.1) {
                    mc.thePlayer.motionY = bounceMotion.get();
                }
                break;

            case "Collision flag":
                if (shouldSetback()) {
                    if (collisionBlock != null) {
                        mc.theWorld.setBlockToAir(collisionBlock);
                    }

                    collisionBlock = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
                    mc.theWorld.setBlockState(collisionBlock, Blocks.barrier.getDefaultState());
                } else {
                    if (collisionBlock != null) {
                        mc.theWorld.setBlockToAir(collisionBlock);
                        collisionBlock = null;
                    }
                }
                break;

            case "Blink":
                if (isSafe()) {
                    lastSafePos = new PlayerInfo(
                            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                            mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ,
                            mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
                            mc.thePlayer.onGround, mc.thePlayer.fallDistance,
                            mc.thePlayer.inventory.currentItem
                    );

                    receivedLagback = false;
                    blinking = false;
                } else if (!receivedLagback) {
                    if (shouldSetback()) {
                        if (blinking) {
                            mc.thePlayer.setPosition(lastSafePos.x, lastSafePos.y, lastSafePos.z);

                            if (stopHorizontalMove.get()) {
                                mc.thePlayer.motionX = 0;
                                mc.thePlayer.motionZ = 0;
                            } else {
                                mc.thePlayer.motionX = lastSafePos.motionX;
                                mc.thePlayer.motionZ = lastSafePos.motionZ;
                            }

                            mc.thePlayer.motionY = lastSafePos.motionY;
                            mc.thePlayer.rotationYaw = lastSafePos.yaw;
                            mc.thePlayer.rotationPitch = lastSafePos.pitch;
                            mc.thePlayer.onGround = lastSafePos.onGround;
                            mc.thePlayer.fallDistance = lastSafePos.fallDist;
                            mc.thePlayer.inventory.currentItem = lastSafePos.itemSlot;
                        }
                    } else {
                        if (!blinking) {
                            blinking = true;
                        }
                    }
                }
                break;
        }
    }

    public void onMotionPre(double x, double y, double z) {
        if (mode.get().equals("Flag")) {
            if (shouldSetback()) {
                y += 8 + Math.random();
            }
        }
    }

    public void onReceivePacket(Object packet) {
        if (packet instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook s08 = (S08PacketPlayerPosLook) packet;

            if (mode.get().equals("Blink") && blinking) {
                mc.thePlayer.onGround = false;
                mc.thePlayer.fallDistance = lastSafePos.fallDist;
                mc.thePlayer.inventory.currentItem = lastSafePos.itemSlot;

                lastSafePos = new PlayerInfo(
                        s08.getX(), s08.getY(), s08.getZ(),
                        0, 0, 0,
                        s08.getYaw(), s08.getPitch(),
                        false, mc.thePlayer.fallDistance,
                        mc.thePlayer.inventory.currentItem
                );

                blinking = false;
                receivedLagback = true;
            }
        }
    }

    private boolean shouldSetback() {
        return mc.thePlayer.fallDistance >= minFallDist.get()
                && !isBlockUnder()
                && mc.thePlayer.ticksExisted >= 100;
    }

    private boolean isSafe() {
        return isBlockUnder()
                || mc.thePlayer.ticksExisted < 100;
    }

    private boolean isBlockUnder() {
        for (int i = (int) mc.thePlayer.posY; i >= 0; i--) {
            BlockPos pos = new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ);
            if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }

    private static class PlayerInfo {
        private final double x, y, z;
        private final double motionX, motionY, motionZ;
        private final float yaw, pitch;
        private final boolean onGround;
        private final float fallDist;
        private final int itemSlot;

        private PlayerInfo(double x, double y, double z, double motionX, double motionY, double motionZ,
                           float yaw, float pitch, boolean onGround, float fallDist, int itemSlot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.fallDist = fallDist;
            this.itemSlot = itemSlot;
        }
    }
}
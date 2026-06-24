package wtf.fentanyl.client.modules.impl.combat;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.ColorValue;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.Event3D;
import wtf.fentanyl.event.impl.UpdateEvent;
import wtf.fentanyl.event.impl.game.player.KeepSprintEvent;
import wtf.fentanyl.event.impl.game.player.MotionEvent;
import wtf.fentanyl.event.impl.game.player.SprintEvent;
import wtf.fentanyl.event.impl.game.player.TickEvent;
import wtf.fentanyl.util.player.Rotation;
import wtf.fentanyl.util.player.RotationUtil;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleInfo(name = "KillAura", category = Category.COMBAT)
public class KillAura extends Module {

    public final ModeValue attackMode = new ModeValue("Attack Mode", new String[]{"Single"}, "Single", this);
    public final ModeValue sortMode = new ModeValue("Sort Mode", new String[]{"Range", "Health", "Armor", "Hurt-time", "Ticks"}, "Range", this);
    public final ModeValue autoBlockMode = new ModeValue("Auto-Block Mode", new String[]{"None", "Vanilla", "Packet", "Spoof", "Hypixel", "Blink", "Interact", "Swap", "Legit", "Fake"}, "None", this);
    public final ModeValue swingMode = new ModeValue("Swing Mode", new String[]{"Legit", "Blatant"}, "Legit", this);

    public final SliderValue attackRange = new SliderValue("Attack Range", 3.4f, 1.0f, 6.0f, 0.1f, this);
    public final SliderValue searchRange = new SliderValue("Search Range", 4.3f, 1.0f, 7.0f, 0.1f, this);
    public final SliderValue maxCPS = new SliderValue("Max CPS", 11.0f, 1.0f, 20.0f, 1.0f, this);
    public final SliderValue minCPS = new SliderValue("Min CPS", 10.0f, 1.0f, 20.0f, 1.0f, this);

    public final BoolValue players = new BoolValue("Players", true, this);
    public final BoolValue animals = new BoolValue("Animals", false, this);
    public final BoolValue monsters = new BoolValue("Monsters", false, this);
    public final BoolValue raytrace = new BoolValue("Raytrace", true, this);
    public final BoolValue angleLock = new BoolValue("AngleLock", true, this);
    public final BoolValue noSwing = new BoolValue("No Swing", false, this);
    public final BoolValue sprintReset = new BoolValue("Sprint Reset", true, this);
    public final BoolValue keepSprint = new BoolValue("Keep Sprint", false, this);
    public final BoolValue visualizeRange = new BoolValue("Range Visualization", false, this);
    public final ColorValue rangeColor = new ColorValue("Visualization Color", Color.WHITE, this);

    private long lastAttackTime = 0;
    private float rangeFix = 3;
    private boolean isBlocking = false;
    private boolean blockingState = false;
    private boolean fakeBlockState = false;
    private int blockTick = 0;
    private Rotation rots;
    private float aimYaw;
    private float aimPitch;
    private float alpha = 0;

    @Subscribe
    private final Listener<SprintEvent> sprintListener = new Listener<>(event -> {
        if (sprintReset.get() && rots != null) {
            event.setCancelled(true);
        }
    });

    @Subscribe
    private final Listener<KeepSprintEvent> keepSprintListener = new Listener<>(event -> {
        if (keepSprint.get()) {
            event.greater = false;
        }
    });

    @Subscribe
    private final Listener<UpdateEvent> updateListener = new Listener<>(event -> {
        // AngleLock (silent aim): the first-person camera and the viewpoint are decoupled
        // (asynchronous) - the player keeps full, free mouse look while the VIEWPOINT is what
        // locks onto the enemy. The viewpoint is eased smoothly toward the closest point of the
        // target, quantised to the mouse-sensitivity step (GCD), and that one value drives the
        // look packet, the body/head orientation and the movement physics. Movement travels in
        // the CAMERA direction (where the player is actually looking) via the move-fix, so
        // strafing around a target feels normal. Runs before the tick's movement so the
        // move-fix picks up this viewpoint.
        EntityLivingBase target = Client.INSTANCE.targetProcess.target;
        if (target != null && angleLock.get()) {
            // Aim at the closest point of the enemy's hitbox to the player's eyes (so the
            // viewpoint locks onto the nearest part of the enemy, not a fixed centre).
            Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
            Vec3 hit = RotationUtil.getBestHitVec(target);
            double dx = hit.xCoord - eyes.xCoord;
            double dy = hit.yCoord - eyes.yCoord;
            double dz = hit.zCoord - eyes.zCoord;
            double dist = Math.sqrt(dx * dx + dz * dz);
            float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

            // The viewpoint persists independently of the free camera. On the first tick of a
            // lock it starts from the camera so it eases on smoothly instead of snapping.
            boolean firstLock = rots == null;
            if (firstLock) {
                aimYaw = mc.thePlayer.rotationYaw;
                aimPitch = mc.thePlayer.rotationPitch;
            }
            stepAimTowards(targetYaw, targetPitch);
            broadcastAim(firstLock);
        } else if (rots != null) {
            // Releasing: ease the viewpoint smoothly back onto the free camera instead of
            // snapping (a one-tick jump from the enemy back to the camera would spike the
            // rotation speed). Keep the move-fix armed during the hand-off so movement still
            // matches the rotation being sent, then drop the override once it has caught up.
            float camYaw = mc.thePlayer.rotationYaw;
            float camPitch = mc.thePlayer.rotationPitch;
            stepAimTowards(camYaw, camPitch);
            boolean caughtUp = Math.abs(MathHelper.wrapAngleTo180_float(camYaw - aimYaw)) < 2.0F
                    && Math.abs(camPitch - aimPitch) < 2.0F;
            if (caughtUp) {
                rots = null;
                RotationUtil.moveFix = false;
                RotationUtil.silentPitchActive = false;
            } else {
                broadcastAim(false);
            }
        } else {
            rots = null;
            RotationUtil.moveFix = false;
            RotationUtil.silentPitchActive = false;
        }
    });

    @Subscribe
    private final Listener<MotionEvent> motionListener = new Listener<>(event -> {
        if (event.getState() == MotionEvent.State.PRE) {
            double clamp = Math.max(1, Math.min(mc.getDebugFPS() / 30.0, 9999));
            if (alpha < 1) {
                alpha = (float) (alpha + (1 - alpha) * (0.99F / clamp));
            }
            alpha = Math.max(0, Math.min(alpha, 1));

            if (mc.thePlayer.ticksExisted % 20 == 0) {
                rangeFix = (int) (attackRange.get() + Math.random() * 0.4);
            }

            // Silent aim: send the viewpoint (computed in the update listener) in the outgoing
            // look packet while the camera the player controls stays free. Point the head at
            // the viewpoint and let the body ease toward it with the natural ~75 deg lag, so
            // the character turns just like vanilla as the viewpoint moves.
            if (rots != null && angleLock.get()) {
                event.setYaw(rots.yaw);
                event.setPitch(rots.pitch);

                mc.thePlayer.rotationYawHead = rots.yaw;
                float ease = MathHelper.wrapAngleTo180_float(rots.yaw - mc.thePlayer.renderYawOffset);
                mc.thePlayer.renderYawOffset += ease * 0.3F;
                float headBody = MathHelper.wrapAngleTo180_float(rots.yaw - mc.thePlayer.renderYawOffset);
                headBody = MathHelper.clamp_float(headBody, -75.0F, 75.0F);
                mc.thePlayer.renderYawOffset = rots.yaw - headBody;
            }
        }
    });

    @Subscribe
    private final Listener<Event3D> render3DListener = new Listener<>(e -> {
        if (visualizeRange.get()) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);

            GL11.glLineWidth(6);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor4f(0, 0, 0, alpha);

            for (int i = 0; i <= 8; i++) {
                double angle = (Math.PI * 2) * i / 8.0;
                double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * e.getPartialTicks() + Math.sin(angle) * attackRange.get() - mc.getRenderManager().viewerPosX;
                double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * e.getPartialTicks() - mc.getRenderManager().viewerPosY;
                double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * e.getPartialTicks() + Math.cos(angle) * attackRange.get() - mc.getRenderManager().viewerPosZ;
                GL11.glVertex3d(x, y, z);
            }

            GL11.glEnd();

            GL11.glLineWidth(3);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor4f(rangeColor.get().getRed() / 255F, rangeColor.get().getGreen() / 255F, rangeColor.get().getBlue() / 255F, 1);

            for (int i = 0; i <= 8; i++) {
                double angle = (Math.PI * 2) * i / 8.0;
                double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * e.getPartialTicks() + Math.sin(angle) * attackRange.get() - mc.getRenderManager().viewerPosX;
                double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * e.getPartialTicks() - mc.getRenderManager().viewerPosY;
                double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * e.getPartialTicks() + Math.cos(angle) * attackRange.get() - mc.getRenderManager().viewerPosZ;
                GL11.glVertex3d(x, y, z);
            }

            GL11.glEnd();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
    });

    @Subscribe
    private final Listener<TickEvent> tickListener = new Listener<>(event -> {
        // The viewpoint lifecycle (including easing back onto the camera when the target is
        // dropped) is owned by the update listener now, so don't clear rots here - just skip
        // attacking when there's nothing to hit.
        if (Client.INSTANCE.targetProcess.target == null) {
            return;
        }

        if (attackMode.get().equals("Single")) {
            if (System.currentTimeMillis() - lastAttackTime >= calculateAttackDelay() &&
                    Client.INSTANCE.targetProcess.target.getDistanceToEntity(mc.thePlayer) <= rangeFix) {

                performBlock(false);

                if (!noSwing.get()) {
                    mc.thePlayer.swingItem();
                } else {
                    mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
                }

                // Attack the locked target directly so the silent rotation actually lands.
                // (mc.clickMouse() raytraces from the free-look view, which would miss when
                // the player isn't physically looking at the target.)
                EntityLivingBase target = Client.INSTANCE.targetProcess.target;
                if (target != null) {
                    mc.playerController.attackEntity(mc.thePlayer, target);
                }

                performBlock(true);
                lastAttackTime = System.currentTimeMillis();
            }
        }
    });

    private long calculateAttackDelay() {
        long cps = (long) ((minCPS.get() + maxCPS.get()) / 2);
        return 1000 / cps;
    }

    // Ease the viewpoint one tick toward (targetYaw, targetPitch). The turn is a gentle
    // fraction of the remaining angle - fast when far, slowing as it settles - with a per-tick
    // speed cap and a little randomness that fades out as it settles, so the motion is smooth
    // and hand-made rather than a robotic straight line, and never exceeds a human turn speed.
    private void stepAimTowards(float targetYaw, float targetPitch) {
        float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - aimYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(targetPitch - aimPitch);

        // Lower fraction + cap = a softer, smoother glide onto the target.
        float smooth = 0.18F + (float) (Math.random() * 0.07F);
        yawDelta *= smooth;
        pitchDelta *= smooth;

        float maxYaw = 18.0F + (float) (Math.random() * 5.0F);
        float maxPitch = 12.0F + (float) (Math.random() * 4.0F);
        yawDelta = MathHelper.clamp_float(yawDelta, -maxYaw, maxYaw);
        pitchDelta = MathHelper.clamp_float(pitchDelta, -maxPitch, maxPitch);

        // Micro-jitter, scaled by how fast we're still moving, so it vanishes once locked on
        // (keeps the glide from being a dead-straight line without adding visible trembling).
        float settle = Math.min(1.0F, (Math.abs(yawDelta) + Math.abs(pitchDelta)) / 10.0F);
        yawDelta += (float) (Math.random() - 0.5) * 0.3F * settle;
        pitchDelta += (float) (Math.random() - 0.5) * 0.15F * settle;

        aimYaw += yawDelta;
        aimPitch = MathHelper.clamp_float(aimPitch + pitchDelta, -90.0F, 90.0F);
    }

    // Quantise the eased viewpoint to the mouse-sensitivity step (GCD) and publish it. The one
    // quantised value feeds everything that must agree: the look packet (sent in the motion
    // listener), the yaw the move-fix runs movement with so travel follows the free camera, and
    // the pitch the local model is rendered at - so the camera stays free yet nothing desyncs.
    private void broadcastAim(boolean firstLock) {
        Rotation aim = RotationUtil.applyGCD(aimYaw, aimPitch);
        rots = aim;

        RotationUtil.prevSilentPitch = firstLock ? mc.thePlayer.rotationPitch : RotationUtil.silentPitch;
        RotationUtil.silentPitch = aim.pitch;
        RotationUtil.silentPitchActive = true;

        RotationUtil.moveFix = true;
        RotationUtil.moveFixYaw = aim.yaw;
    }

    private void performBlock(boolean stop) {
        if (!stop) {
            switch (autoBlockMode.get()) {
                case "None":
                    break;

                case "Vanilla":
                    startVanillaBlock();
                    break;

                case "Packet":
                    startPacketBlock();
                    break;

                case "Spoof":
                    if (Client.INSTANCE.targetProcess.target != null) {
                        int item = mc.thePlayer.inventory.currentItem;
                        if (isPlayerBlocking() && blockTick != 0) {
                            blockTick = 0;
                        } else {
                            int slot = findEmptySlot(item);
                            mc.getNetHandler().getNetworkManager().sendPacket(new C09PacketHeldItemChange(slot));
                            mc.getNetHandler().getNetworkManager().sendPacket(new C09PacketHeldItemChange(item));
                            startPacketBlock();
                            blockTick = 1;
                        }
                        isBlocking = true;
                        fakeBlockState = false;
                    }
                    break;

                case "Hypixel":
                    if (Client.INSTANCE.targetProcess.target != null) {
                        switch (blockTick) {
                            case 0:
                                if (!isPlayerBlocking()) {
                                    startPacketBlock();
                                }
                                blockTick = 1;
                                break;
                            case 1:
                                if (isPlayerBlocking()) {
                                    int randomSlot = (int)(Math.random() * 9);
                                    while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                        randomSlot = (int)(Math.random() * 9);
                                    }
                                    mc.getNetHandler().getNetworkManager().sendPacket(new C09PacketHeldItemChange(randomSlot));
                                    mc.getNetHandler().getNetworkManager().sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                                    stopPacketBlock();
                                }
                                break;
                        }
                        isBlocking = true;
                        fakeBlockState = true;
                    }
                    break;

                case "Blink":
                    if (Client.INSTANCE.targetProcess.target != null) {
                        switch (blockTick) {
                            case 0:
                                if (!isPlayerBlocking()) {
                                    startPacketBlock();
                                }
                                blockTick = 1;
                                break;
                            case 1:
                                if (isPlayerBlocking()) {
                                    stopPacketBlock();
                                }
                                break;
                        }
                        isBlocking = true;
                        fakeBlockState = true;
                    }
                    break;

                case "Interact":
                    if (Client.INSTANCE.targetProcess.target != null) {
                        int item = mc.thePlayer.inventory.currentItem;
                        switch (blockTick) {
                            case 0:
                                if (!isPlayerBlocking()) {
                                    startPacketBlock();
                                }
                                blockTick = 1;
                                break;
                            case 1:
                                if (isPlayerBlocking()) {
                                    int slot = findEmptySlot(item);
                                    mc.getNetHandler().getNetworkManager().sendPacket(new C09PacketHeldItemChange(slot));
                                }
                                break;
                        }
                        isBlocking = true;
                        fakeBlockState = true;
                    }
                    break;

                case "Swap":
                    if (Client.INSTANCE.targetProcess.target != null) {
                        int item = mc.thePlayer.inventory.currentItem;
                        switch (blockTick) {
                            case 0:
                                int slot = findSwordSlot(item);
                                if (slot != -1) {
                                    if (!isPlayerBlocking()) {
                                        startPacketBlock();
                                    }
                                    blockTick = 1;
                                }
                                break;
                            case 1:
                                int swordsSlot = findSwordSlot(item);
                                if (swordsSlot == -1) {
                                    blockTick = 0;
                                } else if (!isPlayerBlocking()) {
                                    startPacketBlock();
                                } else {
                                    mc.getNetHandler().getNetworkManager().sendPacket(new C09PacketHeldItemChange(swordsSlot));
                                    mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(swordsSlot)));
                                    blockTick = 0;
                                }
                                break;
                        }
                        isBlocking = true;
                        fakeBlockState = true;
                    }
                    break;

                case "Legit":
                    if (Client.INSTANCE.targetProcess.target != null) {
                        switch (blockTick) {
                            case 0:
                                if (!isPlayerBlocking()) {
                                    startPacketBlock();
                                }
                                blockTick = 1;
                                break;
                            case 1:
                                if (isPlayerBlocking()) {
                                    stopPacketBlock();
                                }
                                break;
                        }
                        isBlocking = true;
                        fakeBlockState = false;
                    }
                    break;

                case "Fake":
                    isBlocking = false;
                    fakeBlockState = Client.INSTANCE.targetProcess.target != null;
                    break;
            }
        } else {
            if (autoBlockMode.get().equals("Hypixel") || autoBlockMode.get().equals("Blink") ||
                    autoBlockMode.get().equals("Interact") || autoBlockMode.get().equals("Swap") ||
                    autoBlockMode.get().equals("Legit")) {
                blockTick = 0;
            }
        }
    }

    private void startVanillaBlock() {
        if (!isBlocking && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
            isBlocking = true;
        }
    }

    private void startPacketBlock() {
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
    }

    private void stopPacketBlock() {
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN));
        }
    }

    private void stopVanillaBlock() {
        if (isBlocking) {
            mc.thePlayer.stopUsingItem();
            isBlocking = false;
        }
    }

    private boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || blockingState) && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private int findEmptySlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                return i;
            }
        }
        return (currentSlot - 1 + 9) % 9;
    }

    private int findSwordSlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                net.minecraft.item.ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
                if (item != null && item.getItem() instanceof ItemSword) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean isBlocking() {
        return fakeBlockState && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public EntityLivingBase getTarget() {
        return Client.INSTANCE.targetProcess.target;
    }

    @Override
    public String getSuffix() {
        return sortMode.get();
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
        lastAttackTime = 0;
        rangeFix = 3;
        isBlocking = false;
        rots = null;
        RotationUtil.moveFix = false;
        RotationUtil.silentPitchActive = false;
        alpha = 0;
        blockingState = false;
        fakeBlockState = false;
        blockTick = 0;
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        rots = null;
        // Never leave the move-fix armed once we stop ticking.
        RotationUtil.moveFix = false;
        RotationUtil.silentPitchActive = false;
        if (isBlocking) {
            stopVanillaBlock();
        }
        if (blockingState) {
            stopPacketBlock();
        }
        isBlocking = false;
        blockingState = false;
        fakeBlockState = false;
        blockTick = 0;
    }
}

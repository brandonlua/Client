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
        // AngleLock: point the player's actual view at the target in every direction -
        // yaw (left/right) and pitch (up/down). This runs before the tick's movement and
        // body update, so vanilla rotates the character's orientation to follow the view
        // with its natural lag, exactly like normal gameplay. Because we move the real
        // view, the look packet automatically sends this rotation too (no desync).
        if (Client.INSTANCE.targetProcess.target != null && angleLock.get()) {
            rots = RotationUtil.getSimpleRotations(Client.INSTANCE.targetProcess.target);
            mc.thePlayer.rotationYaw = rots.yaw;
            mc.thePlayer.rotationPitch = rots.pitch;
            mc.thePlayer.rotationYawHead = rots.yaw;
        } else {
            rots = null;
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
        if (Client.INSTANCE.targetProcess.target == null) {
            rots = null;
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
        alpha = 0;
        blockingState = false;
        fakeBlockState = false;
        blockTick = 0;
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        rots = null;
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

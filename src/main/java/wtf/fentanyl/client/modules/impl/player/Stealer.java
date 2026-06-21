package wtf.fentanyl.client.modules.impl.player;

import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.GLU;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.Event2D;
import wtf.fentanyl.event.impl.EventWorld;
import wtf.fentanyl.event.impl.PacketEvent;
import wtf.fentanyl.event.impl.UpdateEvent;
import wtf.fentanyl.event.impl.game.player.MotionEvent;
import wtf.fentanyl.util.math.MathUtil;
import wtf.fentanyl.util.math.TimerUtil;
import wtf.fentanyl.util.player.InventoryUtil;
import wtf.fentanyl.util.player.RotationUtil;
import wtf.fentanyl.util.render.RenderUtil;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static wtf.fentanyl.util.InstanceAccess.mc;

@ModuleInfo(name = "Stealer", category = Category.PLAYER)
public final class Stealer extends Module {
    private final SliderValue minDelay = new SliderValue("Min Delay", 1f, 0f, 5f, 1f, this);
    private final SliderValue maxDelay = new SliderValue("Max Delay", 1f, 0f, 5f, 1f, this);
    public final BoolValue menuCheck = new BoolValue("Menu Check", true, this);
    public final BoolValue aura = new BoolValue("Aura", false, this);
    private final BoolValue startDelay = new BoolValue("Start Delay", true, this);
    public final BoolValue avoid = new BoolValue("Avoid", false, this, aura::get);
    private final SliderValue range = new SliderValue("Range", 4f, 1.5f, 4f, this);
    private final TimerUtil timer = new TimerUtil(), timerAura = new TimerUtil(), timerAvoid = new TimerUtil();
    public boolean isStealing;
    private int index;
    private final List<BlockPos> posList = new CopyOnWriteArrayList<>();
    private int prevItem = -1;
    private int chestIndex;
    public static float[] rotation;
    public int slot;
    private BlockPos currentContainerPos;
    private final String[] list = new String[]{"mode", "delivery", "menu", "selector", "game", "gui", "server", "inventory", "play", "teleporter",
            "shop", "melee", "armor", "block", "castle", "mini", "warp", "teleport", "user", "team", "tool", "sure", "trade", "cancel", "accept",
            "soul", "book", "recipe", "profile", "tele", "port", "map", "kit", "select", "lobby", "vault", "lock", "anticheat", "travel", "settings",
            "user", "preference", "compass", "cake", "wars", "buy", "upgrade", "ranged", "potions", "utility"};

    public void rotate(BlockPos blockPos) {
        rotation = RotationUtil.getRotations(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
    }

    @Override
    public void onDisabled() {
        isStealing = false;
        posList.clear();
        chestIndex = 0;
        super.onDisabled();
    }

    @Subscribe
    public void onWorld(EventWorld event) {
        posList.clear();
        chestIndex = 0;
    }

    @Subscribe
    public void onMotion(MotionEvent event) {
        rotation = null;

        if (event.getState() != MotionEvent.State.PRE)
            return;

        if (!aura.get())
            return;

        if (!isStealing) {
            for (TileEntity chest : tileEntityList()) {
                if (!posList.contains(chest.getPos()) && timerAura.hasTimeElapsed(300)) {
                    rotate(chest.getPos());
                    if (rotation != null && RotationUtil.rayTrace(rotation[0], rotation[1], range.get(), 1.0f).getBlockPos().equals(chest.getPos())
                            && chest instanceof TileEntityChest) {
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), chest.getPos(), getBlockFacing(chest.getPos()), getVec3(chest.getPos()));
                        posList.add(chest.getPos());
                        timerAura.reset();
                    }
                }
            }
        } else {
            timerAura.reset();
        }

        if (!avoid.get())
            return;

        if (!isStealing) {
            if (chestIndex >= posList.size()) {
                return;
            }

            BlockPos pos = posList.get(chestIndex);
            double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
            double dy = pos.getY() + 0.5 - mc.thePlayer.posY;
            double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
            double distToPos = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distToPos <= range.get()) {
                BlockPos up = pos.up();
                if (mc.theWorld.getBlockState(up).getBlock() == Blocks.air && getBlockSlot() != -1 && timerAvoid.hasTimeElapsed(1000)) {
                    prevItem = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = getBlockSlot();
                    rotate(pos);
                    if (rotation != null && RotationUtil.rayTrace(rotation[0], rotation[1], range.get(), 1.0f).getBlockPos().equals(pos)) {
                        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), up, getBlockFacing(up), getVec3(up))) {
                            mc.thePlayer.swingItem();
                        }
                        chestIndex += 1;
                        mc.thePlayer.inventory.currentItem = prevItem;
                        timerAvoid.reset();
                    }
                }
            }
        } else {
            timerAvoid.reset();
        }
    }

    private EnumFacing getBlockFacing(BlockPos pos) {
        return EnumFacing.UP;
    }

    @Subscribe
    public void onRender2D(Event2D event) {
        if (mc.thePlayer.openContainer == null || mc.currentScreen == null || !isStealing) return;
        Container container = mc.thePlayer.openContainer;
        int slots = container.inventorySlots.size();

        int scaleFactor = new ScaledResolution(mc).getScaleFactor();

        if (slots > 0) {
            float[] projection = calculate(currentContainerPos, scaleFactor);
            if (projection == null) return;

            float roundX = projection[0] - (164 / 2F);
            float roundY = projection[1] / 1.5F;

            GlStateManager.pushMatrix();
            GlStateManager.translate(roundX + 82, roundY + 30, 0);
            GlStateManager.translate(-(roundX + 82), -(roundY + 30), 0);

            RenderUtil.drawRoundedRect(roundX, roundY, 164, 60, 3, new Color(0, 0, 0, 120));

            double startX = roundX + 5;
            double startY = roundY + 5;

            RenderItem itemRender = mc.getRenderItem();

            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.zLevel = 200.0F;

            for (Slot s : container.inventorySlots) {
                if (!s.inventory.equals(mc.thePlayer.inventory)) {
                    int x = (int) (startX + (s.slotNumber % 9) * 18);
                    int y = (int) (startY + ((double) s.slotNumber / 9) * 18);
                    itemRender.renderItemAndEffectIntoGUI(s.getStack(), x, y);
                }
            }

            GlStateManager.popMatrix();
            itemRender.zLevel = 0.0F;
            GlStateManager.popMatrix();
            GlStateManager.disableLighting();
        }
    }

    public float[] calculate(BlockPos blockPos, int factor) {
        try {
            float renderX = (float) mc.getRenderManager().renderPosX;
            float renderY = (float) mc.getRenderManager().renderPosY;
            float renderZ = (float) mc.getRenderManager().renderPosZ;

            float x = blockPos.getX() + 0.5f - renderX;
            float y = blockPos.getY() + 0.5f - renderY;
            float z = blockPos.getZ() + 0.5f - renderZ;

            float[] projectedCenter = project(x, y, z, factor);
            if (projectedCenter != null && projectedCenter[2] >= 0.0D && projectedCenter[2] < 1.0D) {
                return new float[]{projectedCenter[0], projectedCenter[1], projectedCenter[0], projectedCenter[1]};
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static float[] project(double x, double y, double z, int factor) {
        if (GLU.gluProject((float) x, (float) y, (float) z, ActiveRenderInfo.MODELVIEW, ActiveRenderInfo.PROJECTION, ActiveRenderInfo.VIEWPORT, ActiveRenderInfo.OBJECTCOORDS)) {
            return new float[]{
                    (ActiveRenderInfo.OBJECTCOORDS.get(0) / factor),
                    ((Display.getHeight() - ActiveRenderInfo.OBJECTCOORDS.get(1)) / factor),
                    ActiveRenderInfo.OBJECTCOORDS.get(2)
            };
        }
        return null;
    }

    private List<TileEntity> tileEntityList() {
        return mc.theWorld.loadedTileEntityList.stream()
                .filter(te -> {
                    BlockPos p = te.getPos();
                    double dx = p.getX() + 0.5 - mc.thePlayer.posX;
                    double dy = p.getY() + 0.5 - mc.thePlayer.posY;
                    double dz = p.getZ() + 0.5 - mc.thePlayer.posZ;
                    return Math.sqrt(dx*dx + dy*dy + dz*dz) <= range.get();
                })
                .sorted(Comparator.comparing(o -> {
                    BlockPos p = ((TileEntity) o).getPos();
                    double dx = p.getX() + 0.5 - mc.thePlayer.posX;
                    double dy = p.getY() + 0.5 - mc.thePlayer.posY;
                    double dz = p.getZ() + 0.5 - mc.thePlayer.posZ;
                    return Math.sqrt(dx*dx + dy*dy + dz*dz);
                }).reversed())
                .collect(Collectors.toList());
    }

    @Subscribe
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.openContainer != null) {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                if (isStealing) {
                    ContainerChest container = (ContainerChest) mc.thePlayer.openContainer;
                    if (menuCheck.get()) {
                        String name = container.getLowerChestInventory().getDisplayName().getUnformattedText().toLowerCase();
                        for (String str : list) {
                            if (name.contains(str)) return;
                        }
                    }

                    for (int i = 0; i < container.getLowerChestInventory().getSizeInventory(); ++i) {
                        if (container.getLowerChestInventory().getStackInSlot(i) != null
                                && (timer.hasTimeElapsed((long) (MathUtil.nextInt((int) minDelay.get(), (int) maxDelay.get())) * 100L)
                                || MathUtil.nextInt((int) minDelay.get(), (int) maxDelay.get()) == 0)
                                && InventoryUtil.isValid(container.getLowerChestInventory().getStackInSlot(i))) {
                            slot = i;
                            mc.playerController.windowClick(container.windowId, i, 0, 1, mc.thePlayer);
                            timer.reset();
                        }
                    }
                    if (InventoryUtil.isInventoryFull() || InventoryUtil.isInventoryEmpty(container.getLowerChestInventory())) {
                        mc.thePlayer.closeScreen();
                        isStealing = false;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof S2DPacketOpenWindow) {
            S2DPacketOpenWindow packetOpenWindow = (S2DPacketOpenWindow) event.getPacket();
            String title = packetOpenWindow.getWindowTitle().getUnformattedText().toLowerCase();
            for (String blacklisted : list) {
                if (title.contains(blacklisted)) {
                    isStealing = false;
                    return;
                }
            }
            if (startDelay.get()) timer.reset();
            isStealing = packetOpenWindow.getGuiId().equals("minecraft:chest") || packetOpenWindow.getGuiId().equals("minecraft:container");
        }
    }

    public static int getBlockSlot() {
        for (int i = 0; i < 9; ++i) {
            Slot s = mc.thePlayer.inventoryContainer.getSlot(i + 36);
            if (s.getHasStack() && s.getStack().getItem() instanceof ItemBlock) {
                return i;
            }
        }
        return -1;
    }

    public Vec3 getVec3(BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        EnumFacing facing = EnumFacing.UP;
        double random = ThreadLocalRandom.current().nextDouble();

        switch (facing) {
            case NORTH:
                x += random;
                break;
            case SOUTH:
                x += random;
                z += 1.0;
                break;
            case WEST:
                z += random;
                break;
            case EAST:
                z += random;
                x += 1.0;
                break;
            default:
                break;
        }

        if (facing == EnumFacing.UP) {
            x += random;
            z += random;
            y += 1.0;
        } else {
            y += random;
        }

        return new Vec3(x, y, z);
    }
}
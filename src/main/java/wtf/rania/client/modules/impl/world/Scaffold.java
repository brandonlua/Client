package wtf.rania.client.modules.impl.world;

import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import wtf.rania.Client;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.impl.render.HUD;
import wtf.rania.client.modules.impl.render.PostProcessing;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.Event2D;
import wtf.rania.event.impl.game.player.HitBlockEvent;
import wtf.rania.event.impl.game.player.LivingUpdateEvent;
import wtf.rania.event.impl.game.player.LeftClickMouseEvent;
import wtf.rania.event.impl.game.player.MotionEvent;
import wtf.rania.event.impl.game.player.MoveInputEvent;
import wtf.rania.event.impl.game.player.RightClickMouseEvent;
import wtf.rania.event.impl.game.player.SafeWalkEvent;
import wtf.rania.event.impl.game.player.StrafeEvent;
import wtf.rania.event.impl.game.player.SwapItemEvent;
import wtf.rania.utility.player.MovementUtil;
import wtf.rania.utility.player.RotationUtil;
import wtf.rania.utility.render.RenderUtil;
import wtf.rania.utility.render.shaders.impl.Blur;
import wtf.rania.utility.render.shaders.impl.Bloom;
import wtf.rania.utility.render.shaders.impl.Shadow;

@ModuleInfo(name = "Scaffold", category = Category.WORLD)
public class Scaffold extends Module {
    private static final double[] placeOffsets = new double[]{
            0.03125,
            0.09375,
            0.15625,
            0.21875,
            0.28125,
            0.34375,
            0.40625,
            0.46875,
            0.53125,
            0.59375,
            0.65625,
            0.71875,
            0.78125,
            0.84375,
            0.90625,
            0.96875
    };
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private EnumFacing targetFacing = null;
    private ItemStack blockCounterStack = null;
    private Framebuffer blockCounterStencil = new Framebuffer(1, 1, false);
    private Framebuffer blockCounterShadow = new Framebuffer(1, 1, false);

    public final ModeValue rotationMode = new ModeValue("Rotations", new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS", "GODBRIDGE"}, "BACKWARDS", this);
    public final ModeValue moveFix = new ModeValue("Move-fix", new String[]{"NONE", "SILENT"}, "SILENT", this);
    public final ModeValue sprintMode = new ModeValue("Sprint", new String[]{"NONE", "VANILLA"}, "NONE", this);
    public final SliderValue groundMotion = new SliderValue("Ground-motion", 100.0F, 0.0F, 100.0F, this);
    public final SliderValue airMotion = new SliderValue("Air-motion", 100.0F, 0.0F, 100.0F, this);
    public final SliderValue speedMotion = new SliderValue("Speed-motion", 100.0F, 0.0F, 100.0F, this);
    public final ModeValue tower = new ModeValue("Tower", new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"}, "NONE", this);
    public final ModeValue keepY = new ModeValue("Keep-y", new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"}, "NONE", this);
    public final BoolValue keepYonPress = new BoolValue("Keep-y-on-press", false, this, () -> !this.keepY.is("NONE"));
    public final BoolValue disableWhileJumpActive = new BoolValue("No-keep-y-on-jump-potion", false, this, () -> !this.keepY.is("NONE"));
    public final BoolValue multiplace = new BoolValue("Multi-place", true, this);
    public final BoolValue safeWalk = new BoolValue("Safe-walk", true, this);
    public final BoolValue swing = new BoolValue("Swing", true, this);
    public final BoolValue itemSpoof = new BoolValue("Item-spoof", false, this);
    public final BoolValue blockCounter = new BoolValue("Block-counter", true, this);

    public Scaffold() {
    }

    private boolean shouldStopSprint() {
        if (this.isTowering()) {
            return false;
        } else {
            boolean stage = this.keepY.getIndex() == 1 || this.keepY.getIndex() == 2;
            return (!stage || this.stage <= 0) && this.sprintMode.getIndex() == 0;
        }
    }

    private boolean canPlace() {
        return true;
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.distanceSqToCenter((double) blockPos3.getX() + 0.5, (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (enumFacing == null || distance < offset || distance == offset && facing == EnumFacing.UP) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }
        return enumFacing;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );
        if (!isReplaceable(targetPos)) {
            return null;
        } else {
            ArrayList<BlockPos> positions = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 0; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (!isReplaceable(pos)
                                && !isInteractable(pos)
                                && !(
                                mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5)
                                        > (double) mc.playerController.getBlockReachDistance()
                        )
                                && (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                            for (EnumFacing facing : EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    BlockPos blockPos = pos.offset(facing);
                                    if (isReplaceable(blockPos)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) {
                return null;
            } else {
                positions.sort(
                        Comparator.comparingDouble(
                                o -> o.distanceSqToCenter((double) targetPos.getX() + 0.5, (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)
                        )
                );
                BlockPos blockPos = positions.get(0);
                EnumFacing facing = this.getBestFacing(blockPos, targetPos);
                return facing == null ? null : new BlockData(blockPos, facing);
            }
        }
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (isHoldingBlock() && this.blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockPos, enumFacing, vec3)) {
                if (mc.playerController.getCurrentGameType() != GameType.CREATIVE) {
                    this.blockCount--;
                }
                if (this.swing.get()) {
                    mc.thePlayer.swingItem();
                } else {
                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                }
            }
        }
    }

    private EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    private double distanceToEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH:
                return mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
            case EAST:
                return Math.ceil(mc.thePlayer.posX) - mc.thePlayer.posX;
            case SOUTH:
                return Math.ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ;
            case WEST:
            default:
                return mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        }
    }

    private float getSpeed() {
        if (!mc.thePlayer.onGround) {
            return this.airMotion.get() / 100.0F;
        } else {
            return MovementUtil.getSpeedLevel() > 0
                    ? this.speedMotion.get() / 100.0F
                    : this.groundMotion.get() / 100.0F;
        }
    }

    private double getRandomOffset() {
        return 0.2155 - RotationUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getCurrentYaw() {
        return MovementUtil.adjustYaw(
                mc.thePlayer.rotationYaw, (float) MovementUtil.getForwardValue(), (float) MovementUtil.getLeftValue()
        );
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    private boolean isTowering() {
        if (mc.thePlayer.onGround && MovementUtil.isForwardPressed() && !isAirAbove()) {
            boolean keepY = this.keepY.getIndex() == 3;
            boolean tower = this.tower.getIndex() == 3;
            return keepY && this.stage > 0 || tower && mc.gameSettings.keyBindJump.isKeyDown();
        } else {
            return false;
        }
    }

    public int getSlot() {
        return this.lastSlot;
    }

    public int getBlockCount() {
        return this.blockCount;
    }

    @Subscribe
    private final Listener<MotionEvent> motionListener = new Listener<>(event -> {
        if (event.getState() == MotionEvent.State.PRE) {
            if (this.rotationTick > 0) {
                this.rotationTick--;
            }
            if (mc.thePlayer.onGround) {
                if (this.stage > 0) {
                    this.stage--;
                }
                if (this.stage < 0) {
                    this.stage++;
                }
                if (this.stage == 0
                        && this.keepY.getIndex() != 0
                        && (!this.keepYonPress.get() || mc.gameSettings.keyBindJump.isKeyDown())
                        && (!this.disableWhileJumpActive.get() || !mc.thePlayer.isPotionActive(Potion.jump))
                ) {
                    this.stage = 1;
                }
                this.startY = this.shouldKeepY ? this.startY : MathHelper.floor_double(mc.thePlayer.posY);
                this.shouldKeepY = false;
                this.towering = false;
            }
            if (this.canPlace()) {
                ItemStack stack = mc.thePlayer.getHeldItem();
                int count = isBlock(stack) ? stack.stackSize : 0;
                if (count > 0) {
                    this.blockCounterStack = stack.copy();
                }
                this.blockCount = Math.min(this.blockCount, count);
                if (this.blockCount <= 0) {
                    int slot = mc.thePlayer.inventory.currentItem;
                    if (this.blockCount == 0) {
                        slot--;
                    }
                    for (int i = slot; i > slot - 9; i--) {
                        int hotbarSlot = (i % 9 + 9) % 9;
                        ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);
                        if (isBlock(candidate)) {
                            mc.thePlayer.inventory.currentItem = hotbarSlot;
                            this.blockCount = candidate.stackSize;
                            this.blockCounterStack = candidate.copy();
                            break;
                        }
                    }
                }
                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
                float diagonalYaw = this.isDiagonal(currentYaw)
                        ? yawDiffTo180
                        : RotationUtil.wrapAngleDiff(currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), event.getYaw());
                if (!this.canRotate) {
                    switch (this.rotationMode.getIndex()) {
                        case 1:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 2:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            }
                            break;
                        case 3:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                            } else {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 4: // God Bridge Mode
                            float roundedYaw = Math.round(currentYaw / 45.0f) * 45.0f;
                            this.yaw = RotationUtil.quantizeAngle(roundedYaw);
                            if (this.pitch == 0.0F || !this.canRotate) {
                                float godBridgePitch = 79.3f;
                                this.pitch = RotationUtil.quantizeAngle(godBridgePitch);
                            }
                            break;
                    }
                }
                BlockData blockData = this.getBlockData();
                Vec3 hitVec = null;
                if (blockData != null) {
                    double[] x = placeOffsets;
                    double[] y = placeOffsets;
                    double[] z = placeOffsets;
                    switch (blockData.facing()) {
                        case NORTH:
                            z = new double[]{0.0};
                            break;
                        case EAST:
                            x = new double[]{1.0};
                            break;
                        case SOUTH:
                            z = new double[]{1.0};
                            break;
                        case WEST:
                            x = new double[]{0.0};
                            break;
                        case DOWN:
                            y = new double[]{0.0};
                            break;
                        case UP:
                            y = new double[]{1.0};
                    }
                    float bestYaw = -180.0F;
                    float bestPitch = 0.0F;
                    float bestDiff = 0.0F;
                    for (double dx : x) {
                        for (double dy : y) {
                            for (double dz : z) {
                                double relX = (double) blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                                double relY = (double) blockData.blockPos().getY() + dy - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double relZ = (double) blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;
                                float baseYaw = RotationUtil.wrapAngleDiff(this.yaw, event.getYaw());
                                float[] rotations = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                                MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop != null
                                        && mop.typeOfHit == MovingObjectType.BLOCK
                                        && mop.getBlockPos().equals(blockData.blockPos())
                                        && mop.sideHit == blockData.facing()) {
                                    float totalDiff = Math.abs(rotations[0] - baseYaw) + Math.abs(rotations[1] - this.pitch);
                                    if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                        bestYaw = rotations[0];
                                        bestPitch = rotations[1];
                                        bestDiff = totalDiff;
                                        hitVec = mop.hitVec;
                                    }
                                }
                            }
                        }
                    }
                    if (bestYaw != -180.0F || bestPitch != 0.0F) {
                        this.yaw = bestYaw;
                        this.pitch = bestPitch;
                        this.canRotate = true;
                    }
                }
                if (this.canRotate && MovementUtil.isForwardPressed() && Math.abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - this.yaw)) < 90.0F) {
                    switch (this.rotationMode.getIndex()) {
                        case 2:
                            this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            break;
                        case 3:
                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                    }
                }
                if (this.rotationMode.getIndex() != 0) {
                    float targetYaw = this.yaw;
                    float targetPitch = this.pitch;
                    if (this.towering && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (double) (this.startY + 1))) {
                        float yawDiff = MathHelper.wrapAngleTo180_float(this.yaw - event.getYaw());
                        float tolerance = this.rotationTick >= 2 ? RotationUtil.nextFloat(90.0F, 95.0F) : RotationUtil.nextFloat(30.0F, 35.0F);
                        if (Math.abs(yawDiff) > tolerance) {
                            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
                            targetYaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
                            this.rotationTick = Math.max(this.rotationTick, 1);
                        }
                    }
                    if (this.isTowering()) {
                        float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - event.getYaw());
                        targetYaw = RotationUtil.quantizeAngle(event.getYaw() + yawDelta * RotationUtil.nextFloat(0.98F, 0.99F));
                        targetPitch = RotationUtil.quantizeAngle(RotationUtil.nextFloat(30.0F, 80.0F));
                        this.rotationTick = 3;
                        this.towering = true;
                    }

                    event.setYaw(targetYaw);
                    event.setPitch(targetPitch);

                    mc.thePlayer.rotationYawHead = targetYaw;
                    float ease = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.renderYawOffset);
                    mc.thePlayer.renderYawOffset += ease * 0.3F;
                    float headBody = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.renderYawOffset);
                    headBody = MathHelper.clamp_float(headBody, -75.0F, 75.0F);
                    mc.thePlayer.renderYawOffset = targetYaw - headBody;

                    if (this.moveFix.getIndex() == 1) {
                        RotationUtil.moveFix = true;
                        RotationUtil.moveFixYaw = targetYaw;
                    } else {
                        RotationUtil.moveFix = false;
                    }
                }
                if (blockData != null && hitVec != null && this.rotationTick <= 0) {
                    this.place(blockData.blockPos(), blockData.facing(), hitVec);
                    if (this.multiplace.get()) {
                        for (int i = 0; i < 3; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }
                            MovingObjectPosition mop = RotationUtil.rayTrace(this.yaw, this.pitch, mc.playerController.getBlockReachDistance(), 1.0F);
                            if (mop != null
                                    && mop.typeOfHit == MovingObjectType.BLOCK
                                    && mop.getBlockPos().equals(blockData.blockPos())
                                    && mop.sideHit == blockData.facing()) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            } else {
                                hitVec = getClickVec(blockData.blockPos(), blockData.facing());
                                double dx = hitVec.xCoord - mc.thePlayer.posX;
                                double dy = hitVec.yCoord - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double dz = hitVec.zCoord - mc.thePlayer.posZ;
                                float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, event.getYaw(), event.getPitch());
                                if (!(Math.abs(rotations[0] - this.yaw) < 120.0F) || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                    break;
                                }
                                mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop == null
                                        || mop.typeOfHit != MovingObjectType.BLOCK
                                        || !mop.getBlockPos().equals(blockData.blockPos())
                                        || mop.sideHit != blockData.facing()) {
                                    break;
                                }
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            }
                        }
                    }
                }
                if (this.targetFacing != null) {
                    if (this.rotationTick <= 0) {
                        int playerBlockX = MathHelper.floor_double(mc.thePlayer.posX);
                        int playerBlockY = MathHelper.floor_double(mc.thePlayer.posY);
                        int playerBlockZ = MathHelper.floor_double(mc.thePlayer.posZ);
                        BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                        hitVec = getClickVec(belowPlayer, this.targetFacing);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                    }
                    this.targetFacing = null;
                } else if (this.keepY.getIndex() == 2 && this.stage > 0 && !mc.thePlayer.onGround) {
                    int nextBlockY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY);
                    if (nextBlockY <= this.startY && mc.thePlayer.posY > (double) (this.startY + 1)) {
                        this.shouldKeepY = true;
                        blockData = this.getBlockData();
                        if (blockData != null && this.rotationTick <= 0) {
                            hitVec = getClickVec(blockData.blockPos(), blockData.facing());
                            this.place(blockData.blockPos(), blockData.facing(), hitVec);
                        }
                    }
                }
            }
        }
    });

    @Subscribe
    private final Listener<StrafeEvent> strafeListener = new Listener<>(event -> {
        if (!mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.hurtTime <= 5
                && !mc.thePlayer.isPotionActive(Potion.jump)
                && mc.gameSettings.keyBindJump.isKeyDown()
                && isHoldingBlock()) {
            int yState = (int) (mc.thePlayer.posY % 1.0 * 100.0);
            switch (this.tower.getIndex()) {
                case 1:
                    switch (this.towerTick) {
                        case 0:
                            if (mc.thePlayer.onGround) {
                                this.towerTick = 1;
                                mc.thePlayer.motionY = -0.0784000015258789;
                            }
                            return;
                        case 1:
                            if (yState == 0 && isAirBelow()) {
                                this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                this.towerTick = 2;
                                mc.thePlayer.motionY = 0.42F;
                                if (MovementUtil.isForwardPressed()) {
                                    MovementUtil.setSpeed(MovementUtil.getSpeed(), MovementUtil.getMoveYaw());
                                } else {
                                    MovementUtil.setSpeed(0.0);
                                    event.setForward(0.0F);
                                    event.setStrafe(0.0F);
                                }
                                return;
                            } else {
                                this.towerTick = 0;
                                return;
                            }
                        case 2:
                            this.towerTick = 3;
                            mc.thePlayer.motionY = 0.75 - mc.thePlayer.posY % 1.0;
                            return;
                        case 3:
                            this.towerTick = 1;
                            mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                            return;
                        default:
                            this.towerTick = 0;
                            return;
                    }
                case 2:
                    switch (this.towerTick) {
                        case 0:
                            if (mc.thePlayer.onGround) {
                                this.towerTick = 1;
                                mc.thePlayer.motionY = -0.0784000015258789;
                            }
                            return;
                        case 1:
                            if (yState == 0 && isAirBelow()) {
                                this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                if (!MovementUtil.isForwardPressed()) {
                                    this.towerDelay = 2;
                                    MovementUtil.setSpeed(0.0);
                                    event.setForward(0.0F);
                                    event.setStrafe(0.0F);
                                    EnumFacing facing = this.yawToFacing(MathHelper.wrapAngleTo180_float(this.yaw - 180.0F));
                                    double distance = this.distanceToEdge(facing);
                                    if (distance > 0.1) {
                                        if (mc.thePlayer.onGround) {
                                            Vec3i directionVec = facing.getDirectionVec();
                                            double offset = Math.min(this.getRandomOffset(), distance - 0.05);
                                            double jitter = RotationUtil.nextDouble(0.02, 0.03);
                                            AxisAlignedBB nextBox = mc.thePlayer
                                                    .getEntityBoundingBox()
                                                    .offset((double) directionVec.getX() * (offset - jitter), 0.0, (double) directionVec.getZ() * (offset - jitter));
                                            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, nextBox).isEmpty()) {
                                                mc.thePlayer.motionY = -0.0784000015258789;
                                                mc.thePlayer
                                                        .setPosition(nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0, nextBox.minY, nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0);
                                            }
                                            return;
                                        }
                                    } else {
                                        this.towerTick = 2;
                                        this.targetFacing = facing;
                                        mc.thePlayer.motionY = 0.42F;
                                    }
                                    return;
                                } else {
                                    this.towerTick = 2;
                                    this.towerDelay++;
                                    mc.thePlayer.motionY = 0.42F;
                                    MovementUtil.setSpeed(MovementUtil.getSpeed(), MovementUtil.getMoveYaw());
                                    return;
                                }
                            } else {
                                this.towerTick = 0;
                                this.towerDelay = 0;
                                return;
                            }
                        case 2:
                            this.towerTick = 3;
                            mc.thePlayer.motionY = mc.thePlayer.motionY - RotationUtil.nextDouble(0.00101, 0.00109);
                            return;
                        case 3:
                            if (this.towerDelay >= 4) {
                                this.towerTick = 4;
                                this.towerDelay = 0;
                            } else {
                                this.towerTick = 1;
                                mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                            }
                            return;
                        case 4:
                            this.towerTick = 5;
                            return;
                        case 5:
                            if (!isAirBelow()) {
                                this.towerTick = 0;
                            } else {
                                this.towerTick = 1;
                                mc.thePlayer.motionY -= 0.08;
                                mc.thePlayer.motionY *= 0.98F;
                                mc.thePlayer.motionY -= 0.08;
                                mc.thePlayer.motionY *= 0.98F;
                            }
                            return;
                        default:
                            this.towerTick = 0;
                            this.towerDelay = 0;
                            return;
                    }
                default:
                    this.towerTick = 0;
                    this.towerDelay = 0;
            }
        } else {
            this.towerTick = 0;
            this.towerDelay = 0;
        }
    });

    @Subscribe
    private final Listener<MoveInputEvent> moveInputListener = new Listener<>(event -> {
        if (mc.thePlayer.onGround && this.stage > 0 && MovementUtil.isForwardPressed()) {
            event.setJumping(true);
        }
    });

    @Subscribe
    private final Listener<LivingUpdateEvent> livingUpdateListener = new Listener<>(event -> {
        float speed = this.getSpeed();
        if (speed != 1.0F) {
            if (mc.thePlayer.movementInput.moveForward != 0.0F && mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                mc.thePlayer.movementInput.moveForward = mc.thePlayer.movementInput.moveForward * (1.0F / (float) Math.sqrt(2.0));
                mc.thePlayer.movementInput.moveStrafe = mc.thePlayer.movementInput.moveStrafe * (1.0F / (float) Math.sqrt(2.0));
            }
            mc.thePlayer.movementInput.moveForward *= speed;
            mc.thePlayer.movementInput.moveStrafe *= speed;
        }
        if (this.shouldStopSprint()) {
            mc.thePlayer.setSprinting(false);
        }
    });

    @Subscribe
    private final Listener<SafeWalkEvent> safeWalkListener = new Listener<>(event -> {
        if (this.safeWalk.get()) {
            if (mc.thePlayer.onGround && mc.thePlayer.motionY <= 0.0) {
                event.setSafeWalk(true);
            }
        }
    });

    @Subscribe
    private final Listener<Event2D> render2DListener = new Listener<>(event -> {
        if (this.blockCounter.get()) {
            int count = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && stack.stackSize > 0) {
                    Item item = stack.getItem();
                    if (item instanceof ItemBlock) {
                        Block block = ((ItemBlock) item).getBlock();
                        if (!isInteractable(block) && block.getMaterial().isSolid()) {
                            count += stack.stackSize;
                        }
                    }
                }
            }
            ScaledResolution sr = event.getSr();
            float x = sr.getScaledWidth() / 2.0F + mc.fontRendererObj.FONT_HEIGHT * 1.5F;
            float y = sr.getScaledHeight() / 2.0F - mc.fontRendererObj.FONT_HEIGHT / 2.0F + 1.0F;
            CFontRenderer hudFont = getHudFont();
            String text = String.valueOf(count);
            ItemStack displayStack = this.blockCounterStack;
            if (displayStack != null && !(displayStack.getItem() instanceof ItemBlock)) {
                displayStack = null;
            }
            boolean useHudFont = hudFont != null;
            float textWidth = useHudFont ? hudFont.getStringWidth(text) : mc.fontRendererObj.getStringWidth(text);
            float iconSize = 16.0F;
            float contentWidth = textWidth + (displayStack != null ? iconSize + 4.0F : 0.0F);
            float boxX = x - 4;
            float boxY = y - 3;
            float boxW = contentWidth + 10.0F;
            float boxH = (useHudFont ? hudFont.FONT_HEIGHT : mc.fontRendererObj.FONT_HEIGHT) + 8;
            int textColor = Color.WHITE.getRGB();

            PostProcessing postProcessing = Client.INSTANCE != null && Client.INSTANCE.getModuleManager() != null
                    ? (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing")
                    : null;
            boolean ppOn = postProcessing != null && postProcessing.isToggled();
            boolean blurOn = ppOn && postProcessing.blur.get();
            boolean shadowOn = ppOn && postProcessing.shadow.get();
            boolean bloomOn = ppOn && postProcessing.bloom.get();
            Color bg = new Color(0, 0, 0, blurOn ? 105 : 165);

            if (shadowOn) {
                blockCounterShadow = RenderUtil.createFrameBuffer(blockCounterShadow);
                blockCounterShadow.framebufferClear();
                blockCounterShadow.bindFramebuffer(true);
                RenderUtil.drawRoundedRect(boxX, boxY, boxW, boxH, 4, new Color(0, 0, 0, 255));
                blockCounterShadow.unbindFramebuffer();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.0f);
                GlStateManager.enableBlend();
                Shadow.renderBloom(blockCounterShadow.framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
                GlStateManager.disableBlend();
            }

            if (bloomOn) {
                blockCounterStencil = RenderUtil.createFrameBuffer(blockCounterStencil);
                blockCounterStencil.framebufferClear();
                blockCounterStencil.bindFramebuffer(false);
                RenderUtil.resetColor();
                RenderUtil.drawRoundedRect(boxX, boxY, boxW, boxH, 4, new Color(0, 0, 0, 255));
                RenderUtil.resetColor();
                blockCounterStencil.unbindFramebuffer();
                Bloom.renderBlur(blockCounterStencil.framebufferTexture, (int) postProcessing.bloomRadius.get(), (int) postProcessing.bloomOffset.get());
            }

            if (blurOn) {
                Blur.startBlur();
                RenderUtil.drawRoundedRect(boxX, boxY, boxW, boxH, 4, bg);
                Blur.endBlur(postProcessing.blurRadius.get(), 1);
            }

            RenderUtil.drawRoundedRect(boxX, boxY, boxW, boxH, 4, bg);

            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (displayStack != null) {
                float iconX = boxX + 4.0F;
                float iconY = boxY + 1.0F;
                RenderUtil.renderItemStack(displayStack, iconX, iconY, 1.0F);
            }
            float textX = boxX + 4.0F + (displayStack != null ? iconSize + 4.0F : 0.0F);
            if (useHudFont) {
                hudFont.drawStringWithShadow(text, textX, y + 2, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(text, textX, y + 2, textColor);
            }
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    });

    @Subscribe
    private final Listener<LeftClickMouseEvent> leftClickListener = new Listener<>(event -> {
        event.setCancelled(true);
    });

    @Subscribe
    private final Listener<RightClickMouseEvent> rightClickListener = new Listener<>(event -> {
        event.setCancelled(true);
    });

    @Subscribe
    private final Listener<HitBlockEvent> hitBlockListener = new Listener<>(event -> {
        event.setCancelled(true);
    });

    @Subscribe
    private final Listener<SwapItemEvent> swapListener = new Listener<>(event -> {
        this.lastSlot = event.setSlot(this.lastSlot);
        event.setCancelled(true);
    });

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
        } else {
            this.lastSlot = -1;
        }
        this.blockCount = -1;
        this.blockCounterStack = null;
        this.rotationTick = 3;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
        RotationUtil.moveFix = false;
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null && this.lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.lastSlot;
        }
        RotationUtil.moveFix = false;
    }

    private CFontRenderer getHudFont() {
        if (Client.INSTANCE == null || Client.INSTANCE.getModuleManager() == null) {
            return null;
        }
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        return hud != null ? hud.fr : null;
    }

    private boolean isReplaceable(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().isReplaceable(mc.theWorld, pos);
    }

    private boolean isAirAbove() {
        return mc.thePlayer.worldObj.isAirBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2.0, mc.thePlayer.posZ));
    }

    private boolean isAirBelow() {
        return mc.thePlayer.worldObj.isAirBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ));
    }

    private boolean isHoldingBlock() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemBlock;
    }

    private boolean isBlock(ItemStack candidate) {
        return candidate != null && candidate.getItem() instanceof ItemBlock;
    }

    private boolean isInteractable(BlockPos pos) {
        return isInteractable(mc.theWorld.getBlockState(pos).getBlock());
    }

    private boolean isInteractable(Block block) {
        return block instanceof net.minecraft.block.BlockChest
                || block instanceof net.minecraft.block.BlockFurnace
                || block instanceof net.minecraft.block.BlockEnderChest
                || block instanceof net.minecraft.block.BlockContainer
                || block instanceof net.minecraft.block.BlockAnvil
                || block instanceof net.minecraft.block.BlockDoor
                || block instanceof net.minecraft.block.BlockTrapDoor
                || block instanceof net.minecraft.block.BlockFenceGate
                || block instanceof net.minecraft.block.BlockButton
                || block instanceof net.minecraft.block.BlockLever;
    }

    private Vec3 getClickVec(BlockPos pos, EnumFacing facing) {
        double x = pos.getX() + 0.5 + facing.getFrontOffsetX() * 0.5;
        double y = pos.getY() + 0.5 + facing.getFrontOffsetY() * 0.5;
        double z = pos.getZ() + 0.5 + facing.getFrontOffsetZ() * 0.5;
        return new Vec3(x, y, z);
    }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }
}

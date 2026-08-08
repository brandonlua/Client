package wtf.rania.client.modules.impl.player;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.utility.player.RotationUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@ModuleInfo(name = "BedBreaker", description = "Automatically breaks beds", category = Category.PLAYER)
public class BedBreakerModule extends Module {

    public BlockPos bedPos, lastPos, surroundingPos, surroundingLastPos, spawnPos;
    private IBlockState bedBlock;

    private SliderValue range = new SliderValue("Range", 3F, 3F, 8F, this);
    private SliderValue delay = new SliderValue("Break Delay", 100F, 0F, 500F, this);
    private SliderValue speed = new SliderValue("Break Speed", 1F, 1F, 2F, this);
    private BoolValue surrounding = new BoolValue("Surroundings", false, this);
    private BoolValue rotate = new BoolValue("Only S/S Rotate", false, this);
    private BoolValue whitelist = new BoolValue("Whitelist", true, this);
    private BoolValue dig = new BoolValue("Ignore Slowdown", true, this);

    public double breakProgress;
    private int delayTick;
    private boolean start, surroundingBroken;
    public boolean rotating, check;

    public BedBreakerModule() {
    }

    public void onUpdate() {
        delayTick++;

        if(check) {
            spawnPos = mc.thePlayer.getPosition();
            check = false;
        }

        bedPos = findBed();
        if(bedPos != null) {
            bedBlock = mc.theWorld.getBlockState(bedPos);
        }else {
            bedPos = findBed();
        }

        if(bedPos != null && mc.thePlayer.getDistance(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5) > range.get()) {
            reset();
            return;
        }

        if(surrounding.get() && bedPos != null && getSurrounding(bedPos) != null && delayTick > delay.get() / 30 && !surroundingBroken) {
            surroundingPos = getSurrounding(bedPos);
            breakBlock(surroundingPos);
            return;
        }else {
            surroundingPos = null;
        }

        if(bedPos != null && lastPos != null && mc.theWorld.getBlockState(lastPos) != bedBlock) {
            breakProgress = 0;
            surroundingBroken = false;
        }

        if(surroundingPos != null && surroundingLastPos != null && mc.theWorld.getBlockState(surroundingLastPos) != mc.theWorld.getBlockState(surroundingPos)) {
            breakProgress = 0;
            surroundingBroken = false;
        }

        lastPos = bedPos;
        rotating = false;

        if(bedPos != null && delayTick > delay.get() / 30) {
            breakBlock(bedPos);
        }else {
            breakProgress = 0;
        }
    }

    private void reset() {
        BlockPos pos = surroundingPos != null ? surroundingPos : bedPos;
        if(breakProgress > 0 && pos != null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, EnumFacing.UP));
        }
        breakProgress = 0;
        bedPos = null;
        surroundingPos = null;
        rotating = false;
        surroundingBroken = false;
    }

    private void breakBlock(BlockPos pos) {
        if(pos == null) return;

        double prog = mc.theWorld.getBlockState(pos).getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos) * speed.get();
        if(dig.get() && !mc.thePlayer.onGround) {
            prog *= 1.5;
        }

        if(rotate.get() && breakProgress == 0 || !rotate.get() && breakProgress >= 0) {
            rotateToBlock(pos);
        }

        if(breakProgress >= 0) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), pos, (int) (breakProgress * 10 - 1));
            if(!rotate.get()) {
                mc.thePlayer.swingItem();
            }
        }

        if(breakProgress == 0) {
            start = false;
            mc.thePlayer.swingItem();
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));
        }

        if(breakProgress + prog >= 1.0) {
            rotateToBlock(pos);
        }

        if(breakProgress >= 1.0) {
            if(rotate.get()) {
                rotateToBlock(pos);
                mc.thePlayer.swingItem();
            }
            start = true;
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.UP));
            mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.UP);
            mc.theWorld.setBlockState(pos, Blocks.air.getDefaultState(), 11);
            breakProgress = 0;
            delayTick = 0;
            if(pos == surroundingPos) {
                surroundingBroken = true;
            }else {
                surroundingBroken = false;
            }
        }

        if(breakProgress + prog >= 1.0) {
            rotating = true;
        }

        if(!start) {
            breakProgress += prog;
        }
    }

    private BlockPos findBed() {
        for (int x = (int) -range.get(); x <= range.get(); x++) {
            for (int y = (int) -range.get(); y <= range.get(); y++) {
                for (int z = (int) -range.get(); z <= range.get(); z++) {
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z);
                    if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.bed) {
                        if(spawnPos == null) return pos;
                        if (mc.thePlayer.getDistanceSq(spawnPos) > 800 || !whitelist.get()) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    private BlockPos getSurrounding(BlockPos pos) {
        for(EnumFacing facing : EnumFacing.values()) {
            if(facing == EnumFacing.DOWN) {
                continue;
            }
            BlockPos block = pos.offset(facing);
            if(mc.theWorld.getBlockState(block).getBlock() != Blocks.air) {
                return block;
            }
        }
        return null;
    }

    private void rotateToBlock(BlockPos bp) {
        if(bp != null) {
            rotating = true;
            float[] rots = RotationUtil.getRotationsBlock(bp);
            mc.thePlayer.rotationYawHead = rots[0];
            mc.thePlayer.rotationPitch = rots[1];
        }
    }

    @Override
    public void onEnabled() {
        reset();
    }

    @Override
    public void onDisabled() {
        reset();
    }
}
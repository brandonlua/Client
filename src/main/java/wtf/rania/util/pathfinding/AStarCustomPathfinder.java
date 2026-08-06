package wtf.rania.util.pathfinding;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockWall;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;

// 100% not skidded bro trust me

public class AStarCustomPathfinder {
    private Vec3 startVec3;
    private Vec3 endVec3;
    private ArrayList<Vec3> path = new ArrayList<>();
    private ArrayList<Hub> hubs = new ArrayList<>();
    private ArrayList<Hub> hubsToWork = new ArrayList<>();
    private double minDistanceSquared = 9;
    private boolean nearest = true;

    private static Vec3[] flatCardinalDirections = {
            new Vec3(1, 0, 0),
            new Vec3(-1, 0, 0),
            new Vec3(0, 0, 1),
            new Vec3(0, 0, -1)
    };

    public AStarCustomPathfinder(Vec3 startVec3, Vec3 endVec3) {
        this.startVec3 = startVec3.addVector(0, 0, 0).floor();
        this.endVec3 = endVec3.addVector(0, 0, 0).floor();
    }

    public ArrayList<Vec3> getPath() {
        return path;
    }

    public void compute() {
        compute(100, 2);
    }

    public void compute(int loops, int depth) {
        path.clear();
        hubsToWork.clear();
        ArrayList<Vec3> initPath = new ArrayList<>();
        initPath.add(startVec3);
        hubsToWork.add(new Hub(startVec3, null, initPath, startVec3.squareDistanceTo(endVec3), 0, 0));
        search:
        for (int i = 0; i < loops; i++) {
            hubsToWork.sort(new CompareHub());
            int j = 0;
            if (hubsToWork.size() == 0) {
                break;
            }
            for (Hub hub : new ArrayList<>(hubsToWork)) {
                j++;
                if (j > depth) {
                    break;
                } else {
                    hubsToWork.remove(hub);
                    hubs.add(hub);

                    for (Vec3 direction : flatCardinalDirections) {
                        Vec3 loc = hub.getLoc().add(direction).floor();
                        if (checkPositionValidity(loc, false)) {
                            if (addHub(hub, loc, 0)) {
                                break search;
                            }
                        }
                    }
//
                    Vec3 loc1 = hub.getLoc().addVector(0, 1, 0).floor();
                    if (checkPositionValidity(loc1, false)) {
                        if (addHub(hub, loc1, 0)) {
                            break search;
                        }
                    }
//
                    Vec3 loc2 = hub.getLoc().addVector(0, -1, 0).floor();
                    if (checkPositionValidity(loc2, false)) {
                        if (addHub(hub, loc2, 0)) {
                            break search;
                        }
                    }
                }
            }
        }
        if (nearest) {
            hubs.sort(new CompareHub());
            path = hubs.get(0).getPath();
        }
    }

    public static boolean checkPositionValidity(Vec3 loc, boolean checkGround) {
        return checkPositionValidity((int) loc.getX(), (int) loc.getY(), (int) loc.getZ(), checkGround);
    }

    public static boolean checkPositionValidity(int x, int y, int z, boolean checkGround) {
        BlockPos block1 = new BlockPos(x, y, z);
        BlockPos block2 = new BlockPos(x, y + 1, z);
        BlockPos block3 = new BlockPos(x, y - 1, z);
        Minecraft mc = Minecraft.getMinecraft();
        return !isBlockSolid(block1) && !isBlockSolid(block2) && isBlockSolid(block3);
    }

    private static boolean isBlockSolid(BlockPos block) {
        Block block1 = Minecraft.getMinecraft().theWorld.getBlockState(block).getBlock();
        return block1.isFullBlock() ||
                block1 == Blocks.glass ||
                block1 == Blocks.stained_glass ||
                block1 == Blocks.stone_stairs ||
                block1 == Blocks.chest ||
                block1 == Blocks.oak_stairs ||
                block1 == Blocks.birch_stairs ||
                block1 == Blocks.acacia_stairs ||
                block1 == Blocks.spruce_stairs ||
                block1 == Blocks.brick_stairs ||
                block1 == Blocks.dark_oak_stairs ||
                block1 == Blocks.jungle_stairs ||
                block1 == Blocks.nether_brick_stairs ||
                block1 == Blocks.stone_brick_stairs ||
                block1 == Blocks.sandstone_stairs ||
                block1 == Blocks.quartz_stairs ||
                block1 == Blocks.red_sandstone_stairs ||
                block1 == Blocks.stone_slab2 ||
                block1 == Blocks.stone_slab ||
                block1 == Blocks.wooden_slab ||
                block1 == Blocks.double_stone_slab ||
                block1 == Blocks.double_stone_slab2 ||
                block1 == Blocks.double_wooden_slab;
    }

    private static boolean isSafeToWalkOn(BlockPos block) {
        return !(Minecraft.getMinecraft().theWorld.getBlockState(block).getBlock() instanceof BlockFence) &&
                !(Minecraft.getMinecraft().theWorld.getBlockState(block).getBlock() instanceof BlockWall);
    }

    public Hub isHubExisting(Vec3 loc) {
        for (Hub hub : hubs) {
            if (hub.getLoc().getX() == loc.getX() && hub.getLoc().getY() == loc.getY() && hub.getLoc().getZ() == loc.getZ()) {
                return hub;
            }
        }
        for (Hub hub : hubsToWork) {
            if (hub.getLoc().getX() == loc.getX() && hub.getLoc().getY() == loc.getY() && hub.getLoc().getZ() == loc.getZ()) {
                return hub;
            }
        }
        return null;
    }

    public boolean addHub(Hub parent, Vec3 loc, double cost) {
        Hub existingHub = isHubExisting(loc);
        double totalCost = cost;
        if (parent != null) {
            totalCost += parent.getTotalCost();
        }
        if (existingHub == null) {
            if (loc.getX() == endVec3.getX() && loc.getY() == endVec3.getY() && loc.getZ() == endVec3.getZ() || minDistanceSquared != 0 && loc.squareDistanceTo(endVec3) <= minDistanceSquared) {
                path.clear();
                path = parent.getPath();
                path.add(loc);
                return true;
            } else {
                ArrayList<Vec3> path = new ArrayList<>(parent.getPath());
                path.add(loc);
                hubsToWork.add(new Hub(loc, parent, path, loc.squareDistanceTo(endVec3), cost, totalCost));
            }
        } else if (existingHub.getCost() > cost) {
            ArrayList<Vec3> path = new ArrayList<>(parent.getPath());
            path.add(loc);
            existingHub.setLoc(loc);
            existingHub.setParent(parent);
            existingHub.setPath(path);
            existingHub.setSquareDistanceToFromTarget(loc.squareDistanceTo(endVec3));
            existingHub.setCost(cost);
            existingHub.setTotalCost(totalCost);
        }
        return false;
    }

    private class Hub {
        private Vec3 loc = null;
        private Hub parent = null;
        private ArrayList<Vec3> path;
        private double squareDistanceToFromTarget;
        private double cost;
        private double totalCost;

        public Hub(Vec3 loc, Hub parent, ArrayList<Vec3> path, double squareDistanceToFromTarget, double cost, double totalCost) {
            this.loc = loc;
            this.parent = parent;
            this.path = path;
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
            this.cost = cost;
            this.totalCost = totalCost;
        }

        public Vec3 getLoc() {
            return loc;
        }

        public Hub getParent() {
            return parent;
        }

        public ArrayList<Vec3> getPath() {
            return path;
        }

        public double getSquareDistanceToFromTarget() {
            return squareDistanceToFromTarget;
        }

        public double getCost() {
            return cost;
        }

        public void setLoc(Vec3 loc) {
            this.loc = loc;
        }

        public void setParent(Hub parent) {
            this.parent = parent;
        }

        public void setPath(ArrayList<Vec3> path) {
            this.path = path;
        }

        public void setSquareDistanceToFromTarget(double squareDistanceToFromTarget) {
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(double totalCost) {
            this.totalCost = totalCost;
        }
    }

    public class CompareHub implements Comparator<Hub> {
        @Override
        public int compare(Hub o1, Hub o2) {
            return (int) (
                    o1.getSquareDistanceToFromTarget() + o1.getTotalCost() - (o2.getSquareDistanceToFromTarget() + o2.getTotalCost())
            );
        }
    }
}

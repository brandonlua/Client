package wtf.fentanyl.util.player;

import wtf.fentanyl.util.InstanceAccess;
import wtf.fentanyl.util.world.BlockCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class RotationUtil implements InstanceAccess {

    private static float[] serverAngles = new float[2];
    private static Random random = new Random();
    public static float currentYaw = 0.0f;
    private static float currentPitch = 0.0f;
    private static int rotTick;
    private static final Random theRandom = new Random();
    private static final float GCD_VALUE = getGCDValue();

    public static Rotation getSimpleRotations(EntityLivingBase target) {
        double diffX = target.posX - mc.thePlayer.posX;
        double diffY = target.posY + target.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return applyGCD(yaw, pitch);
    }

    public static Rotation getSimpleRotations(BlockPos blockPos) {
        double diffX = blockPos.getX() + 0.5 - mc.thePlayer.posX;
        double diffY = blockPos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = blockPos.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return applyGCD(yaw, pitch);
    }

    public static Rotation getSimpleRotations(BlockCache blockCache, Rotation lastRotations) {
        double diffX = blockCache.pos.getX() + 0.5 - mc.thePlayer.posX;
        double diffY = blockCache.pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = blockCache.pos.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (blockCache.facing == EnumFacing.UP) diffY += 0.5;
        if (blockCache.facing == EnumFacing.DOWN) diffY -= 0.5;

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        yaw = smooth(lastRotations.yaw, yaw, 30);
        pitch = smooth(lastRotations.pitch, pitch, 20);

        return applyGCD(yaw, pitch);
    }

    public static Rotation getGodbridgeRotations(BlockCache blockCache, Rotation lastRotations) {
        double diffX = blockCache.pos.getX() + 0.5 - mc.thePlayer.posX;
        double diffY = blockCache.pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = blockCache.pos.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (blockCache.facing == EnumFacing.UP) diffY += 0.5;
        if (blockCache.facing == EnumFacing.DOWN) diffY -= 0.5;

        float yaw;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        switch (mc.thePlayer.getHorizontalFacing()) {
            case SOUTH:
                yaw = 150.0f;
                break;
            case NORTH:
                yaw = -34.5f;
                break;
            case EAST:
                yaw = 54.0f;
                break;
            case WEST:
                yaw = -125.5f;
                break;
            default:
                yaw = 45.0f;
        }

        if (lastRotations == null) {
            return applyGCD(yaw, pitch);
        }

        yaw = smooth(lastRotations.yaw, yaw, 30);
        pitch = smooth(lastRotations.pitch, pitch, 20);

        return applyGCD(yaw, pitch);
    }

    private static float smooth(float current, float target, float max) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        if (diff > max) diff = max;
        if (diff < -max) diff = -max;
        return current + diff;
    }

    /**
     * From the minecraft code {@link net.minecraft.client.renderer.EntityRenderer#updateRenderer}
     * @return Returns a GCD mouse fix value.
     */
    public static float getGCDValue() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        return (f * f * f * 8.0F) * 0.15F;
    }

    public static Rotation applyGCD(float yaw, float pitch) {
        yaw -= yaw % GCD_VALUE;
        pitch -= pitch % GCD_VALUE;
        return new Rotation(yaw, pitch);
    }

    public static float getAdjustedYaw() {
        switch (mc.thePlayer.getHorizontalFacing()) {
            case SOUTH:
                return -180;
            case NORTH:
                return 0;
            case EAST:
                return 90;
            case WEST:
                return -90;
            default:
                return mc.thePlayer.rotationYaw;
        }
    }

    public static float wrapAngleDiff(float angle, float target) {
        return target + MathHelper.wrapAngleTo180_float(angle - target);
    }

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    public static long nextLong(long min, long max) {
        return (long) nextDouble((double) min, (double) (max + 1L));
    }

    public static float nextFloat(float min, float max) {
        return theRandom.nextFloat() * (max - min) + min;
    }

    public static double nextDouble(double min, double max) {
        return theRandom.nextDouble() * (max - min) + min;
    }
    public static float smoothAngle(float angle, float smoothFactor) {
        return angle * (0.5f + 0.5f * (1.0f - Math.max(0.0f, Math.min(1.0f, smoothFactor + nextFloat(-0.1f, 0.1f)))));
    }

    public static float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % (double) 0.0096f);
    }

    public static float[] getRotationsToBox(AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle, float smoothFactor) {
        Vec3 eyePos = RotationUtil.mc.thePlayer.getPositionEyes(1.0f);
        double minTargetY = boundingBox.minY + 0.05 * (boundingBox.maxY - boundingBox.minY);
        double maxTargetY = boundingBox.minY + 0.75 * (boundingBox.maxY - boundingBox.minY);
        double deltaX = (boundingBox.minX + boundingBox.maxX) / 2.0 - eyePos.xCoord;
        double deltaY = eyePos.yCoord >= maxTargetY ? maxTargetY - eyePos.yCoord : (eyePos.yCoord <= minTargetY ? minTargetY - eyePos.yCoord : 0.0);
        double deltaZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eyePos.zCoord;
        return RotationUtil.getRotations(deltaX, deltaY, deltaZ, yaw, pitch, maxAngle, smoothFactor);
    }

    public static float[] getRotationsTo(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch) {
        return RotationUtil.getRotations(targetX, targetY, targetZ, currentYaw, currentPitch, 180.0f, 0.0f);
    }

    public static float[] getRotations(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch, float maxAngle, float smoothFactor) {
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        float yawDelta = MathHelper.wrapAngleTo180_float((float) (Math.atan2(targetZ, targetX) * 180.0 / Math.PI) - 90.0f - currentYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float((float) (-Math.atan2(targetY, horizontalDistance) * 180.0 / Math.PI) - currentPitch);
        yawDelta = Math.abs(yawDelta) <= 1.0f ? 0.0f : RotationUtil.smoothAngle(RotationUtil.clampAngle(yawDelta, maxAngle), smoothFactor);
        pitchDelta = Math.abs(pitchDelta) <= 1.0f ? 0.0f : RotationUtil.smoothAngle(RotationUtil.clampAngle(pitchDelta, maxAngle), smoothFactor);
        return new float[]{RotationUtil.quantizeAngle(currentYaw + yawDelta), RotationUtil.quantizeAngle(currentPitch + pitchDelta)};
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB boundingBox) {
        double[] coords = new double[]{vector.xCoord, vector.yCoord, vector.zCoord};
        double[] minCoords = new double[]{boundingBox.minX, boundingBox.minY, boundingBox.minZ};
        double[] maxCoords = new double[]{boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ};
        for (int i = 0; i < 3; ++i) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
                continue;
            }
            if (!(coords[i] < minCoords[i])) continue;
            coords[i] = minCoords[i];
        }
        return new Vec3(coords[0], coords[1], coords[2]);
    }

    public static double distanceToEntity(Entity entity) {
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        return RotationUtil.distanceToBox(boundingBox);
    }

    public static Vec3 getBestHitVec(final Entity entity) {
        final Vec3 positionEyes = mc.thePlayer.getPositionEyes(1);
        final AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox();
        final double ex = MathHelper.clamp_double(positionEyes.xCoord, entityBoundingBox.minX, entityBoundingBox.maxX);
        final double ey = MathHelper.clamp_double(positionEyes.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
        final double ez = MathHelper.clamp_double(positionEyes.zCoord, entityBoundingBox.minZ, entityBoundingBox.maxZ);
        return new Vec3(ex, ey, ez);
    }

    public static double distanceToBox(Entity entity, Vec3 point) {
        float borderSize = entity.getCollisionBorderSize();
        return RotationUtil.clampVecToBox(entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize), point);
    }

    public static double distanceToBox(AxisAlignedBB boundingBox) {
        return RotationUtil.clampVecToBox(boundingBox, RotationUtil.mc.thePlayer.getPositionEyes(1.0f));
    }

    public static double clampVecToBox(AxisAlignedBB boundingBox, Vec3 point) {
        if (boundingBox.isVecInside(point)) {
            return 0.0;
        }
        Vec3 clampedPoint = RotationUtil.clampVecToBox(point, boundingBox);
        double deltaX = clampedPoint.xCoord - point.xCoord;
        double deltaY = clampedPoint.yCoord - point.yCoord;
        double deltaZ = clampedPoint.zCoord - point.zCoord;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static float angleToEntity(Entity entity) {
        Vec3 eyePos = RotationUtil.mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        if (boundingBox.isVecInside(eyePos)) {
            return 0.0f;
        }
        double deltaX = entity.posX - eyePos.xCoord;
        double deltaZ = entity.posZ - eyePos.zCoord;
        return Math.abs(MathHelper.wrapAngleTo180_float((float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f - RotationUtil.mc.thePlayer.rotationYaw)) * 2.0f;
    }

    public static float getYawBetween(double x1, double z1, double x2, double z2) {
        return MathHelper.wrapAngleTo180_float((float) (Math.atan2(z2 - z1, x2 - x1) * 180.0 / Math.PI) - 90.0f - RotationUtil.mc.thePlayer.rotationYaw);
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(AxisAlignedBB boundingBox, float yaw, float pitch, double distance) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
        return boundingBox.calculateIntercept(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        Vec3 targetPos = clampVecToBox(eyePos, entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize));
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }
    private RotationUtil() {
        throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Vec3 getPositionEyes(float partialTicks) {
        return new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    public static float getYawChange(double posX, double posZ) {
        final EntityPlayerSP player = mc.thePlayer;
        final double deltaX = posX - player.posX;
        final double deltaZ = posZ - player.posZ;

        final double yawToEntity;
        final double v1 = Math.toDegrees(Math.atan(deltaZ / deltaX));

        if (deltaZ < 0.0D && deltaX < 0.0D) {
            yawToEntity = 90.0D + v1;
        } else if (deltaZ < 0.0D && deltaX > 0.0D) {
            yawToEntity = -90.0D + v1;
        } else {
            yawToEntity = Math.toDegrees(-Math.atan(deltaX / deltaZ));
        }

        return MathHelper.wrapAngleTo180_float(-(player.rotationYaw - (float) yawToEntity));
    }

    public static float[] getRotations(double posX, double posY, double posZ) {
        EntityPlayerSP player = mc.thePlayer;
        double x = posX - player.posX;
        double y = posY - (player.posY + (double) player.getEyeHeight());
        double z = posZ - player.posZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(y, dist) * 180.0D / Math.PI);
        return new float[]{yaw, pitch};
    }

    public static float[] getRotations(EntityLivingBase entity) {
        return mc.thePlayer.isMoving() ?
                getRotations(
                        entity.posX + ThreadLocalRandom.current().nextDouble(-0.03D, 0.03D),
                        entity.posY + (double) entity.getEyeHeight() - 0.4D + ThreadLocalRandom.current().nextDouble(-0.07D, 0.07D),
                        entity.posZ + ThreadLocalRandom.current().nextDouble(-0.03D, 0.03D)
                ) :
                getRotations(entity.posX, entity.posY + (double) entity.getEyeHeight() - 0.4D, entity.posZ);
    }

    public static float[] getRotationsEntity(EntityLivingBase entity) {
        return getRotations(entity);
    }

    public static float getYawToPoint(double posX, double posZ) {
        Minecraft instance = mc;
        double xDiff = posX - (instance.thePlayer.lastTickPosX + (instance.thePlayer.posX - instance.thePlayer.lastTickPosX) * instance.timer.renderPartialTicks);
        double zDiff = posZ - (instance.thePlayer.lastTickPosZ + (instance.thePlayer.posZ - instance.thePlayer.lastTickPosZ) * instance.timer.renderPartialTicks);
        double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
        return (float) (Math.atan2(zDiff, xDiff) * 180.0D / Math.PI) - 90.0F;
    }

    public static float getPitchChange(Entity entity, double posY) {
        final double deltaX = entity.posX - mc.thePlayer.posX;
        final double deltaZ = entity.posZ - mc.thePlayer.posZ;
        final double deltaY = posY - 2.2D + (double) entity.getEyeHeight() - mc.thePlayer.posY;
        final double distanceXZ = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
        final double pitchToEntity = -Math.toDegrees(Math.atan(deltaY / distanceXZ));

        return -MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationPitch - (float) pitchToEntity) - 2.5F;
    }

    public static float[] getRotationFromPosition(double x, double z, double y) {
        final double xDiff = x - mc.thePlayer.posX;
        final double zDiff = z - mc.thePlayer.posZ;
        final double yDiff = y - mc.thePlayer.posY - 1.2D;
        final double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
        final float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0D / Math.PI) - 90.0F;
        final float pitch = (float) -(Math.atan2(yDiff, dist) * 180.0D / Math.PI);

        return new float[]{yaw, pitch};
    }

    public static float[] aimAtLocation(double positionX, double positionY, double positionZ) {
        final double x = positionX - mc.thePlayer.posX;
        final double y = positionY - mc.thePlayer.posY;
        final double z = positionZ - mc.thePlayer.posZ;
        final double distance = MathHelper.sqrt_double(x * x + z * z);

        return new float[]{(float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f, (float) -(Math.atan2(y, distance) * 180.0 / Math.PI)};
    }

    public static float[] getAngles(EntityLivingBase entity) {
        if (entity == null) return null;
        final EntityPlayerSP player = mc.thePlayer;

        final double diffX = entity.posX - player.posX;
        final double diffY = entity.posY + entity.getEyeHeight() * 0.9 - (player.posY + player.getEyeHeight());
        final double diffZ = entity.posZ - player.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{
                player.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw),
                player.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch)
        };
    }

    public static float[] getAngles(BlockPos pos) {
        double posX = pos.getX();
        double posY = pos.getY();
        double posZ = pos.getZ();
        final EntityPlayerSP player = mc.thePlayer;

        final double diffX = posX - player.posX;
        final double diffY = posY - (player.posY + player.getEyeHeight());
        final double diffZ = posZ - player.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{
                player.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw),
                player.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch)
        };
    }

    public static float[] getAngles(double x, double y, double z) {
        final EntityPlayerSP player = mc.thePlayer;

        final double diffX = x - player.posX;
        final double diffY = y - (player.posY + player.getEyeHeight());
        final double diffZ = z - player.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{
                player.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw),
                player.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch)
        };
    }

    public static float[] getAngles(Vec3 vector) {
        double posX = vector.xCoord;
        double posY = vector.yCoord;
        double posZ = vector.zCoord;
        final EntityPlayerSP player = mc.thePlayer;

        final double diffX = posX - player.posX;
        final double diffY = posY - (player.posY + player.getEyeHeight());
        final double diffZ = posZ - player.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        final float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{
                MathHelper.wrapAngleTo180_float(player.rotationYaw + yaw - player.rotationYaw),
                player.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch)
        };
    }

    public static float getNewAngle(float angle) {
        angle %= 360.0F;

        if (angle >= 180.0F) {
            angle -= 360.0F;
        }

        if (angle < -180.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    public static float getDistanceBetweenAngles(float angle1, float angle2) {
        final float angle = Math.abs(angle1 - angle2) % 360.0F;
        return angle > 180.0F ? 360.0F - angle : angle;
    }

    public static double[] getDistance(double x, double z, double y) {
        final double distance = MathHelper.sqrt_double(x * x + z * z);
        final double yaw = Math.atan2(z, x) * 180.0D / Math.PI - 90.0F;
        final double pitch = -(Math.atan2(y, distance) * 180.0D / Math.PI);

        return new double[]{
                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float) (yaw - mc.thePlayer.rotationYaw)),
                mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float) (pitch - mc.thePlayer.rotationPitch))
        };
    }

    public static float[] applyGCD(float[] rotations, float[] prevRots) {
        float f = (float) (mc.gameSettings.mouseSensitivity * 0.6F + 0.2F);
        float multiplier = f * f * f * 1.2f;
        float yaw = prevRots[0] + (float) (Math.round((rotations[0] - prevRots[0]) / multiplier) * multiplier);
        float pitch = prevRots[1] + (float) (Math.round((rotations[1] - prevRots[1]) / multiplier) * multiplier);

        return new float[]{yaw, MathHelper.clamp_float(pitch, -90, 90)};
    }

    public static float getGCD() {
        float sensitivity = mc.gameSettings.mouseSensitivity;
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        return gcd;
    }

    public static float[] getRotationsBlock(final BlockPos pos) {
        return getRotationFromPosition(pos.getX() + 0.5, pos.getZ() + 0.5, pos.getY() - 0.25);
    }

    public static float smoothYaw(float targetYaw) {
        float playerYaw = mc.thePlayer.rotationYaw;
        if (currentYaw == 0.0f) {
            currentYaw = playerYaw;
        }
        float deltaYaw = wrapAngleTo180(rotDistance(currentYaw, targetYaw));
        currentYaw = wrapAngleTo180((float)(currentYaw + deltaYaw));
        return currentYaw;
    }

    public static float smoothPitch(float targetPitch) {
        float playerPitch = mc.thePlayer.rotationPitch;
        if (currentPitch == 0.0f) {
            currentPitch = playerPitch;
        }
        float deltaPitch = rotDistance(currentPitch, targetPitch);
        float threshold = (float)(8.0);
        if ((Math.abs(deltaPitch) < threshold)) {
            return (float) (clampTo90(currentPitch));
        }
        float smoothing = (float) Math.min(Math.max(Math.abs(deltaPitch) / 50f + Math.random(), 0.05f), 1.0f);

        currentPitch += deltaPitch;
        return currentPitch;
    }

    public static float rotDistance(float src, float target) {
        float difference = wrapAngleTo180(target - src);
        return difference;
    }

    public static float wrapAngleTo180(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float[] rotateToVec3(Vec3 targetVec) {
        Vec3 playerPos = mc.thePlayer.getPositionVector();

        double deltaX = targetVec.xCoord - playerPos.xCoord;
        double deltaY = targetVec.yCoord - playerPos.yCoord;
        double deltaZ = targetVec.zCoord - playerPos.zCoord;

        double yaw = Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI) - 90.0;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double pitch = -Math.atan2(deltaY, horizontalDistance / 2) * (180.0 / Math.PI);

        return new float[] {(float) yaw, (float) pitch};
    }

    public static float getMovementYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.moveForward;
        double moveStrafe = mc.thePlayer.moveStrafing;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        }
        else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        }
        else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return (MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) + yaw % 360 + 360) % 360;
    }

    public static MovingObjectPosition rayCast(double distance, float yaw, float pitch) {
        Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
        float n4 = -yaw * ((float)Math.PI / 180);
        float n5 = -pitch * ((float)Math.PI / 180);
        float cos = MathHelper.cos(n4 - (float)Math.PI);
        float sin = MathHelper.sin(n4 - (float)Math.PI);
        float n6 = -MathHelper.cos(n5);
        Vec3 vec3 = new Vec3(sin * n6, MathHelper.sin(n5), cos * n6);
        return mc.theWorld.rayTraceBlocks(getPositionEyes, getPositionEyes.addVector(vec3.xCoord * distance, vec3.yCoord * distance, vec3.zCoord * distance), true, true, true);
    }

    public static float clampTo90(float n) {
        return MathHelper.clamp_float(n, -90.0f, 90.0f);
    }
}
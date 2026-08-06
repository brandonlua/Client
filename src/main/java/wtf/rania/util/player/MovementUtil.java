package wtf.rania.util.player;

import wtf.rania.util.InstanceAccess;

import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;

import net.minecraft.util.MathHelper;

public class MovementUtil implements InstanceAccess {

    public static boolean isForwardPressed() {
        if (MovementUtil.mc.gameSettings.keyBindForward.isKeyDown() != MovementUtil.mc.gameSettings.keyBindBack.isKeyDown())
            return true;
        return MovementUtil.mc.gameSettings.keyBindLeft.isKeyDown() != MovementUtil.mc.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (MovementUtil.mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (MovementUtil.mc.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (MovementUtil.mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (MovementUtil.mc.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }

    public static float getMoveYaw() {
        return MovementUtil.adjustYaw(RotationState.isActived() ? RotationState.getSmoothedYaw() : MovementUtil.mc.thePlayer.rotationYaw, MovementUtil.mc.thePlayer.movementInput.moveForward, MovementUtil.mc.thePlayer.movementInput.moveStrafe);
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        if (forward < 0.0f) {
            yaw += 180.0f;
        }

        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static float getDirectionYaw() {
        if (MovementUtil.getSpeed() == 0.0) {
            return MathHelper.wrapAngleTo180_float(MovementUtil.mc.thePlayer.rotationYaw);
        }
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(MovementUtil.mc.thePlayer.motionZ, MovementUtil.mc.thePlayer.motionX)) - 90.0f);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.28015;
        if (MovementUtil.getSpeedTime() > 0) {
            baseSpeed = 0.28015 * (1.0 + 0.15 * (double) MovementUtil.getSpeedLevel());
        }
        return baseSpeed;
    }

    public static double getBaseJumpHigh(int speedLevel) {
        double jumpHeight = 0.452;
        if (speedLevel == 1) {
            jumpHeight = 0.49720000000000003;
        } else if (speedLevel >= 2) {
            jumpHeight *= 1.2;
        }
        return jumpHeight;
    }

    public static double getJumpMotion() {
        int speedLevel = 0;
        if (MovementUtil.getSpeedTime() > 0) {
            speedLevel = MovementUtil.getSpeedLevel();
        }
        return MovementUtil.getBaseJumpHigh(speedLevel);
    }

    public static double getSpeed() {
        return MovementUtil.getSpeed(MovementUtil.mc.thePlayer.motionX, MovementUtil.mc.thePlayer.motionZ);
    }

    public static double getSpeed(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeed(double speed) {
        MovementUtil.setSpeed(speed, MovementUtil.getDirectionYaw());
    }

    public static void setSpeed(double speed, float yaw) {
        MovementUtil.mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        MovementUtil.mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static void addSpeed(double speed, float yaw) {
        MovementUtil.mc.thePlayer.motionX += -Math.sin(Math.toRadians(yaw)) * speed;
        MovementUtil.mc.thePlayer.motionZ += Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static int getSpeedLevel() {
        int speedLevel = 0;
        if (MovementUtil.mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            speedLevel = (MovementUtil.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return speedLevel;
    }

    public static int getSpeedTime() {
        if (MovementUtil.mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return MovementUtil.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration();
        }
        return 0;
    }

    public static float getAllowedHorizontalDistance() {
        float slipperiness = MovementUtil.mc.thePlayer.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(MovementUtil.mc.thePlayer.posX), MathHelper.floor_double(MovementUtil.mc.thePlayer.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(MovementUtil.mc.thePlayer.posZ))).getBlock().slipperiness * 0.91f;
        return MovementUtil.mc.thePlayer.getAIMoveSpeed() * (0.16277136f / (slipperiness * slipperiness * slipperiness));
    }

    public static double[] predictMovement() {
        float strafeInput = (float) MovementUtil.getLeftValue() * 0.98f;
        float forwardInput = (float) MovementUtil.getForwardValue() * 0.98f;
        float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
        if (inputMagnitude >= 1.0E-4f) {
            inputMagnitude = MathHelper.sqrt_float(inputMagnitude);
            if (inputMagnitude < 1.0f) {
                inputMagnitude = 1.0f;
            }
            inputMagnitude = MovementUtil.getAllowedHorizontalDistance() / inputMagnitude;
            float sinYaw = MathHelper.sin(MovementUtil.mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            float cosYaw = MathHelper.cos(MovementUtil.mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            strafeInput *= inputMagnitude;
            forwardInput *= inputMagnitude;
            return new double[]{strafeInput * cosYaw - forwardInput * sinYaw, forwardInput * cosYaw + strafeInput * sinYaw};
        }
        return new double[]{0.0, 0.0};
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.wrapAngleTo180_float(MovementUtil.adjustYaw(MovementUtil.mc.thePlayer.rotationYaw, MovementUtil.getForwardValue(), MovementUtil.getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = -1.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 1: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = -1.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 2: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = 0.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 3: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = 1.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 4: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = 1.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 5: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = 1.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 6: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = 0.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 7: {
                MovementUtil.mc.thePlayer.movementInput.moveForward = -1.0f;
                MovementUtil.mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
        }
        if (MovementUtil.mc.thePlayer.movementInput.sneak) {
            MovementUtil.mc.thePlayer.movementInput.moveForward *= 0.3f;
            MovementUtil.mc.thePlayer.movementInput.moveStrafe *= 0.3f;
        }
    }

    public static boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    public static void stop() {
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
    }

    public static void setSpeed(double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            yaw += (strafe > 0.0D) ? (forward > 0.0D ? -45 : 45) : (strafe < 0.0D) ? (forward > 0.0D ? 45 : -45) : 0;
            strafe = 0.0D;
            forward = (forward > 0.0D) ? 1.0D : -1.0D;
        }

        if (strafe != 0.0D) {
            strafe = (strafe > 0.0D) ? 1.0D : -1.0D;
        }

        double radianYaw = Math.toRadians(yaw + 90.0F);
        double cosYaw = Math.cos(radianYaw);
        double sinYaw = Math.sin(radianYaw);

        mc.thePlayer.motionX = forward * moveSpeed * cosYaw + strafe * moveSpeed * sinYaw;
        mc.thePlayer.motionZ = forward * moveSpeed * sinYaw - strafe * moveSpeed * cosYaw;
    }

    public static void strafe(double moveSpeed) {
        if (mc.thePlayer.movementInput.moveForward != 0.0) {
            mc.thePlayer.movementInput.moveForward = (mc.thePlayer.movementInput.moveForward > 0.0) ? 1.0f : -1.0f;
        }

        if (mc.thePlayer.movementInput.moveStrafe != 0.0) {
            mc.thePlayer.movementInput.moveStrafe = (mc.thePlayer.movementInput.moveStrafe > 0.0) ? 1.0f : -1.0f;
        }

        if (mc.thePlayer.movementInput.moveForward == 0.0 && mc.thePlayer.movementInput.moveStrafe == 0.0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
            return;
        }

        if (mc.thePlayer.movementInput.moveForward != 0.0 && mc.thePlayer.movementInput.moveStrafe != 0.0) {
            mc.thePlayer.movementInput.moveForward *= (float) Math.sin(Math.toRadians(36.67));
            mc.thePlayer.movementInput.moveStrafe *= (float) Math.cos(Math.toRadians(36.67));
        }

        double yawRadians = Math.toRadians(mc.thePlayer.rotationYaw);
        mc.thePlayer.motionX = mc.thePlayer.movementInput.moveForward * moveSpeed * -Math.sin(yawRadians)
                + mc.thePlayer.movementInput.moveStrafe * moveSpeed * Math.cos(yawRadians);
        mc.thePlayer.motionZ = mc.thePlayer.movementInput.moveForward * moveSpeed * Math.cos(yawRadians)
                - mc.thePlayer.movementInput.moveStrafe * moveSpeed * -Math.sin(yawRadians);
    }

    public static double getJumpMotion(float motionY) {
        Potion potion = Potion.jump;

        if (mc.thePlayer.isPotionActive(potion)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(potion).getAmplifier();
            motionY += (amplifier + 1) * 0.1F;
        }

        return motionY;
    }
}
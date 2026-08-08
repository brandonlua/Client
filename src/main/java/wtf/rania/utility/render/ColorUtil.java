package wtf.rania.utility.render;

import wtf.rania.utility.math.MathUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import java.awt.*;

public class ColorUtil {

    public static int applyOpacity(int color, float opacity) {
        Color old = new Color(color);
        return applyOpacity(old, opacity).getRGB();
    }

    public static Color applyOpacity(final Color color, float opacity) {
        opacity = Math.min(1.0f, Math.max(0.0f, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int dropShadow(int color) {
        return (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
    }

    public static int alpha(int color, int alpha) {
        return (color & 0xFFFFFF) | (alpha << 24);
    }

    public static int rainbow(float saturation, float brightness) {
        return rainbow(System.currentTimeMillis(), 0, saturation, brightness);
    }

    public static int rainbow(long timeMS, int count, float saturation, float brightness) {
        float hue = (float) ((timeMS - count * 200L) % 4001) / 4000.0F;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static int exhiRainbow(long timeMS, int count) {
        float hue = (float) ((timeMS / 4L + (count * 9L)) % 256) / 255.0F;
        return Color.HSBtoRGB(hue, 0.55F, 0.9F);
    }

    public static int wave(int color, long timeMS, int count) {
        float factor = Math.abs((((timeMS * 2L) - count * 500L) % 8001) / 8000.0F - 0.5F) + 0.5F;
        Color awt = new Color(color);
        float[] hsb = Color.RGBtoHSB(awt.getRed(), awt.getGreen(), awt.getBlue(), null);
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * factor);
    }

    public static int wave(int color, int color2, long timeMS, int count) {
        float factor = Math.abs((((timeMS * 2L) - count * 500L) % 8001) / 4000.0F - 1.0F);
        return interpolate(new Color(color), new Color(color2), factor).getRGB();
    }

    public static int astolfo(long timeMS, int count) {
        float hue = Math.abs(((((timeMS * 2L) - count * 500L) % 8001) / 8000.0F) - 0.5f) + 0.5F;
        return Color.HSBtoRGB(hue, 0.5F, 1.0F);
    }

    public static int health(float factor) {
        return Color.HSBtoRGB(factor / 3.0F, 1.0F, 1.0F);
    }

    public static int health(float health, float maxHealth) {
        return health(health / maxHealth);
    }

    public static int health(EntityLivingBase entity) {
        return health(entity.getHealth(), entity.getMaxHealth());
    }

    public static Color interpolate(Color current, Color target, float factor) {
        return new Color(
                MathUtil.lerp(current.getRed(), target.getRed(), factor),
                MathUtil.lerp(current.getGreen(), target.getGreen(), factor),
                MathUtil.lerp(current.getBlue(), target.getBlue(), factor),
                MathUtil.lerp(current.getAlpha(), target.getAlpha(), factor)
        );
    }

    public static Color interpolate(Color current, Color target, int speed, float delta) {
        return new Color(
                MathUtil.incrementTo(current.getRed(), target.getRed(), (int) (speed * delta)),
                MathUtil.incrementTo(current.getGreen(), target.getGreen(), (int) (speed * delta)),
                MathUtil.incrementTo(current.getBlue(), target.getBlue(), (int) (speed * delta)),
                MathUtil.incrementTo(current.getAlpha(), target.getAlpha(), (int) (speed * delta))
        );
    }

    public static int darken(int color, int amount) {
        Color awt = new Color(color, true);
        for (int i = 0; i < amount; i++) {
            awt = awt.darker();
        }
        return awt.getRGB();
    }

    public static int multiplySatBri(int color, float s, float b) {
        Color awt = new Color(color, true);
        float[] hsb = Color.RGBtoHSB(awt.getRed(), awt.getGreen(), awt.getBlue(), null);
        hsb[1] = MathHelper.clamp_float(hsb[1] * s, 0.0F, 1.0F);
        hsb[2] = MathHelper.clamp_float(hsb[2] * b, 0.0F, 1.0F);
        return alpha(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), awt.getAlpha());
    }

    public static int get(int red, int green, int blue, int alpha) {
        return (MathHelper.clamp_int(alpha, 0, 255) << 24)
                | (MathHelper.clamp_int(red, 0, 255) << 16)
                | (MathHelper.clamp_int(green, 0, 255) << 8)
                | (MathHelper.clamp_int(blue, 0, 255));
    }

    public static int getF(float r, float g, float b, float a) {
        return get((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F), (int)(a * 255.0F));
    }

    public static float[] splitF(final int color) {
        final float r = (float) (color >> 16 & 255) / 255.0F;
        final float g = (float) (color >> 8 & 255) / 255.0F;
        final float b = (float) (color & 255) / 255.0F;
        final float a = (float) (color >> 24 & 255) / 255.0F;
        return new float[]{r, g, b, a};
    }
    public static Color reAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float) interpolationValue).intValue();
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        Color cColor1 = new Color(color1);
        Color cColor2 = new Color(color2);
        return interpolateColorC(cColor1, cColor2, amount).getRGB();
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static int interpolateColor2(Color color1, Color color2, float fraction) {
        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        try {
            return new Color(red, green, blue, alpha).getRGB();
        } catch (Exception ex) {
            return 0xffffffff;
        }
    }

    public static int getColorFromPercentage(float percentage) {
        return Color.HSBtoRGB(Math.min(1.0F, Math.max(0.0F, percentage)) / 3, 0.9F, 0.9F);
    }

    public static Color colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed) {
        return colorSwitch(firstColor, secondColor, time, index, timePerIndex, speed, 255.0D);
    }

    public static Color colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, double alpha) {
        long now = (long) (speed * (double) System.currentTimeMillis() + (double) ((long) index * timePerIndex));
        float redDiff = (float) (firstColor.getRed() - secondColor.getRed()) / time;
        float greenDiff = (float) (firstColor.getGreen() - secondColor.getGreen()) / time;
        float blueDiff = (float) (firstColor.getBlue() - secondColor.getBlue()) / time;
        int red = Math.round((float) secondColor.getRed() + redDiff * (float) (now % (long) time));
        int green = Math.round((float) secondColor.getGreen() + greenDiff * (float) (now % (long) time));
        int blue = Math.round((float) secondColor.getBlue() + blueDiff * (float) (now % (long) time));
        float redInverseDiff = (float) (secondColor.getRed() - firstColor.getRed()) / time;
        float greenInverseDiff = (float) (secondColor.getGreen() - firstColor.getGreen()) / time;
        float blueInverseDiff = (float) (secondColor.getBlue() - firstColor.getBlue()) / time;
        int inverseRed = Math.round((float) firstColor.getRed() + redInverseDiff * (float) (now % (long) time));
        int inverseGreen = Math.round((float) firstColor.getGreen() + greenInverseDiff * (float) (now % (long) time));
        int inverseBlue = Math.round((float) firstColor.getBlue() + blueInverseDiff * (float) (now % (long) time));

        return now % ((long) time * 2L) < (long) time ? (new Color(inverseRed, inverseGreen, inverseBlue, (int) alpha)) : (new Color(red, green, blue, (int) alpha));
    }

    public static int darker(int color, float factor) {
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        int a = color >> 24 & 0xFF;
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF | (a & 0xFF) << 24;
    }

    public static int getRedFromColor(int color) {
        return color >> 16 & 0xFF;
    }

    public static int getGreenFromColor(int color) {
        return color >> 8 & 0xFF;
    }

    public static int getBlueFromColor(int color) {
        return color & 0xFF;
    }

    public static int getAlphaFromColor(int color) {
        return color >> 24 & 0xFF;
    }

    public static int getOverallColorFrom(int color1, int color2, float percentTo2) {
        final int finalRed = (int) MathUtil.lerp(color1 >> 16 & 0xFF, color2 >> 16 & 0xFF, percentTo2),
                finalGreen = (int) MathUtil.lerp(color1 >> 8 & 0xFF, color2 >> 8 & 0xFF, percentTo2),
                finalBlue = (int) MathUtil.lerp(color1 & 0xFF, color2 & 0xFF, percentTo2),
                finalAlpha = (int) MathUtil.lerp(color1 >> 24 & 0xFF, color2 >> 24 & 0xFF, percentTo2);
        return new Color(finalRed, finalGreen, finalBlue, finalAlpha).getRGB();
    }

    public static Color brighter(Color color, float FACTOR) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();

        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / FACTOR), 255),
                Math.min((int) (g / FACTOR), 255),
                Math.min((int) (b / FACTOR), 255),
                alpha);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= alpha << 24;
        color |= red << 16;
        color |= green << 8;
        return color |= blue;
    }

    public static int swapAlpha(int color, float alpha) {
        int f = color >> 16 & 0xFF;
        int f1 = color >> 8 & 0xFF;
        int f2 = color & 0xFF;
        return ColorUtil.getColor(f, f1, f2, (int) alpha);
    }

    public static int fadeTo(int startColour, int endColour, double progress) {
        double invert = 1.0 - progress;
        int r = (int) ((startColour >> 16 & 0xFF) * invert +
                (endColour >> 16 & 0xFF) * progress);
        int g = (int) ((startColour >> 8 & 0xFF) * invert +
                (endColour >> 8 & 0xFF) * progress);
        int b = (int) ((startColour & 0xFF) * invert +
                (endColour & 0xFF) * progress);
        int a = (int) ((startColour >> 24 & 0xFF) * invert +
                (endColour >> 24 & 0xFF) * progress);
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    public static int getHealthColor(EntityLivingBase player) {
        float f = player.getHealth();
        float f1 = player.getMaxHealth();
        float f2 = Math.max(0.0F, Math.min(f, f1) / f1);
        return Color.HSBtoRGB(f2 / 3.0F, 0.75F, 1.0F) | 0xFF000000;
    }
    public static Color getGradientOffset(Color color1, Color color2, double offset) {
        double inverse_percent;
        int redPart;
        if(offset > 1.0D) {
            inverse_percent = offset % 1.0D;
            redPart = (int)offset;
            offset = redPart % 2 == 0?inverse_percent:1.0D - inverse_percent;
        }
        inverse_percent = 1.0D - offset;
        redPart = (int)((double)color1.getRed() * inverse_percent + (double)color2.getRed() * offset);
        int greenPart = (int)((double)color1.getGreen() * inverse_percent + (double)color2.getGreen() * offset);
        int bluePart = (int)((double)color1.getBlue() * inverse_percent + (double)color2.getBlue() * offset);
        return new Color(redPart, greenPart, bluePart);
    }
}
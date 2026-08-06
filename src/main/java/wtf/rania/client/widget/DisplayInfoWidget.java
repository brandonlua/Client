package wtf.rania.client.widget;

import wtf.rania.client.font.CFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DisplayInfoWidget {

    private Minecraft mc;
    private CFontRenderer font;
    private List<InfoComponent> components = new ArrayList<>();
    private List<PotionComponent> potionComponents = new ArrayList<>();

    public boolean showCoords = true;
    public boolean showAngles = true;
    public boolean showSpeed = true;
    public boolean showPing = true;
    public boolean showTPS = true;
    public boolean showFPS = true;
    public boolean showPotions = true;

    public DisplayInfoWidget() {
        this.mc = Minecraft.getMinecraft();
        this.font = new CFontRenderer(new Font("Arial", Font.PLAIN, 14), true, true);
    }

    public void render(int themeColor) {
        if (mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        components.clear();
        potionComponents.clear();

        if (showCoords) {
            components.add(new InfoComponent("XYZ", String.format("%,.1f / %,.1f / %,.1f",
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)));
        }

        if (showSpeed) {
            double speed = Math.hypot(mc.thePlayer.posX - mc.thePlayer.prevPosX,
                    mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * mc.timer.timerSpeed;
            components.add(new InfoComponent("Speed", String.format("%.2f m/s", speed * 20.0)));
        }

        if (showPing) {
            int ping = getPing();
            components.add(new InfoComponent("Ping", String.format("%dms", ping)));
        }

        if (showTPS) {
            double tps = getTickRate();
            components.add(new InfoComponent("TPS", String.format("%.2f", tps)));
        }

        if (showFPS) {
            components.add(new InfoComponent("FPS", String.format("%d", Minecraft.getDebugFPS())));
        }

        if (showPotions && mc.thePlayer.getActivePotionEffects() != null) {
            for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
                String name = "Unknown";
                int potionColor = 0xFFAAAAAA;
                try {
                    name = EnumChatFormatting.getTextWithoutFormattingCodes(
                            I18n.format(Potion.potionTypes[effect.getPotionID()].getName()));
                    potionColor = Potion.potionTypes[effect.getPotionID()].getLiquidColor() | 0xFF000000;
                } catch (Exception ignored) {}

                String display = String.format("%s %d", name, effect.getAmplifier() + 1);
                String time = Potion.getDurationString(effect);
                float potWidth = font.getStringWidth(display + " " + time);

                int timeColor;
                if (effect.getDuration() < 300) {
                    timeColor = 0xFFFF5555;
                } else if (effect.getDuration() < 600) {
                    timeColor = 0xFFFFAA00;
                } else {
                    timeColor = 0xFFAAAAAA;
                }

                potionComponents.add(new PotionComponent(display, time, potionColor,
                        timeColor, potWidth, potWidth - font.getStringWidth(time)));
            }
            potionComponents.sort(Comparator.<PotionComponent>comparingDouble(c -> c.width).reversed());
        }

        float yOff = screenHeight - 7;
        for (InfoComponent component : components) {
            if (component.name.equals("XYZ")) {
                float xyzWidth = font.getStringWidth(component.name + ": " + component.value);
                font.drawStringWithShadow(component.name + ":", screenWidth - xyzWidth - 2, yOff, themeColor);
                float valueX = screenWidth - xyzWidth - 2 + font.getStringWidth(component.name + ": ");
                font.drawStringWithShadow(component.value, valueX, yOff, -1);
            } else {
                font.drawStringWithShadow(component.name + ":", 2, yOff, themeColor);
                float valueX = 2 + font.getStringWidth(component.name + ": ");
                font.drawStringWithShadow(component.value, valueX, yOff, -1);
                yOff -= 8;
            }
        }

        yOff = screenHeight - 7;
        for (PotionComponent component : potionComponents) {
            font.drawStringWithShadow(component.name, screenWidth - component.width - 2, yOff, component.potColor);
            font.drawStringWithShadow(component.time, screenWidth - component.width + component.timeOffset - 2, yOff, component.timeColor);
            yOff -= 8;
        }
    }

    private String getDirection() {
        switch (mc.thePlayer.getHorizontalFacing()) {
            case NORTH: return "[Z-] North";
            case SOUTH: return "[Z+] South";
            case EAST: return "[X+] East";
            case WEST: return "[X-] West";
            default: return "[?] ???";
        }
    }

    private int getPing() {
        try {
            if (mc.getNetHandler() != null && mc.thePlayer != null) {
                return mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private double getTickRate() {
        return 20.0;
    }

    private static class InfoComponent {
        String name;
        String value;

        InfoComponent(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private static class PotionComponent {
        String name;
        String time;
        int potColor;
        int timeColor;
        float width;
        float timeOffset;

        PotionComponent(String name, String time, int potColor, int timeColor, float width, float timeOffset) {
            this.name = name;
            this.time = time;
            this.potColor = potColor;
            this.timeColor = timeColor;
            this.width = width;
            this.timeOffset = timeOffset;
        }
    }
}
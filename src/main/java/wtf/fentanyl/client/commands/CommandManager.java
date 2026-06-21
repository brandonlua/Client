package wtf.fentanyl.client.commands;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.commands.impl.BindCommand;
import wtf.fentanyl.client.commands.impl.ConfigCommand;
import wtf.fentanyl.client.commands.impl.ToggleCommand;
import wtf.fentanyl.client.modules.impl.render.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private final String prefix = ".";

    public CommandManager() {
        commands.add(new BindCommand());
        commands.add(new ToggleCommand());
        commands.add(new ConfigCommand());
    }

    public void handleCommand(String message) {
        if (!message.startsWith(prefix)) return;

        String[] parts = message.substring(prefix.length()).split(" ");
        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        for (Command command : commands) {
            if (command.matches(commandName)) {
                command.execute(args);
                return;
            }
        }

        sendMessage("Unknown command.");
    }

    public void sendMessage(String message) {
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule(HUD.class);
        Color themeColor = hud != null ? hud.theme.get() : new Color(255, 50, 50);

        EnumChatFormatting closestColor = getClosestChatColor(themeColor);

        Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText( closestColor + Client.INSTANCE.getName() + " §r" + "§8>> " + "§f" + message)
        );
    }

    private EnumChatFormatting getClosestChatColor(Color color) {
        EnumChatFormatting[] colors = {
                EnumChatFormatting.DARK_RED, EnumChatFormatting.RED,
                EnumChatFormatting.GOLD, EnumChatFormatting.YELLOW,
                EnumChatFormatting.DARK_GREEN, EnumChatFormatting.GREEN,
                EnumChatFormatting.AQUA, EnumChatFormatting.DARK_AQUA,
                EnumChatFormatting.DARK_BLUE, EnumChatFormatting.BLUE,
                EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.DARK_PURPLE
        };

        int[] colorValues = {
                0xAA0000, 0xFF5555, 0xFFAA00, 0xFFFF55,
                0x00AA00, 0x55FF55, 0x55FFFF, 0x00AAAA,
                0x0000AA, 0x5555FF, 0xFF55FF, 0xAA00AA
        };

        int minDistance = Integer.MAX_VALUE;
        EnumChatFormatting closest = EnumChatFormatting.RED;

        for (int i = 0; i < colors.length; i++) {
            int r2 = (colorValues[i] >> 16) & 0xFF;
            int g2 = (colorValues[i] >> 8) & 0xFF;
            int b2 = colorValues[i] & 0xFF;

            int distance = (color.getRed() - r2) * (color.getRed() - r2) +
                    (color.getGreen() - g2) * (color.getGreen() - g2) +
                    (color.getBlue() - b2) * (color.getBlue() - b2);

            if (distance < minDistance) {
                minDistance = distance;
                closest = colors[i];
            }
        }

        return closest;
    }

    public List<Command> getCommands() {
        return commands;
    }
}
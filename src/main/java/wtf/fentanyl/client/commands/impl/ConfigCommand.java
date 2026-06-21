package wtf.fentanyl.client.commands.impl;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.commands.Command;
import wtf.fentanyl.client.config.Config;
import wtf.fentanyl.client.widget.SessionInfoWidget;
import wtf.fentanyl.client.widget.TargetHUDWidget;

import java.io.File;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manage configs", "cfg", "c");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            Client.INSTANCE.getCommandManager().sendMessage("Usage: .config <save/load/list/delete> [name]");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "save":
            case "s":
                if (args.length < 2) {
                    Client.INSTANCE.getCommandManager().sendMessage("Usage: .config save <name>");
                    return;
                }
                saveConfig(args[1]);
                break;

            case "load":
            case "l":
                if (args.length < 2) {
                    Client.INSTANCE.getCommandManager().sendMessage("Usage: .config load <name>");
                    return;
                }
                loadConfig(args[1]);
                break;

            case "list":
            case "ls":
                listConfigs();
                break;

            case "delete":
            case "del":
            case "d":
                if (args.length < 2) {
                    Client.INSTANCE.getCommandManager().sendMessage("Usage: .config delete <name>");
                    return;
                }
                deleteConfig(args[1]);
                break;

            default:
                Client.INSTANCE.getCommandManager().sendMessage("Invalid action. Use save/load/list/delete");
                break;
        }
    }

    private void saveConfig(String name) {
        try {
            TargetHUDWidget targetHUD = getTargetHUD();
            SessionInfoWidget sessionInfo = getSessionInfo();
            Config config = new Config(name);
            config.save(targetHUD, sessionInfo);
            Client.INSTANCE.getCommandManager().sendMessage("Config '" + name + "' saved successfully.");
        } catch (Exception e) {
            Client.INSTANCE.getCommandManager().sendMessage("Failed to save config: " + e.getMessage());
        }
    }

    private void loadConfig(String name) {
        try {
            Config config = new Config(name);
            if (!config.getFile().exists()) {
                Client.INSTANCE.getCommandManager().sendMessage("config '" + name + "' does not exist lil bro.");
                return;
            }
            TargetHUDWidget targetHUD = getTargetHUD();
            SessionInfoWidget sessionInfo = getSessionInfo();
            config.load(targetHUD, sessionInfo);
            Client.INSTANCE.getCommandManager().sendMessage("Config '" + name + "' loaded successfully.");
        } catch (Exception e) {
            Client.INSTANCE.getCommandManager().sendMessage("Failed to load config: " + e.getMessage());
        }
    }

    private void listConfigs() {
        File configDir = new File(Client.INSTANCE.getMc().mcDataDir, Client.INSTANCE.getName() + "/configs");

        if (!configDir.exists() || !configDir.isDirectory()) {
            Client.INSTANCE.getCommandManager().sendMessage("no configs.");
            return;
        }

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            Client.INSTANCE.getCommandManager().sendMessage("no configs.");
            return;
        }

        Client.INSTANCE.getCommandManager().sendMessage("config list:");
        for (File file : files) {
            String configName = file.getName().replace(".json", "");
            Client.INSTANCE.getCommandManager().sendMessage("  - " + configName);
        }
    }

    private void deleteConfig(String name) {
        try {
            Config config = new Config(name);
            if (!config.getFile().exists()) {
                Client.INSTANCE.getCommandManager().sendMessage("config '" + name + "' does not exist lil bro.");
                return;
            }
            config.delete();
            Client.INSTANCE.getCommandManager().sendMessage("config '" + name + "' has been deleted.");
        } catch (Exception e) {
            Client.INSTANCE.getCommandManager().sendMessage("couldn't delete config: " + e.getMessage());
        }
    }

    private TargetHUDWidget getTargetHUD() {
        try {
            Class<?> hudClass = Class.forName("wtf.fentanyl.client.modules.impl.render.HUD");
            Object hudModule = Client.INSTANCE.getModuleManager().getModule((Class) hudClass);
            if (hudModule != null) {
                return (TargetHUDWidget) hudClass.getDeclaredField("targetHUD").get(hudModule);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private SessionInfoWidget getSessionInfo() {
        try {
            Class<?> hudClass = Class.forName("wtf.fentanyl.client.modules.impl.render.HUD");
            Object hudModule = Client.INSTANCE.getModuleManager().getModule((Class) hudClass);
            if (hudModule != null) {
                return (SessionInfoWidget) hudClass.getDeclaredField("sessionInfo").get(hudModule);
            }
        } catch (Exception e) {
        }
        return null;
    }
}
package wtf.rania.client.config;

import wtf.rania.Client;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.values.Value;
import wtf.rania.client.modules.values.impl.*;
import wtf.rania.client.widget.TargetHUDWidget;
import wtf.rania.client.widget.SessionInfoWidget;
import wtf.rania.utility.misc.FileUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;

public class Config {

    private String name;
    private File file;

    public Config(String name) {
        this.name = name;
        this.file = new File(FileUtil.getRunningPath().toFile(), "configs/" + name + ".json");
    }

    public void save(TargetHUDWidget targetHUD, SessionInfoWidget sessionInfo) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }

            JsonObject configObject = new JsonObject();
            JsonArray modulesArray = new JsonArray();

            for (Module module : Client.INSTANCE.getModuleManager().getModules()) {
                JsonObject moduleObject = new JsonObject();
                moduleObject.addProperty("name", module.getName());
                moduleObject.addProperty("enabled", module.isToggled());
                moduleObject.addProperty("keybind", module.getKey());

                if (!module.getValues().isEmpty()) {
                    JsonArray valuesArray = new JsonArray();

                    for (Value value : module.getValues()) {
                        JsonObject valueObject = new JsonObject();
                        valueObject.addProperty("name", value.getName());

                        if (value instanceof BoolValue) {
                            valueObject.addProperty("type", "bool");
                            valueObject.addProperty("value", ((BoolValue) value).get());
                        } else if (value instanceof SliderValue) {
                            valueObject.addProperty("type", "slider");
                            valueObject.addProperty("value", ((SliderValue) value).get());
                        } else if (value instanceof ModeValue) {
                            valueObject.addProperty("type", "mode");
                            valueObject.addProperty("value", ((ModeValue) value).get());
                        } else if (value instanceof ColorValue) {
                            valueObject.addProperty("type", "color");
                            Color color = ((ColorValue) value).get();
                            valueObject.addProperty("red", color.getRed());
                            valueObject.addProperty("green", color.getGreen());
                            valueObject.addProperty("blue", color.getBlue());
                            valueObject.addProperty("alpha", color.getAlpha());
                        } else if (value instanceof TextValue) {
                            valueObject.addProperty("type", "text");
                            valueObject.addProperty("value", ((TextValue) value).get());
                        }

                        valuesArray.add(valueObject);
                    }

                    moduleObject.add("values", valuesArray);
                }

                modulesArray.add(moduleObject);
            }

            configObject.add("modules", modulesArray);

            JsonObject widgetsObject = new JsonObject();

            if (targetHUD != null) {
                JsonObject targetHUDObject = new JsonObject();
                targetHUDObject.addProperty("dragX", targetHUD.getDragX());
                targetHUDObject.addProperty("dragY", targetHUD.getDragY());
                targetHUDObject.addProperty("mode", targetHUD.getMode());
                widgetsObject.add("targetHUD", targetHUDObject);
            }

            if (sessionInfo != null) {
                JsonObject sessionInfoObject = new JsonObject();
                sessionInfoObject.addProperty("dragX", sessionInfo.getDragX());
                sessionInfoObject.addProperty("dragY", sessionInfo.getDragY());
                sessionInfoObject.addProperty("killed", sessionInfo.killed);
                sessionInfoObject.addProperty("won", sessionInfo.won);
                widgetsObject.add("sessionInfo", sessionInfoObject);
            }

            configObject.add("widgets", widgetsObject);

            FileWriter writer = new FileWriter(file);
            writer.write(FileUtil.gson.toJson(configObject));
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(TargetHUDWidget targetHUD, SessionInfoWidget sessionInfo) {
        try {
            if (!file.exists()) {
                return;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            JsonObject configObject = new JsonParser().parse(content).getAsJsonObject();
            JsonArray modulesArray = configObject.getAsJsonArray("modules");

            for (JsonElement moduleElement : modulesArray) {
                JsonObject moduleObject = moduleElement.getAsJsonObject();
                String moduleName = moduleObject.get("name").getAsString();
                boolean enabled = moduleObject.get("enabled").getAsBoolean();
                int keybind = moduleObject.get("keybind").getAsInt();

                Module module = Client.INSTANCE.getModuleManager().getModule(moduleName);
                if (module == null) continue;

                if (enabled && !module.isToggled()) {
                    module.toggle();
                } else if (!enabled && module.isToggled()) {
                    module.toggle();
                }

                module.setKey(keybind);

                if (moduleObject.has("values")) {
                    JsonArray valuesArray = moduleObject.getAsJsonArray("values");

                    for (JsonElement valueElement : valuesArray) {
                        JsonObject valueObject = valueElement.getAsJsonObject();
                        String valueName = valueObject.get("name").getAsString();
                        String valueType = valueObject.get("type").getAsString();

                        Value value = null;
                        for (Value v : module.getValues()) {
                            if (v.getName().equals(valueName)) {
                                value = v;
                                break;
                            }
                        }
                        if (value == null) continue;

                        switch (valueType) {
                            case "bool":
                                if (value instanceof BoolValue) {
                                    ((BoolValue) value).set(valueObject.get("value").getAsBoolean());
                                }
                                break;
                            case "slider":
                                if (value instanceof SliderValue) {
                                    ((SliderValue) value).setValue(valueObject.get("value").getAsFloat());
                                }
                                break;
                            case "mode":
                                if (value instanceof ModeValue) {
                                    ((ModeValue) value).set(valueObject.get("value").getAsString());
                                }
                                break;
                            case "color":
                                if (value instanceof ColorValue) {
                                    int r = valueObject.get("red").getAsInt();
                                    int g = valueObject.get("green").getAsInt();
                                    int b = valueObject.get("blue").getAsInt();
                                    int a = valueObject.get("alpha").getAsInt();
                                    ((ColorValue) value).set(new Color(r, g, b, a));
                                }
                                break;
                            case "text":
                                if (value instanceof TextValue) {
                                    ((TextValue) value).setText(valueObject.get("value").getAsString());
                                }
                                break;
                        }
                    }
                }
            }

            if (configObject.has("widgets")) {
                JsonObject widgetsObject = configObject.getAsJsonObject("widgets");

                if (widgetsObject.has("targetHUD") && targetHUD != null) {
                    JsonObject targetHUDObject = widgetsObject.getAsJsonObject("targetHUD");
                    targetHUD.setDragX(targetHUDObject.get("dragX").getAsFloat());
                    targetHUD.setDragY(targetHUDObject.get("dragY").getAsFloat());
                    targetHUD.setMode(targetHUDObject.get("mode").getAsString());
                }

                if (widgetsObject.has("sessionInfo") && sessionInfo != null) {
                    JsonObject sessionInfoObject = widgetsObject.getAsJsonObject("sessionInfo");
                    sessionInfo.setDragX(sessionInfoObject.get("dragX").getAsFloat());
                    sessionInfo.setDragY(sessionInfoObject.get("dragY").getAsFloat());
                    sessionInfo.killed = sessionInfoObject.get("killed").getAsInt();
                    sessionInfo.won = sessionInfoObject.get("won").getAsInt();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        if (file.exists()) {
            file.delete();
        }
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }
}
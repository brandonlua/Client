package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.Value;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.Event2D;
import wtf.fentanyl.event.impl.EventKey;
import wtf.fentanyl.util.render.shaders.impl.Blur;
import wtf.fentanyl.util.render.shaders.impl.Shadow;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TabGUI", category = Category.RENDER)
public class TabGUI extends Module {

    public List<TabType> types = new ArrayList<>();
    private int typeN = 0;
    private int moduleN = 0;
    private int settingN = 0;
    private int valueN = 0;

    @Override
    public void onEnabled() {
        for (Category category : Category.values()) {
            this.types.add(new TabType(this, category));
        }
    }

    public int getColor() {
        HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
        return hud != null ? hud.theme.get().getRGB() : 0xFFFF0000;
    }

    @Override
    public void onDisabled() {
        this.types.clear();
    }

    @Subscribe
    private final Listener<Event2D> event2DListener = new Listener<>(e -> {
        if(toggled) {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

            if (postProcessing != null && postProcessing.isToggled() && postProcessing.blur.get()) {
                Blur.startBlur();
                this.types.forEach(TabType::render);
                Blur.endBlur(postProcessing.blurRadius.get(), 1);
            }

            if (postProcessing != null && postProcessing.isToggled() && postProcessing.shadow.get()) {
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.0f);
                GlStateManager.enableBlend();
                this.types.forEach(TabType::render);
                Shadow.renderBloom(mc.getFramebuffer().framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
                GlStateManager.disableBlend();
            }

            this.types.forEach(TabType::render);
        }
    });

    @Subscribe
    private final Listener<EventKey> eventKeyListener = new Listener<>(e -> {
        if(!toggled) return;

        int key = e.getKey();
        TabType selectedType = getSelectedType();
        if(selectedType == null) return;

        final boolean isTypeOpened = selectedType.isOpened();
        final TabModule selectedModule = selectedType.getSelectedModule();
        if(selectedModule == null) return;

        final boolean selectedModuleOpened = selectedModule.isOpened();
        final TabSetting selectedSetting = selectedModule.getSelectedSetting();
        Value value = null;

        if(selectedSetting != null) {
            value = selectedSetting.getValue();
        }

        if(key == 28) {
            if(isTypeOpened) {
                if(selectedModuleOpened) {
                    if(value instanceof BoolValue) {
                        ((BoolValue) value).toggle();
                    } else if(selectedSetting != null) {
                        final TabValue selectedValue = selectedSetting.getSelectedValue();
                        if(selectedValue != null && value instanceof ModeValue) {
                            ((ModeValue) value).set(selectedValue.getValue());
                        }
                    }
                } else {
                    selectedModule.getMod().toggle();
                }
            }
        } else if(key == 200) {
            if(!isTypeOpened) {
                if(this.typeN == 0) {
                    this.typeN = this.types.size() - 1;
                } else {
                    this.typeN--;
                }
            } else {
                if(!selectedModuleOpened) {
                    if(this.moduleN == 0) {
                        this.moduleN = selectedType.getModules().size() - 1;
                    } else {
                        this.moduleN--;
                    }
                } else {
                    if(selectedSetting != null && !selectedSetting.isOpened()) {
                        if(this.settingN == 0) {
                            this.settingN = selectedModule.getSettings().size() - 1;
                        } else {
                            this.settingN--;
                        }
                    } else if(selectedSetting != null) {
                        if(value instanceof SliderValue) {
                            SliderValue slider = (SliderValue) value;
                            slider.setValue(Math.min(slider.getMax(), slider.get() + slider.getIncrement()));
                        } else {
                            if(this.valueN == 0) {
                                this.valueN = selectedSetting.getValues().size() - 1;
                            } else {
                                this.valueN--;
                            }
                        }
                    }
                }
            }
        } else if(key == 208) {
            if(!isTypeOpened) {
                if(this.typeN == this.types.size() - 1) {
                    this.typeN = 0;
                } else {
                    this.typeN++;
                }
            } else {
                if(!selectedModuleOpened) {
                    if(this.moduleN == selectedType.getModules().size() - 1) {
                        this.moduleN = 0;
                    } else {
                        this.moduleN++;
                    }
                } else {
                    if(selectedSetting != null && !selectedSetting.isOpened()) {
                        if(this.settingN == selectedModule.getSettings().size() - 1) {
                            this.settingN = 0;
                        } else {
                            this.settingN++;
                        }
                    } else if(selectedSetting != null) {
                        if(value instanceof SliderValue) {
                            SliderValue slider = (SliderValue) value;
                            slider.setValue(Math.max(slider.getMin(), slider.get() - slider.getIncrement()));
                        } else {
                            if(this.valueN == selectedSetting.getValues().size() - 1) {
                                this.valueN = 0;
                            } else {
                                this.valueN++;
                            }
                        }
                    }
                }
            }
        } else if(key == 205) {
            if(!isTypeOpened) {
                this.moduleN = 0;
                selectedType.setOpened(true);
            } else {
                if(!selectedModuleOpened && !selectedModule.areSettingsEmpty()) {
                    this.settingN = 0;
                    selectedModule.setOpened(true);
                } else if(selectedSetting != null && !selectedSetting.isOpened()) {
                    if(value instanceof ModeValue || value instanceof SliderValue) {
                        selectedSetting.setOpened(true);
                    }
                }
            }
        } else if(key == 203) {
            if(selectedType.isOpened()) {
                if(selectedModule.isOpened()) {
                    if(selectedSetting != null && selectedSetting.isOpened()) {
                        this.valueN = 0;
                        selectedSetting.setOpened(false);
                    } else {
                        this.settingN = 0;
                        selectedModule.setOpened(false);
                    }
                } else {
                    this.moduleN = 0;
                    selectedType.setOpened(false);
                }
            }
        }
    });

    private TabType getSelectedType() {
        for(TabType type : this.types) {
            if(type.isSelected()) {
                return type;
            }
        }
        return null;
    }

    public int getTypeN() {
        return this.typeN;
    }

    public int getModuleN() {
        return this.moduleN;
    }

    public int getSettingN() {
        return this.settingN;
    }

    public int getValueN() {
        return this.valueN;
    }

    public class TabType {
        private final List<TabModule> modules = new ArrayList<>();
        private final Category type;
        private final TabGUI tabGUI;
        private boolean opened;
        private float i = 0;

        public TabType(TabGUI tabGUI, Category category) {
            this.type = category;
            this.tabGUI = tabGUI;
            this.opened = false;

            for(Module module : Client.INSTANCE.getModuleManager().getModules(category)) {
                this.modules.add(new TabModule(module, this));
            }
        }

        public void render() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return;

            double y = 15 + this.tabGUI.types.indexOf(this) * 12;
            double eY = y + 12;
            double v = Client.INSTANCE.getMc().getDebugFPS() / 13.0;

            if(isSelected()) {
                if(i < 3) i = (float) MathHelper.clamp_double(i + 3 / v, 0, 3);
            } else if(i > 0) {
                i = (float) MathHelper.clamp_double(i - 3 / v, 0, 3);
            }

            String name = this.type.name().substring(0, 1) + this.type.name().substring(1).toLowerCase();
            Gui.drawRect(0, (int)y, 65, (int)eY, new Color(20, 20, 20, 170).getRGB());
            if(isSelected()) Gui.drawRect(0, (int)y, 65, (int)eY, tabGUI.getColor());
            hud.fr.drawStringWithShadow(name, 3 + i, (float)(y + 3), 0xffffffff);

            if(isOpened()) {
                this.modules.forEach(TabModule::render);
            }
        }

        public TabModule getSelectedModule() {
            for(TabModule module : this.modules) {
                if(module.isSelected()) {
                    return module;
                }
            }
            return null;
        }

        public boolean isSelected() {
            return this.tabGUI.types.indexOf(this) == this.tabGUI.getTypeN();
        }

        public boolean isOpened() {
            return this.opened;
        }

        public void setOpened(boolean opened) {
            this.opened = opened;
        }

        public TabGUI getTabGUI() {
            return this.tabGUI;
        }

        public List<TabModule> getModules() {
            return this.modules;
        }

        public Category getType() {
            return this.type;
        }
    }

    public class TabModule {
        private final List<TabSetting> settings = new ArrayList<>();
        private final TabType type;
        private final Module mod;
        private boolean opened;
        private float i = 0;

        public TabModule(Module mod, TabType type) {
            this.mod = mod;
            this.type = type;
            this.opened = false;

            for(Value value : mod.getValues()) {
                this.settings.add(new TabSetting(value, this));
            }
        }

        public void render() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return;

            float y = 15 + this.type.getModules().indexOf(this) * 12;
            float eY = y + 12;
            double v = Client.INSTANCE.getMc().getDebugFPS() / 13.0;

            if(isSelected()) {
                if(i < 3) i = (float) MathHelper.clamp_double(i + 3 / v, 0, 3);
            } else if(i > 0) {
                i = (float) MathHelper.clamp_double(i - 3 / v, 0, 3);
            }

            Gui.drawRect(66, (int)y, 88 + getLongest(), (int)eY, new Color(20, 20, 20, 170).getRGB());
            if(isSelected()) Gui.drawRect(66, (int)y, 88 + getLongest(), (int)eY, type.getTabGUI().getColor());
            hud.fr.drawStringWithShadow(this.mod.getName(), 69 + i, (int)y + 3,
                    this.mod.isToggled() ? 0xffffffff : new Color(163, 163, 163, 255).getRGB());

            if(isOpened()) {
                this.settings.forEach(TabSetting::render);
            }
        }

        public TabSetting getSelectedSetting() {
            for(TabSetting setting : this.settings) {
                if(setting.isSelected()) {
                    return setting;
                }
            }
            return null;
        }

        public boolean areSettingsEmpty() {
            return this.settings.isEmpty();
        }

        public int getLongest() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return 0;

            int longest = 0;
            for(Module module : Client.INSTANCE.getModuleManager().getModules(this.type.getType())) {
                if(hud.fr.getStringWidth(module.getName()) > longest) {
                    longest = hud.fr.getStringWidth(module.getName());
                }
            }
            return longest;
        }

        public Module getMod() {
            return this.mod;
        }

        public boolean isSelected() {
            return this.type.getModules().indexOf(this) == this.type.getTabGUI().getModuleN();
        }

        public boolean isOpened() {
            return this.opened;
        }

        public void setOpened(boolean opened) {
            this.opened = opened;
        }

        public List<TabSetting> getSettings() {
            return this.settings;
        }

        public TabType getType() {
            return this.type;
        }
    }

    public class TabSetting {
        private final List<TabValue> values = new ArrayList<>();
        private final Value value;
        private final TabModule module;
        private boolean opened;
        private float i = 0;

        public TabSetting(Value value, TabModule module) {
            this.value = value;
            this.module = module;
            this.opened = false;

            if(value instanceof SliderValue) {
                this.values.add(new TabValue(this));
            } else if(value instanceof ModeValue) {
                for(String mode : ((ModeValue) value).getModes()) {
                    this.values.add(new TabValue(this, mode));
                }
            }
        }

        public void render() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return;

            float y = 15 + this.module.getSettings().indexOf(this) * 12;
            float eY = y + 12;
            double v = Client.INSTANCE.getMc().getDebugFPS() / 13.0;

            if(isSelected()) {
                if(i < 3) i = (float) MathHelper.clamp_double(i + 3 / v, 0, 3);
            } else if(i > 0) {
                i = (float) MathHelper.clamp_double(i - 3 / v, 0, 3);
            }

            Gui.drawRect(89 + this.module.getLongest(), (int)y, 108 + this.module.getLongest() + getLongestS(), (int)eY,
                    new Color(20, 20, 20, 170).getRGB());
            if(isSelected())
                Gui.drawRect(89 + this.module.getLongest(), (int)y, 108 + this.module.getLongest() + getLongestS(), (int)eY, module.getType().getTabGUI().getColor());

            int color = 0xffffffff;
            if(value instanceof BoolValue) {
                color = ((BoolValue) value).get() ? 0xffffffff : new Color(163, 163, 163, 255).getRGB();
            }

            hud.fr.drawStringWithShadow(this.value.getName(), 93 + i + this.module.getLongest(), (int)y + 3, color);

            if(isOpened()) {
                this.values.forEach(TabValue::render);
            }
        }

        public TabValue getSelectedValue() {
            for(TabValue value : this.values) {
                if(value.isSelected()) {
                    return value;
                }
            }
            return null;
        }

        public int getLongestS() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return 0;

            int longest = 0;
            for(Value value : this.module.getMod().getValues()) {
                if(hud.fr.getStringWidth(value.getName()) > longest) {
                    longest = hud.fr.getStringWidth(value.getName());
                }
            }
            return longest;
        }

        public boolean isSelected() {
            return this.module.getSettings().indexOf(this) == this.module.getType().getTabGUI().getSettingN();
        }

        public Value getValue() {
            return this.value;
        }

        public TabModule getModule() {
            return this.module;
        }

        public boolean isOpened() {
            return this.opened;
        }

        public void setOpened(boolean opened) {
            this.opened = opened;
        }

        public List<TabValue> getValues() {
            return this.values;
        }
    }

    public class TabValue {
        private final TabSetting setting;
        private String value;
        private float i = 0;

        public TabValue(TabSetting setting) {
            this.setting = setting;
        }

        public TabValue(TabSetting setting, String value) {
            this.value = value;
            this.setting = setting;
        }

        public void render() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return;

            float y = 15 + this.setting.getValues().indexOf(this) * 12;
            float ey = y + 12;
            double i1 = Client.INSTANCE.getMc().getDebugFPS() / 13.0;

            if(isSelected()) {
                if(i < 3) i = (float) MathHelper.clamp_double(i + 3 / i1, 0, 3);
            } else if(i > 0) {
                i = (float) MathHelper.clamp_double(i - 3 / i1, 0, 3);
            }

            if(this.setting.getValue() instanceof SliderValue) {
                SliderValue slider = (SliderValue) this.setting.getValue();
                double d = slider.get();
                double rounded = (int) (d * 100) / 100D;
                String string = rounded % 1 == 0 ? new DecimalFormat("0.##").format(rounded) : rounded + "";

                Gui.drawRect(109 + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)y,
                        119 + this.setting.getModule().getLongest() + this.setting.getLongestS() + getLong(), (int)ey,
                        new Color(20, 20, 20, 170).getRGB());
                Gui.drawRect(109 + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)y,
                        109 + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)ey,
                        setting.getModule().getType().getTabGUI().getColor());
                hud.fr.drawStringWithShadow(string,
                        114 + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)y + 3, 0xffffffff);
            } else if(this.setting.getValue() instanceof ModeValue) {
                ModeValue mode = (ModeValue) this.setting.getValue();

                Gui.drawRect(109 + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)y,
                        129 + this.setting.getModule().getLongest() + this.setting.getLongestS() + getLong(), (int)ey,
                        new Color(20, 20, 20, 170).getRGB());
                if(isSelected())
                    Gui.drawRect(109 + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)y,
                            129 + this.setting.getModule().getLongest() + this.setting.getLongestS() + getLong(), (int)ey,
                            setting.getModule().getType().getTabGUI().getColor());
                hud.fr.drawStringWithShadow(this.value,
                        114 + i + this.setting.getModule().getLongest() + this.setting.getLongestS(), (int)y + 3,
                        mode.get().equalsIgnoreCase(this.value) ? 0xffffffff : new Color(163, 163, 163, 255).getRGB());
            }
        }

        public int getLong() {
            HUD hud = (HUD) Client.INSTANCE.getModuleManager().getModule("HUD");
            if(hud == null || hud.fr == null) return 0;

            if(this.setting.getValue() instanceof SliderValue) {
                SliderValue slider = (SliderValue) this.setting.getValue();
                double d = slider.get();
                double rounded = (int) (d * 100) / 100D;
                String string = rounded % 1 == 0 ? new DecimalFormat("0.##").format(rounded) : rounded + "";
                return hud.fr.getStringWidth(string);
            } else if(this.setting.getValue() instanceof ModeValue) {
                int longest = 0;
                for(String mode : ((ModeValue) this.setting.getValue()).getModes()) {
                    if(hud.fr.getStringWidth(mode) > longest) {
                        longest = hud.fr.getStringWidth(mode);
                    }
                }
                return longest;
            }
            return 0;
        }

        public String getValue() {
            return this.value;
        }

        public boolean isSelected() {
            return this.setting.getValues().indexOf(this) == this.setting.getModule().getType().getTabGUI().getValueN();
        }
    }
}
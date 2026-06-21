package wtf.fentanyl;

import wtf.fentanyl.client.commands.CommandManager;
import wtf.fentanyl.client.config.Config;
import wtf.fentanyl.client.modules.ModuleManager;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.processes.TargetProcess;
import wtf.fentanyl.event.impl.EventKey;
import wtf.fentanyl.client.widget.SessionInfoWidget;
import wtf.fentanyl.client.widget.TargetHUDWidget;
import wtf.fentanyl.event.impl.game.player.TickEvent;
import wtf.fentanyl.event.impl.Event2D;
import wtf.fentanyl.event.impl.Event3D;
import wtf.fentanyl.event.impl.EventRenderNameTag;
import wtf.fentanyl.event.impl.EventWorld;
import wtf.fentanyl.gui.click.DropdownGUI;
import wtf.fentanyl.gui.notification.NotificationManager;
import lombok.Getter;
import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import me.zero.alpine.listener.Subscriber;
import wtf.fentanyl.client.font.CFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.Display;
import org.lwjgl.input.Keyboard;

@Getter
public enum Client implements Subscriber {
    INSTANCE;

    public String name = "Fentanyl";

    public final Minecraft mc = Minecraft.getMinecraft();
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private NotificationManager notificationManager;
    public TargetProcess targetProcess;
    private CFontRenderer fr;
    private Config defaultConfig;

    public static final EventBus BUS = EventManager.builder()
            .setName("root/github")
            .setSuperListeners()
            .build();

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public void init() {
        fr = new CFontRenderer("arial", 30, 0, true, true);
        BUS.subscribe(this);
        Display.setTitle(name);

        moduleManager = new ModuleManager();
        targetProcess = new TargetProcess();
        commandManager = new CommandManager();
        notificationManager = new NotificationManager();

        defaultConfig = new Config("default");
        defaultConfig.load(getTargetHUD(), getSessionInfo());
    }

    public void shutdown() {
        defaultConfig.save(getTargetHUD(), getSessionInfo());
        BUS.unsubscribe(this);
    }

    public void onTick() {
        BUS.post(new TickEvent(TickEvent.EventType.PRE));
    }

    public void onRender2D() {
        ScaledResolution sr = new ScaledResolution(mc);
        BUS.post(new Event2D(sr));
    }

    public void onRender3D(float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        BUS.post(new Event3D(partialTicks, sr));
    }

    public boolean onRenderNameTag(Entity entity) {
        EventRenderNameTag event = new EventRenderNameTag(entity);
        BUS.post(event);
        return event.isCancelled();
    }

    public void onWorldChange(World oldWorld, World newWorld) {
        BUS.post(new EventWorld(oldWorld, newWorld));
    }

    private TargetHUDWidget getTargetHUD() {
        try {
            Class<?> hudClass = Class.forName("wtf.fentanyl.client.modules.impl.render.HUD");
            Object hudModule = moduleManager.getModule((Class) hudClass);
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
            Object hudModule = moduleManager.getModule((Class) hudClass);
            if (hudModule != null) {
                return (SessionInfoWidget) hudClass.getDeclaredField("sessionInfo").get(hudModule);
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Subscribe
    private final Listener<EventKey> eventkeyListener = new Listener<>(e -> {
        if (e.getKey() == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(new DropdownGUI());
        }

        for (Module module : moduleManager.getModules()) {
            if (module.getKey() == e.getKey()) {
                module.toggle();
            }
        }
    });
}
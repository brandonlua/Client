package wtf.rania.client.modules.impl.render;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.event.impl.Event2D;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

@ModuleInfo(name = "Notification", category = Category.RENDER)
public class Notification extends Module {

    public ModeValue notificationMode = new ModeValue("Mode", new String[]{"Exhi"}, "Exhi", this);

    @Subscribe
    private final Listener<Event2D> event2DListener = new Listener<>(e -> {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        Client.INSTANCE.getNotificationManager().publish(sr, false);
    });

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
package wtf.rania.client.modules.impl.render;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.UpdateEvent;
import wtf.rania.event.impl.PacketEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

@ModuleInfo(name = "Ambience", description = "change time", category = Category.RENDER)
public class Ambience extends Module {

    public final SliderValue time = new SliderValue("Time", 18000, 0, 24000, this);

    @Subscribe
    private Listener<UpdateEvent> updateListener;

    @Subscribe
    private Listener<PacketEvent.Receive> packetListener;

    public Ambience() {
        updateListener = new Listener<>(event -> {
            if (mc.theWorld != null) {
                mc.theWorld.setWorldTime((long) time.get());
            }
        });

        packetListener = new Listener<>(event -> {
            if (event.getPacket() instanceof S03PacketTimeUpdate) {
                event.setCancelled(true);
            }
        });
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
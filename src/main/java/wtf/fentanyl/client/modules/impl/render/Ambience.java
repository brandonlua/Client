package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.UpdateEvent;
import wtf.fentanyl.event.impl.PacketEvent;
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
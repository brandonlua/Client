package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.event.impl.game.player.TickEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;

@ModuleInfo(name = "NoFire", category = Category.RENDER)
public class NoFire extends Module {

    @Subscribe
    private final Listener<TickEvent> tickEventListener = new Listener<>(event -> {
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
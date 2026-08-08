package wtf.rania.client.modules.impl.render;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.event.impl.game.player.TickEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;

@ModuleInfo(name = "NoFire", category = Category.RENDER)
public class NoFireModule extends Module {

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
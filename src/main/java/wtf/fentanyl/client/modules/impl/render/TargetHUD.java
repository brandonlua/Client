package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.widget.TargetHUDWidget;
import wtf.fentanyl.event.impl.Event2D;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;

@ModuleInfo(name = "TargetHUD", category = Category.RENDER)
public class TargetHUD extends Module {

    public ModeValue mode = new ModeValue("Mode", new String[]{"Modern", "Akrien", "Adjust", "Gamesense"}, "Modern", this);
    public TargetHUDWidget widget;

    public TargetHUD() {
        widget = new TargetHUDWidget();
    }

    @Subscribe
    private final Listener<Event2D> event2DListener = new Listener<>(e -> {
        widget.setMode(mode.get());
        widget.render();
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
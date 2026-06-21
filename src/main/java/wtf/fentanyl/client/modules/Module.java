package wtf.fentanyl.client.modules;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.values.Value;
import wtf.fentanyl.event.impl.Event2D;
import wtf.fentanyl.event.impl.EventKey;
import wtf.fentanyl.event.impl.UpdateEvent;
import wtf.fentanyl.gui.notification.NotificationType;
import wtf.fentanyl.util.animations.Animation;
import wtf.fentanyl.util.animations.impl.EaseInOutQuad;
import lombok.Getter;
import lombok.Setter;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import me.zero.alpine.listener.Subscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Module implements Subscriber {
    private final String name, description;
    private final Category category;
    private final boolean enabledByDefault;
    public int key;
    public boolean toggled;
    private boolean expanded;
    private boolean hidden;
    private final List<Value> values = new ArrayList<>();
    private final Animation animation = new EaseInOutQuad(200, 1.0);

    protected static final Minecraft mc = Client.INSTANCE.getMc();

    public Module() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        Validate.notNull(info, "annotation expectation");

        this.name = info.name();
        this.description = info.description();
        this.category = info.category();
        this.enabledByDefault = info.enabled();
        this.key = info.key();
        this.hidden = false;

        if (enabledByDefault) toggle();
    }

    public void toggle() {
        setEnabled(!toggled);

        if (Client.INSTANCE.getNotificationManager() != null) {
            Client.INSTANCE.getNotificationManager().post(
                    toggled ? NotificationType.OKAY : NotificationType.INFO,
                    name,
                    toggled ? "Enabled" : "Disabled"
            );
        }
    }

    public void setEnabled(boolean state) {
        if (this.toggled == state) return;
        this.toggled = state;

        if (state) {
            Client.BUS.subscribe(this);
            Client.BUS.subscribe(updateEventListener);
            Client.BUS.subscribe(event2DListener);
            Client.BUS.subscribe(eventKeyListener);
            onEnabled();
        } else {
            Client.BUS.unsubscribe(this);
            Client.BUS.unsubscribe(updateEventListener);
            Client.BUS.unsubscribe(event2DListener);
            Client.BUS.unsubscribe(eventKeyListener);
            onDisabled();
        }
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void onUpdate() {
    }

    public void on2D(ScaledResolution sr) {
    }

    public void onKey(int key) {
    }

    public String getSuffix() {
        return "";
    }

    public void addValue(Value value) {
        values.add(value);
    }

    public List<Value> getValues() {
        return values;
    }

    public Animation getAnimation() {
        return animation;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isEnabled() {
        return toggled;
    }

    @Subscribe
    private final Listener<UpdateEvent> updateEventListener = new Listener<>(e -> {
        if(toggled) onUpdate();
    });

    @Subscribe
    private final Listener<Event2D> event2DListener = new Listener<>(e -> {
        if(toggled) on2D(e.getSr());
    });

    @Subscribe
    private final Listener<EventKey> eventKeyListener = new Listener<>(e -> {
        if (toggled) onKey(e.getKey());
    });
}
package wtf.fentanyl.event.impl;

import wtf.fentanyl.event.CancellableEvent;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
public class EventRenderNameTag extends CancellableEvent {

    final Entity entity;

    public EventRenderNameTag(Entity entity) {
        this.entity = entity;
    }

}
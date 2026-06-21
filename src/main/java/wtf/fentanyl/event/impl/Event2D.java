package wtf.fentanyl.event.impl;

import wtf.fentanyl.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;

@Getter
@AllArgsConstructor
public class Event2D extends Event {
    private final ScaledResolution sr;
}

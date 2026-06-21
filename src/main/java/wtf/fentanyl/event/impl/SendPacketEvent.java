package wtf.fentanyl.event.impl;

import net.minecraft.network.Packet;
import wtf.fentanyl.event.Event;

public class SendPacketEvent extends Event {
    public Packet<?> packet;

    public SendPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}

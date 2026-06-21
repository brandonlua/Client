package wtf.fentanyl.event.impl;

import net.minecraft.network.Packet;

public class PacketEvent {
    public Packet packet;
    public boolean cancelled;
    public boolean incoming;
    public boolean outgoing;

    public PacketEvent(Packet packet, boolean incoming) {
        this.packet = packet;
        this.incoming = incoming;
        this.outgoing = !incoming;
    }

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(Packet packet) {
        this.packet = packet;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void cancelEvent() {
        this.cancelled = true;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public static class Receive extends PacketEvent {
        public Receive(Packet packet) {
            super(packet, true);
        }
    }

    public static class Send extends PacketEvent {
        public Send(Packet packet) {
            super(packet, false);
        }
    }
}
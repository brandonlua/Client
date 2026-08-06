package wtf.rania.client.modules.impl.misc;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.PacketEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.network.play.server.S02PacketChat;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@ModuleInfo(name = "AutoGG", description = "Says gg after a game", category = Category.MISC
)
public class AutoGG extends Module {

    private ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Custom"}, "Normal", this);
    private SliderValue delay = new SliderValue("Delay", 1000, 0, 5000, this);

    private List<String> normalMessages = Arrays.asList(
            "gg",
            "GG"
    );

    private List<String> triggerPhrases = Arrays.asList(
            "winner",
            "won the game",
            "victory",
            "has won",
            "winners:",
            "winning team",
            "game over"
    );

    private boolean sentMessage = false;
    private long lastGameEnd = 0;
    private Random random = new Random();

    @Subscribe
    private Listener<PacketEvent> packetListener;

    public AutoGG() {
        packetListener = new Listener<>(e -> {
            if (e.getPacket() instanceof S02PacketChat) {
                S02PacketChat packet = (S02PacketChat) e.getPacket();
                String message = packet.getChatComponent().getUnformattedText().toLowerCase();

                if (!sentMessage && isGameEndMessage(message)) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastGameEnd > 10000) {
                        lastGameEnd = currentTime;
                        sendGGMessage();
                        sentMessage = true;

                        new Thread(() -> {
                            try {
                                Thread.sleep(5000);
                                sentMessage = false;
                            } catch (InterruptedException ex) {
                            }
                        }).start();
                    }
                }
            }
        });
    }

    private boolean isGameEndMessage(String message) {
        for (String phrase : triggerPhrases) {
            if (message.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private void sendGGMessage() {
        new Thread(() -> {
            try {
                Thread.sleep((long) delay.get());
                String message = mode.is("Normal") ?
                        normalMessages.get(random.nextInt(normalMessages.size())) :
                        "GG";
                mc.thePlayer.sendChatMessage(message);
            } catch (InterruptedException e) {
            }
        }).start();
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
        sentMessage = false;
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}
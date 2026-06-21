package wtf.fentanyl.client.modules.impl.misc;

import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.TextValue;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.LogLevel;
import de.jcm.discordgamesdk.activity.Activity;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ChatComponentText;

import java.time.Instant;

@ModuleInfo(name = "DiscordRPC", description = "Discord Rich Presence", category = Category.MISC)
public final class DiscordRPCModule extends Module {

    private final BoolValue hideServer = new BoolValue("Hide server", false, this);
    private final TextValue applicationId = new TextValue("Application ID", "1467475483040940186", this);

    private volatile boolean running;

    public DiscordRPCModule() {
        addValue(this.hideServer);
        addValue(this.applicationId);
    }

    private void setupActivity() {
        if (this.running) {
            return;
        }

        this.running = true;

        // Reconnection loop
        while (this.running) {
            try {
                final long clientId = Long.parseLong(this.applicationId.get());
                final CreateParams params = new CreateParams();
                params.setClientID(clientId);
                params.setFlags(CreateParams.Flags.NO_REQUIRE_DISCORD);

                try (final Core core = new Core(params)) {
                    core.setLogHook(
                            LogLevel.INFO,
                            (level, message) -> {
                                switch (level) {
                                    case ERROR:
                                        System.err.println("[Discord SDK Error] " + message);
                                        break;
                                    case INFO:
                                        System.out.println("[Discord SDK Info] " + message);
                                        break;
                                    case WARN:
                                        System.out.println("[Discord SDK Warn] " + message);
                                        break;
                                }
                            });

                    if (mc.thePlayer != null) {
                        mc.thePlayer
                                .addChatMessage(new ChatComponentText("§7[§bDiscord§7] §aDiscord RPC initialized!"));
                    }

                    try (final Activity activity = new Activity()) {
                        activity.timestamps().setStart(Instant.now());
                        activity.assets().setLargeImage("galaxy");
                        activity.assets().setLargeText("Solarium Client");

                        core.activityManager().updateActivity(activity);

                        while (this.running && core.isOpen()) {
                            try {
                                String state;
                                final ServerData serverData = mc.getCurrentServerData();

                                if (serverData == null) {
                                    state = "In Singleplayer";
                                } else {
                                    if (this.hideServer.get()) {
                                        state = "Playing on a Server";
                                    } else {
                                        state = "Playing on " + serverData.serverIP;
                                    }
                                }

                                activity.setState(state);
                                core.activityManager().updateActivity(activity);
                                core.runCallbacks();

                                Thread.sleep(2000L);
                            } catch (Exception e) {
                                System.out.println("[Discord] Error in update loop: " + e.getMessage());
                                break;
                            }
                        }
                    }
                }
            } catch (NumberFormatException nfe) {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(
                            "§7[§bDiscord§7] §cInvalid Application ID! Ensure it is a valid number."));
                }
                this.running = false;
                return;
            } catch (Throwable t) {
                String error = t.getMessage();
                if (t instanceof java.lang.NoClassDefFoundError) {
                    if (mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(
                                new ChatComponentText("§7[§bDiscord§7] §cDiscord SDK missing! Dependency issue."));
                    }
                    System.err.println("[Discord] Missing class: " + error);
                } else if (error != null && error.contains("NOT_RUNNING")) {
                    if (mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(new ChatComponentText(
                                "§7[§bDiscord§7] §cDiscord is not running! Open Discord to use RPC."));
                    }
                } else {
                    String msg = "§cDiscord RPC failed: " + (error != null ? error : t.getClass().getSimpleName());
                    System.err.println("[Discord] " + msg);
                    if (mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(new ChatComponentText("§7[§bDiscord§7] " + msg));
                    }
                    t.printStackTrace();
                }
            }

            if (this.running) {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        new Thread(this::setupActivity, "Discord-RPC-Init").start();
    }

    @Override
    public void onDisabled() {
        this.running = false;
    }
}

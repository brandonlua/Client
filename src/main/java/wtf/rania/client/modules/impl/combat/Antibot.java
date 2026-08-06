package wtf.rania.client.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.event.impl.EventWorld;
import wtf.rania.event.impl.game.player.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(name = "Antibot", category = Category.COMBAT)
public class Antibot extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Tab"}, "Tab", this);

    public static final List<EntityPlayer> botList = new ArrayList<>();

    @Subscribe
    private final Listener<MotionEvent> motionListener = new Listener<>(event -> {
        mc.theWorld.playerEntities.forEach(player -> {
            if (player.maxHurtTime == 0) {
                if (player.getHealth() == 20.0f) {
                    String unformattedText = player.getDisplayName().getUnformattedText();
                    if (unformattedText.length() >= 7 && unformattedText.charAt(2) == '[' && unformattedText.charAt(3) == 'N' && unformattedText.charAt(6) == ']') {
                        botList.add(player);
                    }
                    if (player.getDisplayName().toString().contains(" ")) {
                        botList.add(player);
                    }
                }
            }
            if (player.getDisplayName().toString().isEmpty()) {
                botList.add(player);
            }
            if (player.getEntityId() < 0) {
                botList.add(player);
            }
        });

        mc.theWorld.playerEntities.forEach(player -> {
            if (!getTablist().contains(player.getDisplayName().getUnformattedText())) {
                botList.add(player);
            }
        });
    });

    @Subscribe
    private final Listener<EventWorld> eventWorldListener = new Listener<>(event -> {
        botList.clear();
    });

    private List<String> getTablist() {
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().parallelStream()
                .map(NetworkPlayerInfo::getGameProfile)
                .filter(profile -> profile.getId() != Minecraft.getMinecraft().thePlayer.getUniqueID())
                .map(GameProfile::getName)
                .collect(Collectors.toList());
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        botList.clear();
    }
}
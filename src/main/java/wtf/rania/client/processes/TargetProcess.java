package wtf.rania.client.processes;

import wtf.rania.Client;
import wtf.rania.client.modules.impl.combat.KillAura;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import me.zero.alpine.listener.Subscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import wtf.rania.event.impl.game.player.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetProcess implements Subscriber {

    protected static final Minecraft mc = Minecraft.getMinecraft();

    public List<EntityLivingBase> list = new ArrayList<>();
    public EntityLivingBase target;

    private KillAura killaura;

    public TargetProcess() {
        Client.BUS.subscribe(this);
        killaura = (KillAura) Client.INSTANCE.getModuleManager().getModule(KillAura.class);
    }

    @Subscribe
    private final Listener<TickEvent> tickListener = new Listener<>(event -> {
        if (event.getType() == TickEvent.EventType.PRE) {
            if (mc.theWorld != null && mc.thePlayer != null && shouldLook()) {
                list.clear();

                mc.theWorld.getLoadedEntityList().stream()
                        .filter(e -> e instanceof EntityLivingBase)
                        .map(e -> (EntityLivingBase) e)
                        .filter(this::isValid)
                        .sorted(getComparator())
                        .forEachOrdered(list::add);

                target = list.isEmpty() ? null : list.get(0);
            } else {
                target = null;
            }
        }
    });

    private boolean isValid(EntityLivingBase entity) {
        if (entity == mc.thePlayer) return false;
        if (entity.isDead || !entity.isEntityAlive()) return false;

        double distance = mc.thePlayer.getDistanceToEntity(entity);
        if (distance > killaura.searchRange.get()) return false;

        if (entity instanceof EntityPlayer && killaura.players.get()) return true;
        if (entity instanceof EntityAnimal && killaura.animals.get()) return true;
        if (entity instanceof EntityMob && killaura.monsters.get()) return true;

        return false;
    }

    private Comparator<EntityLivingBase> getComparator() {
        switch (killaura.sortMode.get()) {
            case "Health":
                return Comparator.comparingDouble(EntityLivingBase::getHealth);
            case "Armor":
                return Comparator.comparingInt(EntityLivingBase::getTotalArmorValue);
            case "Hurt-time":
                return Comparator.comparingInt(e -> -e.hurtTime);
            case "Ticks":
                return Comparator.comparingInt(e -> e.ticksExisted);
            case "Range":
            default:
                return Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e));
        }
    }

    public boolean shouldLook() {
        return killaura != null && killaura.isEnabled();
    }
}
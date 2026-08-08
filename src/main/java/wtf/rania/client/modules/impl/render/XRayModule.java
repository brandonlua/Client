package wtf.rania.client.modules.impl.render;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ColorValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.Event3D;
import wtf.rania.event.impl.game.player.TickEvent;
import wtf.rania.utility.render.RenderUtil;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "XRay", category = Category.RENDER)
public class XRayModule extends Module {

    private final BoolValue diamond = new BoolValue("Diamond", true, this);
    private final ColorValue diamondColor = new ColorValue("Diamond Color", new Color(0, 255, 255), this, diamond::get);

    private final BoolValue iron = new BoolValue("Iron", true, this);
    private final ColorValue ironColor = new ColorValue("Iron Color", new Color(200, 200, 200), this, iron::get);

    private final BoolValue gold = new BoolValue("Gold", true, this);
    private final ColorValue goldColor = new ColorValue("Gold Color", new Color(255, 255, 0), this, gold::get);

    private final BoolValue lapis = new BoolValue("Lapis", true, this);
    private final ColorValue lapisColor = new ColorValue("Lapis Color", new Color(0, 0, 255), this, lapis::get);

    private final BoolValue emerald = new BoolValue("Emerald", true, this);
    private final ColorValue emeraldColor = new ColorValue("Emerald Color", new Color(0, 255, 0), this, emerald::get);

    private final BoolValue redstone = new BoolValue("Redstone", true, this);
    private final ColorValue redstoneColor = new ColorValue("Redstone Color", new Color(255, 0, 0), this, redstone::get);

    private final BoolValue coal = new BoolValue("Coal", false, this);
    private final ColorValue coalColor = new ColorValue("Coal Color", new Color(50, 50, 50), this, coal::get);

    private final BoolValue chests = new BoolValue("Chests", true, this);
    private final ColorValue chestColor = new ColorValue("Chest Color", new Color(255, 165, 0), this, chests::get);

    private final BoolValue spawner = new BoolValue("Spawner", true, this);
    private final ColorValue spawnerColor = new ColorValue("Spawner Color", new Color(255, 0, 255), this, spawner::get);

    private final SliderValue searchRange = new SliderValue("Range", 50F, 10F, 150F, this);
    private final BoolValue outline = new BoolValue("Outline", true, this);
    private final BoolValue fill = new BoolValue("Fill", false, this);

    private final Map<BlockPos, Integer> blocks = new HashMap<>();
    private long lastSearch = 0;

    @Subscribe
    private final Listener<TickEvent> tickListener = new Listener<>(event -> {
        if (event.getType() == TickEvent.EventType.POST) {
            if (System.currentTimeMillis() - lastSearch > 1000) {
                searchBlocks();
                lastSearch = System.currentTimeMillis();
            }
        }
    });

    @Subscribe
    private final Listener<Event3D> renderListener = new Listener<>(event -> {
        RenderUtil.start();
        for (Map.Entry<BlockPos, Integer> entry : blocks.entrySet()) {
            drawBlock(entry.getKey(), entry.getValue());
        }
        RenderUtil.stop();
    });

    public XRayModule() {
    }

    private void searchBlocks() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        blocks.clear();

        int range = (int) searchRange.get();
        BlockPos playerPos = mc.thePlayer.getPosition();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();

                    if (diamond.get() && block == Blocks.diamond_ore) {
                        blocks.put(pos, diamondColor.get().getRGB());
                    } else if (iron.get() && block == Blocks.iron_ore) {
                        blocks.put(pos, ironColor.get().getRGB());
                    } else if (gold.get() && block == Blocks.gold_ore) {
                        blocks.put(pos, goldColor.get().getRGB());
                    } else if (lapis.get() && block == Blocks.lapis_ore) {
                        blocks.put(pos, lapisColor.get().getRGB());
                    } else if (emerald.get() && block == Blocks.emerald_ore) {
                        blocks.put(pos, emeraldColor.get().getRGB());
                    } else if (redstone.get() && block == Blocks.redstone_ore) {
                        blocks.put(pos, redstoneColor.get().getRGB());
                    } else if (coal.get() && block == Blocks.coal_ore) {
                        blocks.put(pos, coalColor.get().getRGB());
                    } else if (chests.get() && (block == Blocks.chest || block == Blocks.trapped_chest || block == Blocks.ender_chest)) {
                        blocks.put(pos, chestColor.get().getRGB());
                    } else if (spawner.get() && block == Blocks.mob_spawner) {
                        blocks.put(pos, spawnerColor.get().getRGB());
                    }
                }
            }
        }
    }

    private void drawBlock(BlockPos pos, int color) {
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);

        if (fill.get()) {
            RenderUtil.setColor(color);
            RenderUtil.drawBoundingBox(bb);
        }

        if (outline.get()) {
            glLineWidth(1.5f);
            RenderUtil.setColor(color);
            RenderUtil.drawOutlinedBoundingBox(bb);
            glLineWidth(1.0f);
        }
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
        lastSearch = System.currentTimeMillis();
        searchBlocks();
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        blocks.clear();
    }
}
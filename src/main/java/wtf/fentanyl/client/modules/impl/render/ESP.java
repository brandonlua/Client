package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.ColorValue;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.event.impl.Event2D;
import wtf.fentanyl.event.impl.Event3D;
import wtf.fentanyl.event.impl.EventRenderNameTag;
import wtf.fentanyl.event.impl.EventWorld;
import wtf.fentanyl.util.math.MathUtil;
import wtf.fentanyl.util.render.ColorUtil;
import wtf.fentanyl.util.render.GLUtil;
import wtf.fentanyl.util.render.GifRenderer;
import wtf.fentanyl.util.render.RenderUtil;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "ESP", description = "Renders players esp", category = Category.RENDER)
public class ESP extends Module {

    public final BoolValue tags = new BoolValue("Tags", true, this);
    public final SliderValue tagsSize = new SliderValue("Tags Size", 1f, 0.1f, 2, 0.05f, this, tags::get);
    public final BoolValue tagsHealth = new BoolValue("Tags Health", true, this, tags::get);
    public final BoolValue tagsBackground = new BoolValue("Tags Background", true, this, tags::get);
    public final BoolValue item = new BoolValue("Tags Items", true, this, tags::get);
    public final BoolValue esp2d = new BoolValue("2D ESP", true, this);
    public final BoolValue box = new BoolValue("Box", true, this, esp2d::get);
    public final ColorValue boxColor = new ColorValue("Box Color", Color.RED, this, () -> esp2d.get() && box.get());
    public final BoolValue healthBar = new BoolValue("Health Bar", true, this, esp2d::get);
    public final ColorValue absorptionColor = new ColorValue("Absorption Color", new Color(255, 255, 50), this, () -> esp2d.get() && healthBar.get());
    public final BoolValue armorBar = new BoolValue("Armor Bar", true, this, esp2d::get);
    public final ColorValue armorBarColor = new ColorValue("Armor Bar Color", new Color(50, 255, 255), this, () -> esp2d.get() && armorBar.get());
    public final ModeValue gif = new ModeValue("Gif", new String[]{"None", "Putin", "Meow"}, "None", this);

    private final Map<EntityPlayer, float[]> entityPosMap = new HashMap<>();
    GifRenderer putinGif = new GifRenderer(new ResourceLocation("client/texture/gif/putin.gif"));
    GifRenderer MeowGif = new GifRenderer(new ResourceLocation("client/texture/gif/togif.gif"));

    @Subscribe
    private Listener<EventWorld> worldListener;

    @Subscribe
    private Listener<Event2D> event2DListener;

    @Subscribe
    private Listener<Event3D> event3DListener;

    @Subscribe
    private Listener<EventRenderNameTag> renderNameTagListener;

    public ESP() {
        renderNameTagListener = new Listener<>(event -> {
            Entity entity = event.getEntity();
            if (tags.get() && entityPosMap.containsKey(entity))
                event.setCancelled(true);
        });

        worldListener = new Listener<>(event -> {
            entityPosMap.clear();
        });

        event2DListener = new Listener<>(event -> {
            if (gif.is("Putin")) {
                putinGif.update();
            } else if (gif.is("Meow")) {
                MeowGif.update();
            }

            for (EntityPlayer player : entityPosMap.keySet()) {
                if ((player.getDistanceToEntity(mc.thePlayer) < 1.0F && mc.gameSettings.thirdPersonView == 0) ||
                        !RenderUtil.isInViewFrustum(player))
                    continue;

                final float[] positions = entityPosMap.get(player);
                final float x = positions[0];
                final float y = positions[1];
                final float x2 = positions[2];
                final float y2 = positions[3];

                final float width = x2 - x;
                final float height = y2 - y;

                if (gif.is("Putin")) {
                    putinGif.drawTexture(x, y, width, height);
                } else if (gif.is("Meow")) {
                    MeowGif.drawTexture(x, y, width, height);
                }

                final float health = player.getHealth();
                final float maxHealth = player.getMaxHealth();
                final float healthPercentage = health / maxHealth;

                if (tags.get()) {
                    final FontRenderer fontRenderer = mc.fontRendererObj;

                    final String healthString = tagsHealth.get() ? " " + (MathUtil.roundToHalf(player.getHealth())) + EnumChatFormatting.RED + "❤" : "";
                    final String name = player.getDisplayName().getFormattedText() + healthString;
                    float halfWidth = (float) fontRenderer.getStringWidth(name) / 2 * tagsSize.get();
                    final float xDif = x2 - x;
                    final float middle = x + (xDif / 2);
                    final float textHeight = fontRenderer.FONT_HEIGHT * tagsSize.get();
                    float renderY = y - textHeight - 2;

                    final float left = middle - halfWidth - 1;
                    final float right = middle + halfWidth + 1;

                    if (tagsBackground.get()) {
                        Gui.drawRect((int)left, (int)(renderY - 1), (int)right, (int)(renderY + textHeight + 1), 0x96000000);
                    }

                    fontRenderer.drawString(name, middle - halfWidth, renderY + 0.5F, -1, true);

                    if (item.get()) {
                        List<ItemStack> items = new ArrayList<>();
                        if (player.getHeldItem() != null) {
                            items.add(player.getHeldItem());
                        }
                        for (int index = 3; index >= 0; index--) {
                            ItemStack stack = player.inventory.armorInventory[index];
                            if (stack != null) {
                                items.add(stack);
                            }
                        }
                        float armorX = middle - ((float) (items.size() * 18) / 2) * tagsSize.get();

                        for (ItemStack stack : items) {
                            RenderUtil.renderItemStack(stack, armorX, renderY - 25 * tagsSize.get(), tagsSize.get() + tagsSize.get() / 2, true);
                            armorX += 18 * tagsSize.get();
                        }
                    }
                }

                if (esp2d.get()) {
                    glDisable(GL_TEXTURE_2D);
                    GLUtil.startBlend();

                    if (armorBar.get()) {
                        final float armorPercentage = player.getTotalArmorValue() / 20.0F;
                        final float armorBarWidth = (x2 - x) * armorPercentage;

                        glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);
                        glBegin(GL_QUADS);

                        {
                            glVertex2f(x, y2 + 0.5F);
                            glVertex2f(x, y2 + 2.5F);

                            glVertex2f(x2, y2 + 2.5F);
                            glVertex2f(x2, y2 + 0.5F);
                        }

                        if (armorPercentage > 0) {
                            RenderUtil.color(armorBarColor.get().getRGB());

                            {
                                glVertex2f(x + 0.5F, y2 + 1);
                                glVertex2f(x + 0.5F, y2 + 2);

                                glVertex2f(x + armorBarWidth - 0.5F, y2 + 2);
                                glVertex2f(x + armorBarWidth - 0.5F, y2 + 1);
                            }
                            RenderUtil.resetColor();
                        }

                        if (!healthBar.get())
                            glEnd();
                    }

                    if (healthBar.get()) {
                        float healthBarLeft = x - 2.5F;
                        float healthBarRight = x - 0.5F;

                        glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);

                        if (!armorBar.get())
                            glBegin(GL_QUADS);

                        {
                            glVertex2f(healthBarLeft, y);
                            glVertex2f(healthBarLeft, y2);

                            glVertex2f(healthBarRight, y2);
                            glVertex2f(healthBarRight, y);
                        }

                        healthBarLeft += 0.5F;
                        healthBarRight -= 0.5F;

                        final float heightDif = y - y2;
                        final float healthBarHeight = heightDif * healthPercentage;
                        final float topOfHealthBar = y2 + 0.5F + healthBarHeight;

                        RenderUtil.color(ColorUtil.getColorFromPercentage(healthPercentage));

                        {
                            glVertex2f(healthBarLeft, topOfHealthBar);
                            glVertex2f(healthBarLeft, y2 - 0.5F);

                            glVertex2f(healthBarRight, y2 - 0.5F);
                            glVertex2f(healthBarRight, topOfHealthBar);
                        }

                        RenderUtil.resetColor();

                        final float absorption = player.getAbsorptionAmount();
                        final float absorptionPercentage = Math.min(1.0F, absorption / 20.0F);
                        final float absorptionHeight = heightDif * absorptionPercentage;
                        final float topOfAbsorptionBar = y2 + 0.5F + absorptionHeight;

                        RenderUtil.color(absorptionColor.get().getRGB());

                        {
                            glVertex2f(healthBarLeft, topOfAbsorptionBar);
                            glVertex2f(healthBarLeft, y2 - 0.5F);

                            glVertex2f(healthBarRight, y2 - 0.5F);
                            glVertex2f(healthBarRight, topOfAbsorptionBar);
                        }

                        RenderUtil.resetColor();

                        if (!box.get())
                            glEnd();
                    }

                    if (box.get()) {
                        glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);
                        if (!healthBar.get())
                            glBegin(GL_QUADS);

                        {
                            glVertex2f(x, y);
                            glVertex2f(x, y2);
                            glVertex2f(x + 1.5F, y2);
                            glVertex2f(x + 1.5F, y);

                            glVertex2f(x2 - 1.5F, y);
                            glVertex2f(x2 - 1.5F, y2);
                            glVertex2f(x2, y2);
                            glVertex2f(x2, y);

                            glVertex2f(x + 1.5F, y);
                            glVertex2f(x + 1.5F, y + 1.5F);
                            glVertex2f(x2 - 1.5F, y + 1.5F);
                            glVertex2f(x2 - 1.5F, y);

                            glVertex2f(x + 1.5F, y2 - 1.5F);
                            glVertex2f(x + 1.5F, y2);
                            glVertex2f(x2 - 1.5F, y2);
                            glVertex2f(x2 - 1.5F, y2 - 1.5F);
                        }

                        RenderUtil.color(boxColor.get().getRGB());

                        {
                            glVertex2f(x + 0.5F, y + 0.5F);
                            glVertex2f(x + 0.5F, y2 - 0.5F);
                            glVertex2f(x + 1, y2 - 0.5F);
                            glVertex2f(x + 1, y + 0.5F);

                            glVertex2f(x2 - 1, y + 0.5F);
                            glVertex2f(x2 - 1, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y + 0.5F);

                            glVertex2f(x + 0.5F, y + 0.5F);
                            glVertex2f(x + 0.5F, y + 1);
                            glVertex2f(x2 - 0.5F, y + 1);
                            glVertex2f(x2 - 0.5F, y + 0.5F);

                            glVertex2f(x + 0.5F, y2 - 1);
                            glVertex2f(x + 0.5F, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y2 - 0.5F);
                            glVertex2f(x2 - 0.5F, y2 - 1);
                        }

                        RenderUtil.resetColor();

                        glEnd();
                    }

                    glEnable(GL_TEXTURE_2D);
                    GLUtil.endBlend();
                }
            }
        });

        event3DListener = new Listener<>(event -> {
            final boolean project2D = esp2d.get() || tags.get() || !gif.is("None");
            if (project2D && !entityPosMap.isEmpty())
                entityPosMap.clear();

            final float partialTicks = event.getPartialTicks();

            for (final EntityPlayer player : mc.theWorld.playerEntities) {
                if (project2D) {
                    final double posX = (MathUtil.interpolate(player.prevPosX, player.posX, partialTicks) -
                            mc.getRenderManager().viewerPosX);
                    final double posY = (MathUtil.interpolate(player.prevPosY, player.posY, partialTicks) -
                            mc.getRenderManager().viewerPosY);
                    final double posZ = (MathUtil.interpolate(player.prevPosZ, player.posZ, partialTicks) -
                            mc.getRenderManager().viewerPosZ);

                    final double halfWidth = player.width / 2.0D;
                    final AxisAlignedBB bb = new AxisAlignedBB(posX - halfWidth, posY, posZ - halfWidth,
                            posX + halfWidth, posY + player.height + (player.isSneaking() ? -0.2D : 0.1D), posZ + halfWidth).expand(0.1, 0.1, 0.1);

                    final double[][] vectors = {{bb.minX, bb.minY, bb.minZ},
                            {bb.minX, bb.maxY, bb.minZ},
                            {bb.minX, bb.maxY, bb.maxZ},
                            {bb.minX, bb.minY, bb.maxZ},
                            {bb.maxX, bb.minY, bb.minZ},
                            {bb.maxX, bb.maxY, bb.minZ},
                            {bb.maxX, bb.maxY, bb.maxZ},
                            {bb.maxX, bb.minY, bb.maxZ}};

                    float[] projection;
                    final float[] position = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, -1.0F, -1.0F};

                    for (final double[] vec : vectors) {
                        projection = GLUtil.project2D((float) vec[0], (float) vec[1], (float) vec[2], event.getScaledResolution().getScaleFactor());
                        if (projection != null && projection[2] >= 0.0F && projection[2] < 1.0F) {
                            final float pX = projection[0];
                            final float pY = projection[1];
                            position[0] = Math.min(position[0], pX);
                            position[1] = Math.min(position[1], pY);
                            position[2] = Math.max(position[2], pX);
                            position[3] = Math.max(position[3], pY);
                        }
                    }

                    entityPosMap.put(player, position);
                }
            }
        });
    }

    public boolean isValid(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (!player.isEntityAlive()) {
                return false;
            }

            if (player == mc.thePlayer) {
                return false;
            }

            return RenderUtil.isBBInFrustum(entity.getEntityBoundingBox()) && mc.theWorld.playerEntities.contains(player);
        }

        return false;
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
        entityPosMap.clear();
    }
}
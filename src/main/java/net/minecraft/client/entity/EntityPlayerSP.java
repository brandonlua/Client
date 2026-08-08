package net.minecraft.client.entity;

import wtf.rania.Client;
import wtf.rania.event.impl.UpdateEvent;
import wtf.rania.event.impl.game.player.MotionEvent;
import wtf.rania.event.impl.game.player.SlowdownEvent;
import wtf.rania.utility.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSoundMinecartRiding;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiCommandBlock;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiBeacon;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.*;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;

import static net.minecraft.potion.Potion.*;
import static net.minecraft.potion.Potion.jump;

public class EntityPlayerSP extends AbstractClientPlayer {
    public final NetHandlerPlayClient sendQueue;
    private final StatFileWriter statWriter;
    private double lastReportedPosX;
    private double lastReportedPosY;
    private double lastReportedPosZ;
    private float lastReportedYaw;
    private float lastReportedPitch;
    private boolean serverSneakState;
    private boolean serverSprintState;
    private int positionUpdateTicks;
    private boolean hasValidHealth;
    private String clientBrand;
    public MovementInput movementInput;
    protected Minecraft mc;
    protected int sprintToggleTimer;
    public int sprintingTicksLeft;
    public float renderArmYaw;
    public float renderArmPitch;
    public float prevRenderArmYaw;
    public float prevRenderArmPitch;
    private int horseJumpPowerCounter;
    private float horseJumpPower;
    public float timeInPortal;
    public float prevTimeInPortal;

    public EntityPlayerSP(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandler, StatFileWriter statFile) {
        super(worldIn, netHandler.getGameProfile());
        this.sendQueue = netHandler;
        this.statWriter = statFile;
        this.mc = mcIn;
        this.dimension = 0;
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    public void heal(float healAmount) {
    }

    public void mountEntity(Entity entityIn) {
        super.mountEntity(entityIn);

        if (entityIn instanceof EntityMinecart) {
            this.mc.getSoundHandler().playSound(new MovingSoundMinecartRiding(this, (EntityMinecart) entityIn));
        }
    }

    public void onUpdate() {
        if (this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0D, this.posZ))) {
            // update event
            Client.BUS.post(new UpdateEvent());

            super.onUpdate();

            if (this.isRiding()) {

                this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(this.rotationYaw, this.rotationPitch, this.onGround));
                this.sendQueue.addToSendQueue(new C0CPacketInput(this.moveStrafing, this.moveForward, this.movementInput.jump, this.movementInput.sneak));
            } else {
                this.onUpdateWalkingPlayer();
            }
        }
    }

    public void onUpdateWalkingPlayer() {
        MotionEvent preUpdate = new MotionEvent(posX, posY, posZ, rotationYaw, rotationPitch, onGround, MotionEvent.State.PRE);
        MotionEvent postUpdate = new MotionEvent(MotionEvent.State.POST);
        Client.BUS.post(preUpdate);

        if (!preUpdate.isCancelled()) {
            Client.BUS.post(postUpdate);
        }


        boolean flag = this.isSprinting();

        if (flag != this.serverSprintState) {
            if (flag) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }

            this.serverSprintState = flag;
        }

        boolean flag1 = this.isSneaking();

        if (flag1 != this.serverSneakState) {
            if (flag1) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SNEAKING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SNEAKING));
            }

            this.serverSneakState = flag1;
        }

        if (this.isCurrentViewEntity()) {
            double d0 = this.posX - this.lastReportedPosX;
            double d1 = this.getEntityBoundingBox().minY - this.lastReportedPosY;
            double d2 = this.posZ - this.lastReportedPosZ;
            // Use the (possibly module-modified) rotation from the MotionEvent for the
            // packets sent to the server. The rendered view keeps using this.rotationYaw,
            // so modules like KillAura can aim silently without snapping the camera.
            float yaw = preUpdate.getYaw();
            float pitch = preUpdate.getPitch();
            double d3 = (double) (yaw - this.lastReportedYaw);
            double d4 = (double) (pitch - this.lastReportedPitch);
            boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
            boolean flag3 = d3 != 0.0D || d4 != 0.0D;

            if (this.ridingEntity == null) {
                if (flag2 && flag3) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.posX, this.getEntityBoundingBox().minY, this.posZ, yaw, pitch, this.onGround));
                } else if (flag2) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(this.posX, this.getEntityBoundingBox().minY, this.posZ, this.onGround));
                } else if (flag3) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, this.onGround));
                } else {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer(this.onGround));
                }
            } else {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX, -999.0D, this.motionZ, yaw, pitch, this.onGround));
                flag2 = false;
            }

            ++this.positionUpdateTicks;

            if (flag2) {
                this.lastReportedPosX = this.posX;
                this.lastReportedPosY = this.getEntityBoundingBox().minY;
                this.lastReportedPosZ = this.posZ;
                this.positionUpdateTicks = 0;
            }

            if (flag3) {
                this.lastReportedYaw = yaw;
                this.lastReportedPitch = pitch;
            }
        }
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        // Move-fix for KillAura's silent aim. When armed, run the movement physics with
        // the viewpoint yaw we send to the server (RotationUtil.moveFixYaw), but rotate the
        // WASD inputs by (camera - viewpoint) first. The player still travels in the CAMERA
        // direction (where they're looking), while the motion matches the rotation we send,
        // so the server stays synchronized and doesn't set the player back. The camera yaw
        // is restored immediately so the view stays free.
        if (RotationUtil.moveFix) {
            float cameraYaw = this.rotationYaw;
            double theta = Math.toRadians(cameraYaw - RotationUtil.moveFixYaw);
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);
            float fixedStrafe = strafe * cos - forward * sin;
            float fixedForward = strafe * sin + forward * cos;

            this.rotationYaw = RotationUtil.moveFixYaw;
            super.moveEntityWithHeading(fixedStrafe, fixedForward);
            this.rotationYaw = cameraYaw;
        } else {
            super.moveEntityWithHeading(strafe, forward);
        }
    }

    public EntityItem dropOneItem(boolean dropAll) {
        C07PacketPlayerDigging.Action c07packetplayerdigging$action = dropAll ? C07PacketPlayerDigging.Action.DROP_ALL_ITEMS : C07PacketPlayerDigging.Action.DROP_ITEM;
        this.sendQueue.addToSendQueue(new C07PacketPlayerDigging(c07packetplayerdigging$action, BlockPos.ORIGIN, EnumFacing.DOWN));
        return null;
    }

    protected void joinEntityItemWithWorld(EntityItem itemIn) {
    }

    public void sendChatMessage(String message) {
        // command manager dont touch ok?
        if (message.startsWith(".")) {
            Client.INSTANCE.getCommandManager().handleCommand(message);
            return;
        }
        this.sendQueue.addToSendQueue(new C01PacketChatMessage(message));
    }

    public void swingItem() {
        super.swingItem();
        this.sendQueue.addToSendQueue(new C0APacketAnimation());
    }

    public void respawnPlayer() {
        this.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
    }

    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        if (!this.isEntityInvulnerable(damageSrc)) {
            this.setHealth(this.getHealth() - damageAmount);
        }
    }

    public void closeScreen() {
        this.sendQueue.addToSendQueue(new C0DPacketCloseWindow(this.openContainer.windowId));
        this.closeScreenAndDropStack();
    }

    public void closeScreenAndDropStack() {
        this.inventory.setItemStack((ItemStack) null);
        super.closeScreen();
        this.mc.displayGuiScreen((GuiScreen) null);
    }

    public void setPlayerSPHealth(float health) {
        if (this.hasValidHealth) {
            float f = this.getHealth() - health;

            if (f <= 0.0F) {
                this.setHealth(health);

                if (f < 0.0F) {
                    this.hurtResistantTime = this.maxHurtResistantTime / 2;
                }
            } else {
                this.lastDamage = f;
                this.setHealth(this.getHealth());
                this.hurtResistantTime = this.maxHurtResistantTime;
                this.damageEntity(DamageSource.generic, f);
                this.hurtTime = this.maxHurtTime = 10;
            }
        } else {
            this.setHealth(health);
            this.hasValidHealth = true;
        }
    }

    public void addStat(StatBase stat, int amount) {
        if (stat != null) {
            if (stat.isIndependent) {
                super.addStat(stat, amount);
            }
        }
    }

    public void sendPlayerAbilities() {
        this.sendQueue.addToSendQueue(new C13PacketPlayerAbilities(this.capabilities));
    }

    public boolean isUser() {
        return true;
    }

    protected void sendHorseJump() {
        this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.RIDING_JUMP, (int) (this.getHorseJumpPower() * 100.0F)));
    }

    public void sendHorseInventory() {
        this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.OPEN_INVENTORY));
    }

    public void setClientBrand(String brand) {
        this.clientBrand = brand;
    }

    public String getClientBrand() {
        return this.clientBrand;
    }

    public StatFileWriter getStatFileWriter() {
        return this.statWriter;
    }

    public void addChatComponentMessage(IChatComponent chatComponent) {
        this.mc.ingameGUI.getChatGUI().printChatMessage(chatComponent);
    }

    protected boolean pushOutOfBlocks(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            BlockPos blockpos = new BlockPos(x, y, z);
            double d0 = x - (double) blockpos.getX();
            double d1 = z - (double) blockpos.getZ();

            if (!this.isOpenBlockSpace(blockpos)) {
                int i = -1;
                double d2 = 9999.0D;

                if (this.isOpenBlockSpace(blockpos.west()) && d0 < d2) {
                    d2 = d0;
                    i = 0;
                }

                if (this.isOpenBlockSpace(blockpos.east()) && 1.0D - d0 < d2) {
                    d2 = 1.0D - d0;
                    i = 1;
                }

                if (this.isOpenBlockSpace(blockpos.north()) && d1 < d2) {
                    d2 = d1;
                    i = 4;
                }

                if (this.isOpenBlockSpace(blockpos.south()) && 1.0D - d1 < d2) {
                    d2 = 1.0D - d1;
                    i = 5;
                }

                float f = 0.1F;

                if (i == 0) {
                    this.motionX = (double) (-f);
                }

                if (i == 1) {
                    this.motionX = (double) f;
                }

                if (i == 4) {
                    this.motionZ = (double) (-f);
                }

                if (i == 5) {
                    this.motionZ = (double) f;
                }
            }

            return false;
        }
    }

    private boolean isOpenBlockSpace(BlockPos pos) {
        return !this.worldObj.getBlockState(pos).getBlock().isNormalCube() && !this.worldObj.getBlockState(pos.up()).getBlock().isNormalCube();
    }

    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        this.sprintingTicksLeft = sprinting ? 600 : 0;
    }

    public void setXPStats(float currentXP, int maxXP, int level) {
        this.experience = currentXP;
        this.experienceTotal = maxXP;
        this.experienceLevel = level;
    }

    public void addChatMessage(IChatComponent component) {
        this.mc.ingameGUI.getChatGUI().printChatMessage(component);
    }

    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        return permLevel <= 0;
    }

    public BlockPos getPosition() {
        return new BlockPos(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D);
    }

    public void playSound(String name, float volume, float pitch) {
        this.worldObj.playSound(this.posX, this.posY, this.posZ, name, volume, pitch, false);
    }

    public boolean isServerWorld() {
        return true;
    }

    public boolean isRidingHorse() {
        return this.ridingEntity != null && this.ridingEntity instanceof EntityHorse && ((EntityHorse) this.ridingEntity).isHorseSaddled();
    }

    public float getHorseJumpPower() {
        return this.horseJumpPower;
    }

    public void openEditSign(TileEntitySign signTile) {
        this.mc.displayGuiScreen(new GuiEditSign(signTile));
    }

    public void openEditCommandBlock(CommandBlockLogic cmdBlockLogic) {
        this.mc.displayGuiScreen(new GuiCommandBlock(cmdBlockLogic));
    }

    public void displayGUIBook(ItemStack bookStack) {
        Item item = bookStack.getItem();

        if (item == Items.writable_book) {
            this.mc.displayGuiScreen(new GuiScreenBook(this, bookStack, true));
        }
    }

    public void displayGUIChest(IInventory chestInventory) {
        String s = chestInventory instanceof IInteractionObject ? ((IInteractionObject) chestInventory).getGuiID() : "minecraft:container";

        if ("minecraft:chest".equals(s)) {
            this.mc.displayGuiScreen(new GuiChest(this.inventory, chestInventory));
        } else if ("minecraft:hopper".equals(s)) {
            this.mc.displayGuiScreen(new GuiHopper(this.inventory, chestInventory));
        } else if ("minecraft:furnace".equals(s)) {
            this.mc.displayGuiScreen(new GuiFurnace(this.inventory, chestInventory));
        } else if ("minecraft:brewing_stand".equals(s)) {
            this.mc.displayGuiScreen(new GuiBrewingStand(this.inventory, chestInventory));
        } else if ("minecraft:beacon".equals(s)) {
            this.mc.displayGuiScreen(new GuiBeacon(this.inventory, chestInventory));
        } else if (!"minecraft:dispenser".equals(s) && !"minecraft:dropper".equals(s)) {
            this.mc.displayGuiScreen(new GuiChest(this.inventory, chestInventory));
        } else {
            this.mc.displayGuiScreen(new GuiDispenser(this.inventory, chestInventory));
        }
    }

    public void displayGUIHorse(EntityHorse horse, IInventory horseInventory) {
        this.mc.displayGuiScreen(new GuiScreenHorseInventory(this.inventory, horseInventory, horse));
    }

    public void displayGui(IInteractionObject guiOwner) {
        String s = guiOwner.getGuiID();

        if ("minecraft:crafting_table".equals(s)) {
            this.mc.displayGuiScreen(new GuiCrafting(this.inventory, this.worldObj));
        } else if ("minecraft:enchanting_table".equals(s)) {
            this.mc.displayGuiScreen(new GuiEnchantment(this.inventory, this.worldObj, guiOwner));
        } else if ("minecraft:anvil".equals(s)) {
            this.mc.displayGuiScreen(new GuiRepair(this.inventory, this.worldObj));
        }
    }

    public void displayVillagerTradeGui(IMerchant villager) {
        this.mc.displayGuiScreen(new GuiMerchant(this.inventory, villager, this.worldObj));
    }

    public void onCriticalHit(Entity entityHit) {
        this.mc.effectRenderer.emitParticleAtEntity(entityHit, EnumParticleTypes.CRIT);
    }

    public void onEnchantmentCritical(Entity entityHit) {
        this.mc.effectRenderer.emitParticleAtEntity(entityHit, EnumParticleTypes.CRIT_MAGIC);
    }

    public boolean isSneaking() {
        boolean flag = this.movementInput != null ? this.movementInput.sneak : false;
        return flag && !this.sleeping;
    }

    public void updateEntityActionState() {
        super.updateEntityActionState();

        if (this.isCurrentViewEntity()) {
            this.moveStrafing = this.movementInput.moveStrafe;
            this.moveForward = this.movementInput.moveForward;
            this.isJumping = this.movementInput.jump;
            this.prevRenderArmYaw = this.renderArmYaw;
            this.prevRenderArmPitch = this.renderArmPitch;
            this.renderArmPitch = (float) ((double) this.renderArmPitch + (double) (this.rotationPitch - this.renderArmPitch) * 0.5D);
            this.renderArmYaw = (float) ((double) this.renderArmYaw + (double) (this.rotationYaw - this.renderArmYaw) * 0.5D);
        }
    }

    protected boolean isCurrentViewEntity() {
        return this.mc.getRenderViewEntity() == this;
    }

    public void onLivingUpdate() {
        if (this.sprintingTicksLeft > 0) {
            --this.sprintingTicksLeft;

            if (this.sprintingTicksLeft == 0) {
                this.setSprinting(false);
            }
        }

        if (this.sprintToggleTimer > 0) {
            --this.sprintToggleTimer;
        }

        this.prevTimeInPortal = this.timeInPortal;

        if (this.inPortal) {
            if (this.mc.currentScreen != null && !this.mc.currentScreen.doesGuiPauseGame()) {
                this.mc.displayGuiScreen((GuiScreen) null);
            }

            if (this.timeInPortal == 0.0F) {
                this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("portal.trigger"), this.rand.nextFloat() * 0.4F + 0.8F));
            }

            this.timeInPortal += 0.0125F;

            if (this.timeInPortal >= 1.0F) {
                this.timeInPortal = 1.0F;
            }

            this.inPortal = false;
        } else if (this.isPotionActive(Potion.confusion) && this.getActivePotionEffect(Potion.confusion).getDuration() > 60) {
            this.timeInPortal += 0.006666667F;

            if (this.timeInPortal > 1.0F) {
                this.timeInPortal = 1.0F;
            }
        } else {
            if (this.timeInPortal > 0.0F) {
                this.timeInPortal -= 0.05F;
            }

            if (this.timeInPortal < 0.0F) {
                this.timeInPortal = 0.0F;
            }
        }

        if (this.timeUntilPortal > 0) {
            --this.timeUntilPortal;
        }

        boolean flag = this.movementInput.jump;
        boolean flag1 = this.movementInput.sneak;
        float f = 0.8F;
        boolean flag2 = this.movementInput.moveForward >= f;
        this.movementInput.updatePlayerMoveState();

        if (this.isUsingItem() && !this.isRiding()) {
            SlowdownEvent event = new SlowdownEvent(0.2F, 0.2F);
            Client.BUS.post(event);

            this.movementInput.moveStrafe *= event.getStrafe();
            this.movementInput.moveForward *= event.getForward();

            if (!event.isAllowedSprinting()) {
                this.sprintToggleTimer = 0;
            }
        }

        this.pushOutOfBlocks(this.posX - (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ + (double) this.width * 0.35D);
        this.pushOutOfBlocks(this.posX - (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ - (double) this.width * 0.35D);
        this.pushOutOfBlocks(this.posX + (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ - (double) this.width * 0.35D);
        this.pushOutOfBlocks(this.posX + (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ + (double) this.width * 0.35D);
        boolean flag3 = (float) this.getFoodStats().getFoodLevel() > 6.0F || this.capabilities.allowFlying;

        if (this.onGround && !flag1 && !flag2 && this.movementInput.moveForward >= f && !this.isSprinting() && flag3 && !this.isUsingItem() && !this.isPotionActive(Potion.blindness)) {
            if (this.sprintToggleTimer <= 0 && !this.mc.gameSettings.keyBindSprint.isKeyDown()) {
                this.sprintToggleTimer = 7;
            } else {
                this.setSprinting(true);
            }
        }

        if (!this.isSprinting() && this.movementInput.moveForward >= f && flag3 && !this.isUsingItem() && !this.isPotionActive(Potion.blindness) && this.mc.gameSettings.keyBindSprint.isKeyDown()) {
            this.setSprinting(true);
        }

        if (this.isSprinting() && (this.movementInput.moveForward < f || this.isCollidedHorizontally || !flag3)) {
            this.setSprinting(false);
        }

        if (this.capabilities.allowFlying) {
            if (this.mc.playerController.isSpectatorMode()) {
                if (!this.capabilities.isFlying) {
                    this.capabilities.isFlying = true;
                    this.sendPlayerAbilities();
                }
            } else if (!flag && this.movementInput.jump) {
                if (this.flyToggleTimer == 0) {
                    this.flyToggleTimer = 7;
                } else {
                    this.capabilities.isFlying = !this.capabilities.isFlying;
                    this.sendPlayerAbilities();
                    this.flyToggleTimer = 0;
                }
            }
        }

        if (this.capabilities.isFlying && this.isCurrentViewEntity()) {
            if (this.movementInput.sneak) {
                this.motionY -= (double) (this.capabilities.getFlySpeed() * 3.0F);
            }

            if (this.movementInput.jump) {
                this.motionY += (double) (this.capabilities.getFlySpeed() * 3.0F);
            }
        }

        if (this.isRidingHorse()) {
            if (this.horseJumpPowerCounter < 0) {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter == 0) {
                    this.horseJumpPower = 0.0F;
                }
            }

            if (flag && !this.movementInput.jump) {
                this.horseJumpPowerCounter = -10;
                this.sendHorseJump();
            } else if (!flag && this.movementInput.jump) {
                this.horseJumpPowerCounter = 0;
                this.horseJumpPower = 0.0F;
            } else if (flag) {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter < 10) {
                    this.horseJumpPower = (float) this.horseJumpPowerCounter * 0.1F;
                } else {
                    this.horseJumpPower = 0.8F + 2.0F / (float) (this.horseJumpPowerCounter - 9) * 0.1F;
                }
            }
        } else {
            this.horseJumpPower = 0.0F;
        }

        super.onLivingUpdate();

        if (this.onGround && this.capabilities.isFlying && !this.mc.playerController.isSpectatorMode()) {
            this.capabilities.isFlying = false;
            this.sendPlayerAbilities();
        }
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        super.moveEntity(x, y, z);
    }

    @Override
    public void moveFlying(float strafe, float forward, float friction) {
        float yaw = this.rotationYaw;

        float f = strafe * strafe + forward * forward;

        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);

            if (f < 1.0F) {
                f = 1.0F;
            }

            f = friction / f;
            strafe = strafe * f;
            forward = forward * f;
            float f1 = MathHelper.sin(yaw * (float)Math.PI / 180.0F);
            float f2 = MathHelper.cos(yaw * (float)Math.PI / 180.0F);
            this.motionX += (double)(strafe * f2 - forward * f1);
            this.motionZ += (double)(forward * f2 + strafe * f1);
        }
    }

    public boolean isMoving() {
        return this.moveForward != 0 || this.moveStrafing != 0;
    }

    public boolean isRotating() {
        return this.rotationYaw - this.lastReportedYaw != 0 || this.rotationPitch - this.lastReportedPitch != 0;
    }

    public void drop(int slot) {
        this.mc.playerController.windowClick(this.inventoryContainer.windowId, slot, 1, 4, this);
    }

    public void shiftClick(int slot) {
        this.mc.playerController.windowClick(this.inventoryContainer.windowId, slot, 0, 1, this);
    }

    public void swap(int inventorySlot, int hotbarSlot) {
        this.mc.playerController.windowClick(this.inventoryContainer.windowId, inventorySlot, hotbarSlot, 2, this);
    }

    public Slot getSlotFromPlayerContainer(int slot) {
        return this.inventoryContainer.getSlot(slot);
    }

    public void setSpeed(double speed) {
        final double forward = this.moveForward, strafe = this.moveStrafing;
        double yaw = this.rotationYaw;
        final boolean isMovingForward = forward > 0.0f, isMovingBackward = forward < 0.0f, isMovingRight = strafe > 0.0f, isMovingLeft = strafe < 0.0f, isMovingSideways = isMovingLeft || isMovingRight, isMovingStraight = isMovingForward || isMovingBackward;
        if (isMoving()) {
            if (isMovingForward && !isMovingSideways) {
                yaw += 0.0;
            } else if (isMovingBackward && !isMovingSideways) {
                yaw += 180;
            } else if (isMovingForward && isMovingLeft) {
                yaw += 45;
            } else if (isMovingForward) {
                yaw -= 45;
            } else if (!isMovingStraight && isMovingLeft) {
                yaw += 90;
            } else if (!isMovingStraight && isMovingRight) {
                yaw -= 90;
            } else if (isMovingBackward && isMovingLeft) {
                yaw += 135;
            } else if (isMovingBackward) {
                yaw -= 135;
            }

            float yawRadians = (float) Math.toRadians(yaw);
            this.motionX = -MathHelper.sin(yawRadians) * (double) speed;
            this.motionZ = MathHelper.cos(yawRadians) * (double) speed;
        } else {
            this.motionX = 0;
            this.motionZ = 0;
        }
    }
    public boolean isInWeb() {
        return this.isInWeb;
    }

    public double getSpeed() {
        return Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
    }

    public double getBaseMoveSpeed() {
        double baseSpeed = getBySprinting();
        if (this.isPotionActive(moveSpeed)) {
            int amplifier = this.getActivePotionEffect(moveSpeed).getAmplifier() +
                    1 - (this.isPotionActive(moveSlowdown) ? this.getActivePotionEffect(moveSlowdown).getAmplifier() + 1 : 0);
            baseSpeed *= 1.0 + 0.2 * amplifier;
        }
        return baseSpeed;
    }

    public double getBaseMoveSpeed(double baseSpeed, double multiplier) {
        if (this.isPotionActive(moveSpeed)) {
            int amplifier = this.getActivePotionEffect(moveSpeed).getAmplifier() +
                    1 - (this.isPotionActive(moveSlowdown) ? this.getActivePotionEffect(moveSlowdown).getAmplifier() + 1 : 0);
            baseSpeed *= 1.0 + multiplier * amplifier;
        }
        return baseSpeed;
    }

    public double getBaseMoveSpeed(double multiplier) {
        double baseSpeed = getBySprinting();
        if (this.isPotionActive(moveSpeed)) {
            int amplifier = this.getActivePotionEffect(moveSpeed).getAmplifier() +
                    1 - (this.isPotionActive(moveSlowdown) ? this.getActivePotionEffect(moveSlowdown).getAmplifier() + 1 : 0);
            baseSpeed *= 1.0 + multiplier * amplifier;
        }
        return baseSpeed;
    }

    public double getBaseMoveSpeed(double multiplier, int amplifier) {
        double baseSpeed = getBySprinting();
        if (this.isPotionActive(moveSpeed)) {
            baseSpeed *= 1.0 + multiplier * amplifier;
        }
        return baseSpeed;
    }

    public double getBySprinting() {
        return isSprinting() ? 0.28630206268501246 : 0.2202643217126144;
    }

    public double bySneaking() {
        return 0.09158124432567855;
    }

    public double getBySprinting(boolean sprint) {
        return sprint ? 0.28630206268501246 : 0.2202643217126144;
    }

    public double getBaseMotionY() {
        return this.isPotionActive(jump) ? 0.419999986886978 + 0.1 * (this.getActivePotionEffect(jump).getAmplifier() + 1) : 0.419999986886978;
    }

    public double getBaseMotionY(double motionY) {
        return this.isPotionActive(jump) ? motionY + 0.1 * (this.getActivePotionEffect(jump).getAmplifier() + 1) : motionY;
    }

    public boolean isInLiquid() {
        final double y = this.posY + 0.01;
        for (int x = MathHelper.floor_double(this.posX); x < MathHelper.ceiling_double_int(this.posX); ++x) {
            for (int z = MathHelper.floor_double(this.posZ); z < MathHelper.ceiling_double_int(this.posZ); ++z) {
                final BlockPos pos = new BlockPos(x, (int) y, z);
                if (this.mc.theWorld.getBlockState(pos).getBlock() instanceof BlockLiquid) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOnLiquid() {
        final double y = this.posY - 0.1;
        for (int x = MathHelper.floor_double(this.posX); x < MathHelper.ceiling_double_int(this.posX); ++x) {
            for (int z = MathHelper.floor_double(this.posZ); z < MathHelper.ceiling_double_int(this.posZ); ++z) {
                final BlockPos pos = new BlockPos(x, MathHelper.floor_double(y), z);
                if (this.mc.theWorld.getBlockState(pos).getBlock() instanceof BlockLiquid) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOnWater() {
        final double y = this.posY - 0.01;
        for (int x = MathHelper.floor_double(this.posX); x < MathHelper.ceiling_double_int(this.posX); ++x) {
            for (int z = MathHelper.floor_double(this.posZ); z < MathHelper.ceiling_double_int(this.posZ); ++z) {
                final BlockPos pos = new BlockPos(x, MathHelper.floor_double(y), z);
                if (this.mc.theWorld.getBlockState(pos).getBlock() instanceof BlockLiquid && this.mc.theWorld
                        .getBlockState(pos).getBlock().getMaterial() == Material.water) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInsideBlock(Block Block) {
        for (int x = MathHelper.floor_double(this.getEntityBoundingBox().minX); x < MathHelper.floor_double(
                this.getEntityBoundingBox().maxX) + 1; x++) {
            for (int y = MathHelper.floor_double(this.getEntityBoundingBox().minY); y < MathHelper.floor_double(
                    this.getEntityBoundingBox().maxY) + 1; y++) {
                for (int z = MathHelper.floor_double(this.getEntityBoundingBox().minZ); z < MathHelper.floor_double(
                        this.getEntityBoundingBox().maxZ) + 1; z++) {
                    final Block block = this.mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
                    final AxisAlignedBB boundingBox;
                    if (block == Block && block != null && !(block instanceof BlockAir) && (boundingBox = block
                            .getCollisionBoundingBox(this.mc.theWorld, new BlockPos(x, y, z),
                                    this.mc.theWorld.getBlockState(new BlockPos(x, y, z)))) != null) {
                        if (this.getEntityBoundingBox().intersectsWith(boundingBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isInsideBlock() {
        for (int x = MathHelper.floor_double(this.getEntityBoundingBox().minX); x < MathHelper.floor_double(
                this.getEntityBoundingBox().maxX) + 1; x++) {
            for (int y = MathHelper.floor_double(this.getEntityBoundingBox().minY); y < MathHelper.floor_double(
                    this.getEntityBoundingBox().maxY) + 1; y++) {
                for (int z = MathHelper.floor_double(this.getEntityBoundingBox().minZ); z < MathHelper.floor_double(
                        this.getEntityBoundingBox().maxZ) + 1; z++) {
                    final Block block = this.mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
                    final AxisAlignedBB boundingBox;
                    if (block != null && !(block instanceof BlockAir) && (boundingBox = block
                            .getCollisionBoundingBox(this.mc.theWorld, new BlockPos(x, y, z),
                                    this.mc.theWorld.getBlockState(new BlockPos(x, y, z)))) != null) {
                        if (this.getEntityBoundingBox().intersectsWith(boundingBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void setMotion(double speed) {
        this.motionX *= speed;
        this.motionZ *= speed;
    }

    public boolean isOnGround(double height) {
        return !this.mc.theWorld.getCollidingBoundingBoxes(this, this.getEntityBoundingBox().offset(0.0D, -height, 0.0D)).isEmpty();
    }

    public void updateTool(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        float strength = 1.0F;
        int slot = -1;

        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = inventory.getStackInSlot(i);

            if (itemStack != null && itemStack.getStrVsBlock(block) > strength) {
                slot = i;
                strength = itemStack.getStrVsBlock(block);
            }
        }

        if (slot != -1 && mc.thePlayer.inventory.getStackInSlot(inventory.currentItem) != inventory.getStackInSlot(slot)) {
            inventory.currentItem = slot;
        }
    }

    public int getSlotByItem(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);

            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public void movePlayer(double x, double y, double z) {
        double[] dir = moveLooking(0);
        double xDir = dir[0], zDir = dir[1];
        moveEntity(xDir * x, y, zDir * z);
    }

    public double[] moveLooking(float yawOffset) {
        float dir = rotationYaw + yawOffset;

        if (moveForward < 0.0F) {
            dir += 180.0F;
        }

        if (moveStrafing > 0.0F) {
            dir -= 90.0F * (moveForward < 0.0F ? -0.5F : moveForward > 0.0F ? 0.5F : 1.0F);
        }

        if (moveStrafing < 0.0F) {
            dir += 90.0F * (moveForward < 0.0F ? -0.5F : moveForward > 0.0F ? 0.5F : 1.0F);
        }

        float xD = MathHelper.cos((float) ((dir + 90.0F) * Math.PI / 180.0D));
        float zD = MathHelper.sin((float) ((dir + 90.0F) * Math.PI / 180.0D));
        return new double[]{xD, zD};
    }

    public short getInventoryTransaction() {
        return openContainer.getNextTransactionID(inventory);
    }

    public MovementInput movementInput() {
        return movementInput;
    }

    public void setMovementInput(MovementInput movementInput) {
        this.movementInput = movementInput;
    }
}

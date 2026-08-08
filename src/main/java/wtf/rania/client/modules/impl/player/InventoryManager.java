package wtf.rania.client.modules.impl.player;

import wtf.rania.Client;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.event.impl.game.player.TickEvent;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import wtf.rania.utility.math.TimerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "InventoryManager", category = Category.PLAYER)
public class InventoryManager extends Module {

    private final ModeValue mode = new ModeValue("Mode", new String[]{"Open inv", "Silent", "Legit silent"}, "Open inv", this);
    private final BoolValue notWhileMoving = new BoolValue("Not while moving", false, this, () -> !mode.is("Open inv"));
    private final BoolValue spam = new BoolValue("Spam", false, this, () -> !mode.is("Open inv"));
    private final SliderValue reopenDelay = new SliderValue("Re-open delay", 0, 0, 20, this, () -> spam.get());

    private final SliderValue startDelay = new SliderValue("Start delay", 1, 1, 10, this);

    private final BoolValue autoArmor = new BoolValue("Auto armor", true, this);
    private final SliderValue armorDelay = new SliderValue("Armor delay", 1, 0, 10, this, autoArmor::get);

    private final BoolValue sortItems = new BoolValue("Sort items", true, this);
    private final SliderValue sortDelay = new SliderValue("Sort delay", 1, 0, 10, this, sortItems::get);
    private final SliderValue swordSlot = new SliderValue("Sword slot", 1, 0, 9, this, sortItems::get);
    private final SliderValue bowSlot = new SliderValue("Bow slot", 3, 0, 9, this, sortItems::get);
    private final SliderValue gappleSlot = new SliderValue("Gapple slot", 2, 0, 9, this, sortItems::get);
    private final SliderValue pickaxeSlot = new SliderValue("Pickaxe slot", 4, 0, 9, this, sortItems::get);
    private final SliderValue axeSlot = new SliderValue("Axe slot", 5, 0, 9, this, sortItems::get);
    private final SliderValue shovelSlot = new SliderValue("Shovel slot", 6, 0, 9, this, sortItems::get);
    private final SliderValue blockSlot = new SliderValue("Block slot", 9, 0, 9, this, sortItems::get);

    private final BoolValue dropItems = new BoolValue("Drop items", true, this);
    private final SliderValue dropDelay = new SliderValue("Drop delay", 1, 0, 10, this, dropItems::get);
    private final BoolValue ignoreCompass = new BoolValue("Ignore compass", false, this, dropItems::get);

    private final int[] bestArmorPieces = new int[4];
    private final List<Integer> trash = new ArrayList<>();
    private final int[] bestToolSlots = new int[3];
    private final List<Integer> gappleStackSlots = new ArrayList<>();
    private final List<Integer> targetBlockSlot = new ArrayList<>();
    private int bestSwordSlot;
    private int bestBowSlot;
    public int slot;
    private final TimerUtil armorTimer = new TimerUtil();
    private int armorWait;
    private final TimerUtil sortTimer = new TimerUtil();
    private int sortWait;
    private final TimerUtil dropTimer = new TimerUtil();
    private int dropWait;
    private final TimerUtil startDelayTimer = new TimerUtil();
    private final TimerUtil finishTimer = new TimerUtil();
    private boolean inventoryOpen;
    private final TimerUtil reopenTimer = new TimerUtil();

    @Subscribe
    private final Listener<TickEvent> tickListener = new Listener<>(e -> {
        if (e.getType() != TickEvent.EventType.POST) return;

        boolean open = mc.currentScreen instanceof GuiInventory || !mode.is("Open inv");

        if (!open) {
            startDelayTimer.reset();
        } else {
            clear();

            for (int slot = 9; slot < 36; slot++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();
                if (stack != null) {
                    processInventoryItem(slot, stack);
                }
            }

            for (int slot = 36; slot < 45; slot++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();
                if (stack != null) {
                    processInventoryItem(slot, stack);
                }
            }

            boolean armorReady = armorTimer.hasTimeElapsed((long) armorWait * 50);
            boolean sortReady = sortTimer.hasTimeElapsed((long) sortWait * 50);
            boolean dropReady = dropTimer.hasTimeElapsed((long) dropWait * 50);

            if (!mode.is("Open inv") && (mc.currentScreen != null || (notWhileMoving.get() && isMoving()))) {
                closeInventory();
                return;
            }

            if ((equipArmor(true) || dropItem(trash, true) || sortItems(true)) && !mode.is("Open inv")) {
                if (!spam.get() || (armorReady && sortReady && dropReady)) {
                    openInventory();
                }
            }

            if (startDelayTimer.hasTimeElapsed((long) startDelay.get() * 50) && (inventoryOpen || mode.is("Open inv"))) {
                boolean tickWasValid = false;

                if (armorReady && equipArmor(false)) {
                    resetTimings();
                    tickWasValid = true;
                } else if (dropReady && dropItem(trash, false)) {
                    resetTimings();
                    tickWasValid = true;
                } else if (sortReady && sortItems(false)) {
                    resetTimings();
                    tickWasValid = true;
                } else if (armorReady && sortReady && dropReady) {
                    if (!mode.is("Open inv")) {
                        closeInventory();
                    }
                }

                if (spam.get() && !mode.is("Open inv") && !tickWasValid) {
                    closeInventory();
                }
            }
        }
    });

    private void openInventory() {
        if (!inventoryOpen && (reopenTimer.hasTimeElapsed((long) reopenDelay.get() * 50) || !spam.get())) {
            inventoryOpen = true;
            mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            startDelayTimer.reset();
        }
    }

    private void closeInventory() {
        if (inventoryOpen) {
            inventoryOpen = false;
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(0));
            reopenTimer.reset();
        }
    }

    private boolean equipArmor(boolean test) {
        if (autoArmor.get()) {
            for (int i = 0; i < bestArmorPieces.length; i++) {
                int piece = bestArmorPieces[i];

                if (piece != -1) {
                    int armorPieceSlot = i + 5;
                    ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(armorPieceSlot).getStack();
                    if (stack != null)
                        continue;

                    if (!test) {
                        windowClick(piece, 0, 1);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dropItem(List<Integer> listOfSlots, boolean test) {
        if (dropItems.get()) {
            if (!listOfSlots.isEmpty()) {
                if (!test) {
                    int slot = listOfSlots.remove(0);
                    windowClick(slot, 1, 4);
                }
                return true;
            }
        }
        return false;
    }

    private boolean sortItems(boolean test) {
        if (sortItems.get()) {
            if (bestSwordSlot != -1) {
                int target = 36 + (int) swordSlot.get() - 1;
                if (bestSwordSlot != target) {
                    if (!test) {
                        putItemInSlot(target, bestSwordSlot);
                        bestSwordSlot = target;
                    }
                    return true;
                }
            }

            if (bestBowSlot != -1) {
                int target = 36 + (int) bowSlot.get() - 1;
                if (bestBowSlot != target) {
                    if (!test) {
                        putItemInSlot(target, bestBowSlot);
                        bestBowSlot = target;
                    }
                    return true;
                }
            }

            if (!gappleStackSlots.isEmpty()) {
                gappleStackSlots.sort(Comparator.comparingInt(slot -> mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));
                int bestGappleSlot = gappleStackSlots.get(0);

                int target = 36 + (int) gappleSlot.get() - 1;
                if (bestGappleSlot != target) {
                    if (!test) {
                        putItemInSlot(target, bestGappleSlot);
                        gappleStackSlots.set(0, target);
                    }
                    return true;
                }
            }

            if (!targetBlockSlot.isEmpty()) {
                targetBlockSlot.sort(Comparator.comparingInt(slot -> -mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));
                int blockSlotIndex = targetBlockSlot.get(0);

                int target = 36 + (int) blockSlot.get() - 1;
                if (blockSlotIndex != target) {
                    if (!test) {
                        putItemInSlot(target, blockSlotIndex);
                        targetBlockSlot.set(0, target);
                    }
                    return true;
                }
            }

            int[] toolTargets = {
                    36 + (int) pickaxeSlot.get() - 1,
                    36 + (int) axeSlot.get() - 1,
                    36 + (int) shovelSlot.get() - 1
            };

            for (int toolSlot : bestToolSlots) {
                if (toolSlot != -1) {
                    int type = getToolType(mc.thePlayer.inventoryContainer.getSlot(toolSlot).getStack());
                    if (type != -1 && toolSlot != toolTargets[type]) {
                        if (!test) {
                            putToolsInSlot(type, toolTargets);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void processInventoryItem(int slot, ItemStack stack) {
        if (stack == null || (stack.getItem() == Items.compass && ignoreCompass.get())) return;

        if ((!trash.contains(slot) && !isValidStack(stack)) || forceDropStack(stack)) {
            trash.add(slot);
            return;
        }

        if (processCombatItems(slot, stack)) return;
        if (processToolsAndArmor(slot, stack)) return;
        if (processUtilityItems(slot, stack)) return;
    }

    private boolean forceDropStack(ItemStack stack) {
        return stack.getItem() instanceof ItemSword && swordSlot.get() == 0 ||
                stack.getItem() instanceof ItemBow && bowSlot.get() == 0 ||
                stack.getItem() instanceof ItemAppleGold && gappleSlot.get() == 0 ||
                stack.getItem() instanceof ItemPickaxe && pickaxeSlot.get() == 0 ||
                stack.getItem() instanceof ItemAxe && axeSlot.get() == 0 ||
                stack.getItem() instanceof ItemSpade && shovelSlot.get() == 0 ||
                stack.getItem() instanceof ItemBlock && blockSlot.get() == 0;
    }

    private boolean processCombatItems(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemSword && isBestSword(stack)) {
            bestSwordSlot = slot;
            return true;
        }
        if (stack.getItem() instanceof ItemBow && isBestBow(stack)) {
            bestBowSlot = slot;
            return true;
        }
        if (stack.getItem() instanceof ItemAppleGold) {
            gappleStackSlots.add(slot);
            return true;
        }
        return false;
    }

    private boolean processToolsAndArmor(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemTool && isBestTool(stack)) {
            updateBestTool(slot, stack);
            return true;
        }
        if (stack.getItem() instanceof ItemArmor && isBestArmor(stack)) {
            updateBestArmor(slot, (ItemArmor) stack.getItem());
            return true;
        }
        return false;
    }

    private boolean processUtilityItems(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemBlock) {
            targetBlockSlot.add(slot);
            return true;
        }
        return false;
    }

    private void updateBestTool(int slot, ItemStack stack) {
        int toolType = getToolType(stack);
        if (toolType != -1) {
            bestToolSlots[toolType] = slot;
        }
    }

    private void updateBestArmor(int slot, ItemArmor armor) {
        bestArmorPieces[armor.armorType] = slot;
    }

    private void resetTimings() {
        armorTimer.reset();
        dropTimer.reset();
        sortTimer.reset();

        armorWait = (int) armorDelay.get();
        dropWait = (int) dropDelay.get();
        sortWait = (int) sortDelay.get();
        finishTimer.reset();
    }

    public void windowClick(int slotId, int mouseButtonClicked, int mode) {
        slot = slotId;
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    private void putItemInSlot(int slot, int slotIn) {
        windowClick(slotIn, slot - 36, 2);
    }

    private void putToolsInSlot(int tool, int[] toolSlots) {
        int toolSlot = toolSlots[tool];
        windowClick(bestToolSlots[tool], toolSlot - 36, 2);
        bestToolSlots[tool] = toolSlot;
    }

    private int getToolType(ItemStack stack) {
        if (stack.getItem() instanceof ItemPickaxe) return 0;
        if (stack.getItem() instanceof ItemAxe) return 1;
        if (stack.getItem() instanceof ItemSpade) return 2;
        return -1;
    }

    private boolean isValidStack(ItemStack stack) {
        return stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow ||
                stack.getItem() instanceof ItemAppleGold || stack.getItem() instanceof ItemTool ||
                stack.getItem() instanceof ItemArmor || stack.getItem() instanceof ItemBlock;
    }

    private boolean isBestSword(ItemStack stack) {
        return true;
    }

    private boolean isBestBow(ItemStack stack) {
        return true;
    }

    private boolean isBestTool(ItemStack stack) {
        return true;
    }

    private boolean isBestArmor(ItemStack stack) {
        return true;
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    @Override
    public void onDisabled() {
        clear();
    }

    private void clear() {
        trash.clear();
        bestBowSlot = -1;
        bestSwordSlot = -1;
        gappleStackSlots.clear();
        targetBlockSlot.clear();
        Arrays.fill(bestArmorPieces, -1);
        Arrays.fill(bestToolSlots, -1);
    }
}
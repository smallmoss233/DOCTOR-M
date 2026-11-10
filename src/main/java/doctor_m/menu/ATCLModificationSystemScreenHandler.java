package doctor_m.menu;

import doctor_m.module.creativity.creativity_data.SAR.SAR;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public class ATCLModificationSystemScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;

    private static final Identifier CORE_ID = new Identifier("doctor_m", "atcl_power_core");
    private static final Identifier RE_ID = new Identifier("doctor_m", "atcl_power_re");

    public ATCLModificationSystemScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5));
    }

    public ATCLModificationSystemScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModMenus.ATCL_MODIFICATION_SYSTEM_MENU, syncId);
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.player = playerInventory.player;
        inventory.onOpen(this.player);

        ItemStack mainHand = this.player.getMainHandStack();
        if (mainHand.getItem() instanceof SAR sar) {
            NbtCompound nbt = sar.getOrCreateSARNbt(mainHand);
            for (int i = 0; i < 5; i++) {
                String key = "atcl_module_" + i;
                if (nbt.contains(key, NbtElement.COMPOUND_TYPE)) {
                    this.inventory.setStack(i, ItemStack.fromNbt(nbt.getCompound(key)));
                }
            }
        }

        // 槽位 0：核心槽位
        this.addSlot(new Slot(inventory, 0, 105, 40) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return Registries.ITEM.getId(stack.getItem()).equals(CORE_ID);
            }

            @Override
            public int getMaxItemCount() {
                return 10;
            }
        });

        // 槽位 1：二号槽 (能量恢复模块)
        this.addSlot(new Slot(inventory, 1, 125, 40) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return Registries.ITEM.getId(stack.getItem()).equals(RE_ID);
            }

            @Override
            public int getMaxItemCount() {
                return 4;
            }
        });

        // 槽位 2, 3, 4：暂未开放
        for (int i = 2; i < 5; i++) {
            this.addSlot(new Slot(inventory, i, 105 + i * 20, 40) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return false;
                }
            });
        }

        // 玩家背包
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 24 + j * 18, 108 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 24 + i * 18, 166));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex == 0) {
            ItemStack cursor = this.getCursorStack();
            ItemStack slotStack = this.slots.get(0).getStack();

            if (!cursor.isEmpty() && Registries.ITEM.getId(cursor.getItem()).equals(CORE_ID)) {
                if (slotStack.isEmpty() || Registries.ITEM.getId(slotStack.getItem()).equals(CORE_ID)) {
                    int space = 6 - slotStack.getCount();
                    if (space > 0) {
                        int moveCount = Math.min(cursor.getCount(), space);
                        if (slotStack.isEmpty()) {
                            ItemStack newStack = cursor.copy();
                            newStack.setCount(moveCount);
                            newStack.setNbt(null);
                            this.slots.get(0).setStack(newStack);
                        } else {
                            slotStack.setCount(slotStack.getCount() + moveCount);
                            this.slots.get(0).markDirty();
                        }
                        cursor.decrement(moveCount);
                        if (cursor.isEmpty()) this.setCursorStack(ItemStack.EMPTY);
                        updateWeaponStats();
                        this.syncState();
                        return;
                    }
                }
            }
        }

        super.onSlotClick(slotIndex, button, actionType, player);
        updateWeaponStats();
    }

    private void updateWeaponStats() {
        ItemStack mainHand = this.player.getMainHandStack();
        if (mainHand.getItem() instanceof SAR sar) {
            NbtCompound nbt = sar.getOrCreateSARNbt(mainHand);

            for (int i = 0; i < 5; i++) {
                String key = "atcl_module_" + i;
                ItemStack stack = this.inventory.getStack(i);
                if (!stack.isEmpty()) {
                    NbtCompound cNbt = new NbtCompound();
                    stack.writeNbt(cNbt);
                    nbt.put(key, cNbt);
                } else {
                    nbt.remove(key);
                }
            }

            int coreCount = this.inventory.getStack(0).getCount();
            nbt.putInt(SAR.MAX_ENERGY_BASE_KEY, 10000 + (coreCount * 5000));

            int reCount = this.inventory.getStack(1).getCount();
            nbt.putInt("atcl_extra_regen", reCount * 10);

            if (sar.getEnergy(mainHand) > sar.getMaxEnergy(mainHand)) {
                sar.setEnergy(mainHand, sar.getMaxEnergy(mainHand));
            }
            this.player.getInventory().markDirty();
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player.getMainHandStack().getItem() instanceof SAR) {
            updateWeaponStats();
        } else {
            for (int i = 0; i < 5; i++) {
                ItemStack slotStack = inventory.getStack(i);
                if (!slotStack.isEmpty()) {
                    player.getInventory().offerOrDrop(slotStack);
                }
            }
        }
        inventory.clear();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof SAR;
    }
}
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
    private final Identifier CORE_ID = new Identifier("doctor_m", "atcl_power_core");

    public ATCLModificationSystemScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5));
    }

    public ATCLModificationSystemScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModMenus.ATCL_MODIFICATION_SYSTEM_MENU, syncId);
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.player = playerInventory.player;
        inventory.onOpen(this.player);

        // 从武器 NBT 中读取已存储的核心，并放入槽位 0
        ItemStack mainHand = this.player.getMainHandStack();
        if (mainHand.getItem() instanceof SAR sar) {
            NbtCompound nbt = sar.getOrCreateSARNbt(mainHand);
            if (nbt.contains("atcl_core_0", NbtElement.COMPOUND_TYPE)) {
                this.inventory.setStack(0, ItemStack.fromNbt(nbt.getCompound("atcl_core_0")));
            }
        }

        // 0号主槽位 (强制合并，限制 6 个)
        this.addSlot(new Slot(inventory, 0, 137, 24) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return Registries.ITEM.getId(stack.getItem()).equals(CORE_ID);
            }

            @Override
            public int getMaxItemCount() {
                return 6;
            }
        });

        // 1-4 号副槽位 (完全禁止放入任何物品)
        for (int i = 1; i < 5; i++) {
            int sx = 122 + ((i - 1) % 2) * 36;
            int sy = 58 + ((i - 1) / 2) * 22;
            this.addSlot(new Slot(inventory, i, sx, sy) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return false;
                }
            });
        }

        // 玩家背包
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 108 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 166));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // 拦截 0 号槽位，无视 NBT 强制合并
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
                            newStack.setNbt(null); // 清除 NBT 确保完美堆叠
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
            ItemStack coreStack = this.inventory.getStack(0);

            // 实时将物品存入武器 NBT，实现持久化
            if (!coreStack.isEmpty()) {
                NbtCompound cNbt = new NbtCompound();
                coreStack.writeNbt(cNbt);
                nbt.put("atcl_core_0", cNbt);
            } else {
                nbt.remove("atcl_core_0");
            }

            // 计算能量上限
            int count = coreStack.getCount();
            nbt.putInt(SAR.MAX_ENERGY_BASE_KEY, 10000 + (count * 5000));

            if (sar.getEnergy(mainHand) > sar.getMaxEnergy(mainHand)) {
                sar.setEnergy(mainHand, sar.getMaxEnergy(mainHand));
            }
            this.player.getInventory().markDirty();
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // 检查主手是否还是武器。如果是，数据已在 updateWeaponStats 存好；如果不是，退还物品。
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
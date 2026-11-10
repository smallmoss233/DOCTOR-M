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
    private static final Identifier DAMAGE_ID = new Identifier("doctor_m", "atcl_damage_up");
    private static final Identifier SPEED_ID = new Identifier("doctor_m", "atclc_onstruction_modified_model");

    // 第五槽的两个专属模块
    private static final Identifier COOLING_ID = new Identifier("doctor_m", "atcl_cooling_modified_model");
    private static final Identifier HIGHLAND_ID = new Identifier("doctor_m", "atcl_high_land_model");

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

        // 槽位 0：核心
        this.addSlot(new Slot(inventory, 0, 105, 40) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return Registries.ITEM.getId(stack.getItem()).equals(CORE_ID);
            }
            @Override
            public int getMaxItemCount() {
                return 6;
            }
        });

        // 槽位 1：恢复
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

        // 槽位 2：伤害
        this.addSlot(new Slot(inventory, 2, 145, 40) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return Registries.ITEM.getId(stack.getItem()).equals(DAMAGE_ID);
            }
            @Override
            public int getMaxItemCount() {
                return 2;
            }
        });

        // 槽位 3：攻速
        this.addSlot(new Slot(inventory, 3, 165, 40) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return Registries.ITEM.getId(stack.getItem()).equals(SPEED_ID);
            }
            @Override
            public int getMaxItemCount() {
                return 4;
            }
        });

        // 槽位 4：五号槽 (冷却/高地 二选一，不可堆叠)
        this.addSlot(new Slot(inventory, 4, 185, 40) {
            @Override
            public boolean canInsert(ItemStack stack) {
                Identifier id = Registries.ITEM.getId(stack.getItem());
                return id.equals(COOLING_ID) || id.equals(HIGHLAND_ID);
            }
            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

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

            // 槽位 0 核心加成
            int coreCount = this.inventory.getStack(0).getCount();
            nbt.putInt(SAR.MAX_ENERGY_BASE_KEY, 10000 + (coreCount * 5000));

            // 槽位 1 回能加成
            int reCount = this.inventory.getStack(1).getCount();
            nbt.putInt("atcl_extra_regen", reCount * 40);

            // 槽位 2 伤害加成
            int damageCount = this.inventory.getStack(2).getCount();
            nbt.putInt("atcl_extra_damage", damageCount * 4);

            // 槽位 3 攻速加成
            int speedCount = this.inventory.getStack(3).getCount();
            nbt.putDouble("atcl_extra_speed", speedCount * 0.6);

            // 槽位 4 特殊模块标记 (记录放入的是冷却还是高地，为后续应用效果做准备)
            ItemStack specialStack = this.inventory.getStack(4);
            if (specialStack.isEmpty()) {
                nbt.remove("atcl_special_module");
            } else {
                nbt.putString("atcl_special_module", Registries.ITEM.getId(specialStack.getItem()).toString());
            }

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
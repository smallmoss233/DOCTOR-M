package doctor_m.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class SARGUI extends ScreenHandler {
    private final Inventory inventory;

    // 客户端构造函数
    public SARGUI(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(10));
    }

    // 服务端构造函数
    public SARGUI(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(doctor_m.menu.ModMenus.SAR_WORKBENCH_MENU, syncId);
        checkSize(inventory, 10);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // === 工作台槽位 ===
        // 3x3 输入
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(inventory, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        // 1个输出槽
        this.addSlot(new Slot(inventory, 9, 124, 35));

        // === 玩家背包 ===
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // === 玩家快捷栏 ===
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return ItemStack.EMPTY;
    }
}
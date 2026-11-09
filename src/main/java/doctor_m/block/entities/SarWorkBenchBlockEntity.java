package doctor_m.block.entities;

import doctor_m.block.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

// 注意类名和文件名保持一致
public class SarWorkBenchBlockEntity extends BlockEntity implements Inventory {
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(10, ItemStack.EMPTY);

    public SarWorkBenchBlockEntity(BlockPos pos, BlockState state) {
        // 这里必须用你在 ModBlockEntities 里注册的名字 SAR_WORKBENCH
        super(ModBlockEntities.SAR_WORKBENCH_BLOCK_ENTITY, pos, state);
    }

    // ======= 下面是原版 Inventory 接口要求的 9 个方法 =======

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(items, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world == null || this.world.getBlockEntity(this.pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clear() {
        items.clear();
    }

    // ======= 下面是 NBT 的读写（原版接口不需要，但 BlockEntity 需要重写） =======
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
    }
}
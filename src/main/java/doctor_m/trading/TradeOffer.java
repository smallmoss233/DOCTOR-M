package doctor_m.trading;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class TradeOffer {
    private final Item inputItem;
    private final int inputCount;
    private final Item outputItem;
    private final int outputCount;
    private final int maxUses;
    private int uses;

    // 新增：输出物品附带的 NBT（可为 null）
    @Nullable
    private final NbtCompound outputNbt;

    public TradeOffer(Item inputItem, int inputCount, Item outputItem, int outputCount, int maxUses, @Nullable NbtCompound outputNbt) {
        this.inputItem = inputItem;
        this.inputCount = inputCount;
        this.outputItem = outputItem;
        this.outputCount = outputCount;
        this.maxUses = maxUses;
        this.outputNbt = outputNbt;
        this.uses = 0;
    }

    public Item getInputItem() { return inputItem; }
    public int getInputCount() { return inputCount; }
    public Item getOutputItem() { return outputItem; }
    public int getOutputCount() { return outputCount; }
    public int getMaxUses() { return maxUses; }
    public int getUses() { return uses; }
    public boolean isAvailable() { return uses < maxUses; }

    public boolean matches(ItemStack stack) {
        return stack.isOf(inputItem) && stack.getCount() >= inputCount;
    }

    public void execute(ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        if (!matches(held)) return;

        held.decrement(inputCount);
        if (held.isEmpty()) player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);

        ItemStack output = new ItemStack(outputItem, outputCount);

        // 关键：如果配置了 NBT，就附加到输出物品上
        if (outputNbt != null) {
            output.setNbt(outputNbt.copy());
        }

        if (!player.getInventory().insertStack(output)) {
            player.dropItem(output, false);
        }
        uses++;
    }

    public String getDisplayText() {
        String inName = inputItem.getName().getString();
        String outName = outputItem.getName().getString();
        String nbtHint = (outputNbt != null) ? " §d✦" : "";
        return String.format("%s §7x%d §r→ §b%s%s §7x%d", inName, inputCount, outName, nbtHint, outputCount);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Input", Registries.ITEM.getId(inputItem).toString());
        nbt.putInt("InputCount", inputCount);
        nbt.putString("Output", Registries.ITEM.getId(outputItem).toString());
        nbt.putInt("OutputCount", outputCount);
        nbt.putInt("Uses", uses);
        nbt.putInt("MaxUses", maxUses);
        if (outputNbt != null) {
            nbt.put("OutputNbt", outputNbt.copy());
        }
        return nbt;
    }

    public static TradeOffer fromNbt(NbtCompound nbt) {
        Item input = Registries.ITEM.get(new Identifier(nbt.getString("Input")));
        Item output = Registries.ITEM.get(new Identifier(nbt.getString("Output")));
        int inCount = nbt.getInt("InputCount");
        int outCount = nbt.getInt("OutputCount");
        int maxUses = nbt.getInt("MaxUses");

        NbtCompound outNbt = nbt.contains("OutputNbt", 10) ? nbt.getCompound("OutputNbt") : null;

        TradeOffer offer = new TradeOffer(input, inCount, output, outCount, maxUses, outNbt);
        offer.uses = nbt.getInt("Uses");
        return offer;
    }
}
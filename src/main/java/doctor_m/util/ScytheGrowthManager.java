package doctor_m.util;

import doctor_m.module.creativity.creativity_data.tlipoca_scythe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 镰刀成长系统 - 完全独立，避免NBT重写导致的同步问题
 */
public class ScytheGrowthManager {

    // 缓存每个玩家主手武器的伤害值，避免每tick读取NBT
    private static final Map<UUID, Float> cachedDamage = new HashMap<>();

    /**
     * 处理击杀事件
     * @param weapon 造成伤害的武器（必须已通过ALLOW_DAMAGE验证是镰刀）
     * @param player 玩家
     */
    public static void onKill(ItemStack weapon, ServerPlayerEntity player) {
        if (!(weapon.getItem() instanceof tlipoca_scythe)) return;

        NbtCompound nbt = weapon.getOrCreateNbt();
        int killCount = nbt.getInt(tlipoca_scythe.KILL_COUNT_KEY) + 1;
        nbt.putInt(tlipoca_scythe.KILL_COUNT_KEY, killCount);

        System.out.println("[ScytheGrowth] Kill recorded! Count=" + killCount + " Player=" + player.getName().getString());

        if (killCount % 10 == 0) {
            int growth = nbt.getInt(tlipoca_scythe.GROWTH_KEY) + 5;
            nbt.putInt(tlipoca_scythe.GROWTH_KEY, growth);
            float newDamage = 20.0f + growth;

            // 更新缓存
            cachedDamage.put(player.getUuid(), newDamage);

            // 更新物品名称显示成长值（可选）
            // weapon.setCustomName(Text.literal("§c特斯卡特利波卡之镰 §7[+" + growth + "]"));

            player.sendMessage(
                    Text.literal("§6☠ 特斯卡特利波卡之镰已吸收灵魂，攻击力提升至 " + newDamage + "！"),
                    false
            );

            System.out.println("[ScytheGrowth] LEVEL UP! New damage=" + newDamage);
        }

        // 标记背包需要同步（只同步一次）
        player.getInventory().markDirty();
    }

    /**
     * 获取物品当前总伤害（用于Tooltip等）
     */
    public static float getTotalDamage(ItemStack stack) {
        if (!(stack.getItem() instanceof tlipoca_scythe)) return 0;
        int growth = stack.getOrCreateNbt().getInt(tlipoca_scythe.GROWTH_KEY);
        return 20.0f + growth;
    }

    /**
     * 获取击杀数
     */
    public static int getKillCount(ItemStack stack) {
        if (!(stack.getItem() instanceof tlipoca_scythe)) return 0;
        return stack.getOrCreateNbt().getInt(tlipoca_scythe.KILL_COUNT_KEY);
    }

    /**
     * 获取距离下次升级还需多少击杀
     */
    public static int getKillsToNext(ItemStack stack) {
        int kills = getKillCount(stack);
        if (kills == 0) return 10;
        int remainder = kills % 10;
        return remainder == 0 ? 0 : (10 - remainder);
    }
}
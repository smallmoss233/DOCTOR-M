package doctor_m.client.util.id;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class GlowConditionChecker {

    private static final Identifier GLOW_ITEM_ID = new Identifier("doctor_m", "time_key");
    private static final Identifier SCYTHE_ITEM_ID = new Identifier("doctor_m", "tlipoca_scythe");
    private static Item glowItemCache = null;
    private static Item scytheCache = null;

    public static final int COLOR_GREEN = 0x55FF55;
    public static final int COLOR_RED   = 0xFF5555;

    private GlowConditionChecker() {}

    /* ============================================================
       公开 API（原有）
       ============================================================ */

    public static boolean shouldGlow(PlayerEntity player) {
        return getGlowColor(player) != 0;
    }

    public static String getPlayerTitleDirect(PlayerEntity player) {
        if (player == null) return null;
        return PlayerTitleCache.getTitle(player.getUuid());
    }

    public static boolean shouldGlowByName(String name, MinecraftClient client) {
        return getGlowColorByName(name, client) != 0;
    }

    public static String getTitleByName(String name, MinecraftClient client) {
        PlayerEntity player = findPlayerByName(name, client);
        return player == null ? null : getGlowTitle(player);
    }

    /* ============================================================
       新增：获取颜色和称号（按玩家）
       ============================================================ */

    public static int getGlowColor(PlayerEntity player) {
        if (player == null) return 0;
        if (hasScythe(player)) return COLOR_RED;
        if (hasGlowItem(player)) return COLOR_GREEN;
        return 0;
    }

    public static String getGlowTitle(PlayerEntity player) {
        if (player == null) return null;
        if (hasScythe(player)) {
            return Text.translatable("title.doctor_m.apprentice_reaper").getString();
        }
        if (hasGlowItem(player)) return PlayerTitleCache.getTitle(player.getUuid());
        return null;
    }

    /* ============================================================
       新增：根据名字获取
       ============================================================ */

    public static int getGlowColorByName(String name, MinecraftClient client) {
        PlayerEntity player = findPlayerByName(name, client);
        return player == null ? 0 : getGlowColor(player);
    }

    public static String getGlowTitleByName(String name, MinecraftClient client) {
        PlayerEntity player = findPlayerByName(name, client);
        return player == null ? null : getGlowTitle(player);
    }

    private static PlayerEntity findPlayerByName(String name, MinecraftClient client) {
        if (client.world == null || name == null || name.isBlank()) return null;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p.getName().getString().equals(name)) return p;
        }
        return null;
    }

    /* ============================================================
       内部检测（原有，稍作调整）
       ============================================================ */

    private static boolean hasGlowItem(PlayerEntity player) {
        return hasGlowItemInHand(player)
                || hasGlowItemInArmor(player)
                || hasGlowItemInInventory(player)
                || hasGlowItemInTrinkets(player);
    }

    private static boolean hasGlowItemInHand(PlayerEntity player) {
        return isGlowItem(player.getMainHandStack())
                || isGlowItem(player.getOffHandStack());
    }

    private static boolean hasGlowItemInArmor(PlayerEntity player) {
        for (ItemStack stack : player.getArmorItems()) {
            if (isGlowItem(stack)) return true;
        }
        return false;
    }

    private static boolean hasGlowItemInInventory(PlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (isGlowItem(stack)) return true;
        }
        return false;
    }

    private static boolean hasGlowItemInTrinkets(PlayerEntity player) {
        Optional<dev.emi.trinkets.api.TrinketComponent> comp = TrinketsApi.getTrinketComponent(player);
        return comp.map(c -> !c.getEquipped(GlowConditionChecker::isGlowItem).isEmpty()).orElse(false);
    }

    private static boolean isGlowItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (glowItemCache == null) {
            glowItemCache = Registries.ITEM.get(GLOW_ITEM_ID);
        }
        return stack.isOf(glowItemCache);
    }

    /* ============================================================
       新增：镰刀检测
       ============================================================ */

    private static boolean hasScythe(PlayerEntity player) {
        if (scytheCache == null) {
            scytheCache = Registries.ITEM.get(SCYTHE_ITEM_ID);
        }
        if (scytheCache == null) return false;
        return player.getMainHandStack().isOf(scytheCache)
                || player.getOffHandStack().isOf(scytheCache);
    }
}
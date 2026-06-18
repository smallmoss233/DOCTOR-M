package doctor_m.world_data;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import doctor_m.Item.data_itme.fragment.pocket_watch;

import java.util.Optional;

public class PocketWatchFunction {

    public static final String COOLDOWN_KEY = "table_revival_cooldown_end_ms";
    private static final long COOLDOWN_MILLIS = 24000 * 50L; // 1 游戏日 = 1,200,000 毫秒

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                Optional<Pair<SlotReference, ItemStack>> tableOpt = TrinketsApi.getTrinketComponent(player)
                        .flatMap(comp -> comp.getEquipped(stack -> stack.getItem() instanceof pocket_watch).stream().findFirst());
                if (!tableOpt.isPresent()) return true;

                ItemStack tableStack = tableOpt.get().getRight();
                NbtCompound nbt = tableStack.getOrCreateNbt();
                long currentTime = System.currentTimeMillis();
                long cooldownEnd = nbt.getLong(COOLDOWN_KEY);

                // 冷却中 → 无法复活，不发送消息（已在物品 tooltip 显示）
                if (currentTime < cooldownEnd) {
                    return true;
                }

                float newHealth = player.getHealth() - amount;
                if (newHealth <= 0) {
                    revivePlayer(player);
                    nbt.putLong(COOLDOWN_KEY, System.currentTimeMillis() + COOLDOWN_MILLIS);
                    return false;
                }
            }
            return true;
        });
    }

    private static void revivePlayer(ServerPlayerEntity player) {
        float halfHealth = player.getMaxHealth() / 2;
        player.setHealth(halfHealth);
        player.clearStatusEffects();
        double radius = 10.0;
        player.getServerWorld().getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                player.getBoundingBox().expand(radius),
                entity -> entity != player && entity.isAlive()
        ).forEach(entity -> entity.damage(player.getDamageSources().magic(), 25.0f));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 3, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 300, 1, false, false));
        for (int i = 0; i < 40; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5;
            double y = player.getY() + player.getRandom().nextDouble() * 2.0;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5;
            player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0, 0, 0, 0.1);
        }
        player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("message.doctor_m.pocket_watch.revived"), true);
    }

    // 公开工具方法，供物品 tooltip 使用
    public static String formatRemainingTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "秒";
        long minutes = seconds / 60;
        seconds %= 60;
        if (minutes < 60) return minutes + "分" + seconds + "秒";
        long hours = minutes / 60;
        minutes %= 60;
        return hours + "小时" + minutes + "分";
    }
}
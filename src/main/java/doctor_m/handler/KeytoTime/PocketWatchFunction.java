package doctor_m.handler.KeytoTime;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_item.KeytoTimeFragment.PocketWatchItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.Optional;

public class PocketWatchFunction {

    public static final String COOLDOWN_KEY = "table_revival_cooldown_end_ms";
    private static final long COOLDOWN_MILLIS = 24000L * 50L; // 1 game day

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }

            ItemStack watchStack = findPocketWatch(player);
            if (watchStack == null) {
                return true;
            }

            var nbt = watchStack.getOrCreateNbt();
            long currentTime = System.currentTimeMillis();
            long cooldownEnd = nbt.getLong(COOLDOWN_KEY);

            if (currentTime < cooldownEnd) {
                return true;
            }

            float newHealth = player.getHealth() - amount;
            if (newHealth <= 0) {
                revivePlayer(player);
                PocketWatchItem.startCooldown(watchStack, COOLDOWN_MILLIS);
                return false;
            }
            return true;
        });
    }

    public static Pair<Integer, Integer> getRemainingTimeParts(long millis) {
        long totalSeconds = millis / 1000;
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        return new Pair<>(minutes, seconds);
    }

    private static ItemStack findPocketWatch(ServerPlayerEntity player) {
        // 检查主物品栏
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof PocketWatchItem) {
                return stack;
            }
        }

        // 检查副手
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.getItem() instanceof PocketWatchItem) {
                return stack;
            }
        }

        // 检查饰品槽
        Optional<dev.emi.trinkets.api.TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isPresent()) {
            List<Pair<dev.emi.trinkets.api.SlotReference, ItemStack>> equipped =
                    componentOpt.get().getEquipped(stack -> stack.getItem() instanceof PocketWatchItem);
            if (!equipped.isEmpty()) {
                return equipped.get(0).getRight();
            }
        }

        return null;
    }

    private static void revivePlayer(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth() / 2f);
        player.clearStatusEffects();

        double radius = 10.0;
        player.getServerWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(radius),
                e -> e != player && e.isAlive()
        ).forEach(target -> target.damage(player.getDamageSources().magic(), 25.0f));

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 3, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 300, 1, false, false));

        for (int i = 0; i < 40; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5;
            double y = player.getY() + player.getRandom().nextDouble() * 2.0;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5;
            player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.0, 0.0, 0.0, 0.1);
        }

        player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0f, 1.0f);
        player.sendMessage(Text.translatable("message.doctor_m.pocket_watch.revived"), true);
    }
}
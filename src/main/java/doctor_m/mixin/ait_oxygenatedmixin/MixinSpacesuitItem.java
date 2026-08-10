package doctor_m.mixin.ait_oxygenatedmixin;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.config.ConfigManager;
import doctor_m.module.space_plus.OxygenSystem;
import doctor_m.util.SpaceEnvironmentUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(SpacesuitItem.class)
public abstract class MixinSpacesuitItem {

    // ====== 氧气警告系统 ======
    // 级别：1=<50%, 2=<20%, 3=<5%, 4=0%
    private static final Map<UUID, Long> LAST_WARN_TIME = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_WARN_LEVEL = new HashMap<>();
    private static final Map<UUID, Boolean> EMPTY_WARNED = new HashMap<>();

    private static final long INTERVAL_HALF = 30000L;      // <50%: 每30秒
    private static final long INTERVAL_LOW = 5000L;        // <20%: 每5秒
    private static final long INTERVAL_CRITICAL = 1000L;   // <5%: 每秒

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void onInventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        SpacesuitItem self = (SpacesuitItem)(Object)this;

        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;
        if (world.isClient()) return;
        if (!(entity instanceof LivingEntity living)) return;

        if (living.getEquippedStack(EquipmentSlot.CHEST) != stack) {
            ci.cancel();
            return;
        }

        if (living instanceof PlayerEntity player && player.isCreative()) {
            ci.cancel();
            return;
        }

        var config = ConfigManager.getConfig();
        double consumeUnderwater = config.spacesuitOxygenConsumeUnderwater;
        double consumeSpace = config.spacesuitOxygenConsumeSpace;
        boolean envHasOxygen = SpaceEnvironmentUtil.hasEnvironmentalOxygen(living);
        boolean isSubmerged = living.isSubmergedInWater();

        // ====== 水下分支 ======
        if (isSubmerged) {
            double oxygen = OxygenSystem.getOxygen(stack);
            if (oxygen > 0) {
                living.setAir(300);
                living.removeStatusEffect(StatusEffects.WITHER);
                if (world.getTime() % 40 == 0) {
                    OxygenSystem.consumeOxygen(stack, consumeUnderwater);
                }
                ci.cancel();
                return;
            }

            if (envHasOxygen) {
                return;
            } else {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
                ci.cancel();
                return;
            }
        }

        // ====== 非水下环境 ======
        if (envHasOxygen) {
            living.removeStatusEffect(StatusEffects.WITHER);
            // 有氧环境：彻底重置所有警告状态
            resetWarnings(living);
            ci.cancel();
            return;
        }

        // ====== 无氧环境（太空等）======
        // 每秒检查一次氧气警告（20 ticks = 1s）
        if (world.getTime() % 20 == 0) {
            checkOxygenWarnings(living, stack);
        }

        if (world.getTime() % 60 == 0) {
            double current = OxygenSystem.getOxygen(stack);
            if (current > 0) {
                OxygenSystem.consumeOxygen(stack, consumeSpace);
            }
        }

        if (world.getTime() % 40 == 0) {
            double oxygen = OxygenSystem.getOxygen(stack);
            if (oxygen > 0) {
                living.removeStatusEffect(StatusEffects.WITHER);
            } else {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
            }
        }

        ci.cancel();
    }

    private static void checkOxygenWarnings(LivingEntity living, ItemStack stack) {
        if (!(living instanceof ServerPlayerEntity player)) return;

        double oxygen = OxygenSystem.getOxygen(stack);
        double maxOxygen = OxygenSystem.getMaxOxygen();
        double ratio = oxygen / maxOxygen;
        UUID uuid = player.getUuid();

        // 有氧环境或氧气充足(>=50%)：彻底安静
        if (SpaceEnvironmentUtil.hasEnvironmentalOxygen(player) || ratio >= 0.5) {
            resetWarnings(player);
            return;
        }

        // 确定当前警告级别
        int currentLevel;
        if (oxygen <= 0) {
            currentLevel = 4;
        } else if (ratio < 0.05) {
            currentLevel = 3;
        } else if (ratio < 0.20) {
            currentLevel = 2;
        } else {
            currentLevel = 1;
        }

        int lastLevel = ACTIVE_WARN_LEVEL.getOrDefault(uuid, 0);

        // 只要氧气不是彻底耗尽，就允许下次耗尽时再次触发最终警告
        if (currentLevel != 4) {
            EMPTY_WARNED.remove(uuid);
        }

        // 级别上升 = 氧气在下降（更危险了）：立即发消息
        if (currentLevel > lastLevel) {
            sendWarning(player, currentLevel);
            ACTIVE_WARN_LEVEL.put(uuid, currentLevel);
            LAST_WARN_TIME.put(uuid, System.currentTimeMillis());
            return;
        }

        // 级别下降 = 氧气在回升（好消息）：静默更新，不打扰玩家
        if (currentLevel < lastLevel) {
            ACTIVE_WARN_LEVEL.put(uuid, currentLevel);
            return;
        }

        // 级别相同：按各自频率轮询
        long now = System.currentTimeMillis();
        long last = LAST_WARN_TIME.getOrDefault(uuid, 0L);
        long interval = switch (currentLevel) {
            case 1 -> INTERVAL_HALF;
            case 2 -> INTERVAL_LOW;
            case 3 -> INTERVAL_CRITICAL;
            default -> Long.MAX_VALUE; // level 4 只发一次，不走这里
        };

        if (now - last >= interval) {
            sendWarning(player, currentLevel);
            LAST_WARN_TIME.put(uuid, now);
        }
    }

    private static void sendWarning(ServerPlayerEntity player, int level) {
        MutableText msg;
        Formatting color;
        switch (level) {
            case 4 -> {
                UUID uuid = player.getUuid();
                if (EMPTY_WARNED.getOrDefault(uuid, false)) return;
                EMPTY_WARNED.put(uuid, true);
                msg = Text.translatable("tooltip.doctor_m.oxygen_empty");
                color = Formatting.DARK_RED;
            }
            case 3 -> {
                msg = Text.translatable("tooltip.doctor_m.oxygen_critical");
                color = Formatting.RED;
            }
            case 2 -> {
                msg = Text.translatable("tooltip.doctor_m.oxygen_low");
                color = Formatting.GOLD;
            }
            case 1 -> {
                msg = Text.translatable("tooltip.doctor_m.oxygen_half");
                color = Formatting.YELLOW;
            }
            default -> {
                return;
            }
        }
        player.sendMessage(msg.formatted(color), true);
    }

    private static void resetWarnings(LivingEntity living) {
        if (!(living instanceof ServerPlayerEntity player)) return;
        UUID uuid = player.getUuid();
        LAST_WARN_TIME.remove(uuid);
        ACTIVE_WARN_LEVEL.remove(uuid);
        EMPTY_WARNED.remove(uuid);
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        SpacesuitItem self = (SpacesuitItem)(Object)this;
        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;

        double oxygen = OxygenSystem.getOxygen(stack);
        double maxOxygen = OxygenSystem.getMaxOxygen();
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, maxOxygen));

        ci.cancel();
    }
}
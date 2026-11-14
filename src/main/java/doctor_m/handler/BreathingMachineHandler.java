package doctor_m.handler;

import doctor_m.Item.data_item.BreathingMachineItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.UUID;

public class BreathingMachineHandler {
    private static final UUID KNOCKBACK_UUID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            ItemStack machine = getMachine(player);
            if (machine.isEmpty()) return true;

            NbtCompound nbt = machine.getOrCreateNbt();

            if (player.getHealth() - amount <= 0 && !nbt.getBoolean(BreathingMachineItem.ACTIVE_KEY)) {
                if (nbt.getInt(BreathingMachineItem.ENERGY_KEY) > 0) {
                    nbt.putBoolean(BreathingMachineItem.ACTIVE_KEY, true);
                    nbt.putInt(BreathingMachineItem.TIME_LEFT_KEY, BreathingMachineItem.MAX_TIME);
                    player.setHealth(10.0f);

                    player.setInvulnerable(true);
                    EntityAttributeInstance kbAttr = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
                    if (kbAttr != null && kbAttr.getModifier(KNOCKBACK_UUID) == null) {
                        kbAttr.addPersistentModifier(new EntityAttributeModifier(KNOCKBACK_UUID, "breathing_machine_kb", 1.0, EntityAttributeModifier.Operation.ADDITION));
                    }

                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 0.5f);
                    return false;
                }
            }
            return true;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                ItemStack machine = getMachine(player);
                if (!machine.isEmpty() && machine.getOrCreateNbt().getBoolean(BreathingMachineItem.ACTIVE_KEY)) {
                    return false;
                }
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack machine = getMachine(player);
                if (machine.isEmpty()) continue;

                NbtCompound nbt = machine.getOrCreateNbt();
                if (nbt.getBoolean(BreathingMachineItem.ACTIVE_KEY)) {
                    if (player.age % 20 == 0) {
                        int timeLeft = nbt.getInt(BreathingMachineItem.TIME_LEFT_KEY) - 20;
                        int energy = nbt.getInt(BreathingMachineItem.ENERGY_KEY) - 1;

                        nbt.putInt(BreathingMachineItem.TIME_LEFT_KEY, timeLeft);
                        nbt.putInt(BreathingMachineItem.ENERGY_KEY, energy);

                        if (timeLeft <= 0 || energy <= 0) {
                            triggerFatalEffect(player, machine, true);
                        }
                    }
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (tryStopMachine(player, world)) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (tryStopMachine(player, world)) {
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }

    private static boolean tryStopMachine(PlayerEntity player, World world) {
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer && player.isSneaking()) {
            ItemStack machine = getMachine(serverPlayer);
            if (!machine.isEmpty() && machine.getOrCreateNbt().getBoolean(BreathingMachineItem.ACTIVE_KEY)) {
                triggerFatalEffect(serverPlayer, machine, false);
                return true;
            }
        }
        return false;
    }

    public static void triggerFatalEffect(ServerPlayerEntity player, ItemStack machine, boolean destroyItem) {
        NbtCompound nbt = machine.getOrCreateNbt();
        nbt.putBoolean(BreathingMachineItem.ACTIVE_KEY, false);

        player.setInvulnerable(false);
        EntityAttributeInstance kbAttr = player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.removeModifier(KNOCKBACK_UUID);

        player.setAbsorptionAmount(0);

        player.damage(player.getDamageSources().outOfWorld(), Float.MAX_VALUE);

        if (destroyItem) {
            TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
                comp.getEquipped(stack -> stack.getItem() instanceof BreathingMachineItem).forEach(pair -> {
                    pair.getLeft().inventory().setStack(pair.getLeft().index(), ItemStack.EMPTY);
                });
            });
        } else {
            nbt.putInt(BreathingMachineItem.ENERGY_KEY, 0);
        }
    }

    private static ItemStack getMachine(ServerPlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(comp -> comp.getEquipped(stack -> stack.getItem() instanceof BreathingMachineItem)
                        .stream().findFirst().map(pair -> pair.getRight()).orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }
}
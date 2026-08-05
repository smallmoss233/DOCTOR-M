package doctor_m.handler;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.lock.LockedDimensionRegistry;
import dev.amble.ait.core.util.WorldUtil;
import doctor_m.Item.data_itme.VortexManipulatorItem;
import doctor_m.network.VMNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class VMServerHandler {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(VMNetwork.CYCLE_DIM, (server, player, handler, buf, responseSender) -> {
            if (buf.readableBytes() < 1) return;
            boolean left = buf.readBoolean();
            server.execute(() -> cycleDimension(player, left));
        });

        ServerPlayNetworking.registerGlobalReceiver(VMNetwork.SET_CURRENT_DEST, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> setCurrentDest(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VMNetwork.SET_PREV_DEST, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> setPrevDest(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(VMNetwork.TELEPORT, (server, player, handler, buf, responseSender) -> {
            if (buf.readableBytes() < 24) return; // 3 * double
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            server.execute(() -> teleport(player, x, y, z));
        });
    }

    private static ItemStack getVM(ServerPlayerEntity player) {
        return VortexManipulatorItem.findInHands(player);
    }

    private static long getTime(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private static void cycleDimension(ServerPlayerEntity player, boolean left) {
        ItemStack stack = getVM(player);
        if (stack.isEmpty()) return;

        List<ServerWorld> dims = new ArrayList<>(WorldUtil.getTravelWorlds());
        dims.removeIf(w -> w.getRegistryKey().getValue().getNamespace().equals("ait-tardis"));
        if (dims.isEmpty()) return;

        String currentDim = VortexManipulatorItem.getDestDim(stack);
        int index = -1;
        for (int i = 0; i < dims.size(); i++) {
            if (dims.get(i).getRegistryKey().getValue().toString().equals(currentDim)) {
                index = i;
                break;
            }
        }
        if (index == -1) index = 0;

        int nextIndex = left
                ? Math.floorMod(index - 1, dims.size())
                : (index + 1) % dims.size();

        VortexManipulatorItem.setDestDim(stack, dims.get(nextIndex).getRegistryKey().getValue().toString());
    }

    private static void setCurrentDest(ServerPlayerEntity player) {
        ItemStack stack = getVM(player);
        if (stack.isEmpty()) return;

        String dim = player.getWorld().getRegistryKey().getValue().toString();
        if (dim.startsWith("ait-tardis:")) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.invalid_dimension")
                    .formatted(Formatting.RED), true);
            return;
        }

        VortexManipulatorItem.saveCurrentAsDest(player, stack);
    }

    private static void setPrevDest(ServerPlayerEntity player) {
        ItemStack stack = getVM(player);
        if (stack.isEmpty()) return;
        VortexManipulatorItem.swapDestWithPrev(stack);
    }

    // ==================== Teleport ====================

    private static void teleport(ServerPlayerEntity player, double x, double y, double z) {
        ItemStack stack = getVM(player);
        if (stack.isEmpty()) return;

        long time = getTime(player);

        // 1. 损坏状态
        if (handleBrokenState(player, stack, time)) return;

        // 2. 冷却
        if (VortexManipulatorItem.isOnCooldown(stack, time)) {
            int sec = VortexManipulatorItem.getCooldownRemaining(stack, time);
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.cooldown", sec)
                    .formatted(Formatting.YELLOW), true);
            return;
        }

        // 3. 目标维度
        String dimId = VortexManipulatorItem.getDestDim(stack);
        if (dimId.startsWith("ait-tardis:")) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.invalid_dimension")
                    .formatted(Formatting.RED), true);
            return;
        }

        RegistryKey<World> targetKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimId));
        ServerWorld targetWorld = player.getServer().getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.invalid_dimension")
                    .formatted(Formatting.RED), true);
            return;
        }

        // 4. 维度锁定
        if (AITMod.CONFIG.lockDimensions) {
            var locked = LockedDimensionRegistry.getInstance().get(targetWorld);
            if (locked != null) {
                player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.dimension_locked")
                        .formatted(Formatting.RED), true);
                return;
            }
        }

        // ===== 5. 坐标硬边界检查（防止世界生成崩溃） =====
        if (Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || y < -128 || y > 512) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.out_of_bounds")
                    .formatted(Formatting.RED), true);
            return;
        }

        // ===== 6. 燃料计算（含各种附加费） =====
        double dist = distance(player.getX(), player.getY(), player.getZ(), x, y, z);
        boolean crossDimension = !player.getWorld().getRegistryKey().getValue().toString().equals(dimId);

        int fuelCost = VortexManipulatorItem.calcFuelCost(dist);

        // 跨维度附加费
        if (crossDimension) {
            fuelCost = (int) Math.ceil(fuelCost * 1.5);
        }

        // 极端海拔附加费（高空或深地）
        if (y > 200 || y < 16) {
            fuelCost = (int) Math.ceil(fuelCost * 1.15);
        }

        // 移动中惩罚（锁定高速目标更耗能）
        if (player.getVelocity().lengthSquared() > 0.01) {
            fuelCost = (int) Math.ceil(fuelCost * 1.1);
        }

        // 危险着陆附加费（脚下没方块，需要额外能量稳定）
        if (!isSafeLanding(targetWorld, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) {
            fuelCost = (int) Math.ceil(fuelCost * 1.2);
        }

        // 超距拒绝：超过能量上限2倍直接不给传
        if (fuelCost > VortexManipulatorItem.MAX_FUEL * 2) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.distance_too_far", fuelCost)
                    .formatted(Formatting.RED), true);
            return;
        }

        int overheatCost = VortexManipulatorItem.calcOverheat(dist);
        int fuel = VortexManipulatorItem.getFuel(stack);

        if (fuel < fuelCost) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.not_enough_fuel", fuelCost)
                    .formatted(Formatting.RED), true);
            return;
        }

        // 7. 执行
        performTeleport(player, stack, targetWorld, x, y, z, fuel, fuelCost, overheatCost, time, dist);
    }

    private static boolean isSafeLanding(ServerWorld world, int x, int y, int z) {
        for (int i = 0; i < 5; i++) {
            if (y - i < world.getBottomY()) return false;
            if (!world.getBlockState(new BlockPos(x, y - i, z)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean handleBrokenState(ServerPlayerEntity player, ItemStack stack, long time) {
        long brokenUntil = VortexManipulatorItem.getBrokenUntil(stack);
        if (brokenUntil == 0) return false;

        if (brokenUntil > time) {
            VortexManipulatorItem.punishBrokenUse(player);
            long days = (brokenUntil - time) / 24000;
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.broken_days", days)
                    .formatted(Formatting.DARK_RED), true);
            return true;
        }

        // 自动修复
        VortexManipulatorItem.setBrokenUntil(stack, 0);
        VortexManipulatorItem.setOverheat(stack, 0);
        player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.cooled_down")
                .formatted(Formatting.GREEN), true);
        return false;
    }

    private static void performTeleport(ServerPlayerEntity player, ItemStack stack, ServerWorld targetWorld,
                                        double x, double y, double z,
                                        int fuel, int fuelCost, int overheatCost, long time, double dist) {
        // 保存 prev
        VortexManipulatorItem.setPrevX(stack, VortexManipulatorItem.getDestX(stack));
        VortexManipulatorItem.setPrevY(stack, VortexManipulatorItem.getDestY(stack));
        VortexManipulatorItem.setPrevZ(stack, VortexManipulatorItem.getDestZ(stack));
        VortexManipulatorItem.setPrevDim(stack, VortexManipulatorItem.getDestDim(stack));

        // 更新 dest（实际坐标）
        VortexManipulatorItem.setDestX(stack, x);
        VortexManipulatorItem.setDestY(stack, y);
        VortexManipulatorItem.setDestZ(stack, z);

        // 扣费
        VortexManipulatorItem.setFuel(stack, fuel - fuelCost);
        int newOverheat = VortexManipulatorItem.getOverheat(stack) + overheatCost;
        VortexManipulatorItem.setOverheat(stack, newOverheat);
        VortexManipulatorItem.setLastUsed(stack, time);
        VortexManipulatorItem.setCooldownEndSys(stack, System.currentTimeMillis() + VortexManipulatorItem.COOLDOWN_TICKS * 50L);

        // 传送
        player.teleport(targetWorld, x, y, z, player.getYaw(), player.getPitch());
        targetWorld.playSound(null, player.getBlockPos(),
                net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);

        player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.teleported", fuelCost, overheatCost)
                .formatted(Formatting.GREEN), true);

        if (dist > 500.0) {
            double chance = dist > 5000.0 ? 0.15 : 0.10;
            if (player.getRandom().nextDouble() < chance) {
                // 持续时间：每 50 格 1 秒（20 ticks）
                int durationTicks = (int) (dist / 50.0) * 20;
                int durationSec = durationTicks / 20;

                // 凋零 I
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.WITHER, durationTicks, 0, false, false, true));
                // 反胃 I
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.NAUSEA, durationTicks, 0, false, false, true));
                // 虚弱 V
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.WEAKNESS, durationTicks, 4, false, false, true));
                // 缓慢 IV
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.SLOWNESS, durationTicks, 3, false, false, true));
                // 饥饿 II
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.HUNGER, durationTicks, 1, false, false, true));

                player.sendMessage(net.minecraft.text.Text.translatable(
                                "message.doctor_m.vm.time_sickness", durationSec)
                        .formatted(Formatting.DARK_PURPLE), true);
            }
        }

        // 过热损坏判定（不预检，传完再看炸没炸）
        if (newOverheat >= VortexManipulatorItem.MAX_OVERHEAT) {
            VortexManipulatorItem.setBrokenUntil(stack, time + VortexManipulatorItem.BROKEN_COOLDOWN_TICKS);
            player.sendMessage(net.minecraft.text.Text.translatable("message.doctor_m.vm.overheated_3days")
                    .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
            player.damage(player.getDamageSources().generic(), 4.0f);
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.POISON, 100, 0));
        }
    }

    private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
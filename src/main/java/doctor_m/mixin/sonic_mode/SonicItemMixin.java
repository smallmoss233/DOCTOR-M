package doctor_m.mixin.sonic_mode;

import dev.amble.ait.core.item.SonicItem;
import dev.amble.ait.core.item.sonic.SonicMode;
import doctor_m.module.sonic_plus.AmethystSonicModes;
import doctor_m.module.sonic_plus.ChargedZeitonSonicModes;
import doctor_m.module.sonic_plus.CrystalManager;
import doctor_m.module.sonic_plus.UpgradeModuleManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SonicItem.class)
public class SonicItemMixin {

    // 1. 安装逻辑
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void doctor_m$installModuleOrCrystal(World world, PlayerEntity user, Hand hand,
                                                 CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!user.isSneaking()) return;

        ItemStack stack = user.getStackInHand(hand);
        ItemStack offhand = user.getOffHandStack();

        if (offhand.getItem() instanceof SonicItem) {
            SonicMode offhandMode = SonicItem.mode(offhand);
            if (offhandMode == SonicMode.Modes.INTERACTION) {
                Identifier currentModule = UpgradeModuleManager.getInstalledModule(stack);

                if (!currentModule.equals(UpgradeModuleManager.EMPTY)) {
                    if (!world.isClient()) {
                        // 弹出旧模块
                        ItemStack oldStack = UpgradeModuleManager.createModuleStack(currentModule);
                        if (!oldStack.isEmpty()) {
                            if (!user.getInventory().insertStack(oldStack)) {
                                user.dropItem(oldStack, false);
                            }
                        }

                        UpgradeModuleManager.setInstalledModule(stack, UpgradeModuleManager.EMPTY);

                        // 刷新能量上限（移除扩容后截断 / 移除再生后补到 1000 上限）
                        double maxFuel = ((SonicItem) (Object) this).getMaxFuel(stack);
                        double currentFuel = ((SonicItem) (Object) this).getCurrentFuel(stack);
                        if (currentFuel > maxFuel) {
                            double excess = currentFuel - maxFuel;
                            ((SonicItem) (Object) this).removeFuel(excess, stack);
                        }
                    }

                    world.playSound(null, user.getX(), user.getY(), user.getZ(),
                            SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.PLAYERS, 0.8f, 1.0f);

                    if (!world.isClient()) {
                        user.sendMessage(Text.translatable("sonic_mode.message.doctor_m.module_removed"), true);
                    }

                    cir.setReturnValue(TypedActionResult.consume(stack));
                    return;
                }
            }
        }

        //升级模块安装
        if (UpgradeModuleManager.isValidUpgradeModule(offhand)) {
            Identifier newModule = Registries.ITEM.getId(offhand.getItem());
            Identifier currentModule = UpgradeModuleManager.getInstalledModule(stack);

            // 已经装了同样的，不做任何事
            if (currentModule.equals(newModule)) return;

            if (!world.isClient()) {
                // 弹出旧模块（互斥关键逻辑）
                if (!currentModule.equals(UpgradeModuleManager.EMPTY)) {
                    ItemStack oldStack = UpgradeModuleManager.createModuleStack(currentModule);
                    if (!oldStack.isEmpty()) {
                        if (!user.getInventory().insertStack(oldStack)) {
                            user.dropItem(oldStack, false);
                        }
                    }
                }

                offhand.decrement(1);
                UpgradeModuleManager.setInstalledModule(stack, newModule);

                // 刷新能量上限：如果当前能量超出新上限，直接截断
                double maxFuel = ((SonicItem) (Object) this).getMaxFuel(stack);
                double currentFuel = ((SonicItem) (Object) this).getCurrentFuel(stack);
                if (currentFuel > maxFuel) {
                    double excess = currentFuel - maxFuel;
                    ((SonicItem) (Object) this).removeFuel(excess, stack);
                }
            }

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.8f, 1.2f);

            if (!world.isClient()) {
                user.sendMessage(Text.translatable("sonic_mode.message.doctor_m.module_installed")
                        .append(UpgradeModuleManager.createModuleStack(newModule).getName()), true);
            }

            cir.setReturnValue(TypedActionResult.consume(stack));
            return;
        }

        //水晶安装
        if (!CrystalManager.isValidCrystal(offhand)) return;

        Identifier newCrystal = Registries.ITEM.getId(offhand.getItem());
        Identifier currentCrystal = CrystalManager.getInstalledCrystal(stack);

        if (currentCrystal.equals(newCrystal)) return;

        if (!world.isClient()) {
            ItemStack oldStack = CrystalManager.createCrystalStack(currentCrystal);
            if (!oldStack.isEmpty()) {
                if (!user.getInventory().insertStack(oldStack)) {
                    user.dropItem(oldStack, false);
                }
            }

            offhand.decrement(1);
            CrystalManager.setInstalledCrystal(stack, newCrystal);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (!world.isClient()) {
            user.sendMessage(Text.translatable("sonic_mode.message.doctor_m.crystal_installed",
                    CrystalManager.createCrystalStack(newCrystal).getName()), true);
        }

        cir.setReturnValue(TypedActionResult.consume(stack));
    }

    //模式重映射
    @Inject(method = "mode", at = @At("RETURN"), cancellable = true)
    private static void doctor_m$remapMode(ItemStack stack,
                                           CallbackInfoReturnable<SonicMode> cir) {
        SonicMode original = cir.getReturnValue();
        if (original == null) return;

        Identifier crystal = CrystalManager.getInstalledCrystal(stack);

        if (crystal.equals(CrystalManager.AMETHYST_CRYSTAL)) {
            SonicMode remapped = AmethystSonicModes.map(original);
            if (remapped != original) {
                cir.setReturnValue(remapped);
            }
        } else if (crystal.equals(CrystalManager.CHARGED_ZEITON_CRYSTAL)) {
            SonicMode remapped = ChargedZeitonSonicModes.map(original);
            if (remapped != original) {
                cir.setReturnValue(remapped);
            }
        }
    }

    @ModifyVariable(
            method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Ldev/amble/ait/core/item/sonic/SonicMode;next()Ldev/amble/ait/core/item/sonic/SonicMode;"
            )
    )
    private SonicMode doctor_m$remapAfterNext(SonicMode original, World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        Identifier crystal = CrystalManager.getInstalledCrystal(stack);
        if (crystal.equals(CrystalManager.AMETHYST_CRYSTAL)) {
            return AmethystSonicModes.map(original);
        } else if (crystal.equals(CrystalManager.CHARGED_ZEITON_CRYSTAL)) {
            return ChargedZeitonSonicModes.map(original);
        }
        return original;
    }

    //动态修改能量上限
    @Inject(method = "getMaxFuel", at = @At("RETURN"), cancellable = true)
    private void doctor_m$applyModuleMaxFuel(ItemStack stack, CallbackInfoReturnable<Double> cir) {
        Identifier module = UpgradeModuleManager.getInstalledModule(stack);
        if (module.equals(UpgradeModuleManager.ENERGY_UPGRADE)) {
            cir.setReturnValue(cir.getReturnValue() * 2);
        } else if (module.equals(UpgradeModuleManager.REGENERATION_MODULE)) {
            cir.setReturnValue(UpgradeModuleManager.REGENERATION_MAX);
        }
    }

    //Tooltip：模块 + 水晶（合并显示）
    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void doctor_m$appendAllTooltips(ItemStack stack, @Nullable World world,
                                            List<Text> tooltip, TooltipContext context,
                                            CallbackInfo ci) {
        // 升级模块
        Identifier moduleId = UpgradeModuleManager.getInstalledModule(stack);
        if (!moduleId.equals(UpgradeModuleManager.EMPTY)) {
            ItemStack moduleStack = UpgradeModuleManager.createModuleStack(moduleId);
            tooltip.add(Text.translatable("sonic_mode.tooltip.doctor_m.installed_module")
                    .append(moduleStack.getName())
                    .formatted(Formatting.GOLD));
        }

        // 水晶
        Identifier crystalId = CrystalManager.getInstalledCrystal(stack);
        ItemStack crystalStack = CrystalManager.createCrystalStack(crystalId);
        tooltip.add(Text.translatable("sonic_mode.tooltip.doctor_m.installed_crystal")
                .append(crystalStack.getName())
                .formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
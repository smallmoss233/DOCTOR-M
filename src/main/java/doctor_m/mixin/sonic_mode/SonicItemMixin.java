package doctor_m.mixin.sonic_mode;

import java.util.List;

import doctor_m.module.sonic_plus.AmethystSonicModes;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import dev.amble.ait.core.item.SonicItem;
import dev.amble.ait.core.item.sonic.SonicMode;
import doctor_m.module.sonic_plus.CrystalManager;

@Mixin(SonicItem.class)
public class SonicItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void doctor_m$installCrystal(World world, PlayerEntity user, Hand hand,
                                         CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!user.isSneaking()) return;

        ItemStack stack = user.getStackInHand(hand);
        ItemStack offhand = user.getOffHandStack();

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
        // original 是 mode.next() 返回的原版实例，这里替换成映射后的
        ItemStack stack = user.getStackInHand(hand);
        Identifier crystal = CrystalManager.getInstalledCrystal(stack);
        if (crystal.equals(CrystalManager.AMETHYST_CRYSTAL)) {
            return AmethystSonicModes.map(original);
        }
        return original;
    }

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void doctor_m$crystalTooltip(ItemStack stack, @Nullable World world,
                                         List<Text> tooltip, TooltipContext context,
                                         CallbackInfo ci) {
        Identifier crystalId = CrystalManager.getInstalledCrystal(stack);
        ItemStack crystalStack = CrystalManager.createCrystalStack(crystalId);

        tooltip.add(Text.translatable("sonic_mode.tooltip.doctor_m.installed_crystal")
                .append(crystalStack.getName())
                .formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
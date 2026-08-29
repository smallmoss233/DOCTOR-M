package doctor_m.Item.data_item;

import dev.amble.ait.module.gun.core.item.BaseGunItem;
import doctor_m.Item.Authorizable;
import doctor_m.api.ModSounds;
import doctor_m.util.creativity.DynamicColorHelper;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.awt.*;
import java.util.List;

public class DeMatGunItem extends BaseGunItem implements Authorizable {

    private static final String NBT_AUTHORIZED = "Authorized";

    public DeMatGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isAuthorized(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(NBT_AUTHORIZED);
    }

    @Override
    public void setAuthorized(ItemStack stack, boolean authorized) {
        stack.getOrCreateNbt().putBoolean(NBT_AUTHORIZED, authorized);
    }

    @Override
    public SoundEvent getAuthorizeSound() {
        return ModSounds.DE_MAT_GUN_FIRE;
    }

    @Override
    public SoundEvent getRevokeSound() {
        return SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE;
    }

    @Override
    public void onAuthorizationChanged(PlayerEntity player, ItemStack stack, boolean newState) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        Vec3d origin = player.getEyePos().add(player.getRotationVec(1.0f).multiply(0.5));
        Vector3f orange = new Vector3f(1.0f, 0.5f, 0.0f);

        if (newState) {
            serverWorld.spawnParticles(
                    new DustParticleEffect(orange, 1.5f),
                    origin.x, origin.y, origin.z,
                    30, 0.5, 0.5, 0.5, 0.2
            );
            serverWorld.spawnParticles(
                    new DustParticleEffect(orange, 2.0f),
                    origin.x, origin.y, origin.z,
                    15, 0.1, 0.1, 0.1, 0.05
            );
        } else {
            serverWorld.spawnParticles(
                    new DustParticleEffect(orange, 1.0f),
                    origin.x, origin.y, origin.z,
                    10, 0.3, 0.3, 0.3, 0.1
            );
        }
    }

    @Override
    public void tryShoot(World world, Entity entity, boolean selected) {
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public double getMaxAmmo() {
        return Double.MAX_VALUE;
    }

    @Override
    public double getCurrentAmmo(ItemStack stack) {
        return Double.MAX_VALUE;
    }

    @Override
    public void setCurrentAmmo(double var, ItemStack stack) {
    }

    @Override
    public int getCooldown() {
        return 60;
    }

    @Override
    public float getAimDeviation(boolean isAds) {
        return isAds ? 0.15f : 1.2f;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("message.doctor_m.de_mat_gun.tooltip.line1")
                .formatted(Formatting.WHITE, Formatting.BOLD));
        MutableText line2 = Text.translatable("message.doctor_m.de_mat_gun.tooltip.line2");
        line2.setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
        tooltip.add(line2);

        tooltip.add(Text.translatable(
                isAuthorized(stack) ? "message.doctor_m.de_mat_gun.authorized" : "message.doctor_m.de_mat_gun.unauthorized"
        ).formatted(isAuthorized(stack) ? Formatting.GREEN : Formatting.RED));
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.de_mat_gun.detail")
        );
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),
                Color.GRAY,
                Color.GRAY,
                new Color(128, 0, 128),
                Color.GRAY,
                Color.GRAY
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 20000);
    }
}
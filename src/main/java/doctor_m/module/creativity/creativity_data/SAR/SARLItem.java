package doctor_m.module.creativity.creativity_data.SAR;

import doctor_m.module.EmissiveItem;
import doctor_m.network.INVERTSCREENPACKETNetwork;
import doctor_m.util.creativity.DynamicColorHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.List;

public class SARLItem extends SAR implements EmissiveItem {

    public SARLItem() {
        super(new Item.Settings().rarity(Rarity.EPIC),
                "SAR-09L",
                20f,
                2.8f,
                10000,
                0.80f,
                "message.doctor_m.stcl.description");
    }

    @Override
    public float getEnergyCostPerDamage() {
        return 50.0f;
    }

    @Override
    public void onSkillPressed(ServerPlayerEntity player, ItemStack stack) {
        int cd = getSkillCooldown(stack);
        if (cd > 0) return;
        if (getEnergy(stack) < STCL_SKILL_COST) {
            player.sendMessage(Text.translatable("message.doctor_m.stcs.skill_low_energy")
                    .formatted(Formatting.RED), true);
            return;
        }

        setEnergy(stack, getEnergy(stack) - STCL_SKILL_COST);

        Vec3d eyePos = player.getPos().add(0.0, player.getStandingEyeHeight(), 0.0);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = eyePos.add(look.multiply(STCL_SKILL_DASH));

        var result = player.getWorld().raycast(new RaycastContext(
                eyePos, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        Vec3d targetPos = (result.getType() == HitResult.Type.BLOCK)
                ? result.getPos().subtract(look.multiply(0.5))
                : end;

        spawnBurstParticles(player, player.getX(), player.getY() + 1.0, player.getZ());

        Vec3d start = player.getPos().add(0.0, player.getStandingEyeHeight() - 0.5, 0.0);
        Vec3d endVec = targetPos.add(0.0, 0.5, 0.0);
        double distance = start.distanceTo(endVec);
        int steps = Math.max((int) (distance * 4), 1);

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = start.x + (endVec.x - start.x) * t;
            double y = start.y + (endVec.y - start.y) * t;
            double z = start.z + (endVec.z - start.z) * t;

            if (i % 2 == 0) {
                player.getWorld().addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.0, 0.0);
            } else {
                player.getWorld().addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
            }

            if (i % 3 == 0) {
                player.getWorld().addParticle(
                        ParticleTypes.CLOUD,
                        x, y, z,
                        (Math.random() - 0.5) * 0.2,
                        (Math.random() - 0.5) * 0.2,
                        (Math.random() - 0.5) * 0.2);
            }
        }

        spawnBurstParticles(player, targetPos.x, targetPos.y + 1.0, targetPos.z);

        player.teleport(targetPos.x, targetPos.y, targetPos.z);
        player.fallDistance = 0f;

        INVERTSCREENPACKETNetwork.sendInvertScreenPacket(player, 10);

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        int finalCd = STCL_SKILL_COOLDOWN;
        if (isCoreActive(stack)) finalCd -= 20;
        finalCd = Math.max(finalCd, 0);
        setSkillCooldown(stack, finalCd);
    }

    private void spawnBurstParticles(ServerPlayerEntity player, double x, double y, double z) {
        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            double radius = 0.8;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            player.getServerWorld().spawnParticles(ParticleTypes.END_ROD, px, y, pz, 1, 0.0, 0.1, 0.0, 0.0);
        }
        player.getServerWorld().spawnParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 1.0);
        player.getServerWorld().spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, 0.3, 0.2, 0.3, 0.05);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),
                new Color(128, 0, 128),
                new Color(255, 0, 0)
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000);
    }
}
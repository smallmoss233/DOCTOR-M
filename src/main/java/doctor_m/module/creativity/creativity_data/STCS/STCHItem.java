package doctor_m.module.creativity.creativity_data.STCS;

import doctor_m.module.EmissiveItem;
import doctor_m.util.creativity.DynamicColorHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.awt.*;
import java.util.List;

public class STCHItem extends STCS implements EmissiveItem {

    public STCHItem() {
        super(new Item.Settings().rarity(Rarity.EPIC),
                "STC-07H",
                30f,
                1.2f,
                10000,
                0.90f,
                "message.doctor_m.stch.description");
    }

    @Override
    public float getEnergyCostPerDamage() {
        return 10.0f;
    }

    @Override
    public void onSkillPressed(ServerPlayerEntity player, ItemStack stack) {
        int cd = getSkillCooldown(stack);
        if (cd > 0) return;
        if (getEnergy(stack) < STCH_SKILL_COST) {
            player.sendMessage(Text.translatable("message.doctor_m.stcs.skill_low_energy")
                    .formatted(Formatting.RED), true);
            return;
        }

        setEnergy(stack, getEnergy(stack) - STCH_SKILL_COST);

        var box = player.getBoundingBox().expand(STCH_SKILL_RADIUS);
        player.getServerWorld().getEntitiesByClass(LivingEntity.class, box, e -> e != player)
                .forEach(entity -> {
                    entity.hurtTime = 0;
                    entity.timeUntilRegen = 0;
                    entity.damage(player.getDamageSources().playerAttack(player), STCH_SKILL_DAMAGE);

                    entity.getWorld().addParticle(
                            ParticleTypes.SWEEP_ATTACK,
                            entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ(),
                            0.0, 0.0, 0.0);
                    entity.getWorld().addParticle(
                            ParticleTypes.CRIT,
                            entity.getX(), entity.getY() + entity.getHeight(), entity.getZ(),
                            0.0, 0.5, 0.0);
                });

        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double x = player.getX() + Math.cos(angle) * STCH_SKILL_RADIUS;
            double z = player.getZ() + Math.sin(angle) * STCH_SKILL_RADIUS;
            player.getServerWorld().spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    x, player.getY() + 0.5, z,
                    1, 0.0, 0.0, 0.0, 0.1);
        }

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f);

        int finalCd = STCH_SKILL_COOLDOWN;
        if (isCoreActive(stack)) finalCd -= 40;
        setSkillCooldown(stack, finalCd);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),
                new Color(128, 0, 128),
                new Color(255, 165, 0)
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000);
    }
}
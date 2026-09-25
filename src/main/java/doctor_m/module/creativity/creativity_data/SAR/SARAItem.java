package doctor_m.module.creativity.creativity_data.SAR;

import doctor_m.module.EmissiveItem;
import doctor_m.util.creativity.DynamicColorHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.awt.*;
import java.util.List;

public class SARAItem extends SAR implements EmissiveItem {

    public SARAItem() {
        super(new Item.Settings().rarity(Rarity.EPIC),
                "SAR-08A",
                24f,
                2.0f,
                10000,
                0.85f,
                "message.doctor_m.sara.description");
    }

    @Override
    public void onSkillPressed(ServerPlayerEntity player, ItemStack stack) {
        int cd = getSkillCooldown(stack);
        if (cd > 0) return;
        if (getEnergy(stack) < SARA_SKILL_COST) {
            player.sendMessage(Text.translatable("message.doctor_m.sar.skill_low_energy")
                    .formatted(Formatting.RED), true);
            return;
        }

        setEnergy(stack, getEnergy(stack) - SARA_SKILL_COST);

        var box = player.getBoundingBox().expand(SARA_SKILL_RADIUS);
        player.getServerWorld().getEntitiesByClass(LivingEntity.class, box, e -> e != player)
                .forEach(entity -> {
                    entity.damage(player.getDamageSources().playerAttack(player), SARA_SKILL_DAMAGE);
                    var dir = entity.getPos().subtract(player.getPos()).normalize();
                    entity.addVelocity(dir.x * 2.0, 0.5, dir.z * 2.0);
                    entity.velocityDirty = true;

                    if (entity instanceof ServerPlayerEntity sp) {
                        sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
                    }

                    entity.getWorld().addParticle(
                            ParticleTypes.POOF,
                            entity.getX(), entity.getY() + 1.0, entity.getZ(),
                            0.0, 0.2, 0.0);
                });

        player.getServerWorld().spawnParticles(
                ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0.5, 0.5, 0.5, 1.0);
        player.getServerWorld().spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.8, 0.5, 0.8, 0.05);

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.5f);

        int finalCd = SARA_SKILL_COOLDOWN;
        if (isCoreActive(stack)) finalCd -= 40;
        setSkillCooldown(stack, finalCd);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),
                new Color(128, 0, 128),
                new Color(0, 100, 255)
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000);
    }
}
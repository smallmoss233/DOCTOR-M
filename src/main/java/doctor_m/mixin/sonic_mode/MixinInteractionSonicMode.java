package doctor_m.mixin.sonic_mode;

import dev.amble.ait.core.engine.DurableSubSystem;
import dev.amble.ait.core.engine.block.SubSystemBlockEntity;
import dev.amble.ait.core.item.sonic.InteractionSonicMode;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InteractionSonicMode.class)
public class MixinInteractionSonicMode {

    @Inject(
            method = "interactBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInteractBlock(BlockPos pos, ServerWorld world, LivingEntity user,
                                 int ticks, BlockHitResult blockHit, CallbackInfo ci) {
        // 只处理玩家
        if (!(user instanceof PlayerEntity player)) return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof SubSystemBlockEntity subSystem)) return;

        // 检查是否为可修复子系统
        if (!(subSystem.system() instanceof DurableSubSystem durable)) return;

        // 显示当前耐久（动作栏）
        player.sendMessage(
                Text.literal(Math.round(durable.durability()) + "/" + DurableSubSystem.MAX_DURABILITY)
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)),
                true
        );

        // 如果耐久未满，执行修复
        if (durable.durability() < DurableSubSystem.MAX_DURABILITY) {
            // 恢复 2%~10% 最大耐久（同 RepairToolItem）
            float val = world.getRandom().nextBetween(2, 10) * DurableSubSystem.MAX_DURABILITY / 100f;
            durable.addDurability(val);

            // 音效（修复成功）
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                    SoundCategory.BLOCKS, 0.5f, 1.5f);

            // 粒子效果（按恢复量生成数量，至少 1 个）
            int particleCount = Math.max(1, (int) (val / 2));
            for (int i = 0; i < particleCount; i++) {
                world.addImportantParticle(ParticleTypes.ENCHANT,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        0, 0.1f, 0);
            }

            // 注意：不消耗起子耐久（起子自身消耗能量由其它逻辑处理）
        } else {
            // 耐久已满，给个提示音
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_HIT,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }
}
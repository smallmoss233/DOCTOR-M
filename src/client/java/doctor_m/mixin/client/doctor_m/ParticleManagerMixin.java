package doctor_m.mixin.client.doctor_m;

import doctor_m.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    // ===== 方块完全破坏时的爆发粒子 =====
    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void doctor_m$customObeliskBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!isObelisk(state)) return;

        ci.cancel();
        spawnArtronBurst(pos);
    }

    // ← 新增：挖掘进度中的碎片粒子
    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void doctor_m$customObeliskBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        if (!isObelisk(state)) return;

        ci.cancel();

        ParticleManager self = (ParticleManager) (Object) this;
        Random random = world.random;

        // 挖掘时飘出少量紫色能量微粒（比破坏时少，因为每 tick 都会调用）
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;

            double vx = (random.nextDouble() - 0.5) * 0.08;
            double vy = random.nextDouble() * 0.05;
            double vz = (random.nextDouble() - 0.5) * 0.08;

            self.addParticle(ParticleTypes.WITCH, x, y, z, vx, vy, vz);
        }

        // 偶尔闪一个发光碎片
        if (random.nextInt(5) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            self.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
        }
    }

    // ===== 辅助方法 =====

    private static boolean isObelisk(BlockState state) {
        return state.isOf(ModBlocks.EYE_OF_HARMONY_OBELISK)
                || state.isOf(ModBlocks.EYE_OF_HARMONY_PART);
    }

    private static void spawnArtronBurst(BlockPos pos) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        ParticleManager self = MinecraftClient.getInstance().particleManager;
        Random random = world.random;

        // 紫色 Artron 核心
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.2;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 1.2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.2;

            double vx = (random.nextDouble() - 0.5) * 0.25;
            double vy = (random.nextDouble() - 0.5) * 0.25 + 0.15;
            double vz = (random.nextDouble() - 0.5) * 0.25;

            if (random.nextInt(3) == 0) {
                self.addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
            } else {
                self.addParticle(ParticleTypes.WITCH, x, y, z, vx, vy, vz);
            }
        }

        // 发光碎片
        for (int i = 0; i < 10; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            self.addParticle(ParticleTypes.END_ROD, x, y, z,
                    (random.nextDouble() - 0.5) * 0.1,
                    random.nextDouble() * 0.15,
                    (random.nextDouble() - 0.5) * 0.1);
        }

        // 底部残留
        for (int i = 0; i < 6; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            self.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.08, 0);
        }
    }
}
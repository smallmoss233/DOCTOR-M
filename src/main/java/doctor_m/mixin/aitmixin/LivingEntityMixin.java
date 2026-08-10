package doctor_m.mixin.aitmixin;

import net.fabricmc.fabric.api.util.TriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import dev.amble.ait.api.ExtraPushableEntity;
import dev.amble.ait.core.AITDimensions;
import dev.amble.ait.core.AITTags;
import dev.amble.ait.core.util.SafePosSearch;
import dev.amble.ait.core.util.WorldUtil;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import dev.amble.lib.util.TeleportUtil;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ExtraPushableEntity {

    @Unique
    private TriState doctor_m$pushable = TriState.DEFAULT;

    @Unique
    private boolean doctor_m$voidTeleporting = false;

    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot var1);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    // ========== 1. 恢复 AIT 原生命支持逻辑 + 循环呼吸器 ==========
    @Inject(method = "tick", at = @At("HEAD"))
    public void doctor_m$tick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 跳过创造/旁观
        if (entity instanceof PlayerEntity player
                && (player.isCreative() || player.isSpectator()))
            return;

        ItemStack stack = entity.getEquippedStack(EquipmentSlot.HEAD);
        boolean hasRespirator = stack.isIn(AITTags.Items.FULL_RESPIRATORS)
                || stack.isIn(AITTags.Items.HALF_RESPIRATORS);

        // ========== 循环呼吸器：减缓水下氧气流失 ==========
        if (hasRespirator && entity.isSubmergedIn(FluidTags.WATER)) {
            // 每 3 tick 补 1 点空气，约等于把流失速度砍掉 1/3
            if (entity.age % 3 == 0 && entity.getAir() < entity.getMaxAir()) {
                entity.setAir(Math.min(entity.getAir() + 1, entity.getMaxAir()));
            }
        }

        // ========== 原 AIT 逻辑：戴呼吸器免 TARDIS 缺氧扣血 ==========
        if (hasRespirator)
            return;

        if (entity.getWorld() instanceof TardisServerWorld tardisWorld && !tardisWorld.getTardis().isGrowth()
                && !tardisWorld.getTardis().subsystems().lifeSupport().isEnabled()) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 1,
                    200, false, false));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                    200, 1, false, false));
        }
    }

    // ========== 2. 推动行为（原样保留） ==========
    @Override
    public void ait$setPushBehaviour(TriState pushable) {
        this.doctor_m$pushable = pushable;
    }

    @Override
    public TriState ait$pushBehaviour() {
        return doctor_m$pushable;
    }

    @Inject(method = "isPushable", at = @At("RETURN"), cancellable = true)
    public void doctor_m$isPushable(CallbackInfoReturnable<Boolean> cir) {
        boolean pushable = cir.getReturnValueZ();

        if (this.doctor_m$pushable != TriState.DEFAULT)
            pushable = this.doctor_m$pushable.get();

        cir.setReturnValue(pushable);
    }

    // ========== 3. 虚空救援（修复重复传送） ==========
    @Inject(method = "tickInVoid", at = @At("HEAD"))
    public void doctor_m$tickVoid(CallbackInfo ci) {
        if (this.getWorld().isClient())
            return;
        if (this.getWorld().getRegistryKey() != AITDimensions.TIME_VORTEX_WORLD)
            return;
        if (doctor_m$voidTeleporting)
            return;
        if (WorldUtil.getTravelWorlds().isEmpty())
            return;

        LivingEntity entity = (LivingEntity) (Object) this;
        doctor_m$voidTeleporting = true;

        int worldIndex = this.getWorld().getRandom().nextInt(WorldUtil.getTravelWorlds().size());
        ServerWorld world = WorldUtil.getTravelWorlds().get(worldIndex);
        CachedDirectedGlobalPos safe = CachedDirectedGlobalPos.create(world, entity.getBlockPos(), (byte) 0);

        SafePosSearch.wrapSafe(safe, SafePosSearch.Kind.MEDIAN, true,
                result -> TeleportUtil.teleport(entity, world, result.getPos().toCenterPos(), entity.getYaw()));
    }
}
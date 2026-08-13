package doctor_m.mixin.doctor_m;

import doctor_m.module.PlayerTitleManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给 PlayerEntity 注入自定义 NBT 存储
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerTitleManager.CustomNbtAccessor {

    @Unique
    private NbtCompound doctor_m$customData = new NbtCompound();

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        PlayerTitleManager.writeToNbt((PlayerEntity)(Object)this, nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        PlayerTitleManager.readFromNbt((PlayerEntity)(Object)this, nbt);
    }

    @Override
    public NbtCompound doctor_m$getCustomData() {
        return this.doctor_m$customData;
    }

    @Override
    public void doctor_m$markDirty() {
        // 标记数据已变更，如果需要可以在这里触发同步
    }
}
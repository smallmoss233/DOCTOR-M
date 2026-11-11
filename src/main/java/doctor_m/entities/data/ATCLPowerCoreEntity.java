package doctor_m.entities.data;

import doctor_m.entities.Entities;
import doctor_m.Item.items;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ATCLPowerCoreEntity extends ThrownItemEntity {

    public ATCLPowerCoreEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    public ATCLPowerCoreEntity(World world, LivingEntity owner, ItemStack stack) {
        super(Entities.ATCL_POWER_CORE, owner, world);
        this.setItem(stack);
    }

    @Override
    protected Item getDefaultItem() {
        return items.ATCL_POWER_CORE;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        explode();
    }

    private void explode() {
        World world = this.getWorld();
        if (world.isClient()) return;

        // 1. 产生爆炸视觉效果（破坏方块，但不造成原版伤害）
        world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 6.0F, false, World.ExplosionSourceType.BLOCK);

        // 2. 用 AABB 粗筛周围 6 格内的所有生物
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(6.0), e -> true);

        // 3. 施加固定 140 点伤害
        for (LivingEntity entity : entities) {
            // 精筛：判断实体到爆炸中心的距离是否在 6 格之内（距离平方 <= 36.0）
            if (entity.squaredDistanceTo(this) <= 36.0) {
                // 忽略创造模式的玩家
                if (entity instanceof PlayerEntity player && player.isCreative()) continue;

                // 强制清空无敌帧
                entity.timeUntilRegen = 0;

                // 造成伤害
                entity.damage(world.getDamageSources().explosion(this, this.getOwner()), 140.0F);
            }
        }

        this.discard();
    }
}
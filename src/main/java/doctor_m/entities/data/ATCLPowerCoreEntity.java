package doctor_m.entities.data;

import doctor_m.entities.Entities;
import doctor_m.Item.items;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

import java.util.List;

public class ATCLPowerCoreEntity extends ThrownItemEntity {
    private float charge = 0.5F;

    public ATCLPowerCoreEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    public ATCLPowerCoreEntity(World world, LivingEntity owner, ItemStack stack) {
        super(Entities.ATCL_POWER_CORE, owner, world);
        this.setItem(stack);
    }

    public void setCharge(float charge) {
        this.charge = charge;
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

        world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 4.0F, false, World.ExplosionSourceType.BLOCK);

        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(4.0), e -> true);

        for (LivingEntity entity : entities) {
            entity.damage(world.getDamageSources().explosion(this, this.getOwner()), 40.0F);
        }

        this.discard();
    }
}
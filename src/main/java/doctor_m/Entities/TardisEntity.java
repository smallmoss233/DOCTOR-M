package doctor_m.Entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

public class TardisEntity extends PathAwareEntity {
    public TardisEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    // 创建默认属性（生命值、移动速度、攻击力等）
    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)    // 10颗心
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D) // 移动速度
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D);  // 攻击力（可选）
    }
}
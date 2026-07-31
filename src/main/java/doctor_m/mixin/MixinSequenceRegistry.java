package doctor_m.mixin;

import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.impl.CloakControl;
import dev.amble.ait.core.tardis.control.impl.RandomiserControl;
import dev.amble.ait.core.tardis.control.sequences.Sequence;
import dev.amble.ait.registry.impl.SequenceRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

@Mixin(SequenceRegistry.class)
public abstract class MixinSequenceRegistry {

    @Shadow
    @Final
    public static Sequence CLOAK_TO_AVOID_VORTEX_TRAPPED_MOBS;

    // 安全奖励池
    private static final List<Supplier<ItemStack>> REWARD_POOL = List.of(
            () -> new ItemStack(Items.COOKIE, 1),
            () -> new ItemStack(Items.POPPY, 1),
            () -> {
                // 安全获取 amblekit 的礼物盒（不会崩溃）
                Identifier id = new Identifier("amblekit", "gift_box");
                Item item = Registries.ITEM.get(id); // 直接 get，可能 null
                if (item == null) {
                    // 尝试备选（可选）
                    item = Registries.ITEM.get(new Identifier("plushies", "gift_box"));
                }
                return item != null ? new ItemStack(item, 1) : null;
            },
            // 你可以自由添加更多物品
            () -> new ItemStack(Items.NETHERITE_INGOT, 1),
            () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1)
    );

    @Inject(method = "init", at = @At("TAIL"))
    private static void replaceCloakSequence(CallbackInfo ci) {
        Identifier originalId = CLOAK_TO_AVOID_VORTEX_TRAPPED_MOBS.id();

        // 使用 Lambda 表达式（而非方法引用）来避免类型不匹配
        Sequence newSequence = Sequence.Builder.create(
                originalId,
                tardis -> giveSafeReward(tardis),   // 成功回调
                tardis -> spawnMonsters(tardis),    // 失败回调
                80L,
                Text.translatable("sequence.ait.cloak_to_avoid_vortex_trapped_mobs")
                        .formatted(Formatting.ITALIC, Formatting.YELLOW),
                new CloakControl(),
                new RandomiserControl()
        );

        // 反射替换 final 字段
        try {
            Field field = SequenceRegistry.class.getDeclaredField("CLOAK_TO_AVOID_VORTEX_TRAPPED_MOBS");
            field.setAccessible(true);
            field.set(null, newSequence);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 安全给予奖励：从池中随机选取物品，若无效则给饼干。
     */
    private static void giveSafeReward(Tardis tardis) {
        if (!(tardis instanceof ServerTardis serverTardis)) return;

        var doorPos = serverTardis.getDesktop().getDoorPos();
        if (doorPos == null) return;

        BlockPos pos = doorPos.getPos();          // DirectedBlockPos → BlockPos
        ServerWorld world = serverTardis.asServer().world();
        if (world == null) return;

        int idx = world.getRandom().nextInt(REWARD_POOL.size());
        ItemStack reward = REWARD_POOL.get(idx).get();

        if (reward == null || reward.isEmpty()) {
            reward = new ItemStack(Items.COOKIE, 1);
        }

        ItemEntity itemEntity = new ItemEntity(EntityType.ITEM, world);
        itemEntity.setPosition(pos.toCenterPos()); // BlockPos → Vec3d
        itemEntity.setStack(reward);
        world.spawnEntity(itemEntity);
    }

    /**
     * 失败时的怪物生成（完全复制原逻辑）
     */
    private static void spawnMonsters(Tardis tardis) {
        if (!(tardis instanceof ServerTardis serverTardis)) return;

        var doorPos = serverTardis.getDesktop().getDoorPos();
        if (doorPos == null) return;

        BlockPos pos = doorPos.getPos();
        serverTardis.travel().increaseFlightTime(200);

        if (serverTardis.door().isOpen()) return;

        ServerWorld interior = serverTardis.asServer().world();
        Vec3d centered = pos.toCenterPos();

        ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, interior);
        zombie.setPosition(centered);

        DrownedEntity drowned = new DrownedEntity(EntityType.DROWNED, interior);
        drowned.setPosition(centered);

        PhantomEntity phantom = new PhantomEntity(EntityType.PHANTOM, interior);
        phantom.setPosition(centered);

        Random random = Random.create();
        interior.spawnEntity(random.nextBoolean() ? random.nextBoolean() ? drowned : zombie : phantom);
    }
}
package doctor_m.mixin;

import dev.amble.ait.AITMod;
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
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mixin(SequenceRegistry.class)
public abstract class MixinSequenceRegistry {

    // ==================== 安全奖励池 ====================
    private static final List<Supplier<ItemStack>> REWARD_POOL;
    static {
        List<Supplier<ItemStack>> list = new ArrayList<>();
        list.add(() -> new ItemStack(Items.COOKIE, 1));
        list.add(() -> new ItemStack(Items.POPPY, 1));

        // 尝试获取礼物盒，模组不存在时自动回退（Registries.ITEM.get 找不到会返回 AIR）
        Item giftBox = Registries.ITEM.get(new Identifier("amblekit", "gift_box"));
        if (giftBox != Items.AIR) {
            final Item safeGiftBox = giftBox;
            list.add(() -> new ItemStack(safeGiftBox, 1));
        }

        list.add(() -> new ItemStack(Items.NETHERITE_INGOT, 1));
        list.add(() -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
        REWARD_POOL = List.copyOf(list);
    }

    /**
     * 拦截 SequenceRegistry.init() 中所有 register(Sequence) 调用。
     * 当发现是 cloak_to_avoid_vortex_trapped_mobs 时，直接构造安全版本并注册，
     * 旧的 schema 会被丢弃，永远不会进入 REGISTRY，也不会赋值给字段。
     */
    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/amble/ait/registry/impl/SequenceRegistry;register(Ldev/amble/ait/core/tardis/control/sequences/Sequence;)Ldev/amble/ait/core/tardis/control/sequences/Sequence;"
            )
    )
    private static Sequence replaceCloakSequence(Sequence schema) {
        // 匹配目标 sequence（用 id 判断，不触碰 lambda 内部）
        if (schema.id().equals(AITMod.id("cloak_to_avoid_vortex_trapped_mobs"))) {
            Sequence safe = Sequence.Builder.create(
                    schema.id(),
                    tardis -> giveSafeReward(tardis),
                    tardis -> spawnMonsters(tardis),
                    80L,
                    Text.translatable("sequence.ait.cloak_to_avoid_vortex_trapped_mobs")
                            .formatted(Formatting.ITALIC, Formatting.YELLOW),
                    new CloakControl(),
                    new RandomiserControl()
            );
            // 直接写注册表，绕过 SequenceRegistry.register 避免递归进入本 Redirect
            return Registry.register(SequenceRegistry.REGISTRY, safe.id(), safe);
        }

        // 其他 sequence 正常注册（同样直接写注册表，避免递归）
        return Registry.register(SequenceRegistry.REGISTRY, schema.id(), schema);
    }

    // ==================== 安全回调实现 ====================

    private static void giveSafeReward(Tardis tardis) {
        if (!(tardis instanceof ServerTardis serverTardis)) return;

        var doorPos = serverTardis.getDesktop().getDoorPos();
        if (doorPos == null) return;

        BlockPos pos = doorPos.getPos();
        ServerWorld world = serverTardis.asServer().world();
        if (world == null) return;

        ItemStack reward = REWARD_POOL.get(world.getRandom().nextInt(REWARD_POOL.size())).get();
        if (reward.isEmpty()) {
            reward = new ItemStack(Items.COOKIE, 1);
        }

        ItemEntity itemEntity = new ItemEntity(EntityType.ITEM, world);
        itemEntity.setPosition(pos.toCenterPos());
        itemEntity.setStack(reward);
        world.spawnEntity(itemEntity);
    }

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
        interior.spawnEntity(random.nextBoolean()
                ? (random.nextBoolean() ? drowned : zombie)
                : phantom);
    }
}
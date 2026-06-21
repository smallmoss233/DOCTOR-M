package doctor_m.module.creativity.creativity_data;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class tlipoca_scythe extends SwordItem {
    private static final String KILL_COUNT_KEY = "TlipocaKillCount";
    private static final String GROWTH_KEY = "TlipocaGrowth";
    private static final long COOLDOWN_TICKS = 60; // 3秒冷却
    private static final ConcurrentHashMap<UUID, Long> lastSlashTime = new ConcurrentHashMap<>();

    public tlipoca_scythe(Settings settings) {
        super(TlipocaMaterial.INSTANCE, 0, -2.4f, settings);
    }

    // 初始化NBT（附魔+无法破坏）
    public static void initScytheNbt(ItemStack stack) { // 改为 public
        NbtCompound nbt = stack.getOrCreateNbt();
        if (EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) == 0) {
            stack.addEnchantment(Enchantments.SHARPNESS, 10);
        }
        if (EnchantmentHelper.getLevel(Enchantments.LOOTING, stack) == 0) {
            stack.addEnchantment(Enchantments.LOOTING, 10);
        }
        if (!nbt.getBoolean("Unbreakable")) {
            nbt.putBoolean("Unbreakable", true);
        }
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        initScytheNbt(stack);
        return stack;
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        super.onCraft(stack, world, player);
        initScytheNbt(stack);
    }

    // 右键触发横扫攻击
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        UUID uuid = user.getUuid();
        long now = world.getTime();

        // 服务端：冷却检测 + 攻击 + 粒子触发（通过发包？为了简化，我们服务端只处理攻击，客户端通过标志同步）
        if (!world.isClient) {
            Long last = lastSlashTime.get(uuid);
            if (last != null && now - last < COOLDOWN_TICKS) {
                user.sendMessage(Text.literal("§c镰刀还在冷却中！"), true);
                return TypedActionResult.fail(stack);
            }

            // 执行攻击
            performSlashAttack(world, (ServerPlayerEntity) user);
            lastSlashTime.put(uuid, now);
            user.getItemCooldownManager().set(this, 20); // 防止连点
        }

        // 客户端：生成粒子效果
        if (world.isClient) {
            spawnSlashParticles(user);
        }

        return TypedActionResult.success(stack);
    }

    // 获取当前总伤害（含成长）
    public static float getTotalAttackDamage(ItemStack stack) {
        int growth = stack.getOrCreateNbt().getInt(GROWTH_KEY);
        return 20.0f + growth;
    }

    // 击杀增长（每10个增加5伤害）
    public static void onKill(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int killCount = nbt.getInt(KILL_COUNT_KEY) + 1;
        nbt.putInt(KILL_COUNT_KEY, killCount);
        if (killCount % 10 == 0) {
            int growth = nbt.getInt(GROWTH_KEY);
            nbt.putInt(GROWTH_KEY, growth + 5);
        }
    }

    // 工具提示
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int growth = stack.getOrCreateNbt().getInt(GROWTH_KEY);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("当前伤害: " + (20 + growth)).formatted(Formatting.DARK_RED));
        if (growth > 0) {
            tooltip.add(Text.literal("成长加成: +" + growth).formatted(Formatting.GREEN));
        }
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("右键：死亡斩击（冷却3秒）").formatted(Formatting.DARK_PURPLE));
    }

    // ---------- 横扫攻击 ----------
    private static void performSlashAttack(World world, ServerPlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        ItemStack stack = player.getMainHandStack();
        float damage = getTotalAttackDamage(stack);

        double reach = 8.0;
        double width = 5.0;
        double height = 3.0;
        Vec3d center = eyePos.add(look.multiply(reach / 2));
        Box slashBox = new Box(
                center.x - width / 2, center.y - height / 2, center.z - width / 2,
                center.x + width / 2, center.y + height / 2, center.z + width / 2
        );

        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                slashBox,
                entity -> entity != player && entity.isAlive()
        );

        for (LivingEntity target : targets) {
            Vec3d toTarget = target.getPos().subtract(eyePos).normalize();
            double angle = Math.acos(look.dotProduct(toTarget));
            if (angle < Math.PI / 3) {
                target.damage(world.getDamageSources().playerAttack(player), damage);
            }
        }
    }

    // ---------- 客户端粒子效果 ----------
    private static void spawnSlashParticles(PlayerEntity player) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);

        // 计算水平方向向量
        Vec3d forward = look.normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) {
            right = new Vec3d(1, 0, 0);
        }
        Vec3d realUp = right.crossProduct(forward).normalize();

        int count = 200; // 增加粒子数量使扇形更饱满
        double minDistance = 2.0;
        double maxDistance = 6.0;
        double maxAngle = Math.PI / 3; // 60度，形成宽扇形

        for (int i = 0; i < count; i++) {
            // 随机角度（从左到右）和距离（从近到远）
            double angle = (world.random.nextDouble() - 0.5) * 2 * maxAngle; // -60° ~ 60°
            double distance = minDistance + (world.random.nextDouble()) * (maxDistance - minDistance);

            // 计算粒子位置：在水平面上沿 right 方向偏移，同时沿 forward 方向前进
            double horizontalOffset = Math.sin(angle) * distance;
            double forwardOffset = Math.cos(angle) * distance;

            Vec3d basePos = eyePos.add(forward.multiply(forwardOffset));
            Vec3d offset = right.multiply(horizontalOffset);

            // 添加微小的垂直随机偏移，增加厚度
            double verticalSpread = 0.5;
            Vec3d verticalOffset = realUp.multiply((world.random.nextDouble() - 0.5) * verticalSpread);

            Vec3d pos = basePos.add(offset).add(verticalOffset);

            // 随机散布微调
            double spread = 0.15;
            pos = pos.add(
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread
            );

            // 红黑交替
            if (i % 2 == 0) {
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
            } else {
                world.addParticle(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    // ---------- 生命加成（+30%） ----------
    private static final UUID TLIPOCA_HEALTH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    public static void applyMaxHealthBoost(ServerPlayerEntity player) {
        var attribute = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(TLIPOCA_HEALTH_UUID);
            attribute.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                    TLIPOCA_HEALTH_UUID,
                    "Tlipoca Health Boost",
                    0.3,
                    net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    public static void removeMaxHealthBoost(ServerPlayerEntity player) {
        var attribute = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(TLIPOCA_HEALTH_UUID);
        }
    }
}
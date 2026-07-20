package doctor_m.mixin.doctor_m;

import net.minecraft.advancement.Advancement;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.item.KeyItem;
import dev.amble.ait.core.tardis.Tardis;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public abstract ItemStack getStack();

    @Unique
    private boolean doctor_m$achievementTriggered = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 直接转型获取 ItemEntity 实例
        ItemEntity self = (ItemEntity) (Object) this;

        if (doctor_m$achievementTriggered) return;

        ItemStack stack = self.getStack();
        if (!(stack.getItem() instanceof KeyItem)) return;

        if (!self.isInLava()) return;

        // 直接调用 getWorld()，不需要 @Shadow
        World world = self.getWorld();
        if (world.isClient()) return;

        Tardis tardis = KeyItem.getTardisStatic(world, stack);
        if (tardis == null) return;

        // 找最近的玩家
        ServerPlayerEntity closestPlayer = null;
        double closestDist = Double.MAX_VALUE;

        for (ServerPlayerEntity player : ((ServerWorld) world).getServer().getPlayerManager().getPlayerList()) {
            double dist = player.squaredDistanceTo(self.getPos());
            if (dist < closestDist) {
                closestDist = dist;
                closestPlayer = player;
            }
        }

        if (closestPlayer == null) return;

        triggerAchievement(closestPlayer);

        doctor_m$achievementTriggered = true;
        self.discard(); // 让钥匙消失
    }

    @Unique
    private void triggerAchievement(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement advancement = server.getAdvancementLoader()
                .get(new Identifier("doctor_m", "not_a_hallucination"));
        if (advancement == null) return;

        player.getAdvancementTracker().grantCriterion(advancement, "not_a_hallucination");
    }
}
package doctor_m.mixin.aitmixin;

import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.SiegeModeControl;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(SiegeModeControl.class)
public class MixinSiegeModeControl extends Control {

    private static final long CONFIRMATION_WINDOW = 20 * 15;

    // 存储每个 TARDIS 的武装时间
    private static final Map<UUID, Long> SIEGE_ARMED = new ConcurrentHashMap<>();

    public MixinSiegeModeControl() {
        super(SiegeModeControl.ID);
    }

    @Overwrite
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        if (tardis.travel().isCrashing() || tardis.travel().getState() != TravelHandlerBase.State.LANDED) {
            SIEGE_ARMED.remove(tardis.getUuid());
            return Result.FAILURE;
        }

        UUID tardisId = tardis.getUuid();
        boolean isActive = tardis.siege().isActive();

        // === 关闭围攻：直接执行 ===
        if (isActive) {
            tardis.siege().setActive(false);
            tardis.alarm().disable();
            player.sendMessage(Text.translatable("tardis.message.control.siege.disabled").formatted(Formatting.RED), true);
            world.playSound(null, player.getBlockPos(), AITSounds.SIEGE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            SIEGE_ARMED.remove(tardisId);
            return Result.SUCCESS_ALT;
        }

        // === 开启围攻 ===
        long currentTick = world.getTime();
        Long armedAt = SIEGE_ARMED.get(tardisId);

        // 已武装且在有效期内 → 二次确认
        if (armedAt != null && (currentTick - armedAt) <= CONFIRMATION_WINDOW) {
            SIEGE_ARMED.remove(tardisId);
            tardis.siege().setActive(true);
            tardis.alarm().disable();
            player.sendMessage(Text.translatable("tardis.message.control.siege.enabled").formatted(Formatting.GREEN), true);
            world.playSound(null, player.getBlockPos(), AITSounds.SIEGE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return Result.SUCCESS;
        }

        // 首次点击 → 武装
        SIEGE_ARMED.put(tardisId, currentTick);
        player.sendMessage(Text.translatable("tardis.message.control.siege.confirm_prompt").formatted(Formatting.YELLOW), true);
        world.playSound(null, player.getBlockPos(), AITSounds.BWEEP, SoundCategory.BLOCKS, 0.5f, 1.5f);

        return Result.SUCCESS_ALT;
    }
}
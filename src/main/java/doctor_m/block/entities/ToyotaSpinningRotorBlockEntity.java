package doctor_m.block.entities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.AnimationState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import dev.amble.ait.api.tardis.link.v2.block.InteriorLinkableBlockEntity;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import doctor_m.block.ModBlockEntities;

public class ToyotaSpinningRotorBlockEntity extends InteriorLinkableBlockEntity {

    public ToyotaSpinningRotorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOYOTA_SPINNING_ROTOR_ENTITY, pos, state);
    }

    public final AnimationState ANIM_STATE = new AnimationState();
    public int age;

    // 声音标志（包私有给 client 类用）
    Object sound;
    public boolean startSoundPlayed = false;
    public boolean stopSoundPlayed = false;

    // ========== 新增：动画状态机 ==========
    /** 当前实际显示的动画状态（可能与 TARDIS 真实状态不同） */
    public TravelHandlerBase.State displayState = null;
    /** TARDIS 上一帧的真实状态 */
    public TravelHandlerBase.State lastTardisState = null;
    /** 进入当前 displayState 时的 age */
    public int stateEntryAge = 0;
    /** MAT 动画最短播放时长（tick），根据你的动画长度调整 */
    public static final int MAT_ANIMATION_DURATION = 180;

    public int getAge() {
        return age;
    }

    @Override
    public void onLinked() {
        if (this.tardis().isEmpty()) return;
        Tardis tardis = this.tardis().get();
        if (tardis instanceof ClientTardis) return;
        tardis.getDesktop().getConsolePos().add(this.pos);
        tardis.asServer().markDirty(tardis.getDesktop());
    }

    public void tick(World world, BlockPos pos, BlockState blockState, ToyotaSpinningRotorBlockEntity blockEntity) {
        if (world instanceof ServerWorld) return;
        if (!this.isLinked()) return;

        Tardis tardis = this.tardis().get();
        TravelHandlerBase.State realState = tardis.travel().getState();

        // 初始化
        if (this.displayState == null) {
            this.displayState = realState;
            this.lastTardisState = realState;
            this.stateEntryAge = this.age;
            this.ANIM_STATE.startIfNotRunning(this.age);
        }

        // 检测 TARDIS 真实状态变化
        if (realState != this.lastTardisState) {
            if (realState == TravelHandlerBase.State.LANDED && this.lastTardisState == TravelHandlerBase.State.MAT) {
                // 从 MAT 到 LANDED：不立即切换，让 MAT 动画继续播完
            } else {
                // 其他切换（DEMAT/FLIGHT/LANDED 等）：直接同步，重置动画时间轴
                this.displayState = realState;
                this.stateEntryAge = this.age;
                this.ANIM_STATE.stop();
                this.ANIM_STATE.startIfNotRunning(this.age);
            }
            this.lastTardisState = realState;
        }

        // 强制延迟：MAT 必须播够时间才允许切到 LANDED
        if (this.displayState == TravelHandlerBase.State.MAT && this.lastTardisState == TravelHandlerBase.State.LANDED) {
            int elapsed = this.age - this.stateEntryAge;
            if (elapsed >= MAT_ANIMATION_DURATION) {
                // MAT 时间够了，切换到真正的 LANDED/IDLE
                this.displayState = TravelHandlerBase.State.LANDED;
                this.stateEntryAge = this.age;
                this.ANIM_STATE.stop();
                this.ANIM_STATE.startIfNotRunning(this.age);
            }
            // 否则继续 MAT，不重置 ANIM_STATE，动画自然推进
        }

        this.age++;
        this.ANIM_STATE.startIfNotRunning(this.getAge());
    }
}
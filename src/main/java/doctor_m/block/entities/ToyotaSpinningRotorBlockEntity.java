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
    Object sound;
    public boolean startSoundPlayed = false;
    public boolean stopSoundPlayed = false;
    public TravelHandlerBase.State displayState = null;
    public TravelHandlerBase.State lastTardisState = null;
    public int stateEntryAge = 0;
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

        this.age++;
        this.ANIM_STATE.startIfNotRunning(this.getAge());

        if (!this.isLinked()) {
            this.displayState = null;
            this.lastTardisState = null;
            return;
        }

        Tardis tardis = this.tardis().get();
        TravelHandlerBase.State realState = tardis.travel().getState();

        if (this.displayState == null) {
            this.displayState = realState;
            this.lastTardisState = realState;
            this.stateEntryAge = this.age;
        }

        if (realState != this.lastTardisState) {
            if (realState == TravelHandlerBase.State.LANDED && this.lastTardisState == TravelHandlerBase.State.MAT) {
            } else {
                this.displayState = realState;
                this.stateEntryAge = this.age;
                this.ANIM_STATE.stop();
                this.ANIM_STATE.startIfNotRunning(this.age);
            }
            this.lastTardisState = realState;
        }

        if (this.displayState == TravelHandlerBase.State.MAT && this.lastTardisState == TravelHandlerBase.State.LANDED) {
            int elapsed = this.age - this.stateEntryAge;
            if (elapsed >= MAT_ANIMATION_DURATION) {
                this.displayState = TravelHandlerBase.State.LANDED;
                this.stateEntryAge = this.age;
                this.ANIM_STATE.stop();
                this.ANIM_STATE.startIfNotRunning(this.age);
            }
        }
    }
}
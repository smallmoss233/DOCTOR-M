package doctor_m.block.entities;

import net.minecraft.sound.SoundCategory;
import dev.amble.ait.client.sounds.PositionedLoopingSound;
import dev.amble.ait.client.sounds.SoundHandler;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import doctor_m.api.ModSounds;

public class ToyotaSpinningRotorBlockEntityClient {

    public static void tick(ToyotaSpinningRotorBlockEntity entity, TravelHandlerBase.State displayState) {
        if (!entity.isLinked()) return;

        SoundHandler sound = getSound(entity);

        switch (displayState) {
            case DEMAT -> {
                sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_STOP));
                sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_LOOP));

                if (!entity.startSoundPlayed) {
                    sound.startSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_START));
                    entity.startSoundPlayed = true;
                }

                if (entity.startSoundPlayed && !sound.isPlaying(ModSounds.TOYOTA_TICKING_START)) {
                    if (!sound.isPlaying(ModSounds.TOYOTA_TICKING_LOOP)) {
                        sound.startSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_LOOP));
                    }
                } else {
                    sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_LOOP));
                }

                entity.stopSoundPlayed = false;
            }
            case FLIGHT -> {
                entity.startSoundPlayed = false;
                entity.stopSoundPlayed = false;

                sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_START));
                sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_STOP));

                if (!sound.isPlaying(ModSounds.TOYOTA_TICKING_LOOP)) {
                    sound.startSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_LOOP));
                }
            }
            case MAT -> {
                entity.startSoundPlayed = false;

                sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_LOOP));
                sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_START));

                if (!entity.stopSoundPlayed) {
                    sound.startSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_STOP));
                    entity.stopSoundPlayed = true;
                }
                int STOP_SOUND_LENGTH_TICKS = 180;
                if (entity.age - entity.stateEntryAge >= STOP_SOUND_LENGTH_TICKS) {
                    sound.stopSound(sound.findSoundByEvent(ModSounds.TOYOTA_TICKING_STOP));
                }
            }
            default -> {
                entity.startSoundPlayed = false;
                entity.stopSoundPlayed = false;
                sound.stopSounds();
            }
        }
    }

    private static SoundHandler getSound(ToyotaSpinningRotorBlockEntity entity) {
        if (entity.sound == null) {
            entity.sound = SoundHandler.create(
                    new PositionedLoopingSound(ModSounds.TOYOTA_TICKING_LOOP, SoundCategory.BLOCKS, entity.getPos()),
                    new PositionedLoopingSound(ModSounds.TOYOTA_TICKING_START, SoundCategory.BLOCKS, entity.getPos()),
                    new PositionedLoopingSound(ModSounds.TOYOTA_TICKING_STOP, SoundCategory.BLOCKS, entity.getPos())
            );
        }
        return (SoundHandler) entity.sound;
    }
}
package doctor_m.handler.KeytoTime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;

public class KeytoTimeActive {

    public static void toggleGameMode(ServerPlayerEntity player) {
        GameMode current = player.interactionManager.getGameMode();
        GameMode next = switch (current) {
            case SURVIVAL -> GameMode.CREATIVE;
            case CREATIVE -> GameMode.ADVENTURE;
            case ADVENTURE -> GameMode.SPECTATOR;
            case SPECTATOR -> GameMode.SURVIVAL;
            default -> GameMode.SURVIVAL;
        };
        player.changeGameMode(next);
    }

    public static void toggleDifficulty(ServerPlayerEntity player) {
        var server = player.getServer();
        if (server == null) return;

        Difficulty current = server.getOverworld().getDifficulty();
        Difficulty next = switch (current) {
            case PEACEFUL -> Difficulty.EASY;
            case EASY -> Difficulty.NORMAL;
            case NORMAL -> Difficulty.HARD;
            case HARD -> Difficulty.PEACEFUL;
        };
        server.setDifficulty(next, true);
    }
}
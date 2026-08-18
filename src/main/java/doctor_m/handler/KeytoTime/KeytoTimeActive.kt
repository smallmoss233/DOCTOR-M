package doctor_m.handler.KeytoTime

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.Difficulty
import net.minecraft.world.GameMode

object KeytoTimeActive {

    @JvmStatic
    fun toggleGameMode(player: ServerPlayerEntity) {
        val next = when (player.interactionManager.gameMode) {
            GameMode.SURVIVAL -> GameMode.CREATIVE
            GameMode.CREATIVE -> GameMode.ADVENTURE
            GameMode.ADVENTURE -> GameMode.SPECTATOR
            GameMode.SPECTATOR -> GameMode.SURVIVAL
            else -> GameMode.SURVIVAL
        }
        player.changeGameMode(next)
    }

    @JvmStatic
    fun toggleDifficulty(player: ServerPlayerEntity) {
        val server = player.server ?: return
        val next = when (server.overworld.difficulty) {
            Difficulty.PEACEFUL -> Difficulty.EASY
            Difficulty.EASY -> Difficulty.NORMAL
            Difficulty.NORMAL -> Difficulty.HARD
            Difficulty.HARD -> Difficulty.PEACEFUL
        }
        server.setDifficulty(next, true)
    }
}
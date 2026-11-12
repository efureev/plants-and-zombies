package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.raid.GardenHeroRewards
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent

/**
 * Hero of the Garden rewards survive a respawn that keeps everything (leaving the End after the credits).
 * The rewards are a list on the player (`LivingEntityMixin`); vanilla `restoreFrom` copies the effect but not
 * the list, and the re-added effect filled the new player's empty list with plain waves, so the boss bonus and
 * the waves' own loot were lost. `PlayerEvent.Clone` comes after `restoreFrom` has re-added the effects.
 */
object HeroRewardsClone {
    fun register() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.Clone::class.java) { event ->
            if (event.isWasDeath) return@addListener // effects are not kept on death
            val original = event.original as? GardenHeroRewards ?: return@addListener
            (event.entity as? GardenHeroRewards)?.`plantz$setWaveList`(original.`plantz$getWaveList`())
        }
    }
}

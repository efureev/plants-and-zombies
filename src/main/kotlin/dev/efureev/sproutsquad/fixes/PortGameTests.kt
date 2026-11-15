package dev.efureev.sproutsquad.fixes

import com.mojang.authlib.GameProfile
import joshxviii.plantz.neoforge.PazSyncedData
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.storage.TagValueInput
import java.util.UUID

/** Bugs of the NeoForge port layer itself (`joshxviii.plantz.neoforge`, `mixin.neoforge`). */
object PortGameTests {
    /**
     * A saved player loads: the server reads the player's save before the player has a connection
     * (`PrepareSpawnTask`), and restoring the mod's synced flags tried to send them to that missing
     * connection. Every saved world failed to load with "Invalid player data".
     */
    fun savedPlayerLoads(helper: GameTestHelper) {
        val level = helper.level
        // as PrepareSpawnTask creates it: not in the world yet, no connection
        val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "sprout_load_test"), ClientInformation.createDefault())
        val save = CompoundTag().apply {
            putBoolean("sproutsquad:IsChilled", true)
            putBoolean("sproutsquad:IsButtered", false)
        }
        try {
            player.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), save))
        } catch (e: RuntimeException) {
            helper.fail("loading a saved player failed: ${e.cause ?: e}")
        }
        helper.assertTrue(PazSyncedData.CHILLED.get(player), "the saved chill flag was not restored")
        helper.assertFalse(PazSyncedData.BUTTERED.get(player), "the butter flag was restored wrong")
        helper.succeed()
    }
}

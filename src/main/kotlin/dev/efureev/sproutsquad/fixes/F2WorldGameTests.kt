package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazEntities
import joshxviii.plantz.entity.zombie.Imp
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.UUID
import net.minecraft.resources.ResourceKey
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.EventHooks

/**
 * GameTests for the last F2 items: Zombie Goat spawns ([ZombieGoatSpawns]) and the balloon
 * flight kick ([BalloonFlight]). Registered in [dev.efureev.sproutsquad.debug.SquadGameTests].
 */
object F2WorldGameTests {
    private const val GOATS = 60

    /** Mobs a biome spawns in [category]; in 26.x the lists live in the biome's environment attributes. */
    fun biomeSpawns(server: MinecraftServer, biome: ResourceKey<Biome>, category: MobCategory): List<EntityType<*>> {
        val value = server.registryAccess().lookupOrThrow(Registries.BIOME).getValue(biome) ?: return emptyList()
        val spawns = value.attributes.applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY)
        return spawns.getMobsToSpawn(category).unwrap().map { it.value().type() }
    }

    private fun monsterSpawns(helper: GameTestHelper, biome: ResourceKey<Biome>) = biomeSpawns(helper.level.server, biome, MobCategory.MONSTER)

    /** Goats are in the mountain spawn lists; a natural spawn often brings an Imp rider, a spawn egg never. */
    fun zombieGoat(helper: GameTestHelper) {
        val problems = mutableListOf<String>()
        if (PazEntities.ZOMBIE_GOAT !in monsterSpawns(helper, Biomes.SNOWY_SLOPES)) problems += "snowy_slopes has no zombie goat spawn"
        for (biome in listOf(Biomes.PLAINS, Biomes.MEADOW, Biomes.CHERRY_GROVE)) {
            if (PazEntities.ZOMBIE_GOAT in monsterSpawns(helper, biome)) problems += "${biome.identifier().path} spawns zombie goats"
        }
        // the code-defined spawns of the original (PazSpawnsBiomeModifier) must reach the 26.3 biome attribute too
        if (PazEntities.BROWN_COAT !in monsterSpawns(helper, Biomes.PLAINS)) problems += "plains have no browncoat spawn"

        val level = helper.level
        // open air beside the structure: inside its barrier box an Imp would not fit above the goat
        val pos = helper.absolutePos(BlockPos(0, 1, 5))
        val difficulty = level.getCurrentDifficultyAt(pos)
        fun riders(reason: EntitySpawnReason): Int = (1..GOATS).count {
            val goat = PazEntities.ZOMBIE_GOAT.create(level, reason)!!
            goat.snapTo(Vec3.atBottomCenterOf(pos), 0f, 0f)
            EventHooks.finalizeMobSpawn(goat, level, difficulty, reason, null)
            val rider = goat.passengers.firstOrNull()
            goat.passengers.forEach { it.discard() }
            goat.discard()
            rider is Imp
        }
        val natural = riders(EntitySpawnReason.NATURAL)
        if (natural == 0 || natural == GOATS) problems += "$natural of $GOATS natural goats got an Imp rider, expected some"
        val eggs = riders(EntitySpawnReason.SPAWN_ITEM_USE)
        if (eggs != 0) problems += "$eggs goats from spawn eggs got a rider"

        if (problems.isNotEmpty()) helper.fail(problems.joinToString("; ")) else helper.succeed()
    }

    /**
     * A player floating on a balloon is not counted towards the "flying" kick; without one they are.
     * The player gets a connection but does not join the server: a joining mock player would be sent
     * the mod's config payload, which the test connection did not negotiate.
     */
    fun balloonFlight(helper: GameTestHelper) {
        val server = helper.level.server
        val cookie = CommonListenerCookie.createInitial(GameProfile(UUID.randomUUID(), "balloon-test"), false)
        val player = ServerPlayer(server, helper.level, cookie.gameProfile(), cookie.clientInformation())
        val connection = Connection(PacketFlow.SERVERBOUND)
        EmbeddedChannel(connection)
        ServerGamePacketListenerImpl(server, connection, player, cookie)
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(BlockPos(0, 4, 0))), 0f, 0f)
        val balloon = PazEntities.BALLOON.create(helper.level, EntitySpawnReason.COMMAND)!!
        try {
            player.connection.aboveGroundTickCount = BalloonFlight.CHECK_AFTER_TICKS + 5
            player.doTick()
            helper.assertTrue(player.connection.aboveGroundTickCount == BalloonFlight.CHECK_AFTER_TICKS + 5, "the counter was reset without a balloon")

            // a balloon at the player's height does not lift: still counted
            balloon.snapTo(player.position().add(0.0, 1.0, 0.0), 0f, 0f)
            helper.level.addFreshEntity(balloon)
            balloon.setLeashedTo(player, true)
            player.doTick()
            helper.assertTrue(player.connection.aboveGroundTickCount == BalloonFlight.CHECK_AFTER_TICKS + 5, "a balloon that does not lift reset the counter")

            balloon.snapTo(player.position().add(0.0, 3.0, 0.0), 0f, 0f)
            player.doTick()
            val counter = player.connection.aboveGroundTickCount
            helper.assertTrue(counter == 0, "the counter was not reset while a balloon lifts the player ($counter)")
        } finally {
            balloon.discard()
            connection.disconnect(Component.literal("test done"))
        }
        helper.succeed()
    }
}

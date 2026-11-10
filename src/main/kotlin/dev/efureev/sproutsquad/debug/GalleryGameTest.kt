package dev.efureev.sproutsquad.debug

import joshxviii.plantz.PazMain
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.FunctionGameTestInstance
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestData
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.function.Consumer

/**
 * GameTest for `/pz gallery`: builds the gallery in the test world, checks that every exhibit
 * spawned frozen, then clears it. Runs with `./gradlew runGameTestServer`.
 */
object GalleryGameTest {
    private val ID: Identifier = Identifier.fromNamespaceAndPath(PazMain.MODID, "gallery")
    private val FUNCTION: ResourceKey<Consumer<GameTestHelper>> = ResourceKey.create(Registries.TEST_FUNCTION, ID)

    /** Lower bound: the mod has 34 plants and 20+ zombies, plus variants. */
    private const val MIN_EXHIBITS = 60

    fun registerFunction(event: RegisterEvent) {
        event.register(Registries.TEST_FUNCTION, ID) { Consumer<GameTestHelper>(::run) }
    }

    fun registerTest(event: RegisterGameTestsEvent) {
        val environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(PazMain.MODID, "default"))
        event.registerTest(ID, FunctionGameTestInstance(FUNCTION, TestData(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true)))
    }

    private fun run(helper: GameTestHelper) {
        val level = helper.level
        val pos = helper.absolutePos(BlockPos(0, 1, 0))
        val source = level.server.createCommandSourceStack()
            .withLevel(level)
            .withPosition(Vec3.atBottomCenterOf(pos))
            .withRotation(Vec2(0f, 0f))
            .withSuppressedOutput()
        val commands = level.server.commands

        commands.performPrefixedCommand(source, "pz gallery")
        val exhibits = level.allEntities.filter { GalleryCommand.TAG in it.entityTags() }.toList()
        if (exhibits.size < MIN_EXHIBITS) {
            helper.fail("gallery spawned only ${exhibits.size} exhibits, expected at least $MIN_EXHIBITS")
            return
        }
        val unfrozen = exhibits.filter { it is net.minecraft.world.entity.Mob && !it.isNoAi }
        if (unfrozen.isNotEmpty()) {
            helper.fail("exhibits with AI enabled: ${unfrozen.joinToString { it.type.toShortString() }}")
            return
        }
        PazMain.LOGGER.info("Gallery GameTest: {} exhibits", exhibits.size)

        commands.performPrefixedCommand(source, "pz gallery clear")
        val left = level.allEntities.count { GalleryCommand.TAG in it.entityTags() && !it.isRemoved }
        if (left != 0) {
            helper.fail("gallery clear left $left exhibits")
            return
        }
        helper.succeed()
    }
}

package dev.efureev.sproutsquad.debug

import joshxviii.plantz.PazMain
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3

/**
 * GameTest for `/pz gallery`: builds the gallery in the test world, checks that every exhibit
 * spawned frozen, then clears it. Registered in [SquadGameTests].
 *
 * The test's structure is `gametest/gallery_area`, an empty template as large as the gallery: the
 * runner loads every chunk under a test's structure before the test starts, and an exhibit added
 * to a chunk whose entities are not loaded is missing from `level.allEntities`. With the 1x1x1
 * "empty" structure the test failed whenever its position was near the southern edge of a chunk.
 * When the roster outgrows the area the test says so; enlarge the template's size, for example
 * `pictures.write_nbt(path, {"size": [56, 8, 96], "entities": [], "blocks": [],
 * "palette": [{"id": "minecraft:air"}], "DataVersion": 5023})` with `tools/docs/pictures.py`.
 */
object GalleryGameTest {
    /** Lower bound: the mod has 34 plants and 20+ zombies, plus variants. */
    private const val MIN_EXHIBITS = 60

    /**
     * Where the command runs, facing south in the template: the rows grow to the south and each
     * row to the west (GalleryCommand), so it stands at the north-eastern corner of the area.
     */
    private val COMMAND_POS = BlockPos(52, 1, 0)

    fun run(helper: GameTestHelper) {
        val level = helper.level
        val pos = helper.absolutePos(COMMAND_POS)
        // "south in the template" turns with the test (`/test run ... <rotation>`), like the area
        val facing = helper.testRotation.rotate(Direction.SOUTH)
        val source = level.server.createCommandSourceStack()
            .withLevel(level)
            .withPosition(Vec3.atBottomCenterOf(pos))
            .withRotation(Vec2(0f, facing.toYRot()))
            .withSuppressedOutput()
        val commands = level.server.commands

        commands.performPrefixedCommand(source, "pz gallery")
        val exhibits = level.allEntities.filter { GalleryCommand.TAG in it.entityTags() }.toList()
        if (exhibits.size < MIN_EXHIBITS) helper.fail("gallery spawned only ${exhibits.size} exhibits, expected at least $MIN_EXHIBITS")
        val outside = exhibits.filter { !helper.bounds.contains(it.position()) }
        if (outside.isNotEmpty()) helper.fail("the gallery outgrew gametest/gallery_area (${outside.size} exhibits outside it): enlarge the template")
        val unfrozen = exhibits.filter { it is net.minecraft.world.entity.Mob && !it.isNoAi }
        if (unfrozen.isNotEmpty()) helper.fail("exhibits with AI enabled: ${unfrozen.joinToString { it.type.toShortString() }}")
        PazMain.LOGGER.info("Gallery GameTest: {} exhibits", exhibits.size)

        commands.performPrefixedCommand(source, "pz gallery clear")
        val left = level.allEntities.count { GalleryCommand.TAG in it.entityTags() && !it.isRemoved }
        if (left != 0) helper.fail("gallery clear left $left exhibits")
        helper.succeed()
    }
}

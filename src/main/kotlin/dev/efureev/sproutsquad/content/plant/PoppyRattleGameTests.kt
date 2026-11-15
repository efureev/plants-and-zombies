package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntitySpawnReason

/** GameTests of the Poppy Rattle (docs/en/characters/poppy_rattle.md). Registered in SquadGameTests. */
object PoppyRattleGameTests {
    /** Health, price, planting limit, never asleep, and the plants it never hands out. */
    fun stats(helper: GameTestHelper) {
        val type = SquadEntities.POPPY_RATTLE.get()
        val rattle = type.create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Poppy Rattle does not spawn")
        helper.assertValueEqual(rattle.maxHealth, 16f, "max health")
        helper.assertValueEqual(PazConfig.getSunCost(type), 6, "sun cost")
        helper.assertValueEqual(PazConfig.getEntityLimit(type), 4, "planting limit")
        helper.assertFalse(rattle.sleepsDuringDay(), "sleeps by day")
        helper.assertFalse(rattle.sleepsDuringNight(), "sleeps at night")
        listOf(PazEntities.MARIGOLD, PazEntities.CITRON, PazEntities.ROYAL_SUNFLOWER, type).forEach {
            helper.assertTrue(it.builtInRegistryHolder().`is`(PoppyRattle.EXCLUDED), "$it is excluded")
        }
        helper.succeed()
    }
}

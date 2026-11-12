package joshxviii.plantz.neoforge

import com.mojang.serialization.MapCodec
import joshxviii.plantz.PazSpawnPlacements
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.level.biome.Biome
import net.neoforged.neoforge.common.world.BiomeModifier
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo

/**
 * Adds every spawn declared through [PazSpawnPlacements.addBiomeSpawn] to the biomes in its tag
 * (Fabric: BiomeModifications.addSpawn). Enabled by `data/sproutsquad/neoforge/biome_modifier/spawns.json`.
 */
object PazSpawnsBiomeModifier : BiomeModifier {
    val CODEC: MapCodec<PazSpawnsBiomeModifier> = MapCodec.unit(this)

    override fun modify(registries: RegistryAccess, biome: Holder<Biome>, phase: BiomeModifier.Phase, builder: ModifiableBiomeInfo.BiomeInfo.Builder) {
        if (phase != BiomeModifier.Phase.ADD) return
        val spawns = builder.mobSpawnSettings
        for (spawn in PazSpawnPlacements.allBiomeSpawns) {
            if (!biome.`is`(spawn.biomeTag)) continue
            spawns.addSpawn(spawn.entityType, spawn.category, spawn.weight, UniformInt.of(spawn.minGroupSize, spawn.maxGroupSize))
        }
    }

    override fun codec(): MapCodec<out BiomeModifier> = CODEC
}

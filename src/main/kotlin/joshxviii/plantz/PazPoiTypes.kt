package joshxviii.plantz

import net.minecraft.core.Registry

import com.google.common.collect.ImmutableSet
import joshxviii.plantz.PazBlocks.PLANTZ_FLAG_BLOCK
import joshxviii.plantz.PazBlocks.SUN_BATTERY_BLOCK
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.ai.village.poi.PoiType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

object PazPoiTypes {

    @JvmField val SUN_BATTERY = createKey("sun_battery")
    @JvmField val PLANTZ_FLAG = createKey("plantz_flag")

    private fun createKey(name: String): ResourceKey<PoiType> {
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, pazResource(name))
    }

    private fun getBlockStates(block: Block): MutableSet<BlockState> {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates())
    }

    fun initialize() {
        val r = BuiltInRegistries.POINT_OF_INTEREST_TYPE
        Registry.register(r, SUN_BATTERY, PoiType(getBlockStates(SUN_BATTERY_BLOCK), 1, 1))
        Registry.register(r, PLANTZ_FLAG, PoiType(getBlockStates(PLANTZ_FLAG_BLOCK), 1, 1))
    }
}
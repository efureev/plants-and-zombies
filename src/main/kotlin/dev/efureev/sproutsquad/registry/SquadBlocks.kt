package dev.efureev.sproutsquad.registry

import dev.efureev.sproutsquad.content.block.NeedleSpruceStumpBlock
import joshxviii.plantz.PazMain
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.UnaryOperator

/** Sprout Squad's own blocks, registered through DeferredRegister (see docs/en/WORKSHOP.md). */
object SquadBlocks {
    val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(PazMain.MODID)

    /** What a Needle Spruce leaves where it stood when it dies; no item of its own. */
    val NEEDLE_SPRUCE_STUMP: DeferredBlock<NeedleSpruceStumpBlock> = BLOCKS.registerBlock("needle_spruce_stump", ::NeedleSpruceStumpBlock, UnaryOperator {
        it.mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(1.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
    })

    fun register(modBus: IEventBus) {
        BLOCKS.register(modBus)
    }
}

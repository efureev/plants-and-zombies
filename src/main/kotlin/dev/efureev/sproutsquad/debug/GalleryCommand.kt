package dev.efureev.sproutsquad.debug

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.zombie.AllStarVariant
import joshxviii.plantz.entity.zombie.BrownCoatVariant
import joshxviii.plantz.entity.zombie.ElectricSlideVariant
import joshxviii.plantz.entity.zombie.GargantuarVariant
import joshxviii.plantz.entity.zombie.ImpVariant
import joshxviii.plantz.entity.zombie.RoboZombieVariant
import joshxviii.plantz.entity.zombie.SuperBrainzVariant
import joshxviii.plantz.item.ZombieSpawnEggs
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.StringRepresentable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import kotlin.math.ceil
import kotlin.math.max

/**
 * `/pz gallery [labels]` — lines up every plant, zombie (with variants and loadouts) and other
 * mob of the mod in front of the player, frozen (no AI, invulnerable, persistent), for screenshots
 * and art review. `/pz gallery clear` removes it again (placed pedestal blocks stay).
 */
object GalleryCommand {
    const val TAG = "sproutsquad_gallery"
    private const val PER_ROW = 10
    private const val ROW_GAP = 6

    private class Exhibit(val type: EntityType<*>, val egg: ItemStack?, val label: Component)

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("pz").then(
                Commands.literal("gallery")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes { build(it, labels = true) }
                    .then(Commands.argument("labels", BoolArgumentType.bool())
                        .executes { build(it, BoolArgumentType.getBool(it, "labels")) })
                    .then(Commands.literal("clear").executes(::clear))
            )
        )
    }

    private fun build(ctx: CommandContext<CommandSourceStack>, labels: Boolean): Int {
        val source = ctx.source
        val level = source.level
        clearIn(level)

        val (plants, zombies, others) = collect(level)
        val forward = Direction.fromYRot(source.rotation.y.toDouble())
        val right = forward.clockWise
        val origin = BlockPos.containing(source.position).relative(forward, 4)
        val facingYaw = forward.opposite.toYRot()

        var row = 0
        for (group in listOf(plants, zombies, others)) {
            group.chunked(PER_ROW).forEach { line ->
                var offset = 0
                for (exhibit in line) {
                    val step = spacing(level, exhibit.type)
                    val pos = origin.relative(forward, row * ROW_GAP).relative(right, offset + step / 2)
                    place(level, exhibit, pos, facingYaw, labels)
                    offset += step
                }
                row++
            }
        }

        source.sendSuccess({
            Component.translatable("commands.sproutsquad.gallery.success", plants.size, zombies.size, others.size)
        }, true)
        return plants.size + zombies.size + others.size
    }

    private fun clear(ctx: CommandContext<CommandSourceStack>): Int {
        val removed = clearIn(ctx.source.level)
        ctx.source.sendSuccess({ Component.translatable("commands.sproutsquad.gallery.cleared", removed) }, true)
        return removed
    }

    private fun clearIn(level: ServerLevel): Int {
        val gallery = level.allEntities.filter { TAG in it.entityTags() }.toList()
        gallery.forEach { it.discard() }
        return gallery.size
    }

    /** Every mob type of the mod, sorted into plants / zombies / others, plus zombie variant and loadout eggs. */
    private fun collect(level: ServerLevel): Triple<List<Exhibit>, List<Exhibit>, List<Exhibit>> {
        val plants = mutableListOf<Exhibit>()
        val zombies = mutableListOf<Exhibit>()
        val others = mutableListOf<Exhibit>()
        val variantEggs = variantEggs()

        for (type in BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).namespace != PazMain.MODID) continue
            val probe = type.create(level, EntitySpawnReason.COMMAND) ?: continue
            val kind = when (probe) {
                is Plant -> plants
                is Zombie -> zombies
                is Mob -> others
                else -> null
            }
            probe.discard()
            if (kind == null) continue
            val eggs = variantEggs[type]
            kind += Exhibit(type, eggs?.first, type.description)
            eggs?.second?.forEach { egg ->
                kind += Exhibit(type, egg, egg.get(DataComponents.ITEM_NAME) ?: type.description)
            }
        }
        return Triple(plants, zombies, others)
    }

    /** type -> (egg forcing the default variant, eggs for the other variants / loadouts). */
    private fun variantEggs(): Map<EntityType<*>, Pair<ItemStack, List<ItemStack>>> {
        fun <V> base(type: EntityType<*>, item: Item, values: Collection<V>): ItemStack
            where V : Enum<V>, V : StringRepresentable =
            ZombieSpawnEggs.egg(type, item) { variant(values.first()) }

        return mapOf(
            PazEntities.BROWN_COAT to (base(PazEntities.BROWN_COAT, PazItems.BROWN_COAT_SPAWN_EGG, BrownCoatVariant.entries) to ZombieSpawnEggs.brownCoat()),
            PazEntities.IMP to (base(PazEntities.IMP, PazItems.IMP_SPAWN_EGG, ImpVariant.entries) to ZombieSpawnEggs.imp()),
            PazEntities.GARGANTUAR to (base(PazEntities.GARGANTUAR, PazItems.GARGANTUAR_SPAWN_EGG, GargantuarVariant.entries) to ZombieSpawnEggs.gargantuar()),
            PazEntities.SUPER_BRAINZ to (base(PazEntities.SUPER_BRAINZ, PazItems.SUPER_BRAINZ_SPAWN_EGG, SuperBrainzVariant.entries) to ZombieSpawnEggs.superBrainz()),
            PazEntities.ALL_STAR to (base(PazEntities.ALL_STAR, PazItems.ALL_STAR_SPAWN_EGG, AllStarVariant.entries) to ZombieSpawnEggs.allStar()),
            PazEntities.ROBO_ZOMBIE to (base(PazEntities.ROBO_ZOMBIE, PazItems.ROBO_ZOMBIE_SPAWN_EGG, RoboZombieVariant.entries) to ZombieSpawnEggs.roboZombie()),
            PazEntities.ELECTRIC_SLIDE to (base(PazEntities.ELECTRIC_SLIDE, PazItems.ELECTRIC_SLIDE_SPAWN_EGG, ElectricSlideVariant.entries) to ZombieSpawnEggs.electricSlide()),
        )
    }

    private fun spacing(level: ServerLevel, type: EntityType<*>): Int =
        max(3, ceil(type.dimensions.width().toDouble()).toInt() + 2)

    private fun place(level: ServerLevel, exhibit: Exhibit, pos: BlockPos, yaw: Float, labels: Boolean) {
        @Suppress("UNCHECKED_CAST")
        val type = exhibit.type as EntityType<Entity>
        val processor = exhibit.egg?.let { EntityType.createDefaultStackConfig<Entity>(level, it, null) }
        val entity = type.create(level, processor, pos, EntitySpawnReason.SPAWN_ITEM_USE, false, false) ?: return

        // pedestal: grass for plants (farmland for water plants), smooth stone for everyone else
        val pedestal = when {
            entity is Plant && entity.canSurviveOn(Blocks.GRASS_BLOCK.defaultBlockState()) -> Blocks.GRASS_BLOCK
            entity is Plant -> Blocks.FARMLAND
            else -> Blocks.SMOOTH_STONE
        }
        level.setBlock(pos.below(), pedestal.defaultBlockState(), Block.UPDATE_ALL)
        for (dy in 0..4) {
            val air = pos.above(dy)
            if (!level.getBlockState(air).isAir) level.setBlock(air, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
        }

        entity.snapTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5, yaw, 0f)
        (entity as? Zombie)?.isBaby = false
        (listOf(entity) + entity.indirectPassengers).forEach { freeze(it, yaw) }
        if (labels) {
            entity.customName = exhibit.label
            entity.isCustomNameVisible = true
        }
        level.addFreshEntityWithPassengers(entity)
    }

    private fun freeze(entity: Entity, yaw: Float) {
        entity.addTag(TAG)
        entity.isPermanentlyInvulnerable = true
        entity.isSilent = true
        if (entity is Mob) {
            entity.isNoAi = true
            entity.setPersistenceRequired()
            entity.yHeadRot = yaw
            entity.yBodyRot = yaw
        }
    }
}

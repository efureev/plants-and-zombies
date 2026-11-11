package dev.efureev.sproutsquad.structures

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import joshxviii.plantz.PazMain
import joshxviii.plantz.block.MailboxBlock
import joshxviii.plantz.block.MailboxState
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Items
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * GameTests for the mod's structures (docs/en/STRUCTURES.md). Registered in
 * [dev.efureev.sproutsquad.debug.SquadGameTests].
 */
object StructuresGameTests {
    private fun id(path: String) = Identifier.fromNamespaceAndPath(PazMain.MODID, path)

    /**
     * Every structure template of the mod: no unknown blocks (an id that no longer exists loads as
     * air, for example after a cherry-pick without `tools/upstream/nbt_rename.py`), every container
     * with a loot table points to an existing table; every pool points to existing templates and
     * every structure has biomes.
     */
    fun templates(helper: GameTestHelper) {
        val server = helper.level.server
        val problems = mutableListOf<String>()
        val templates = server.resourceManager.listResources("structure") { it.path.endsWith(".nbt") }
            .filterKeys { it.namespace == PazMain.MODID }
        if (templates.isEmpty()) problems += "no structure templates found"
        for ((file, resource) in templates) {
            val tag = resource.open().use { NbtIo.readCompressed(it, NbtAccounter.unlimitedHeap()) }
            val name = file.path.removePrefix("structure/").removeSuffix(".nbt")
            problems += templateProblems(helper, name, tag)
        }

        val ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)
        val pools = server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
        for ((key, pool) in pools.entrySet()) {
            if (key.identifier().namespace != PazMain.MODID) continue
            for (element in pool.templates.map { it.first }) {
                val json = StructurePoolElement.CODEC.encodeStart(ops, element).result().orElse(null) ?: continue
                for (location in locations(json)) {
                    if (helper.level.structureTemplateManager.get(Identifier.parse(location)).isEmpty)
                        problems += "pool ${key.identifier()} names a missing template $location"
                }
            }
        }
        val structures = server.registryAccess().lookupOrThrow(Registries.STRUCTURE)
        for ((key, structure) in structures.entrySet()) {
            if (key.identifier().namespace == PazMain.MODID && structure.biomes().size() == 0)
                problems += "structure ${key.identifier()} has no biomes"
        }
        if (problems.isNotEmpty()) helper.fail(problems.joinToString("; "))
        helper.succeed()
    }

    private fun templateProblems(helper: GameTestHelper, name: String, tag: CompoundTag): List<String> {
        val problems = mutableListOf<String>()
        val loot = helper.level.server.reloadableRegistries()
        for (entry in tag.getListOrEmpty("palette")) {
            val state = entry as? CompoundTag ?: continue
            val block = state.getString("id").or { state.getString("Name") }.orElse("")
            if (BuiltInRegistries.BLOCK.getOptional(Identifier.parse(block)).isEmpty) problems += "$name: unknown block $block"
        }
        for (entry in tag.getListOrEmpty("blocks")) {
            val nbt = (entry as? CompoundTag)?.getCompoundOrEmpty("nbt") ?: continue
            val table = nbt.getString("LootTable").orElse(null) ?: continue
            if (loot.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table))) == LootTable.EMPTY)
                problems += "$name: missing loot table $table"
        }
        return problems
    }

    /** The template locations a pool element encodes, wherever they sit (single, list or feature elements). */
    private fun locations(json: JsonElement): List<String> = when {
        json.isJsonObject -> json.asJsonObject.entrySet().flatMap { (key, value) ->
            if (key == "location" && value.isJsonPrimitive) listOf(value.asString) else locations(value)
        }
        json.isJsonArray -> json.asJsonArray.flatMap { locations(it) }
        else -> emptyList()
    }

    private val GARDEN_SHACK_VARIANTS = listOf("oak", "spruce", "cherry")

    /**
     * The Garden Shack: every wood variant is placed as the world generation places it, and has
     * its wild plants, its loot tables and the Gardener's Letter in the Mailbox; the structure,
     * its biomes, the cartographer's map trade and the advancement are registered.
     */
    fun gardenShack(helper: GameTestHelper) {
        val level = helper.level
        val problems = mutableListOf<String>()
        // the test's arena is the oak shack (SquadGameTests): its area is loaded; every variant is placed over it
        val origin = helper.absolutePos(BlockPos.ZERO)
        for (wood in GARDEN_SHACK_VARIANTS) {
            val template = level.structureTemplateManager.get(id("garden_shack/$wood")).orElse(null)
            if (template == null) {
                problems += "no template garden_shack/$wood"
                continue
            }
            val box = AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(origin.offset(template.size)))
            level.getEntitiesOfClass(Plant::class.java, box).forEach { it.discard() }
            val settings = StructurePlaceSettings().setFinalizeEntities(true)
            template.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS)
            problems += shackProblems(helper, wood, origin, box)
        }
        problems += registrationProblems(helper)
        if (problems.isNotEmpty()) helper.fail(problems.joinToString("; "))
        helper.succeed()
    }

    private fun registrationProblems(helper: GameTestHelper): List<String> {
        val level = helper.level
        val problems = mutableListOf<String>()
        val registries = level.registryAccess()
        val structure = registries.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id("garden_shack"))).orElse(null)
        if (structure == null) problems += "structure sproutsquad:garden_shack is not registered"
        else {
            val plains = registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)
            if (!structure.value().biomes().contains(plains)) problems += "the Garden Shack does not stand in plains"
        }
        if (registries.lookupOrThrow(Registries.STRUCTURE_SET).get(ResourceKey.create(Registries.STRUCTURE_SET, id("garden_shack"))).isEmpty)
            problems += "no structure set for the Garden Shack"
        val trades = registries.lookupOrThrow(Registries.VILLAGER_TRADE)
        val mapTrade = ResourceKey.create(Registries.VILLAGER_TRADE, id("cartographer/garden_shack_map"))
        val cartographer = TagKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("cartographer/level_1"))
        val tagged = trades.get(cartographer).map { set -> set.any { it.`is`(mapTrade) } }.orElse(false)
        if (!tagged) problems += "the cartographer does not sell the Garden Shack map"
        if (level.server.advancements.get(id("find_garden_shack")) == null) problems += "no advancement find_garden_shack"
        return problems
    }

    private fun shackProblems(helper: GameTestHelper, wood: String, origin: BlockPos, box: AABB): List<String> {
        val level = helper.level
        val problems = mutableListOf<String>()
        fun lootTable(pos: BlockPos) = (level.getBlockEntity(origin.offset(pos)) as? RandomizableContainerBlockEntity)?.lootTable?.identifier()?.path

        if (lootTable(BlockPos(3, 1, 3)) != "chests/garden_shack/shed") problems += "$wood: the shed chest has loot ${lootTable(BlockPos(3, 1, 3))}"
        for (y in 1..2) {
            if (lootTable(BlockPos(7, y, 5)) != "chests/garden_shack/barrel") problems += "$wood: barrel $y has loot ${lootTable(BlockPos(7, y, 5))}"
        }
        if (!level.getBlockState(origin.offset(10, 0, 8)).`is`(Blocks.WATER)) problems += "$wood: no water between the beds"

        val mailboxPos = origin.offset(12, 1, 18)
        val mailbox = level.getBlockEntity(mailboxPos) as? MailboxBlockEntity
        val letter = mailbox?.let { box -> (0 until box.containerSize).map { box.getItem(it) }.firstOrNull { !it.isEmpty } }
        val name = (letter?.get(DataComponents.CUSTOM_NAME)?.contents as? TranslatableContents)?.key
        if (letter == null || !letter.`is`(Items.WRITTEN_BOOK)) problems += "$wood: no letter in the mailbox"
        else {
            if (name != "item.sproutsquad.gardener_letter") problems += "$wood: the letter is named $name"
            if (letter.get(DataComponents.WRITTEN_BOOK_CONTENT)?.pages()?.size != 3) problems += "$wood: the letter has no three pages"
        }
        if (level.getBlockState(mailboxPos).getValue(MailboxBlock.STATE) != MailboxState.HAS_MAIL) problems += "$wood: the mailbox shows no mail"

        val plants = level.getEntitiesOfClass(Plant::class.java, box)
        if (plants.size != 4) problems += "$wood: ${plants.size} plants instead of 4"
        if (plants.any { it.isTame }) problems += "$wood: a plant is tame"
        if (plants.any { !it.isPersistenceRequired }) problems += "$wood: a plant may despawn"
        plants.forEach { it.discard() }
        return problems
    }
}

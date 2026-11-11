package dev.efureev.sproutsquad.structures

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import dev.efureev.sproutsquad.machines.PlayerMachines
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.block.MailboxBlock
import joshxviii.plantz.block.MailboxState
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.entity.blueprint_machines.ElectroTurret
import joshxviii.plantz.entity.blueprint_machines.ZombieTurret
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.zombie.ZombieRobot
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
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.entity.SpawnPlacements
import net.minecraft.world.item.Items
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
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
        problems += registrationProblems(helper, "garden_shack", Biomes.PLAINS, tradeLevel = 1)
        if (problems.isNotEmpty()) helper.fail(problems.joinToString("; "))
        helper.succeed()
    }

    /**
     * Structure [name] is registered with its structure set and stands in [biome]; a cartographer of
     * [tradeLevel] sells the map to it (`cartographer/<name>_map`) and finding it is the advancement
     * `find_<name>`.
     */
    private fun registrationProblems(helper: GameTestHelper, name: String, biome: ResourceKey<Biome>, tradeLevel: Int): List<String> {
        val level = helper.level
        val problems = mutableListOf<String>()
        val registries = level.registryAccess()
        val structure = registries.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id(name))).orElse(null)
        if (structure == null) problems += "structure sproutsquad:$name is not registered"
        else {
            val holder = registries.lookupOrThrow(Registries.BIOME).getOrThrow(biome)
            if (!structure.value().biomes().contains(holder)) problems += "$name does not stand in ${biome.identifier()}"
        }
        if (registries.lookupOrThrow(Registries.STRUCTURE_SET).get(ResourceKey.create(Registries.STRUCTURE_SET, id(name))).isEmpty)
            problems += "no structure set for $name"
        val trades = registries.lookupOrThrow(Registries.VILLAGER_TRADE)
        val mapTrade = ResourceKey.create(Registries.VILLAGER_TRADE, id("cartographer/${name}_map"))
        val cartographer = TagKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("cartographer/level_$tradeLevel"))
        val tagged = trades.get(cartographer).map { set -> set.any { it.`is`(mapTrade) } }.orElse(false)
        if (!tagged) problems += "a level $tradeLevel cartographer does not sell the map to $name"
        if (level.server.advancements.get(id("find_$name")) == null) problems += "no advancement find_$name"
        return problems
    }

    private val JUNKYARD_CHEST = BlockPos(20, 1, 15)
    private val JUNKYARD_BARRELS = listOf(BlockPos(20, 1, 17), BlockPos(10, 1, 3), BlockPos(23, 1, 12))
    private val JUNKYARD_ELECTRO_TURRET = BlockPos(17, 1, 22)
    private const val LOOT_ROLLS = 50

    /**
     * The Robo Junkyard: placed as the world generation places it, it has its loot tables and two
     * hostile turrets that never despawn; the workshop chest always holds a blueprint of a machine
     * and Brainz Alloy; the zombies it brings at night have spawn rules; the structure, its biomes,
     * the cartographer's map trade and the advancement are registered. Then a plant is put next to
     * the electro turret by the door, and the turret must take it as its target: the turrets of the
     * template are awake.
     */
    fun roboJunkyard(helper: GameTestHelper) {
        val level = helper.level
        val problems = mutableListOf<String>()
        // the test's arena is the junkyard itself (SquadGameTests): its area is loaded
        val origin = helper.absolutePos(BlockPos.ZERO)
        val template = level.structureTemplateManager.get(id("robo_junkyard/yard")).orElse(null)
            ?: return helper.fail("no template robo_junkyard/yard")
        val box = AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(origin.offset(template.size)))
        level.getEntitiesOfClass(ZombieRobot::class.java, box).forEach { it.discard() }
        template.placeInWorld(level, origin, origin, StructurePlaceSettings().setFinalizeEntities(true), level.random, Block.UPDATE_CLIENTS)

        fun lootTable(pos: BlockPos) = (level.getBlockEntity(origin.offset(pos)) as? RandomizableContainerBlockEntity)?.lootTable?.identifier()?.path
        if (lootTable(JUNKYARD_CHEST) != "chests/robo_junkyard/workshop") problems += "the workshop chest has loot ${lootTable(JUNKYARD_CHEST)}"
        for (pos in JUNKYARD_BARRELS) {
            if (lootTable(pos) != "chests/robo_junkyard/scrap") problems += "the barrel at ${pos.toShortString()} has loot ${lootTable(pos)}"
        }

        val turrets = level.getEntitiesOfClass(ZombieTurret::class.java, box)
        val electroTurrets = level.getEntitiesOfClass(ElectroTurret::class.java, box)
        if (turrets.size != 1 || electroTurrets.size != 1) problems += "${turrets.size} turrets and ${electroTurrets.size} electro turrets instead of one each"
        val guards = turrets + electroTurrets
        if (guards.any { PlayerMachines.isPlayerMachine(it) }) problems += "a turret of the junkyard is a player's machine"
        if (guards.any { !it.isPersistenceRequired }) problems += "a turret of the junkyard may despawn"

        problems += junkyardLootProblems(helper, origin)
        for (type in listOf(PazEntities.ENGINEER_ZOMBIE, PazEntities.SOLDIER_ZOMBIE, PazEntities.ROBO_ZOMBIE)) {
            if (SpawnPlacements.getPlacementType(type) != SpawnPlacementTypes.ON_GROUND) problems += "${type.toShortString()} has no spawn rules"
        }
        problems += registrationProblems(helper, "robo_junkyard", Biomes.BADLANDS, tradeLevel = 3)
        if (problems.isNotEmpty()) return helper.fail(problems.joinToString("; "))

        val electro = electroTurrets.single()
        val plant = helper.spawn(PazEntities.WALL_NUT, JUNKYARD_ELECTRO_TURRET.south(2))
        helper.startSequence()
            .thenWaitUntil {
                helper.assertTrue(level.isPositionEntityTicking(electro.blockPosition()), "the junkyard does not tick entities yet")
                helper.assertTrue(electro.target == plant, "the electro turret targets ${electro.target?.type?.toShortString()} instead of the plant")
            }
            .thenExecute {
                plant.discard()
                level.getEntitiesOfClass(ZombieRobot::class.java, box).forEach { it.discard() }
            }
            .thenSucceed()
    }

    /** The workshop chest always gives one blueprint of a machine and Brainz Alloy; the barrels give something. */
    private fun junkyardLootProblems(helper: GameTestHelper, origin: BlockPos): List<String> {
        val level = helper.level
        val problems = mutableListOf<String>()
        val tables = level.server.reloadableRegistries()
        fun table(path: String) = tables.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id(path)))
        val chest = LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin)).create(LootContextParamSets.CHEST)
        val machines = setOf(PazEntities.ZOMBIE_TURRET, PazEntities.ELECTRO_TURRET, PazEntities.ZOMBIE_DRONE, PazEntities.LAWN_MOWER)
        val workshop = (1..LOOT_ROLLS).map { table("chests/robo_junkyard/workshop").getRandomItems(chest) }
        if (workshop.any { items -> items.count { it.`is`(PazItems.BLUEPRINT) } != 1 }) problems += "a workshop chest has no blueprint or more than one"
        val built = workshop.flatten().filter { it.`is`(PazItems.BLUEPRINT) }.map { it.get(DataComponents.ENTITY_DATA)?.type() }
        if (built.any { it !in machines }) problems += "a workshop blueprint builds ${built.firstOrNull { it !in machines }}"
        if (workshop.any { items -> items.none { it.`is`(PazItems.BRAINZ_ALLOY) } }) problems += "a workshop chest has no Brainz Alloy"
        if ((1..LOOT_ROLLS).all { table("chests/robo_junkyard/scrap").getRandomItems(chest).isEmpty() }) problems += "the scrap barrels give nothing"
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

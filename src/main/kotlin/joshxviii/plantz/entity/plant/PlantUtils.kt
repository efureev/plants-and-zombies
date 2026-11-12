package joshxviii.plantz.entity.plant

import joshxviii.plantz.item.component.SunCost
import com.mojang.logging.LogUtils
import joshxviii.plantz.*
import joshxviii.plantz.item.GardeningGloveItem
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ProblemReporter.ScopedCollector
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.Vec3


object PlantUtils {
}

// PLANT ITEM INTERACTIONS
// sun interaction
fun Plant.processSunItem(player: Player, item: ItemStack, hand: InteractionHand, growNeeds: PlantGrowNeeds): Boolean {
    val hasStoredSun = item.get(PazComponents.STORED_SUN)?.hasSun() == true
    val isSunItem = item.`is`(PazItems.SUN)
    if (!hasStoredSun && !isSunItem) return false
    var success = false

    if (isTame && health < maxHealth) {// heal
        sunHeal(1)
        success = true
    }
    else if (!isTame) {// try to tame
        val level = level() as? ServerLevel?: return false
        if (random.nextFloat() < PazConfig.getTameChance(type)) {
            tame(player)
            level.broadcastEntityEvent(this, 7.toByte())
        } else level.broadcastEntityEvent(this, 6.toByte())
        success = true
    }
    else if (growNeeds == PlantGrowNeeds.SUN && verifyOwner(player)) {// grow seeds
        playSound(
            SoundEvents.BUBBLE_POP, 1.0f,
            receivedSun.toFloat()/sunRequiredForSeeds() + 0.9f
        )
        if (++receivedSun >= sunRequiredForSeeds()) awardSeedPacket(player) // Sprout Squad: was receivedSun++, one sun too many
        funnyBounce()
        success = true
    }

    if (success) {
        if (hasStoredSun) item.set(PazComponents.STORED_SUN, item.get(PazComponents.STORED_SUN)?.removeSun(1))
        else item.consume(1, player)
    }
    return success
}

// watering interaction
fun Plant.processWateringItem(player: Player, item: ItemStack, hand: InteractionHand, growNeeds: PlantGrowNeeds): Boolean {
    if (growNeeds != PlantGrowNeeds.WATER) return false
    val isWaterBottle = item.components.get(DataComponents.POTION_CONTENTS)?.`is`(Potions.WATER) == true
    val isWaterBucket = item.`is`(Items.WATER_BUCKET)
    val hasStoredWater = item.get(PazComponents.STORED_WATER)?.hasWater() == true
    val infiniteWater = player.hasInfiniteMaterials()
    val waterAmount = when (true) {
        isWaterBottle -> {
            if (!infiniteWater) player.setItemInHand(hand, ItemUtils.createFilledResult(item, player, ItemStack(Items.GLASS_BOTTLE)))
            this.playSound(SoundEvents.BOTTLE_EMPTY)
            1
        }
        isWaterBucket -> {
            if (!infiniteWater) player.setItemInHand(hand, ItemStack(Items.BUCKET))
            this.playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f)
            8
        }
        (hasStoredWater) -> {
            this.playSound(PazSounds.WATERING_CAN)
            if (!infiniteWater) item.set(PazComponents.STORED_WATER, item.get(PazComponents.STORED_WATER)?.removeWater(2))
            2
        }
        else -> 0
    }
    this.receivedWater+=waterAmount
    if (waterAmount>0) {
        addParticlesAroundSelf()
        funnyBounce()
        return true
    }
    return false
}

// glove interaction
fun Plant.processGloveItem(player: Player, item: ItemStack, hand: InteractionHand): Boolean {
    if (!item.`is`(PazItems.GARDENING_GLOVE)) return false
    if (isAttached()) return false
    when {
        // owner check
        !verifyOwner(player) -> return false
        // roll wallnut
        (this.rolling) -> {
            dropAllLeashConnections(player)
            deltaMovement = Vec3.ZERO
        }
        // hold plant
        (player.isSecondaryUseActive) -> {
            if (item.has(DataComponents.ENTITY_DATA)) {
                player.sendOverlayMessage(Component.translatable("message.sproutsquad.glove_full").withStyle(ChatFormatting.RED))
                return true
            }

            if (level().isClientSide) return true

            val data = this.saveAsCompoundTag()
            item.set(DataComponents.ENTITY_DATA, TypedEntityData.of(this.type, data))

            playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value())
            addParticlesAroundSelf(level(), ParticleTypes.DUST_PLUME, verticalSpreadScale = 0.5, height = 0f)
            this.restorePumpkinFromArmor()
            this.discard()
            return true
        }
        // exit early when holding plant
        (item.has(DataComponents.ENTITY_DATA)) -> return false
        // pet :)
        else -> {
            if (!this.onValidGround()) return false
            this.funnyBounce()
            addParticlesAroundSelf(level(), ParticleTypes.HEART, amount = 0..0, height = eyeHeight)
        }
    }
    return true
}

// seed packet / held glove interaction
fun Plant.processSeedPacketInteraction(player: Player, itemStack: ItemStack, hand: InteractionHand): PacketInteractionResult {
    if (!itemStack.has(DataComponents.ENTITY_DATA)) return PacketInteractionResult.FAIL
    if (player.cooldowns.isOnCooldown(itemStack)) return PacketInteractionResult.FAIL
    if (!verifyOwner(player)) return PacketInteractionResult.FAIL
    val type = itemStack.get(DataComponents.ENTITY_DATA)?.type()
    val availableSun = player.getTotalSun()
    val sunCost = itemStack.get(PazComponents.SUN_COST)?.getSunCost(type)?: 0
    val cantAfford = sunCost > availableSun && !player.hasInfiniteMaterials()

    val result = when (type) {
        PazEntities.COFFEE_BEAN -> {
            when {
                !isAsleep -> PacketInteractionResult.NO_INTERACTION
                isGrowingSeeds -> {
                    player.sendOverlayMessage(Component.translatable("message.sproutsquad.growing", name.copy().withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_RED))
                    PacketInteractionResult.FAIL
                }
                cantAfford -> PacketInteractionResult.CANT_AFFORD
                coffeeBuff>0 -> PacketInteractionResult.FAIL
                else -> {
                    applyCoffeeBuff()
                    PacketInteractionResult.SUCCESS
                }
            }
        }
        PazEntities.PUMPKIN -> pumpkinSeedInteraction(itemStack, cantAfford, player)
        else -> PacketInteractionResult.NO_INTERACTION
    }

    when (result) {
        PacketInteractionResult.CANT_AFFORD -> {// show error message
            player.sendOverlayMessage(Component.translatable("message.sproutsquad.not_enough_item", PazItems.SUN.name(), availableSun, sunCost).withStyle(ChatFormatting.RED))
        }
        PacketInteractionResult.SUCCESS -> {
            if (!player.hasInfiniteMaterials()) {// remove used sun
                player.removeSunFromStorageAndInventory(sunCost)
                if (!itemStack.`is`(PazItems.SEED_PACKET)) SeedPacketItem.applyCooldown(itemStack, player)
            }
        }
        PacketInteractionResult.NO_INTERACTION -> {
            return when (this) {// placing plant into pumpkin
                is Pumpkin -> if (isGrowingSeeds) PacketInteractionResult.NO_INTERACTION else pumpkinSeedInteraction(itemStack, cantAfford, player)
                else -> PacketInteractionResult.NO_INTERACTION
            }
        }
        else -> {}
    }
    return result
}

fun Plant.pumpkinSeedInteraction(itemStack: ItemStack, cantAfford: Boolean, player: Player): PacketInteractionResult {
    if (this.hasPumpkinArmor()) return PacketInteractionResult.NO_INTERACTION
    if (this !is Pumpkin) {
        if (this.`is`(PazTags.EntityTypes.CANNOT_PUMPKIN)) return PacketInteractionResult.NO_INTERACTION
        // Sprout Squad: the armor costs sun like planting does; without this check it was free
        if (cantAfford) return PacketInteractionResult.CANT_AFFORD
        val pumpkinType = itemStack.get(DataComponents.ENTITY_DATA)?.type()
        if (dev.efureev.sproutsquad.fixes.PlantingCooldown.refuse(player, pumpkinType)) return PacketInteractionResult.NO_INTERACTION
        applyPumpkinArmor(pumpkinData = itemStack.get(DataComponents.ENTITY_DATA))
        dev.efureev.sproutsquad.fixes.PlantingCooldown.start(player, pumpkinType, SunCost.getSunCost(itemStack))
        return PacketInteractionResult.SUCCESS
    }
    val seedData = itemStack.get(DataComponents.ENTITY_DATA)?: return PacketInteractionResult.NO_INTERACTION
    val seedType = seedData.type()
    if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(seedType).`is`(PazTags.EntityTypes.CANNOT_PUMPKIN)) return PacketInteractionResult.NO_INTERACTION
    if (cantAfford) return PacketInteractionResult.CANT_AFFORD
    // Sprout Squad: planting into a Pumpkin is planting too (plantCooldownEnabled)
    if (dev.efureev.sproutsquad.fixes.PlantingCooldown.refuse(player, seedType)) return PacketInteractionResult.NO_INTERACTION
    (level() as? ServerLevel)?.let { level ->
        seedType.create(
            level, EntityType.createDefaultStackConfig(level, itemStack, null), blockPosition(), EntitySpawnReason.SPAWN_ITEM_USE, true, true
        )?.let {
            val pumpkinData = this.saveAsCompoundTag()

            val filteredData = pumpkinData.copy().apply {
                remove("Health")
                remove("CustomName")
                remove("sproutsquad:State")
                remove("sproutsquad:Cooldown")
                // Sprout Squad: the new plant keeps its own attributes (it got the Pumpkin's: speed 0, its damage, its range)
                remove("attributes")
            }
            val mergedData = TypedEntityData.of(seedType, seedData.copyTagWithoutId().merge(filteredData))
            mergedData.loadInto(it)
            if (it is Plant) it.applyPumpkinArmor(
                amount = pumpkinData.getFloatOr("Health", PazConfig.PUMPKIN_ARMOR_AMOUNT.toFloat()),
                pumpkinData = TypedEntityData.of(this.type, pumpkinData))
            if (level.addFreshEntity(it)) {
                discard()
                dev.efureev.sproutsquad.fixes.PlantingCooldown.start(player, seedType, SunCost.getSunCost(itemStack))
            }
        }
        return PacketInteractionResult.SUCCESS
    }
    return PacketInteractionResult.SUCCESS
}

fun Entity.saveAsCompoundTag(discards: List<String> = listOf("Pos")): CompoundTag {
    if (level().isClientSide) return CompoundTag()
    ScopedCollector(problemPath(), LogUtils.getLogger()).use { reporter ->
        val output = TagValueOutput.createWithContext(reporter, registryAccess())
        saveWithoutId(output)
        if (this is Plant) plantSaveData(output)
        discards.forEach { output.discard(it) }
        output.discard("sproutsquad:PumpkinArmor")
        output.discard("Motion")
        output.discard("leash")
        output.discard("UUID")
        return output.buildResult()
    }
}

fun Entity.loadFromCompoundTag(data: CompoundTag) {
    ScopedCollector(problemPath(), LogUtils.getLogger()).use { reporter ->
        val newData = TagValueInput.create(reporter, registryAccess(), data)
        this.load(newData)
    }
}

enum class PacketInteractionResult {
    SUCCESS,
    FAIL,
    CANT_AFFORD,
    NO_INTERACTION
}

enum class PlantGrowNeeds {
    CANNOT_GROW,
    SOIL,
    SUN,
    WATER,
    TIME;
}
package joshxviii.plantz.entity

import dev.efureev.sproutsquad.fixes.PlantingCooldown
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazCriteria
import joshxviii.plantz.PazItems
import joshxviii.plantz.block.PlantPotBlock
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.getTotalSun
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.name
import joshxviii.plantz.removeSunFromStorageAndInventory
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3

class PlantPotMinecart(type: EntityType<out AbstractMinecart>, level: Level) : AbstractMinecart(type, level) {

    override fun interact(player: Player, hand: InteractionHand, location: Vec3): InteractionResult {
        val itemStack = player.getItemInHand(hand)
        val serverLevel = this.level()

        if (passengers.isNotEmpty()) return InteractionResult.PASS

        if (itemStack.`is`(PazItems.SEED_PACKET)) {
            // Sprout Squad: the same rules as planting on the ground (SeedPacketItem.tryPlant): it was free, with no
            // entity limit or cooldown, and lost the packet's data; a packet with no plant was used up for nothing
            if (serverLevel !is ServerLevel) return InteractionResult.SUCCESS
            val plantType = SeedPacketItem.typeFromStack(itemStack) ?: return InteractionResult.FAIL
            if (PlantingCooldown.refuse(player, plantType)) return InteractionResult.FAIL
            val availableSun = player.getTotalSun()
            val sunCost = itemStack.get(PazComponents.SUN_COST)?.getSunCost(plantType) ?: 0
            if (sunCost > availableSun && !player.hasInfiniteMaterials()) {
                player.sendOverlayMessage(Component.translatable("message.sproutsquad.not_enough_item", PazItems.SUN.name(), availableSun, sunCost).withStyle(ChatFormatting.RED))
                return InteractionResult.FAIL
            }

            val entity = plantType.create(serverLevel, EntityType.createDefaultStackConfig(serverLevel, itemStack, player), BlockPos.containing(this.position()), EntitySpawnReason.SPAWN_ITEM_USE, true, false)
                ?: return InteractionResult.FAIL
            itemStack.get(DataComponents.ENTITY_DATA)?.loadInto(entity)
            if (entity is Plant && PazConfig.getEntityLimit(plantType) > -1 && !player.hasInfiniteMaterials() && entity.isOverEntityLimit()) {
                player.sendOverlayMessage(Component.translatable("message.sproutsquad.entity_limit", plantType.description.copy().withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_RED))
                return InteractionResult.FAIL
            }

            // snap rotation
            if (entity is Plant) {
                entity.startRiding(this, true, true)
                entity.snapTo(this.position())
                val yaw = this.yRot
                entity.yHeadRot = yaw
                entity.yBodyRot = yaw
                entity.yRot = yaw
            }

            if (!serverLevel.addFreshEntity(entity)) {
                entity.discard()
                return InteractionResult.FAIL
            }

            PlantingCooldown.start(player, plantType, sunCost)
            itemStack.consume(1, player)
            if (!player.hasInfiniteMaterials()) player.removeSunFromStorageAndInventory(sunCost)
            entity.playSound(SoundEvents.BIG_DRIPLEAF_PLACE, 1.0f, 1.0f)
            if (entity is TamableAnimal) entity.tame(player)
            serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, this.position())
            if (player is ServerPlayer) PazCriteria.PLANT_POT_MINECRAFT.trigger(player, hasPassenger(entity))
        }
        return InteractionResult.SUCCESS_SERVER
    }

    override fun isRideable(): Boolean = false
    override fun getPickResult(): ItemStack = ItemStack(PazItems.PLANT_POT_MINECART)
    override fun getDropItem(): Item = PazItems.PLANT_POT_MINECART

    override fun getDefaultDisplayBlockState(): BlockState {
        return PazBlocks.PLANT_POT.defaultBlockState().setValue<Direction, Direction>(PlantPotBlock.FACING, Direction.NORTH)
    }
}
package joshxviii.plantz.neoforge

import joshxviii.plantz.pazResource
import net.minecraft.core.Registry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.attachment.IAttachmentHolder
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * Synced per-entity values that the Fabric version stored as extra SynchedEntityData on
 * vanilla entities (LivingEntity, Zombie). NeoForge forbids adding synced data to foreign
 * entities, so they are syncable data attachments instead. Persistence is still handled by
 * the mixins' own save/load code, so the attachments themselves are not serialized.
 *
 * Must be initialized during registration ([initialize] is called from PazMain.onInitialize).
 */
object PazSyncedData {
    class SyncedValue<T : Any>(val type: AttachmentType<T>, private val default: () -> T) {
        /** Current value, or the default when the entity has none yet (does not create or sync it). */
        fun get(holder: IAttachmentHolder): T = holder.getExistingDataOrNull(type) ?: default()

        /** Stores the value; syncs to tracking clients only when it actually changes. */
        fun set(holder: IAttachmentHolder, value: T) {
            if (holder.getExistingDataOrNull(type) != value) holder.setData(type, value)
        }
    }

    // LivingEntity
    @JvmField val HYPNOTIZED = bool("hypnotized", false)
    @JvmField val CHILLED = bool("chilled", false)
    @JvmField val FROZEN = bool("frozen", false)
    @JvmField val BUTTERED = bool("buttered", false)
    @JvmField val PAINTED_COLORS: SyncedValue<Map<Int, Int>> =
        register("painted_colors", { HashMap() }, ByteBufCodecs.map(::HashMap, ByteBufCodecs.INT, ByteBufCodecs.INT))
    @JvmField val HEAD_PLANT_ID: SyncedValue<Int> = register("head_plant_id", { -1 }, ByteBufCodecs.INT)

    // Zombie (raid state)
    @JvmField val FROM_RAID = bool("from_raid", false)
    @JvmField val ADDS_HEALTH_TO_RAID = bool("adds_health_to_raid", true)
    @JvmField val RAID_LEADER = bool("raid_leader", false)
    @JvmField val RAID_BOSS = bool("raid_boss", false)
    @JvmField val IGNORE_FLAG = bool("ignore_flag", false)

    fun initialize() {}

    private fun bool(name: String, default: Boolean): SyncedValue<Boolean> = register(name, { default }, ByteBufCodecs.BOOL)

    private fun <T : Any> register(
        name: String,
        default: () -> T,
        streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>,
    ): SyncedValue<T> {
        val type = AttachmentType.builder(Supplier(default)).sync(streamCodec).build()
        Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, pazResource(name), type)
        return SyncedValue(type, default)
    }
}

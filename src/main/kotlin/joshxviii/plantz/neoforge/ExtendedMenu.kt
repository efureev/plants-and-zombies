package joshxviii.plantz.neoforge

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.neoforged.neoforge.network.IContainerFactory

/**
 * Menu type whose client-side menu is created from extra data sent by the server.
 * Mirrors the Fabric API `ExtendedMenuType`, so menu code stays loader-agnostic.
 */
class ExtendedMenuType<T : AbstractContainerMenu, D : Any>(
    factory: ExtendedFactory<T, D>,
    val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, D>,
) : MenuType<T>(
    IContainerFactory { containerId, inventory, buf -> factory.create(containerId, inventory, streamCodec.decode(buf)) },
    FeatureFlags.VANILLA_SET,
) {
    fun interface ExtendedFactory<T : AbstractContainerMenu, D : Any> {
        fun create(containerId: Int, inventory: Inventory, data: D): T
    }
}

/**
 * Menu provider that sends [getScreenOpeningData] to the client when the menu opens.
 * Works with a plain `player.openMenu(provider)` call.
 */
interface ExtendedMenuProvider<D : Any> : MenuProvider {
    fun getScreenOpeningData(player: ServerPlayer): D

    override fun writeClientSideData(player: ServerPlayer, menu: AbstractContainerMenu, buffer: RegistryFriendlyByteBuf) {
        @Suppress("UNCHECKED_CAST")
        val type = menu.type as ExtendedMenuType<*, D>
        type.streamCodec.encode(buffer, getScreenOpeningData(player))
    }
}

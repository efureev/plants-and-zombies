package joshxviii.plantz

import joshxviii.plantz.api.SeedMutationData
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.core.registries.Registries.ROOT_REGISTRY_NAME
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceKey.InternKey

object PazRegistries {
    val PAZ_ROOT_REGISTRY: Identifier = pazResource("root")

    @JvmField val SEED_MUTATION: ResourceKey<Registry<SeedMutationData>> = register("seed_mutation")

    fun <T : Any> register(name: String): ResourceKey<Registry<T>> {
        return ResourceKey.createRegistryKey(Identifier.withDefaultNamespace(name))
    }

    fun initialize() {
    }

    /** Server-only datapack registries (Fabric: DynamicRegistries.register). */
    fun registerDataPackRegistries(event: NewDatapackRegistryEvent) {
        event.worldRegistry(SEED_MUTATION, SeedMutationData.CODEC)
    }
}
package joshxviii.plantz.mixin.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server reads a player's save before the player has a connection (PrepareSpawnTask), and the
 * mod restores its synced flags there (LivingEntityMixin.loadCustomFlags). NeoForge then sends the
 * changed attachment to the player itself and fails on the missing connection: every saved world
 * was rejected with "Invalid player data". Until the player is connected there is no one to send
 * it to; the value is already stored, and NeoForge sends all of a player's synced attachments once
 * the player joins (PlayerList.placeNewPlayer and respawn).
 */
@Mixin(Entity.class)
public abstract class EntitySyncDataMixin {
    @Inject(method = "syncData", at = @At("HEAD"), cancellable = true)
    private void plantz$skipSyncWithoutConnection(AttachmentType<?> type, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && player.connection == null) ci.cancel();
    }
}

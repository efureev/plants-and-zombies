package joshxviii.plantz.mixin;

import joshxviii.plantz.PazComponents;
import joshxviii.plantz.entity.projectile.PazProjectile;
import joshxviii.plantz.item.component.ProtectsHeadDataComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityProtectsHeadMixin {
    @Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
    private void plantz$applyPassiveHeadProtection(
        ServerLevel level,
        DamageSource source,
        float damage,
        CallbackInfoReturnable<Float> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (damage <= 0.0F) return;

        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return;
        if (source.getDirectEntity() instanceof PazProjectile projectile && projectile.getPierceLevel() > 0) return;

        float totalBlocked = cir.getReturnValue();
        float remainingDamage = damage - totalBlocked;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = entity.getItemBySlot(slot);
            ProtectsHeadDataComponent component = item.get(PazComponents.PROTECTS_HEAD);
            if (component == null || !component.getSlot().test(slot)) continue;
            if (!component.canBlockFrom(source, entity)) continue;
            // Sprout Squad: checked before the item wears and the block sound plays; a hit from the side or below,
            // which is not blocked, wore the cone or bucket (and rolled its break chance) and sounded like a block
            if (!component.isFromAbove(source, entity)) continue;

            float blocked = component.resolveBlockedDamage(source, remainingDamage);
            component.hurtBlockingItem(entity.level(), item, entity, slot, blocked);
            component.onBlocked(level, entity);

            if (blocked <= 0.0F) continue;
            totalBlocked += blocked;
            remainingDamage -= blocked;
            if (remainingDamage <= 0.0F) break;
        }

        cir.setReturnValue(totalBlocked);
    }
}

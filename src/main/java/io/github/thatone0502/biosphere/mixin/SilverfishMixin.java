package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.foodchain.FoodChainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 抑制蠹虫对「关系表里规避对象」的群体仇恨/援护：
 * 被鸡等规避对象攻击时，不唤醒同伴，只继续逃跑。
 */
@Mixin(Silverfish.class)
public abstract class SilverfishMixin {
    @Unique
    private boolean biosphere$skipWakeUp;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void biosphere$checkAttacker(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.biosphere$skipWakeUp = false;
        Entity attacker = source.getEntity();
        if (attacker == null) return;
        EntityType<?> selfType = ((Silverfish) (Object) this).getType();
        if (!FoodChainManager.hasAvoidFilters(selfType)) return;
        for (FoodChainManager.EntityFilter filter : FoodChainManager.avoidFilters(selfType)) {
            if (filter.matches(attacker.getType())) {
                this.biosphere$skipWakeUp = true;
                return;
            }
        }
    }

    @Redirect(method = "hurtServer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Silverfish$SilverfishWakeUpFriendsGoal;notifyHurt()V"))
    private void biosphere$redirectNotifyHurt(Silverfish.SilverfishWakeUpFriendsGoal goal) {
        if (!this.biosphere$skipWakeUp) {
            goal.notifyHurt();
        }
    }
}

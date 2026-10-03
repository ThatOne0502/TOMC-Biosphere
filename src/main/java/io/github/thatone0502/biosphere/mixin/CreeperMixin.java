package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.config.BiosphereConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * creefern 条件联动：苦力怕额外敌视当前生命值 > min 且 <= max 的生物（与仇视玩家同优先级）。
 * 有效开关由 override / forceOverride / auto 三态决定（见 BiosphereConfig#creefernEffective）。
 */
@Mixin(Creeper.class)
public abstract class CreeperMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void biosphere$registerGoals(CallbackInfo ci) {
        BiosphereConfig config = AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig();
        if (!config.creefernEffective()) return;
        Creeper self = (Creeper) (Object) this;
        ((MobAccessor) this).getTargetSelector().addGoal(1, new NearestAttackableTargetGoal<>(
                self, LivingEntity.class, 10, true, false,
                (target, level) -> isTarget(self, target, config)));
    }

    private static boolean isTarget(Creeper self, LivingEntity target, BiosphereConfig config) {
        if (target == null || !target.isAlive() || target == self) return false;
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) return false;
            return true;
        }
        float hp = target.getHealth();
        return hp > config.creefernMinHealth && hp <= config.creefernMaxHealth;
    }

    @ModifyArg(
            method = "registerGoals",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V",
                    ordinal = 2),
            index = 0
    )
    private int biosphere$raiseOcelotAvoidPriority(int priority) {
        // 把原版苦力怕规避豹猫(AvoidEntityGoal<Ocelot>)的优先级从 3 提到 1，
        // 高于 SwellGoal(膨胀)的 2，被豹猫攻击时只会逃跑而不是膨胀爆炸，不影响对玩家的爆炸 AI。
        return 1;
    }
}

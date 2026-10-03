package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.ai.EatingGoals;
import io.github.thatone0502.biosphere.ai.RelationGoals;
import io.github.thatone0502.biosphere.config.BiosphereConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 Mob 构造完成后追加关系 Goal（仅服务端）。 */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Shadow
    @Final
    public GoalSelector goalSelector;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void biosphere$injectGoals(EntityType<?> entityType, Level level, CallbackInfo ci) {
        if (!level.isClientSide()) {
            BiosphereConfig config = AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig();
            Mob mob = (Mob) (Object) this;
            // 被动生物攻击模块：攻击/躲避关系 + 进食行为，统一受 passiveAttackEnabled 控制。
            if (config.passiveAttackEnabled) {
                RelationGoals.inject(mob, this.goalSelector);
                EatingGoals.inject(mob, this.goalSelector);
            }
        }
    }
}

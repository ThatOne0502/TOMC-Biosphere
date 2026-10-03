package io.github.thatone0502.biosphere.ai;

import io.github.thatone0502.biosphere.config.BiosphereConfig;
import io.github.thatone0502.biosphere.foodchain.FoodChainManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import java.util.List;

/** 按关系表给实体追加 Goal（只追加，不覆盖原版）。 */
public final class RelationGoals {
    private RelationGoals() {}

    public static void inject(Mob mob, GoalSelector goalSelector) {
        BiosphereConfig config = AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig();
        EntityType<?> type = mob.getType();
        // 规避优先（priority 1），高于攻击，避免「既追又逃」。
        if (FoodChainManager.hasAvoidFilters(type) && mob instanceof PathfinderMob pathfinderMob) {
            List<FoodChainManager.EntityFilter> avoidFilters = FoodChainManager.avoidFilters(type);
            goalSelector.addGoal(1, new BiosphereAvoidGoal(pathfinderMob, avoidFilters, config.avoidSpeed));
        }
        // 攻击目标选择（priority 2）。
        if (FoodChainManager.hasAttackFilters(type)) {
            List<FoodChainManager.EntityFilter> attackFilters = FoodChainManager.attackFilters(type);
            goalSelector.addGoal(2, new BiosphereTargetGoal(mob, attackFilters));
            // 原版无攻击能力的生物补最简近战（priority 3）。
            if (AiCapabilities.needsMelee(type) && mob instanceof PathfinderMob pathfinderMob) {
                goalSelector.addGoal(3, new BiosphereMeleeAttackGoal(
                        pathfinderMob, config.meleeSpeed, config.meleeDamage, config.meleeCooldownTicks));
            }
        }
    }
}

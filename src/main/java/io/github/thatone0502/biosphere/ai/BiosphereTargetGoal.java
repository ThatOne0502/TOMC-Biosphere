package io.github.thatone0502.biosphere.ai;

import io.github.thatone0502.biosphere.foodchain.FoodChainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.List;

/** 定期搜索附近符合关系表的目标，设为攻击目标（追加，不覆盖原版目标选择器）。 */
public class BiosphereTargetGoal extends TargetGoal {
    private final List<FoodChainManager.EntityFilter> filters;

    public BiosphereTargetGoal(Mob mob, List<FoodChainManager.EntityFilter> filters) {
        super(mob, false);
        this.filters = filters;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = findNearest();
        if (target == null) return false;
        this.targetMob = target;
        return true;
    }

    private LivingEntity findNearest() {
        Level level = this.mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        List<LivingEntity> candidates = serverLevel.getNearbyEntities(
                LivingEntity.class,
                TargetingConditions.DEFAULT,
                this.mob,
                this.mob.getBoundingBox().inflate(16.0, 8.0, 16.0)
        );
        for (LivingEntity e : candidates) {
            if (e == this.mob || !e.isAlive()) continue;
            if (!matches(e.getType())) continue;
            double d = this.mob.distanceToSqr(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private boolean matches(EntityType<?> type) {
        for (FoodChainManager.EntityFilter f : filters) {
            if (f.matches(type)) return true;
        }
        return false;
    }
}

package io.github.thatone0502.biosphere.ai;

import io.github.thatone0502.biosphere.foodchain.FoodChainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 主动逃跑：搜索附近符合关系表的威胁，朝远离方向逃跑（模仿苦力怕逃豹猫）。 */
public class BiosphereAvoidGoal extends Goal {
    private final PathfinderMob mob;
    private final List<FoodChainManager.EntityFilter> filters;
    private final double sprintSpeed;

    public BiosphereAvoidGoal(PathfinderMob mob, List<FoodChainManager.EntityFilter> filters, double sprintSpeed) {
        this.mob = mob;
        this.filters = filters;
        this.sprintSpeed = sprintSpeed;
    }

    @Override
    public boolean canUse() {
        LivingEntity threat = findNearestThreat();
        if (threat == null) return false;
        return moveAway(threat);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    private LivingEntity findNearestThreat() {
        Level level = this.mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        List<LivingEntity> candidates = serverLevel.getNearbyEntities(
                LivingEntity.class,
                TargetingConditions.DEFAULT,
                this.mob,
                this.mob.getBoundingBox().inflate(8.0, 4.0, 8.0)
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

    private boolean moveAway(LivingEntity threat) {
        Vec3 dir = this.mob.position().subtract(threat.position());
        if (dir.lengthSqr() < 0.001) {
            dir = new Vec3(this.mob.getRandom().nextDouble() - 0.5, 0.0, this.mob.getRandom().nextDouble() - 0.5);
        }
        dir = dir.normalize();
        Vec3 target = this.mob.position().add(dir.scale(10.0));
        return this.mob.getNavigation().moveTo(target.x, target.y, target.z, this.sprintSpeed);
    }
}

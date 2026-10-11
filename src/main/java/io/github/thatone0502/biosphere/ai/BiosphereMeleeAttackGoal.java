package io.github.thatone0502.biosphere.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/** 最简近战：追击目标并在攻击距离内直接造成伤害（供原版无攻击能力的被动生物使用）。 */
public class BiosphereMeleeAttackGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private final float damage;
    private final int cooldownTicks;
    private int attackCooldown;

    public BiosphereMeleeAttackGoal(PathfinderMob mob, double speedModifier, float damage, int cooldownTicks) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }
        this.attackCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;
        this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        this.attackCooldown--;
        double distSqr = this.mob.distanceToSqr(target);
        double reachSqr = getAttackReachSqr(target);
        if (distSqr <= reachSqr) {
            if (this.attackCooldown <= 0) {
                this.attackCooldown = this.cooldownTicks;
                target.hurt(this.mob.damageSources().mobAttack(this.mob), this.damage);
            }
        } else {
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }
    }

    private double getAttackReachSqr(LivingEntity target) {
        // 攻击范围 = 双方宽度之和的 2 倍 + 1 格，让碰撞箱小的生物（鸡 vs 蠹虫）也能稳定命中。
        double reach = this.mob.getBbWidth() * 2.0f + target.getBbWidth() * 2.0f + 1.0;
        return reach * reach;
    }
}

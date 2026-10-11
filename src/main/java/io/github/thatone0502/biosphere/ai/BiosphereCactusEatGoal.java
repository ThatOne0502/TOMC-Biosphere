package io.github.thatone0502.biosphere.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 骆驼进食：寻路到仙人掌柱底附近后播放吃动画并破坏柱顶。
 * <p>
 * 匹配柱高≥2 的柱顶（上方非仙人掌、下方是仙人掌），反推柱底作为寻路目标，
 * 到位后破坏柱顶（保留柱底继续生长）。频率为默认 MoveToBlockGoal 的 1/5。
 */
public class BiosphereCactusEatGoal extends MoveToBlockGoal {
    private static final int EAT_ANIM_TICKS = 40;
    private int eatAnimationTick;

    public BiosphereCactusEatGoal(PathfinderMob mob) {
        super(mob, 1.0, 16, 3);
    }

    /** 匹配柱高≥2 的仙人掌柱顶。 */
    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.CACTUS)
                && !level.getBlockState(pos.above()).is(Blocks.CACTUS)
                && level.getBlockState(pos.below()).is(Blocks.CACTUS);
    }

    /** 从柱顶向下找柱底（最下方一格仙人掌）。 */
    private BlockPos cactusBase() {
        BlockPos p = this.blockPos;
        while (this.mob.level().getBlockState(p.below()).is(Blocks.CACTUS)) {
            p = p.below();
        }
        return p;
    }

    @Override
    protected void moveMobToBlock() {
        BlockPos target = this.getMoveToTarget();
        this.mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, this.speedModifier);
    }

    @Override
    protected BlockPos getMoveToTarget() {
        return this.cactusBase().above();
    }

    @Override
    public double acceptedDistance() {
        return 2.5;
    }

    /** 频率降为 1/5：仅 1/5 的 tick 真正进入搜索/寻路流程。 */
    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(5) != 0) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.eatAnimationTick = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.eatAnimationTick > 0 || super.canContinueToUse();
    }

    @Override
    public void tick() {
        if (this.eatAnimationTick > 0) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
            if (this.eatAnimationTick == this.adjustedTickDelay(4)) {
                if (this.mob.level() instanceof ServerLevel serverLevel
                        && !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                    return;
                }
                if (this.mob.level().getBlockState(this.blockPos).is(Blocks.CACTUS)) {
                    this.mob.level().destroyBlock(this.blockPos, true, this.mob);
                }
            }
            return;
        }
        super.tick();
        if (this.isReachedTarget()) {
            this.eatAnimationTick = this.adjustedTickDelay(EAT_ANIM_TICKS);
            this.mob.level().broadcastEntityEvent(this.mob, (byte) 10);
            this.mob.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.eatAnimationTick = 0;
        super.stop();
    }
}

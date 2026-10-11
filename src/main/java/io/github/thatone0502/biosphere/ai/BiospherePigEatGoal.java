package io.github.thatone0502.biosphere.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** 猪进食：吃成熟农作物（重置 age）与西瓜/南瓜（破坏），受 mobGriefing 控制。 */
public class BiospherePigEatGoal extends MoveToBlockGoal {
    private static final int EAT_TICKS = 40;
    private int eatTicks;

    public BiospherePigEatGoal(PathfinderMob mob) {
        super(mob, 0.8, 16);
    }

    @Override
    public boolean canUse() {
        if (this.nextStartTick <= 0
                && this.mob.level() instanceof ServerLevel serverLevel
                && !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isReachedTarget()) {
            this.eatTicks++;
            if (this.eatTicks >= EAT_TICKS) {
                this.eatBlock();
                this.eatTicks = 0;
            }
        } else {
            this.eatTicks = 0;
        }
    }

    private void eatBlock() {
        BlockPos pos = this.blockPos;
        BlockState state = this.mob.level().getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
            this.mob.level().setBlock(pos, cropBlock.getStateForAge(0), 2);
            this.mob.level().levelEvent(2001, pos, Block.getId(state));
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            this.mob.level().destroyBlock(pos, true, this.mob);
        }
    }

    @Override
    public double acceptedDistance() {
        // 猪站在作物旁（方块上方中心不可站立），需放宽判定，否则永远够不到 blockPos.above()。
        return 2.5;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        return block == Blocks.MELON || block == Blocks.PUMPKIN;
    }
}

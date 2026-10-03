package io.github.thatone0502.biosphere.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 哞菇进食：优先吃菌丝上的小蘑菇，否则把菌丝变成泥土（受 mobGriefing 控制）。 */
public class BiosphereMushroomEatGoal extends Goal {
    private static final int EAT_ANIM_TICKS = 40;
    private final Mob mob;
    private int eatAnimationTick;

    public BiosphereMushroomEatGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(this.mob.isBaby() ? 20 : 200) != 0) {
            return false;
        }
        BlockPos pos = this.mob.blockPosition();
        BlockState here = this.mob.level().getBlockState(pos);
        if (isSmallMushroom(here.getBlock())) {
            return true;
        }
        BlockState below = this.mob.level().getBlockState(pos.below());
        if (below.is(Blocks.MYCELIUM)) {
            return true;
        }
        return findNearbyMushroom(pos) != null;
    }

    @Override
    public void start() {
        this.eatAnimationTick = this.adjustedTickDelay(EAT_ANIM_TICKS);
        this.mob.level().broadcastEntityEvent(this.mob, (byte) 10);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return this.eatAnimationTick > 0;
    }

    @Override
    public void stop() {
        this.eatAnimationTick = 0;
    }

    @Override
    public void tick() {
        this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        if (this.eatAnimationTick != this.adjustedTickDelay(4)) {
            return;
        }
        if (this.mob.level() instanceof ServerLevel serverLevel
                && !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }
        BlockPos pos = this.mob.blockPosition();
        BlockState here = this.mob.level().getBlockState(pos);
        if (isSmallMushroom(here.getBlock())) {
            this.mob.level().destroyBlock(pos, true, this.mob);
            return;
        }
        BlockPos mushroom = findNearbyMushroom(pos);
        if (mushroom != null) {
            this.mob.level().destroyBlock(mushroom, true, this.mob);
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = this.mob.level().getBlockState(below);
        if (belowState.is(Blocks.MYCELIUM)) {
            this.mob.level().levelEvent(2001, below, Block.getId(Blocks.MYCELIUM.defaultBlockState()));
            this.mob.level().setBlock(below, Blocks.DIRT.defaultBlockState(), 2);
        }
    }

    private BlockPos findNearbyMushroom(BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos near = center.offset(dx, 0, dz);
                if (isSmallMushroom(this.mob.level().getBlockState(near).getBlock())) {
                    return near;
                }
            }
        }
        return null;
    }

    private boolean isSmallMushroom(Block block) {
        return block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM;
    }
}

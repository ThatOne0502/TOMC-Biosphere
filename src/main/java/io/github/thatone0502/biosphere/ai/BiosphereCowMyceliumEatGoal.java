package io.github.thatone0502.biosphere.ai;

import io.github.thatone0502.biosphere.config.BiosphereConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/** 牛进食：吃脚下菌丝（菌丝→泥土），并有概率转化为哞菇（受 mobGriefing 与转换概率控制）。 */
public class BiosphereCowMyceliumEatGoal extends Goal {
    private static final int EAT_ANIM_TICKS = 40;
    private final Mob mob;
    private int eatAnimationTick;

    public BiosphereCowMyceliumEatGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(this.mob.isBaby() ? 20 : 200) != 0) {
            return false;
        }
        BlockPos pos = this.mob.blockPosition();
        BlockState below = this.mob.level().getBlockState(pos.below());
        return below.is(Blocks.MYCELIUM);
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
        BlockPos below = this.mob.blockPosition().below();
        if (!this.mob.level().getBlockState(below).is(Blocks.MYCELIUM)) {
            return;
        }
        // 菌丝 → 泥土。
        this.mob.level().levelEvent(2001, below, Block.getId(Blocks.MYCELIUM.defaultBlockState()));
        this.mob.level().setBlock(below, Blocks.DIRT.defaultBlockState(), 2);
        this.mob.ate();
        // 概率转化为哞菇（[0,1] 夹取）。
        BiosphereConfig config = AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig();
        double chance = Math.max(0.0, Math.min(1.0, config.mushroomConversionChance));
        if (this.mob.getRandom().nextDouble() < chance) {
            this.mob.convertTo(EntityType.MOOSHROOM,
                    ConversionParams.single(this.mob, false, false), mooshroom -> {});
        }
    }
}

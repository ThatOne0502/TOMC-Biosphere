package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.environmental.EnvironmentalChangeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 草方块/菌丝：原版蔓延逻辑后追加数据驱动演替。 */
@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class SpreadingSnowyDirtBlockMixin {

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void biosphere$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        EnvironmentalChangeManager.tick(level, pos, state, random);
    }
}

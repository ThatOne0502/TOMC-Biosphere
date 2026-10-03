package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.environmental.EnvironmentalChangeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在原版 randomTick 后追加数据驱动演替逻辑（isRandomlyTicking 缓存修复见 BlockStateMixin）。 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void biosphere$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        EnvironmentalChangeManager.tick(level, pos, state, random);
    }
}

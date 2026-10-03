package io.github.thatone0502.biosphere.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 骆驼（含僵尸骆驼变种）免疫仙人掌刺伤，以便进食时不被打断。 */
@Mixin(CactusBlock.class)
public abstract class CactusBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void biosphere$skipCamelDamage(BlockState state, Level level, BlockPos pos, Entity entity,
                                           InsideBlockEffectApplier applier, boolean inside, CallbackInfo ci) {
        if (entity instanceof Camel) {
            ci.cancel();
        }
    }
}

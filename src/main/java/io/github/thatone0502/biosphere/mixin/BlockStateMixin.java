package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.environmental.EnvironmentalChangeManager;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 根因修复：isRandomlyTicking() 无参方法（声明在 {@code BlockBehaviour$BlockStateBase}，
 * BlockState 继承它）读取缓存字段 {@code private boolean isRandomlyTicking}，该值在 initCache 时固化，
 * 可能早于生态规则加载。原 mixin 注入 BlockBehaviour.isRandomlyTicking(BlockState)（protected）
 * 无法影响此缓存读取。这里直接注入无参 isRandomlyTicking()，对生态方块动态返回 true，绕过缓存。
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateMixin {

    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void biosphere$isRandomlyTicking(CallbackInfoReturnable<Boolean> cir) {
        if (EnvironmentalChangeManager.isEcologyBlock(((BlockBehaviour.BlockStateBase) (Object) this).getBlock())) {
            cir.setReturnValue(true);
        }
    }
}

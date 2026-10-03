package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatMob;
import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 把骆驼进食标量搬运到渲染状态。 */
@Mixin(CamelRenderer.class)
public abstract class CamelRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/camel/Camel;Lnet/minecraft/client/renderer/entity/state/CamelRenderState;F)V",
            at = @At("TAIL")
    )
    private void biosphere$applyEatAnim(Camel camel, CamelRenderState state, float partialTick, CallbackInfo ci) {
        HeadEatRenderState eatState = (HeadEatRenderState) state;
        HeadEatMob eatMob = (HeadEatMob) camel;
        eatState.biosphere$setHeadEatPositionScale(eatMob.biosphere$getHeadEatPositionScale(partialTick));
        eatState.biosphere$setHeadEatAngleScale(eatMob.biosphere$getHeadEatAngleScale(partialTick));
    }
}

package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatMob;
import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.world.entity.animal.cow.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 把实体吃草标量搬运到渲染状态。 */
@Mixin(CowRenderer.class)
public abstract class CowRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/cow/Cow;Lnet/minecraft/client/renderer/entity/state/CowRenderState;F)V",
            at = @At("TAIL")
    )
    private void biosphere$applyEatAnim(Cow cow, CowRenderState state, float partialTick, CallbackInfo ci) {
        HeadEatRenderState eatState = (HeadEatRenderState) state;
        HeadEatMob eatMob = (HeadEatMob) cow;
        eatState.biosphere$setHeadEatPositionScale(eatMob.biosphere$getHeadEatPositionScale(partialTick));
        eatState.biosphere$setHeadEatAngleScale(eatMob.biosphere$getHeadEatAngleScale(partialTick));
    }
}

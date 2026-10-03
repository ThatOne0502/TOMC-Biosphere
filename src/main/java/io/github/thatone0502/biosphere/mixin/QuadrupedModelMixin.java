package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 牛吃草低头：复用羊式头部下压动画。 */
@Mixin(QuadrupedModel.class)
public abstract class QuadrupedModelMixin {

    @Shadow
    @Final
    protected ModelPart head;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)V",
            at = @At("TAIL")
    )
    private void biosphere$applyHeadEat(LivingEntityRenderState state, CallbackInfo ci) {
        if (state instanceof HeadEatRenderState eatState) {
            float posScale = eatState.biosphere$getHeadEatPositionScale();
            float angle = eatState.biosphere$getHeadEatAngleScale();
            this.head.y += posScale * 9.0F * state.ageScale;
            this.head.xRot = angle;
        }
    }
}

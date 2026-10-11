package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 骆驼吃仙人掌：头小幅上抬（吃的是高处），配合轻微点头。 */
@Mixin(CamelModel.class)
public abstract class CamelModelMixin {

    @Shadow
    @Final
    protected ModelPart head;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/CamelRenderState;)V",
            at = @At("TAIL")
    )
    private void biosphere$applyHeadEat(CamelRenderState state, CallbackInfo ci) {
        if (state instanceof HeadEatRenderState eatState) {
            float posScale = eatState.biosphere$getHeadEatPositionScale();
            float angle = eatState.biosphere$getHeadEatAngleScale();
            this.head.y += posScale * 4.0F * state.ageScale;
            this.head.xRot += angle * 0.4F;
        }
    }
}

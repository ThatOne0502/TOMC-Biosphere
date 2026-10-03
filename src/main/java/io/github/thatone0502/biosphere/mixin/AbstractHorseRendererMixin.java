package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatMob;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 马科动物吃草低头：把实体吃草标量并入 EquineRenderState.eatAnimation。 */
@Mixin(AbstractHorseRenderer.class)
public abstract class AbstractHorseRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/equine/AbstractHorse;Lnet/minecraft/client/renderer/entity/state/EquineRenderState;F)V",
            at = @At("TAIL")
    )
    private void biosphere$applyEatAnim(AbstractHorse horse, EquineRenderState state, float partialTick, CallbackInfo ci) {
        float posScale = ((HeadEatMob) horse).biosphere$getHeadEatPositionScale(partialTick);
        state.eatAnimation = Math.max(state.eatAnimation, posScale);
    }
}

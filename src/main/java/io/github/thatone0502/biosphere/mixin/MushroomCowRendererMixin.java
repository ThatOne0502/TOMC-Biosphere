package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatMob;
import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 把哞菇进食标量搬运到渲染状态。 */
@Mixin(MushroomCowRenderer.class)
public abstract class MushroomCowRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/cow/MushroomCow;Lnet/minecraft/client/renderer/entity/state/MushroomCowRenderState;F)V",
            at = @At("TAIL")
    )
    private void biosphere$applyEatAnim(MushroomCow cow, MushroomCowRenderState state, float partialTick, CallbackInfo ci) {
        HeadEatRenderState eatState = (HeadEatRenderState) state;
        HeadEatMob eatMob = (HeadEatMob) cow;
        eatState.biosphere$setHeadEatPositionScale(eatMob.biosphere$getHeadEatPositionScale(partialTick));
        eatState.biosphere$setHeadEatAngleScale(eatMob.biosphere$getHeadEatAngleScale(partialTick));
    }
}

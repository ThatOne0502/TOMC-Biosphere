package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** 给牛渲染状态附加吃草动画标量。 */
@Mixin(CowRenderState.class)
public abstract class CowRenderStateMixin implements HeadEatRenderState {

    @Unique
    private float biosphere$headEatPositionScale;

    @Unique
    private float biosphere$headEatAngleScale;

    @Override
    public float biosphere$getHeadEatPositionScale() {
        return biosphere$headEatPositionScale;
    }

    @Override
    public void biosphere$setHeadEatPositionScale(float scale) {
        biosphere$headEatPositionScale = scale;
    }

    @Override
    public float biosphere$getHeadEatAngleScale() {
        return biosphere$headEatAngleScale;
    }

    @Override
    public void biosphere$setHeadEatAngleScale(float angle) {
        biosphere$headEatAngleScale = angle;
    }
}

package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatRenderState;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** 给骆驼渲染状态附加进食动画标量。 */
@Mixin(CamelRenderState.class)
public abstract class CamelRenderStateMixin implements HeadEatRenderState {

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

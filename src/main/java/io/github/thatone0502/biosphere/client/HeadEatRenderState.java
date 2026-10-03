package io.github.thatone0502.biosphere.client;

/**
 * 渲染状态侧吃草动画标量接口，由 {@code CowRenderStateMixin} 实现。
 */
public interface HeadEatRenderState {
    float biosphere$getHeadEatPositionScale();

    void biosphere$setHeadEatPositionScale(float scale);

    float biosphere$getHeadEatAngleScale();

    void biosphere$setHeadEatAngleScale(float angle);
}

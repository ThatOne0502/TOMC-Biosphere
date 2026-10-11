package io.github.thatone0502.biosphere.client;

/**
 * 客户端吃草低头动画的实体侧接口，由 {@code LivingEntityMixin} 实现。
 * 使用羊式公式：EatBlockGoal 广播实体事件 10(EAT_GRASS) 驱动动画计时。
 */
public interface HeadEatMob {
    float biosphere$getHeadEatPositionScale(float partialTick);

    float biosphere$getHeadEatAngleScale(float partialTick);
}

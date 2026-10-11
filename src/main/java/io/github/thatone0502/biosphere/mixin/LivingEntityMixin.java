package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.client.HeadEatMob;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端吃草低头动画（羊式）。EatBlockGoal 广播实体事件 10(EAT_GRASS)，
 * 此处拦截并驱动头部动画计时；牛/驴/马/骡经 EatingGoals 复用 EatBlockGoal 故自动生效。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements HeadEatMob {

    @Unique
    private int biosphere$eatAnimTick;

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void biosphere$onEntityEvent(byte id, CallbackInfo ci) {
        if (id == 10) {
            biosphere$eatAnimTick = 40;
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void biosphere$tickEatAnimation(CallbackInfo ci) {
        if (biosphere$eatAnimTick > 0) {
            biosphere$eatAnimTick--;
        }
    }

    @Override
    public float biosphere$getHeadEatPositionScale(float partialTick) {
        if (biosphere$eatAnimTick <= 0) {
            return 0.0F;
        } else if (biosphere$eatAnimTick >= 4 && biosphere$eatAnimTick <= 36) {
            return 1.0F;
        } else if (biosphere$eatAnimTick < 4) {
            return (biosphere$eatAnimTick - partialTick) / 4.0F;
        } else {
            return (40 - biosphere$eatAnimTick + partialTick) / 4.0F;
        }
    }

    @Override
    public float biosphere$getHeadEatAngleScale(float partialTick) {
        if (biosphere$eatAnimTick > 4 && biosphere$eatAnimTick <= 36) {
            float f = ((float) (biosphere$eatAnimTick - 4) - partialTick) / 32.0F;
            return 0.62831855F + 0.21991149F * Mth.sin(f * 28.7F);
        } else if (biosphere$eatAnimTick > 0) {
            return 0.62831855F;
        } else {
            return ((LivingEntity) (Object) this).getXRot(partialTick) * 0.017453292F;
        }
    }
}

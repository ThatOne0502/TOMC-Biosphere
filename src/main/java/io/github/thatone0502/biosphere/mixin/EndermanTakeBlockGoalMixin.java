package io.github.thatone0502.biosphere.mixin;

import io.github.thatone0502.biosphere.enderman.EndermanHoldableProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 末影人搬运方块白名单覆写。
 * <p>
 * {@code EndermanTakeBlockGoal.tick()} 里判定能否拿起的语句只有一处
 * （字节码偏移 227：{@code blockState.is(BlockTags.ENDERMAN_HOLDABLE)}），这里把它整体
 * 重定向到 {@link EndermanHoldableProvider#isHoldable}，从而支持「覆写原版标签 + 黑名单 + 分类白名单」。
 * <p>
 * 目标类是包私有内部类，必须用 {@code targets} 字符串形式；{@code enderman} 字段为
 * {@code private final EnderMan}，用 {@code @Shadow @Final} 读取。
 */
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockGoalMixin {

    @Shadow
    @Final
    private EnderMan enderman;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private boolean biosphere$overrideHoldable(BlockState state, TagKey<Block> tag) {
        if (tag == BlockTags.ENDERMAN_HOLDABLE && enderman.level() instanceof ServerLevel serverLevel) {
            return EndermanHoldableProvider.isHoldable(state, serverLevel);
        }
        return state.is(tag);
    }
}

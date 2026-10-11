package io.github.thatone0502.biosphere.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 访问 Mob 父类的 targetSelector（protected 字段，跨类访问用 Accessor）。 */
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}

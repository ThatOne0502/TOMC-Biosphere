package io.github.thatone0502.biosphere.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import java.util.Set;

/** 按实体类型追加进食 Goal（只追加，不覆盖原版）。 */
public final class EatingGoals {
    private static final Set<String> GRASS_EATERS = Set.of(
            "minecraft:horse", "minecraft:donkey", "minecraft:mule"
    );

    private EatingGoals() {}

    public static void inject(Mob mob, GoalSelector goalSelector) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (key == null) return;
        String id = key.toString();
        if (id.equals("minecraft:cow")) {
            // 牛：吃菌丝（更高优先级，概率变哞菇）+ 吃草（复用羊的 EatBlockGoal）。
            goalSelector.addGoal(4, new BiosphereCowMyceliumEatGoal(mob));
            goalSelector.addGoal(5, new EatBlockGoal(mob));
        } else if (GRASS_EATERS.contains(id)) {
            // 驴/马/骡吃草：复用羊的 EatBlockGoal（吃 EDIBLE_FOR_SHEEP 植物 + 草方块变泥土）。
            goalSelector.addGoal(5, new EatBlockGoal(mob));
        } else if (id.equals("minecraft:mooshroom")) {
            goalSelector.addGoal(5, new BiosphereMushroomEatGoal(mob));
        } else if (id.equals("minecraft:pig") && mob instanceof PathfinderMob pathfinderMob) {
            goalSelector.addGoal(5, new BiospherePigEatGoal(pathfinderMob));
        } else if (id.equals("minecraft:camel") && mob instanceof PathfinderMob pathfinderMob) {
            goalSelector.addGoal(5, new BiosphereCactusEatGoal(pathfinderMob));
        }
    }
}

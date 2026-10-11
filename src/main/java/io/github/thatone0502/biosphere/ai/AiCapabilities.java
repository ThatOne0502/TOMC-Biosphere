package io.github.thatone0502.biosphere.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/** 判断主体是否原版就具备攻击能力（无需额外补近战）。 */
public final class AiCapabilities {
    private static final Set<String> MELEE_CAPABLE = Set.of(
            "minecraft:polar_bear", "minecraft:dolphin",
            "minecraft:bogged", "minecraft:parched", "minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton",
            "minecraft:husk", "minecraft:zombie", "minecraft:zombie_villager", "minecraft:drowned",
            "minecraft:phantom", "minecraft:shulker", "minecraft:vex"
    );

    private AiCapabilities() {}

    public static boolean needsMelee(EntityType<?> type) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null || !MELEE_CAPABLE.contains(key.toString());
    }
}

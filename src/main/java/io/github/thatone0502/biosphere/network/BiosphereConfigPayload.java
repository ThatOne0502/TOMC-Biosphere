package io.github.thatone0502.biosphere.network;

import io.github.thatone0502.biosphere.config.BiosphereConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * 服务器权威配置同步（S2C）：服务器在玩家加入时下发全部影响行为的配置字段，
 * 客户端缓存并用于 ModMenu 只读显示。玩家改动只影响本地值，不回传服务器。
 */
public record BiosphereConfigPayload(
        boolean passiveAttackEnabled,
        boolean environmentalChangeEnabled,
        boolean spawnDataEnabled,
        float meleeDamage,
        double meleeSpeed,
        int meleeCooldownTicks,
        double avoidSpeed,
        double mushroomConversionChance,
        boolean creefernOverride,
        boolean creefernForceOverride,
        float creefernMinHealth,
        float creefernMaxHealth
) implements CustomPacketPayload {

    public static final Type<BiosphereConfigPayload> TYPE =
            new Type<>(Identifier.parse("biosphere:config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BiosphereConfigPayload> STREAM_CODEC =
            StreamCodec.of(BiosphereConfigPayload::encode, BiosphereConfigPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, BiosphereConfigPayload p) {
        buf.writeBoolean(p.passiveAttackEnabled);
        buf.writeBoolean(p.environmentalChangeEnabled);
        buf.writeBoolean(p.spawnDataEnabled);
        buf.writeFloat(p.meleeDamage);
        buf.writeDouble(p.meleeSpeed);
        buf.writeInt(p.meleeCooldownTicks);
        buf.writeDouble(p.avoidSpeed);
        buf.writeDouble(p.mushroomConversionChance);
        buf.writeBoolean(p.creefernOverride);
        buf.writeBoolean(p.creefernForceOverride);
        buf.writeFloat(p.creefernMinHealth);
        buf.writeFloat(p.creefernMaxHealth);
    }

    private static BiosphereConfigPayload decode(RegistryFriendlyByteBuf buf) {
        return new BiosphereConfigPayload(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readDouble(),
                buf.readInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务器配置下发后，覆盖客户端本地缓存字段（不回传，玩家改动仅本地有效）。 */
    public void applyTo(BiosphereConfig config) {
        config.passiveAttackEnabled = passiveAttackEnabled;
        config.environmentalChangeEnabled = environmentalChangeEnabled;
        config.spawnDataEnabled = spawnDataEnabled;
        config.meleeDamage = meleeDamage;
        config.meleeSpeed = meleeSpeed;
        config.meleeCooldownTicks = meleeCooldownTicks;
        config.avoidSpeed = avoidSpeed;
        config.mushroomConversionChance = mushroomConversionChance;
        config.creefernOverride = creefernOverride;
        config.creefernForceOverride = creefernForceOverride;
        config.creefernMinHealth = creefernMinHealth;
        config.creefernMaxHealth = creefernMaxHealth;
    }

    public static BiosphereConfigPayload of(BiosphereConfig config) {
        return new BiosphereConfigPayload(
                config.passiveAttackEnabled,
                config.environmentalChangeEnabled,
                config.spawnDataEnabled,
                config.meleeDamage,
                config.meleeSpeed,
                config.meleeCooldownTicks,
                config.avoidSpeed,
                config.mushroomConversionChance,
                config.creefernOverride,
                config.creefernForceOverride,
                config.creefernMinHealth,
                config.creefernMaxHealth
        );
    }
}

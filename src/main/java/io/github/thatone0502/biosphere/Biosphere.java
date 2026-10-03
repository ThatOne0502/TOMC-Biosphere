package io.github.thatone0502.biosphere;

import io.github.thatone0502.biosphere.config.BiosphereConfig;
import io.github.thatone0502.biosphere.environmental.EnvironmentalChangeManager;
import io.github.thatone0502.biosphere.foodchain.FoodChainManager;
import io.github.thatone0502.biosphere.network.BiosphereConfigPayload;
import io.github.thatone0502.biosphere.spawn.SpawnManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Biosphere implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "biosphere";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AutoConfig.register(BiosphereConfig.class, GsonConfigSerializer::new);
        BiosphereConfig config = AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig();

        // creefern 自动检测：检测到 creefern 时 auto 自动变 true（每次加载重算，覆盖文件值）。
        config.creefernAuto = FabricLoader.getInstance().isModLoaded("creefern");

        // 服务器权威配置同步（S2C）：玩家加入时下发，客户端只读缓存。
        PayloadTypeRegistry.playS2C().register(BiosphereConfigPayload.TYPE, BiosphereConfigPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.player, BiosphereConfigPayload.of(config)));

        // 数据驱动生物刷新（初始化阶段 API，须在 BiomeModifications 应用前注册）。
        if (config.spawnDataEnabled) {
            SpawnManager.register();
        }

        // 世界加载：加载食物链关系 + 环境变化规则。
        ServerWorldEvents.LOAD.register((server, world) -> {
            FoodChainManager.load(server, world.registryAccess());
            EnvironmentalChangeManager.load(server, world);
        });
        LOGGER.info("TOMC-Biosphere enabled");
    }

    @Override
    public void onInitializeClient() {
        // 客户端接收服务器下发的配置，覆盖本地缓存字段（玩家改动仅本地有效）。
        ClientPlayNetworking.registerGlobalReceiver(BiosphereConfigPayload.TYPE, (payload, context) ->
                payload.applyTo(AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig()));
        LOGGER.info("TOMC-Biosphere client initialized");
    }
}

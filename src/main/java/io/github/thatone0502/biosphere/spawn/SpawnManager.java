package io.github.thatone0502.biosphere.spawn;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据驱动的生物刷新位置：onInitialize 阶段从 mod 自带资源
 * data/biosphere/biosphere_spawns/ 目录读取规则（每个生物一个 json 文件），
 * 注册 BiomeModifications.addSpawn。
 * <p>
 * 单文件规则格式：
 * <pre>
 * {
 *   "biomes": ["minecraft:small_end_islands", "minecraft:end_barrens"],
 *   "entity": "minecraft:phantom",
 *   "category": "monster",
 *   "weight": 8, "min": 2, "max": 4
 * }
 * </pre>
 * BiomeModifications.addSpawn 是初始化阶段 API，因此该数据文件为 mod 内置资源，非运行时数据包。
 */
public final class SpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("biosphere");

    private SpawnManager() {}

    public static void register() {
        ModContainer container = FabricLoader.getInstance().getModContainer("biosphere").orElse(null);
        if (container == null) {
            LOGGER.warn("[biosphere] mod container not found");
            return;
        }
        int count = 0;
        for (Path root : container.getRootPaths()) {
            count += loadFromRoot(root);
        }
        LOGGER.info("[biosphere] registered {} spawn rules", count);
    }

    /** 从单个 mod root（目录或 jar）加载 data/biosphere/biosphere_spawns/ 下全部 *.json。 */
    private static int loadFromRoot(Path root) {
        try {
            if (Files.isDirectory(root)) {
                return loadFromDir(root.resolve("data/biosphere/biosphere_spawns"));
            }
            if (root.toString().endsWith(".jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(root, (ClassLoader) null)) {
                    return loadFromDir(fs.getPath("data/biosphere/biosphere_spawns"));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[biosphere] failed to load spawns from {}", root, ex);
        }
        return 0;
    }

    private static int loadFromDir(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return 0;
        int n = 0;
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (root.isJsonObject() && registerOne(root.getAsJsonObject())) {
                        n++;
                    }
                } catch (Exception ex) {
                    LOGGER.error("[biosphere] failed to load spawn file {}", file, ex);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[biosphere] failed to list spawn dir {}", dir, ex);
        }
        return n;
    }

    private static boolean registerOne(JsonObject obj) {
        if (!obj.has("biomes") || !obj.has("entity")) return false;

        Identifier[] biomeIds = parseStringArray(obj.get("biomes"));
        if (biomeIds.length == 0) return false;

        ResourceKey<Biome>[] keys = new ResourceKey[biomeIds.length];
        for (int i = 0; i < biomeIds.length; i++) {
            keys[i] = ResourceKey.create(Registries.BIOME, biomeIds[i]);
        }

        EntityType<?> entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(obj.get("entity").getAsString()));
        if (entity == null) return false;

        MobCategory category = MobCategory.MONSTER;
        if (obj.has("category") && obj.get("category").isJsonPrimitive()) {
            category = MobCategory.valueOf(obj.get("category").getAsString().toUpperCase());
        }

        int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 8;
        int min = obj.has("min") ? obj.get("min").getAsInt() : 2;
        int max = obj.has("max") ? obj.get("max").getAsInt() : 4;

        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(keys), category, entity, weight, min, max);
        return true;
    }

    private static Identifier[] parseStringArray(JsonElement element) {
        if (!element.isJsonArray()) return new Identifier[0];
        JsonArray arr = element.getAsJsonArray();
        Identifier[] out = new Identifier[arr.size()];
        int n = 0;
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive()) out[n++] = Identifier.parse(e.getAsString());
        }
        Identifier[] trimmed = new Identifier[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }
}

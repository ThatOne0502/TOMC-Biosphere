package io.github.thatone0502.biosphere.foodchain;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.github.thatone0502.biosphere.config.BiosphereConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据驱动的食物链关系（生物间攻击/规避）。
 * <p>
 * 关系文件来自 data/&lt;namespace&gt;/biosphere_food_chain/ 目录（含全部子目录，支持数据包扩展），
 * 每文件建议只定义一条关系：
 * <pre>
 * {
 *   "subjects": ["minecraft:chicken"],
 *   "objects": ["minecraft:silverfish"],
 *   "behavior_hint": "attack"      // attack | avoid
 * }
 * </pre>
 * 主体/目标均可为实体 ID 或 #标签。
 */
public final class FoodChainManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("biosphere");
    private static final Gson GSON = new Gson();

    private static final Map<EntityType<?>, List<EntityFilter>> ATTACK = new HashMap<>();
    private static final Map<EntityType<?>, List<EntityFilter>> AVOID = new HashMap<>();
    private static volatile boolean loaded = false;

    private FoodChainManager() {}

    public static void load(MinecraftServer server, RegistryAccess registryAccess) {
        if (loaded) return;
        loaded = true;
        if (!AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig().passiveAttackEnabled) return;

        ATTACK.clear();
        AVOID.clear();
        ResourceManager rm = server.getResourceManager();
        Map<Identifier, Resource> files = rm.listResources(
                "biosphere_food_chain", id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                RelationEntry entryObj = GSON.fromJson(reader, RelationEntry.class);
                if (entryObj == null || entryObj.subjects == null || entryObj.objects == null) continue;
                register(entryObj, registryAccess);
            } catch (Exception ex) {
                LOGGER.error("[biosphere] failed to load food_chain relation {}", entry.getKey(), ex);
            }
        }
        LOGGER.info("[biosphere] loaded {} attack subjects, {} avoid subjects",
                ATTACK.size(), AVOID.size());
    }

    private static void register(RelationEntry entry, RegistryAccess registryAccess) {
        boolean attack = "attack".equals(entry.behaviorHint);
        Map<EntityType<?>, List<EntityFilter>> target = attack ? ATTACK : AVOID;
        for (String subjectSpec : entry.subjects) {
            for (EntityType<?> subject : resolveSpec(subjectSpec, registryAccess)) {
                List<EntityFilter> filters = target.computeIfAbsent(subject, k -> new ArrayList<>());
                for (String objectSpec : entry.objects) {
                    filters.add(EntityFilter.of(objectSpec));
                }
            }
        }
    }

    private static List<EntityType<?>> resolveSpec(String spec, RegistryAccess registryAccess) {
        List<EntityType<?>> out = new ArrayList<>();
        if (spec.startsWith("#")) {
            Identifier id = Identifier.parse(spec.substring(1));
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, id);
            Registry<EntityType<?>> registry = registryAccess.lookupOrThrow(Registries.ENTITY_TYPE);
            for (Holder<EntityType<?>> holder : registry.getTagOrEmpty(tag)) {
                out.add(holder.value());
            }
        } else {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(spec));
            if (type != null) out.add(type);
        }
        return out;
    }

    public static boolean hasAttackFilters(EntityType<?> subject) {
        return ATTACK.containsKey(subject);
    }

    public static List<EntityFilter> attackFilters(EntityType<?> subject) {
        return ATTACK.get(subject);
    }

    public static boolean hasAvoidFilters(EntityType<?> subject) {
        return AVOID.containsKey(subject);
    }

    public static List<EntityFilter> avoidFilters(EntityType<?> subject) {
        return AVOID.get(subject);
    }

    /** 目标过滤器：实体 ID 或 #标签。 */
    public static final class EntityFilter {
        private final TagKey<EntityType<?>> tag;
        private final EntityType<?> entity;

        private EntityFilter(TagKey<EntityType<?>> tag, EntityType<?> entity) {
            this.tag = tag;
            this.entity = entity;
        }

        public static EntityFilter of(String spec) {
            if (spec.startsWith("#")) {
                Identifier id = Identifier.parse(spec.substring(1));
                return new EntityFilter(TagKey.create(Registries.ENTITY_TYPE, id), null);
            }
            return new EntityFilter(null, BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(spec)));
        }

        public boolean matches(EntityType<?> type) {
            if (tag != null) {
                return type.builtInRegistryHolder().is(tag);
            }
            return entity == type;
        }
    }

    private static final class RelationEntry {
        public List<String> subjects;
        public List<String> objects;
        @SerializedName("behavior_hint")
        public String behaviorHint;
    }
}

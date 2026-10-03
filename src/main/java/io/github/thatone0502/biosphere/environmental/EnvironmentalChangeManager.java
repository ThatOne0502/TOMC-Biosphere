package io.github.thatone0502.biosphere.environmental;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.thatone0502.biosphere.config.BiosphereConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 数据驱动的环境变化（随机刻生态演替）。
 * <p>
 * 规则来自 data/&lt;namespace&gt;/biosphere_environmental_change/ 目录（含全部子目录，
 * 支持数据包扩展），每文件可含单条规则（对象）或多条规则（数组）。单条规则语法：
 * <pre>
 * {
 *   "change_type": "setblock",           // setblock(默认) | data_merge
 *   "origin": ["minecraft:short_grass"], // 源方块 ID 或 #标签（建议单个）
 *   "result": ["minecraft:tall_grass"],  // 结果方块 ID 或 #标签（setblock 用，可含 air）
 *   "random_tick_chance": 5,             // 1/N 频率，模组驱动
 *   "mode": "replace",                   // replace(默认) | destroy | keep
 *   "position": "self",                  // self(默认) | 6 方向
 *   "offset": {"x":0,"y":0,"z":0},       // 精确偏移
 *   "result_states": {"age":"7"},        // 方块状态 key-value
 *   "result_nbt": "{...}",               // SNBT，方块实体数据
 *   "conditions": { ... },               // 单个 LootItemCondition（location_check 等）
 *   "summon": "{id:\"minecraft:item\",...}" // SNBT 含 id 实体
 * }
 * </pre>
 * 多格方块（DoublePlantBlock）放置自动识别两半格；origin 为多格方块时仅下半格触发。
 */
public final class EnvironmentalChangeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("biosphere");

    private static final List<ChangeRule> RULES = new ArrayList<>();
    private static final Set<Block> FROM_BLOCKS = new HashSet<>();
    private static volatile boolean loaded = false;

    private static final TagKey<Block> FLOWER_BLACKLIST =
            TagKey.create(Registries.BLOCK, Identifier.parse("biosphere:natural_flower_blacklist"));

    /** 谓词求值所需的 LootContext 参数集（origin 位置 + 方块状态，均为必需）。 */
    private static final ContextKeySet CONTEXT_KEYS = new ContextKeySet.Builder()
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
            .build();

    private EnvironmentalChangeManager() {}

    private static boolean isEnabled() {
        BiosphereConfig config = AutoConfig.getConfigHolder(BiosphereConfig.class).getConfig();
        return config.environmentalChangeEnabled;
    }

    /** 从资源管理器加载全部规则（首次世界加载时调用一次）。 */
    public static void load(MinecraftServer server, ServerLevel world) {
        if (loaded) return;
        loaded = true;
        RULES.clear();
        FROM_BLOCKS.clear();
        Registry<Block> registry = world.registryAccess().lookupOrThrow(Registries.BLOCK);
        ResourceManager rm = server.getResourceManager();
        Map<Identifier, Resource> files = rm.listResources(
                "biosphere_environmental_change", id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                for (JsonObject obj : toRuleObjects(root)) {
                    ChangeRule rule = parseRule(obj, registry, world.registryAccess());
                    if (rule != null) RULES.add(rule);
                }
            } catch (Exception ex) {
                LOGGER.error("[biosphere] failed to load environmental_change rule {}", entry.getKey(), ex);
            }
        }
        for (ChangeRule rule : RULES) FROM_BLOCKS.addAll(rule.origin);
        LOGGER.info("[biosphere] loaded {} environmental_change rules over {} source blocks",
                RULES.size(), FROM_BLOCKS.size());
    }

    private static List<JsonObject> toRuleObjects(JsonElement root) {
        List<JsonObject> out = new ArrayList<>();
        if (root.isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray()) {
                if (e.isJsonObject()) out.add(e.getAsJsonObject());
            }
        } else if (root.isJsonObject()) {
            out.add(root.getAsJsonObject());
        }
        return out;
    }

    private static ChangeRule parseRule(JsonObject obj, Registry<Block> registry, RegistryAccess registryAccess) {
        ChangeType type = parseEnum(obj, "change_type", ChangeType.SETBLOCK,
                Map.of("setblock", ChangeType.SETBLOCK, "data_merge", ChangeType.DATA_MERGE));

        Set<Block> origin = new HashSet<>();
        if (obj.has("origin") && obj.get("origin").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("origin")) {
                origin.addAll(resolveBlockSpec(e.getAsString(), registry));
            }
        }

        List<Block> result = new ArrayList<>();
        if (obj.has("result") && obj.get("result").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("result")) {
                result.addAll(resolveBlockSpec(e.getAsString(), registry));
            }
        }

        int randomTickChance = obj.has("random_tick_chance")
                ? Math.max(1, obj.get("random_tick_chance").getAsInt()) : 1;

        Mode mode = parseEnum(obj, "mode", Mode.REPLACE,
                Map.of("replace", Mode.REPLACE, "destroy", Mode.DESTROY, "keep", Mode.KEEP));

        Vec3i posOffset = parsePositionOffset(obj);

        Map<String, String> resultStates = new HashMap<>();
        if (obj.has("result_states") && obj.get("result_states").isJsonObject()) {
            JsonObject states = obj.getAsJsonObject("result_states");
            for (Map.Entry<String, JsonElement> e : states.entrySet()) {
                resultStates.put(e.getKey(), e.getValue().getAsString());
            }
        }

        CompoundTag resultNbt = null;
        if (obj.has("result_nbt") && obj.get("result_nbt").isJsonPrimitive()) {
            resultNbt = parseSnbt(obj.get("result_nbt").getAsString());
        }

        LootItemCondition condition = null;
        if (obj.has("conditions") && obj.get("conditions").isJsonObject()) {
            JsonElement condJson = obj.get("conditions");
            DataResult<LootItemCondition> parsed =
                    LootItemCondition.DIRECT_CODEC.parse(
                            RegistryOps.create(JsonOps.INSTANCE, registryAccess), condJson);
            condition = parsed.result().orElse(null);
            if (condition == null) {
                LOGGER.error("[biosphere] 条件谓词解析失败已忽略 origin={} conditions={} error={}",
                        obj.get("origin"), condJson,
                        parsed.error().map(e -> e.message()).orElse("未知错误"));
            }
        }

        CompoundTag summon = null;
        if (obj.has("summon") && obj.get("summon").isJsonPrimitive()) {
            summon = parseSnbt(obj.get("summon").getAsString());
        }

        if (origin.isEmpty()) return null;

        // 语义校验：setblock 必须有 result；data_merge 不允许 result，且必须有 result_states 或 result_nbt。
        if (type == ChangeType.SETBLOCK) {
            if (result.isEmpty()) return null;
        } else {
            if (!result.isEmpty() || (resultStates.isEmpty() && resultNbt == null)) return null;
        }

        return new ChangeRule(type, origin, result, randomTickChance, mode,
                posOffset.x, posOffset.y, posOffset.z,
                resultStates, resultNbt, condition, summon);
    }

    private static <E extends Enum<E>> E parseEnum(JsonObject obj, String key, E fallback,
                                                   Map<String, E> mapping) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
            E value = mapping.get(obj.get(key).getAsString());
            if (value != null) return value;
        }
        return fallback;
    }

    private static Vec3i parsePositionOffset(JsonObject obj) {
        Vec3i position = Position.SELF.vec;
        if (obj.has("position") && obj.get("position").isJsonPrimitive()) {
            Position pos = Position.byName(obj.get("position").getAsString());
            if (pos != null) position = pos.vec;
        }
        int ox = 0, oy = 0, oz = 0;
        if (obj.has("offset") && obj.get("offset").isJsonObject()) {
            JsonObject off = obj.getAsJsonObject("offset");
            ox = off.has("x") ? off.get("x").getAsInt() : 0;
            oy = off.has("y") ? off.get("y").getAsInt() : 0;
            oz = off.has("z") ? off.get("z").getAsInt() : 0;
        }
        return new Vec3i(position.x + ox, position.y + oy, position.z + oz);
    }

    private static CompoundTag parseSnbt(String snbt) {
        try {
            return TagParser.parseCompoundFully(snbt);
        } catch (CommandSyntaxException ex) {
            LOGGER.error("[biosphere] invalid SNBT: {}", snbt, ex);
            return null;
        }
    }

    /** 解析方块 ID 或 #标签，展开后按黑名单过滤。 */
    private static List<Block> resolveBlockSpec(String spec, Registry<Block> registry) {
        List<Block> out = new ArrayList<>();
        if (spec.startsWith("#")) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.parse(spec.substring(1)));
            for (Holder<Block> holder : registry.getTagOrEmpty(tag)) {
                Block block = holder.value();
                if (!block.builtInRegistryHolder().is(FLOWER_BLACKLIST)) out.add(block);
            }
        } else {
            Optional<Block> opt = registry.getOptional(Identifier.parse(spec));
            if (opt.isPresent()) {
                Block block = opt.get();
                if (!block.builtInRegistryHolder().is(FLOWER_BLACKLIST)) out.add(block);
            }
        }
        return out;
    }

    /** 该方块是否参与环境变化随机刻（供 BlockStateMixin 调用）。 */
    public static boolean isEcologyBlock(Block block) {
        return FROM_BLOCKS.contains(block);
    }

    /** 统一随机刻入口：每条匹配规则独立按 random_tick_chance 触发。 */
    public static void tick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!isEnabled()) return;
        Block block = state.getBlock();
        boolean originIsDouble = block instanceof DoublePlantBlock;
        // 多格方块仅在下半格触发（上半格由 placeAt 一并处理）。
        if (originIsDouble
                && state.hasProperty(DoublePlantBlock.HALF)
                && state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        for (ChangeRule rule : RULES) {
            if (!rule.origin.contains(block)) continue;
            if (random.nextInt(rule.randomTickChance) != 0) continue;
            if (!rule.matches(level, pos, state, random)) continue;
            rule.execute(level, pos, state, random, originIsDouble);
        }
    }

    /** 单条变化规则。 */
    private record ChangeRule(
            ChangeType type,
            Set<Block> origin,
            List<Block> result,
            int randomTickChance,
            Mode mode,
            int dx, int dy, int dz,
            Map<String, String> resultStates,
            CompoundTag resultNbt,
            LootItemCondition condition,
            CompoundTag summon
    ) {
        boolean matches(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            if (condition == null) return true;
            LootParams params = new LootParams.Builder(level)
                    .withOptionalParameter(LootContextParams.ORIGIN,
                            new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
                    .withOptionalParameter(LootContextParams.BLOCK_STATE, state)
                    .create(CONTEXT_KEYS);
            LootContext context = new LootContext.Builder(params)
                    .withOptionalRandomSource(random)
                    .create(Optional.empty());
            return condition.test(context);
        }

        void execute(ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
                     boolean originIsDouble) {
            BlockPos targetPos = pos.offset(dx, dy, dz);
            if (type == ChangeType.SETBLOCK) {
                Block target = result.get(random.nextInt(result.size()));
                executeSetblock(level, pos, targetPos, target, originIsDouble);
            } else {
                executeDataMerge(level, targetPos);
            }
        }

        private void executeSetblock(ServerLevel level, BlockPos pos, BlockPos targetPos,
                                     Block target, boolean originIsDouble) {
            BlockState current = level.getBlockState(targetPos);

            // mode 决定 targetPos 原方块的处理方式。
            if (mode == Mode.KEEP && !current.isAir()) return;
            if (mode == Mode.DESTROY && !current.isAir()) {
                level.destroyBlock(targetPos, true);
            }

            // origin 是双层方块且替换自身位置：先静默移除两半格，避免上半格残留或掉落。
            if (originIsDouble && targetPos.equals(pos)) {
                silentRemoveBoth(level, pos);
            }

            BlockState toPlace = target == Blocks.AIR
                    ? Blocks.AIR.defaultBlockState()
                    : applyStates(target.defaultBlockState());

            if (target == Blocks.AIR) {
                level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if (target instanceof DoublePlantBlock) {
                if (!level.isEmptyBlock(targetPos.above())) return;
                DoublePlantBlock.placeAt(level, toPlace, targetPos, Block.UPDATE_ALL);
            } else {
                level.setBlock(targetPos, toPlace, Block.UPDATE_ALL);
            }

            applyNbt(level, targetPos);
            spawn(level, targetPos);
        }

        private void executeDataMerge(ServerLevel level, BlockPos targetPos) {
            BlockState current = level.getBlockState(targetPos);
            if (current.isAir()) return;

            BlockState merged = applyStates(current);
            if (merged != current) {
                level.setBlock(targetPos, merged, Block.UPDATE_ALL);
            }
            applyNbt(level, targetPos);
            spawn(level, targetPos);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private BlockState applyStates(BlockState state) {
            BlockState out = state;
            for (Map.Entry<String, String> e : resultStates.entrySet()) {
                Property<?> prop = out.getBlock().getStateDefinition().getProperty(e.getKey());
                if (prop == null) continue;
                Optional<?> value = prop.getValue(e.getValue());
                if (value.isPresent()) {
                    out = out.setValue((Property) prop, (Comparable) value.get());
                }
            }
            return out;
        }

        private void applyNbt(ServerLevel level, BlockPos targetPos) {
            if (resultNbt == null) return;
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity != null) {
                blockEntity.loadWithComponents(TagValueInput.create(
                        ProblemReporter.DISCARDING, level.registryAccess(), resultNbt));
            }
        }

        private void spawn(ServerLevel level, BlockPos targetPos) {
            if (summon == null) return;
            Entity entity = EntityType.loadEntityRecursive(summon, level, EntitySpawnReason.LOAD, e -> {
                e.snapTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                        e.getYRot(), e.getXRot());
                return e;
            });
            if (entity != null) {
                level.tryAddFreshEntityWithPassengers(entity);
            }
        }
    }

    /**
     * 静默移除双层方块两半格（bug#1 修复）。
     * flags 仅含 UPDATE_SUPPRESS_DROPS(32)：移除下半格不通知上半格 updateShape，
     * 故上半格不自毁、不掉落；客户端等待随后 placeAt(UPDATE_ALL) 一次性同步。
     */
    private static void silentRemoveBoth(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
    }

    private enum ChangeType { SETBLOCK, DATA_MERGE }

    private enum Mode { REPLACE, DESTROY, KEEP }

    private enum Position {
        SELF(0, 0, 0),
        POSITIVE_X(1, 0, 0),
        NEGATIVE_X(-1, 0, 0),
        POSITIVE_Y(0, 1, 0),
        NEGATIVE_Y(0, -1, 0),
        POSITIVE_Z(0, 0, 1),
        NEGATIVE_Z(0, 0, -1);

        final Vec3i vec;

        Position(int x, int y, int z) {
            this.vec = new Vec3i(x, y, z);
        }

        static Position byName(String name) {
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /** 轻量三元整数偏移，避免依赖 net.minecraft.core.Vec3i 的构造细节。 */
    private record Vec3i(int x, int y, int z) {}
}

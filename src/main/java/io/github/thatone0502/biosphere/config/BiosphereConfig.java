package io.github.thatone0502.biosphere.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.github.thatone0502.biosphere.Biosphere;
import io.github.thatone0502.biosphere.enderman.EndermanHoldableProvider;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 服务端权威配置：外置 {@code config/biosphere.json}，手写 Gson 读写（参照 TOMC-Creefern）。
 * <p>
 * 从 AutoConfig（Cloth Config 的注解驱动）迁移而来：配置页改为自定义单页
 * {@code BiosphereModConfigScreen}（{@code setGlobalized(true)}，顶部搜索栏 + 左侧分类引用栏），
 * 不再依赖 AutoConfig 的多页签自动屏幕。字段名与默认值保持不变，旧配置 JSON 可直接沿用。
 */
public final class BiosphereConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "biosphere.json";

    private static BiosphereConfig instance = new BiosphereConfig();

    // ---- modules 类 -------------------------------------------------------

    /** 是否加载被动攻击（食物链关系）。 */
    public boolean passiveAttackEnabled = true;
    /** 是否加载环境演替（随机刻方块变化）。 */
    public boolean environmentalChangeEnabled = true;
    /** 是否加载自然刷新数据。 */
    public boolean spawnDataEnabled = true;
    /** 牛吃菌丝后转化为哞菇的概率，取值 [0, 1]。 */
    public double mushroomConversionChance = 0.2;

    // ---- passive_attack 类 ------------------------------------------------

    /** 被动生物新增近战攻击的伤害。 */
    public float meleeDamage = 3.0f;
    /** 追击关系目标时的速度倍率。 */
    public double meleeSpeed = 1.5;
    /** 两次近战攻击之间的间隔（tick，20 = 1 秒）。 */
    public int meleeCooldownTicks = 20;
    /** 逃离关系威胁时的速度倍率。 */
    public double avoidSpeed = 1.2;

    // ---- creefern 类 ------------------------------------------------------

    /** 是否启用 creefern 检测的手动覆盖。 */
    public boolean creefernOverride = false;
    /** 手动覆盖值：true = 强制开启，false = 强制跳过（仅 override=true 时生效）。 */
    public boolean creefernForceOverride = false;
    /** creefern 自动检测结果（启动时由 {@code isModLoaded("creefern")} 覆盖）。 */
    public boolean creefernAuto = false;
    /** 苦力怕敌视目标的生命值下限。 */
    public float creefernMinHealth = 20.0f;
    /** 苦力怕敌视目标的生命值上限。 */
    public float creefernMaxHealth = 64.5f;

    // ---- enderman 类 ------------------------------------------------------

    /** 白名单默认勾选的分类：原版「自然方块」。 */
    public static final String DEFAULT_ENDERMAN_TAB = "minecraft:natural_blocks";

    /**
     * 是否覆写末影人可搬运方块白名单。
     * <p>
     * true = 无视数据包里的 {@code #minecraft:enderman_holdable}，改用「勾选分类展开出的方块集合」；
     * false = 完全按原版标签。无论开关如何，{@code #biosphere:enderman_holdable_blacklist} 永远生效。
     */
    public boolean endermanHoldableOverride = true;

    /** 参与白名单的创造模式物品栏分类 ID（默认仅原版「自然方块」）。 */
    public List<String> endermanHoldableTabs = new ArrayList<>(List.of(DEFAULT_ENDERMAN_TAB));

    public BiosphereConfig() {
        // 字段默认值见上方声明；构造器保持显式存在，便于 Gson 反序列化覆盖。
    }

    /** 全局配置实例；任何调用方都应通过它读取（不持有副本）。 */
    public static BiosphereConfig get() {
        return instance;
    }

    /** 配置文件路径：{@code <configDir>/biosphere.json}。 */
    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /** 载入配置：文件不存在则用默认值并立即落盘；存在则反序列化覆盖，随后做越界钳制。 */
    public static void load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                BiosphereConfig loaded = GSON.fromJson(reader, BiosphereConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                // 配置损坏不应阻断启动：保留默认值并覆盖写回一份干净文件。
                Biosphere.LOGGER.warn("[biosphere] 配置读取失败，回退默认值: {}", e.toString());
            }
        }
        clamp();
        save();
    }

    /** 保存配置到磁盘（配置页的 setSavingRunnable 直接引用本方法）。 */
    public static void save() {
        // 配置变更后让末影人白名单缓存失效，下次判定时按新配置重建（隐式热加载）。
        EndermanHoldableProvider.markDirty();
        Path path = configPath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            Biosphere.LOGGER.warn("[biosphere] 配置写入失败: {}", e.toString());
        }
    }

    /** 越界钳制：负值归零；概率钳到 [0,1]；生命值下限 > 上限时按下限 = 上限兜底。 */
    private static void clamp() {
        instance.meleeDamage = Math.max(0.0f, instance.meleeDamage);
        instance.meleeSpeed = Math.max(0.0, instance.meleeSpeed);
        instance.meleeCooldownTicks = Math.max(0, instance.meleeCooldownTicks);
        instance.avoidSpeed = Math.max(0.0, instance.avoidSpeed);
        instance.mushroomConversionChance = Math.max(0.0, Math.min(1.0, instance.mushroomConversionChance));
        instance.creefernMinHealth = Math.max(0.0f, instance.creefernMinHealth);
        instance.creefernMaxHealth = Math.max(0.0f, instance.creefernMaxHealth);
        if (instance.creefernMinHealth > instance.creefernMaxHealth) {
            instance.creefernMinHealth = instance.creefernMaxHealth;
        }
        if (instance.endermanHoldableTabs == null) {
            instance.endermanHoldableTabs = new ArrayList<>(List.of(DEFAULT_ENDERMAN_TAB));
        }
    }

    /**
     * 苦力怕新设计有效开关：
     * override=false 时以 auto 为准；override=true 时 forceOverride 生效
     * （true=强制开启，false=强制跳过）。
     */
    public boolean creefernEffective() {
        return creefernOverride ? creefernForceOverride : creefernAuto;
    }
}

package io.github.thatone0502.biosphere.client;

import io.github.thatone0502.biosphere.config.BiosphereConfig;
import io.github.thatone0502.biosphere.enderman.EndermanHoldableProvider;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Arrays;
import java.util.List;

/**
 * Cloth Config 配置页（顶部搜索栏 + 左侧分类引用栏 + 滚轮），参照 TOMC-Creefern。
 * <p>
 * 用 {@code setGlobalized(true)} 提供顶部搜索栏，并把整页换成
 * {@code GlobalizedClothConfigScreen}——它没有顶部分类页签，分类以<b>左侧引用栏</b>呈现
 * （「全部整合到 1 页 + 可滚动 + 左侧目录跳转」）。四个分类
 * modules / passive_attack / creefern / enderman 分别成为左侧目录的一个跳转项。
 * <p>
 * <b>服务端权威</b>：配置始终以服务端为准。仅当「本机即服务端」（单人/本地集成服务器，
 * 或尚未进入任何世界）时页面可编辑；其余情况（作为客人连入多人服务器）所有条目置灰只读，
 * 只展示服务端下发的值。
 * <p>
 * 客户端专属类：只能被 modmenu entrypoint（ModMenuIntegration）引用。
 */
public final class BiosphereModConfigScreen {

    private BiosphereModConfigScreen() {
    }

    /** 构造配置页。{@code parent} 可为 null（Mod Menu 在游戏内打开时会传入模组列表页）。 */
    public static Screen create(Screen parent) {
        BiosphereConfig config = BiosphereConfig.get();
        boolean editable = isEditable();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.biosphere.title"));
        if (editable) {
            // 保存按钮的落盘动作：写 config/biosphere.json。
            builder.setSavingRunnable(BiosphereConfig::save);
        } else {
            // 只读模式：不给保存按钮任何落盘动作，避免把服务端下发的值写回本地文件。
            builder.setSavingRunnable(() -> {
            });
        }
        // 顶部搜索栏 + 左侧分类引用栏（设计要求）。setGlobalized 返回 void，只能单独一行。
        builder.setGlobalized(true);

        ConfigEntryBuilder entries = builder.entryBuilder();

        // ---- 分类：功能模块 -------------------------------------------------
        ConfigCategory modules = builder.getOrCreateCategory(
                Component.translatable("config.biosphere.category.modules"));
        addReadonlyNotice(modules, entries, editable);
        modules.addEntry(gate(entries.startBooleanToggle(label("passiveAttackEnabled"), config.passiveAttackEnabled)
                .setDefaultValue(true)
                .setTooltip(tooltip("passiveAttackEnabled"))
                .setSaveConsumer(value -> config.passiveAttackEnabled = value)
                .build(), editable));
        modules.addEntry(gate(entries.startBooleanToggle(label("environmentalChangeEnabled"), config.environmentalChangeEnabled)
                .setDefaultValue(true)
                .setTooltip(tooltip("environmentalChangeEnabled"))
                .setSaveConsumer(value -> config.environmentalChangeEnabled = value)
                .build(), editable));
        modules.addEntry(gate(entries.startBooleanToggle(label("spawnDataEnabled"), config.spawnDataEnabled)
                .setDefaultValue(true)
                .setTooltip(tooltip("spawnDataEnabled"))
                .setSaveConsumer(value -> config.spawnDataEnabled = value)
                .build(), editable));
        modules.addEntry(gate(entries.startDoubleField(label("mushroomConversionChance"), config.mushroomConversionChance)
                .setDefaultValue(0.2)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(tooltip("mushroomConversionChance"))
                .setSaveConsumer(value -> config.mushroomConversionChance = value)
                .build(), editable));

        // ---- 分类：被动生物攻击 ---------------------------------------------
        ConfigCategory passiveAttack = builder.getOrCreateCategory(
                Component.translatable("config.biosphere.category.passive_attack"));
        addReadonlyNotice(passiveAttack, entries, editable);
        passiveAttack.addEntry(gate(entries.startFloatField(label("meleeDamage"), config.meleeDamage)
                .setDefaultValue(3.0f)
                .setMin(0.0f)
                .setTooltip(tooltip("meleeDamage"))
                .setSaveConsumer(value -> config.meleeDamage = value)
                .build(), editable));
        passiveAttack.addEntry(gate(entries.startDoubleField(label("meleeSpeed"), config.meleeSpeed)
                .setDefaultValue(1.5)
                .setMin(0.0)
                .setTooltip(tooltip("meleeSpeed"))
                .setSaveConsumer(value -> config.meleeSpeed = value)
                .build(), editable));
        passiveAttack.addEntry(gate(entries.startIntField(label("meleeCooldownTicks"), config.meleeCooldownTicks)
                .setDefaultValue(20)
                .setMin(0)
                .setTooltip(tooltip("meleeCooldownTicks"))
                .setSaveConsumer(value -> config.meleeCooldownTicks = value)
                .build(), editable));
        passiveAttack.addEntry(gate(entries.startDoubleField(label("avoidSpeed"), config.avoidSpeed)
                .setDefaultValue(1.2)
                .setMin(0.0)
                .setTooltip(tooltip("avoidSpeed"))
                .setSaveConsumer(value -> config.avoidSpeed = value)
                .build(), editable));

        // ---- 分类：末影人搬运方块 -------------------------------------------
        ConfigCategory enderman = builder.getOrCreateCategory(
                Component.translatable("config.biosphere.category.enderman"));
        addReadonlyNotice(enderman, entries, editable);
        enderman.addEntry(gate(entries.startBooleanToggle(
                        label("endermanHoldableOverride"), config.endermanHoldableOverride)
                .setDefaultValue(true)
                .setTooltip(tooltip("endermanHoldableOverride"))
                .setSaveConsumer(value -> config.endermanHoldableOverride = value)
                .build(), editable));

        // 子区域：勾选参与白名单的创造模式物品栏分类（默认仅「自然方块」）。
        //
        // 这里必须只取「物品分类」（CreativeModeTab.Type.CATEGORY）。原因见
        // EndermanHoldableProvider#selectableTabs()：CreativeModeTab#shouldDisplay() 的判定是
        // type == CATEGORY ? hasAnyItems() : true，所以未进入世界时漏出来的一直是
        // 「已保存的物品栏 / 搜索物品 / 生存模式物品栏」这三类非分类页签——它们不是可选项。
        List<CreativeModeTab> availableTabs = EndermanHoldableProvider.selectableTabs();
        // 真正的成因是「有没有进过世界」：分类内容由客户端进入世界后才构建，在此之前
        // 一个物品分类都不满足 hasAnyItems()。所以按环境判断，比按「列表是否为空」更精准；
        // 空列表的兜底仍然保留，免得万一真出现空列表时给玩家画一个空盒子。
        boolean tabsReady = Minecraft.getInstance().level != null && !availableTabs.isEmpty();
        if (!tabsReady) {
            enderman.addEntry(entries.startTextDescription(
                            Component.translatable("config.biosphere.endermanHoldableTabs.notLoaded")
                                    .withStyle(ChatFormatting.YELLOW))
                    .build());
        } else {
            SubCategoryBuilder tabs = entries.startSubCategory(
                            Component.translatable("config.biosphere.endermanHoldableTabs"))
                    .setExpanded(false)
                    .setTooltip(tooltip("endermanHoldableTabs"));
            for (CreativeModeTab tab : availableTabs) {
                Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                if (id == null) {
                    continue;
                }
                String key = id.toString();
                boolean checked = config.endermanHoldableTabs.contains(key);
                tabs.add(gate(entries.startBooleanToggle(tab.getDisplayName(), checked)
                        .setDefaultValue(BiosphereConfig.DEFAULT_ENDERMAN_TAB.equals(key))
                        .setSaveConsumer(selected -> {
                            if (selected) {
                                if (!config.endermanHoldableTabs.contains(key)) {
                                    config.endermanHoldableTabs.add(key);
                                }
                            } else {
                                config.endermanHoldableTabs.remove(key);
                            }
                        })
                        .build(), editable));
            }
            enderman.addEntry(tabs.build());
        }

        // ---- 分类：creefern 联动 -------------------------------------------
        ConfigCategory creefern = builder.getOrCreateCategory(
                Component.translatable("config.biosphere.category.creefern"));
        addReadonlyNotice(creefern, entries, editable);
        // 这两个开关要留引用：下面那行状态是**实时**读它们当前的值算出来的。
        BooleanListEntry overrideToggle = gate(entries.startBooleanToggle(
                        label("creefernOverride"), config.creefernOverride)
                .setDefaultValue(false)
                .setTooltip(tooltip("creefernOverride"))
                .setSaveConsumer(value -> config.creefernOverride = value)
                .build(), editable);
        creefern.addEntry(overrideToggle);
        BooleanListEntry forceToggle = gate(entries.startBooleanToggle(
                        label("creefernForceOverride"), config.creefernForceOverride)
                .setDefaultValue(false)
                .setTooltip(tooltip("creefernForceOverride"))
                .setSaveConsumer(value -> config.creefernForceOverride = value)
                .build(), editable);
        creefern.addEntry(forceToggle);
        // 这两行是两个不同的判据，别混：
        //   第一行 creefernAuto          ——「检测到了吗」，纯粹的环境事实，启动时由 isModLoaded 写入；
        //   第二行 creefernEffective()   ——「功能实际开没开」= 覆盖 creefern 检测 ? 强制覆盖值 : 自动检测。
        // 之前第二行直接读 creefernAuto，于是即使开了「强制覆盖」它也照样显示成关——是错的。
        ChatFormatting detectedColor = config.creefernAuto ? ChatFormatting.GREEN : ChatFormatting.RED;
        creefern.addEntry(entries.startTextDescription(
                        Component.translatable("config.biosphere.creefern.status",
                                        Component.translatable(config.creefernAuto
                                                ? "config.biosphere.creefern.yes"
                                                : "config.biosphere.creefern.no"))
                                .withStyle(detectedColor))
                .build());
        // 第二行跟随「实际是否生效」，读的是上面两个开关<b>当前</b>的值（不是 config 里保存过的值），
        // 所以拨开关的瞬间这一行就会改字变色，不必退出配置页重进。
        // 按「为什么关」分两种说法：没检测到 → 提示可以用上面的开关强制开启；
        // 被强制覆盖关掉 → 说明是「强制覆盖值」为 false，提示改 true。
        // 提示语只在确实能用得上的那一种里出现。
        creefern.addEntry(new LiveTextListEntry(
                creefernState(config, overrideToggle.getValue(), forceToggle.getValue()),
                () -> creefernState(config, overrideToggle.getValue(), forceToggle.getValue())));
        creefern.addEntry(gate(entries.startFloatField(label("creefernMinHealth"), config.creefernMinHealth)
                .setDefaultValue(20.0f)
                .setMin(0.0f)
                .setTooltip(tooltip("creefernMinHealth"))
                .setSaveConsumer(value -> config.creefernMinHealth = value)
                .build(), editable));
        creefern.addEntry(gate(entries.startFloatField(label("creefernMaxHealth"), config.creefernMaxHealth)
                .setDefaultValue(64.5f)
                .setMin(0.0f)
                .setTooltip(tooltip("creefernMaxHealth"))
                .setSaveConsumer(value -> config.creefernMaxHealth = value)
                .build(), editable));

        return builder.build();
    }

    /**
     * 是否可编辑：仅当「本机即服务端」时为 true。
     * <p>
     * 未进入任何世界（{@code getConnection() == null}，例如主菜单里打开配置）→ 可编辑（改的是本地
     * config 文件）；单人/本地集成服务器（{@code hasSingleplayerServer()}）→ 可编辑；其余（作为客人
     * 连入多人服务器）→ 只读。
     */
    private static boolean isEditable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() == null || minecraft.hasSingleplayerServer();
    }

    /** 只读模式下在分类顶部插入一行说明（否则玩家不知道条目为什么点不动）。 */
    private static void addReadonlyNotice(ConfigCategory category, ConfigEntryBuilder entries, boolean editable) {
        if (!editable) {
            category.addEntry(entries.startTextDescription(
                    Component.translatable("config.biosphere.readonly")).build());
        }
    }

    /** 只读模式下把条目置灰（Cloth 的 setEditable 只能在 build() 之后调用）。 */
    private static <T extends AbstractConfigListEntry<?>> T gate(T entry, boolean editable) {
        if (!editable) {
            entry.setEditable(false);
        }
        return entry;
    }

    /**
     * creefern 状态行的正文：{@code 覆盖 creefern 检测 ? 强制覆盖值 : creefern 自动检测}。
     * <p>
     * 两个参数取的是开关控件当前的值，因此拨动开关时这一行的内容与颜色会立刻跟着变。
     */
    private static Component creefernState(BiosphereConfig config, boolean override, boolean forced) {
        boolean active = override ? forced : config.creefernAuto;
        String key;
        if (active) {
            key = "config.biosphere.creefern.on";
        } else {
            key = override
                    ? "config.biosphere.creefern.off.forced"
                    : "config.biosphere.creefern.off.notDetected";
        }
        return Component.translatable(key).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** 条目标题：{@code config.biosphere.<key>}。 */
    private static Component label(String key) {
        return Component.translatable("config.biosphere." + key);
    }

    /**
     * 条目 tooltip：{@code config.biosphere.<key>.tooltip}；值里的 {@code \n} 切成<b>多行</b>。
     * 理由与 Creefern 一致：Cloth 的 {@code setTooltip(Component...)} 每个实参就是独立一行，
     * 自己按 {@code \n} 切开再逐个传，换行是确定的。
     */
    private static Component[] tooltip(String key) {
        return Arrays.stream(Component.translatable("config.biosphere." + key + ".tooltip").getString().split("\n"))
                .map(Component::literal)
                .toArray(Component[]::new);
    }
}

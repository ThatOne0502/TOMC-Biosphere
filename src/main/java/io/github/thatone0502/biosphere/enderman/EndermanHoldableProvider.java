package io.github.thatone0502.biosphere.enderman;

import io.github.thatone0502.biosphere.Biosphere;
import io.github.thatone0502.biosphere.BiosphereApi;
import io.github.thatone0502.biosphere.BiosphereTags;
import io.github.thatone0502.biosphere.config.BiosphereConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 末影人可拾取白名单的展开与判定（服务端）。
 * <p>
 * 「勾选的创造模式物品栏分类」在服务端第一次判定时展开成方块集合并缓存；配置保存后
 * 标记失效，下次判定自动重建（隐式热加载，无需重启）。
 */
public final class EndermanHoldableProvider {

    private static final Object LOCK = new Object();

    private static Set<Block> cachedBlocks;
    private static volatile boolean dirty = true;

    private EndermanHoldableProvider() {
    }

    /** 标记缓存失效（配置保存后调用）。 */
    public static void markDirty() {
        dirty = true;
    }

    /**
     * 预热：世界加载时先展开一次白名单，避免末影人第一次尝试拿方块时才做展开。
     * 失败不抛出（内部已捕获并记录日志）。
     */
    public static void warmUp(ServerLevel level) {
        expandedBlocks(level);
    }

    /**
     * 判定方块状态是否可被末影人拾取。
     * <p>
     * 判定顺序：
     * <ol>
     *   <li>黑名单标签 {@code #biosphere:enderman_holdable_blacklist} —— <b>永远生效</b>，优先于一切；</li>
     *   <li>覆写关闭 —— 退回原版 {@code #minecraft:enderman_holdable} 标签；</li>
     *   <li>覆写开启 —— 排除多方块结构，其余以「勾选分类展开出的方块集合」为准。</li>
     * </ol>
     */
    public static boolean isHoldable(BlockState state, ServerLevel level) {
        if (state.is(BiosphereTags.ENDERMAN_HOLDABLE_BLACKLIST)) {
            return false;
        }
        if (!BiosphereConfig.get().endermanHoldableOverride) {
            return state.is(BlockTags.ENDERMAN_HOLDABLE);
        }
        if (BiosphereApi.isMultiblock(state)) {
            return false;
        }
        return expandedBlocks(level).contains(state.getBlock());
    }

    /**
     * 可作为白名单来源的创造模式物品栏分类：只保留 {@link CreativeModeTab.Type#CATEGORY}。
     * <p>
     * 必须按<b>类型</b>过滤，不能按身份：「已保存的物品栏 / 搜索物品 / 生存模式物品栏」
     * 分别是 {@code HOTBAR / SEARCH / INVENTORY} 类型，它们不是物品分类，永远不该出现在可选项里。
     * （1.21.11 的 {@code CreativeModeTabs} 只有 {@code private static final ResourceKey} 常量，
     * 没有公开的 {@code CreativeModeTab} 常量，按身份过滤也无从下手。）
     * <p>
     * 另一个必须过滤的理由在 {@link CreativeModeTab#shouldDisplay()}：
     * 判定是 {@code type == CATEGORY ? hasAnyItems() : true} —— 非分类页签恒为可见，
     * 而真正的物品分类要等内容构建完（客户端进世界后 / 服务端显式 rebuild）才有 {@code hasAnyItems()}。
     */
    public static List<CreativeModeTab> selectableTabs() {
        return CreativeModeTabs.tabs().stream()
                .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
                .toList();
    }

    private static Set<Block> expandedBlocks(ServerLevel level) {
        if (cachedBlocks != null && !dirty) {
            return cachedBlocks;
        }
        synchronized (LOCK) {
            if (cachedBlocks != null && !dirty) {
                return cachedBlocks;
            }
            cachedBlocks = build(level);
            dirty = false;
            return cachedBlocks;
        }
    }

    private static Set<Block> build(ServerLevel level) {
        List<String> selectedList = BiosphereConfig.get().endermanHoldableTabs;
        if (selectedList == null || selectedList.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> selected = new HashSet<>(selectedList);

        MinecraftServer server = level.getServer();
        try {
            // 服务端默认从不构建创造模式物品栏内容（原版只在客户端打开物品栏时构建），
            // 这里显式触发一次，使 CreativeModeTab#getDisplayItems() 可用。
            CreativeModeTabs.tryRebuildTabContents(
                    server.getWorldData().enabledFeatures(), false, server.registryAccess());

            Set<Block> blocks = new HashSet<>();
            for (CreativeModeTab tab : selectableTabs()) {
                Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                if (id == null || !selected.contains(id.toString())) {
                    continue;
                }
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (stack.getItem() instanceof BlockItem blockItem) {
                        blocks.add(blockItem.getBlock());
                    }
                }
            }
            Biosphere.LOGGER.info("[biosphere] 展开末影人可拾取白名单：{} 个分类 → {} 个方块",
                    selected.size(), blocks.size());
            return blocks;
        } catch (Throwable t) {
            Biosphere.LOGGER.warn("[biosphere] 展开创造模式物品栏分类失败，白名单退化为空: {}", t.toString());
            return Collections.emptySet();
        }
    }
}

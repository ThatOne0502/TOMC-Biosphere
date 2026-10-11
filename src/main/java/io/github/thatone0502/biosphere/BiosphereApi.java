package io.github.thatone0502.biosphere;

import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 供其他模组调用的扩展接口。
 * <p>
 * 目前提供「多方块结构」判定扩展：被判定为多方块结构的方块不会被末影人拾取
 * （避免拿起床/门/大型花这类占位两格的方块的其中一半而破坏结构）。
 */
public final class BiosphereApi {

    private static final List<Predicate<BlockState>> MULTIBLOCK_FILTERS = new CopyOnWriteArrayList<>();

    static {
        // 内置三类占位两格的方块：
        //   BedBlock            床
        //   DoorBlock           门
        //   DoublePlantBlock    双层植物（含子类 TallFlowerBlock：向日葵/丁香/玫瑰丛/牡丹等大型花）
        registerMultiblockFilter(BedBlock.class);
        registerMultiblockFilter(DoorBlock.class);
        registerMultiblockFilter(DoublePlantBlock.class);
    }

    private BiosphereApi() {
    }

    /**
     * 注册一个自定义「多方块结构」方块类：该类及其子类都不会被末影人拾取。
     *
     * @param blockClass 方块类（如 {@code MyMultiblockBlock.class}）
     */
    public static void registerMultiblockFilter(Class<? extends Block> blockClass) {
        Objects.requireNonNull(blockClass, "blockClass");
        registerMultiblockFilter(state -> blockClass.isInstance(state.getBlock()));
    }

    /**
     * 注册一个自定义「多方块结构」判定：返回 true 的方块状态不会被末影人拾取。
     *
     * @param filter 判定谓词
     */
    public static void registerMultiblockFilter(Predicate<BlockState> filter) {
        MULTIBLOCK_FILTERS.add(Objects.requireNonNull(filter, "filter"));
    }

    /** 是否为多方块结构（不可被末影人拾取）。 */
    public static boolean isMultiblock(BlockState state) {
        for (Predicate<BlockState> filter : MULTIBLOCK_FILTERS) {
            if (filter.test(state)) {
                return true;
            }
        }
        return false;
    }
}

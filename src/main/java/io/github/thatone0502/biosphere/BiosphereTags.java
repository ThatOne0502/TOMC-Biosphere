package io.github.thatone0502.biosphere;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** 本模组定义/使用的数据包标签。 */
public final class BiosphereTags {

    /**
     * 末影人永不可拾取的方块。
     * <p>
     * 无论「末影人移动方块白名单覆写」开关如何，本标签始终生效（硬排除）。
     * 默认含：末地石、基岩、末地传送门框架、折跃门方块，以及管理员标签页的全部方块
     * （命令方块 ×3、拼图方块、结构方块、结构空位、屏障、光照方块）。
     */
    public static final TagKey<Block> ENDERMAN_HOLDABLE_BLACKLIST =
            TagKey.create(Registries.BLOCK, Identifier.parse(Biosphere.MOD_ID + ":enderman_holdable_blacklist"));

    private BiosphereTags() {
    }
}

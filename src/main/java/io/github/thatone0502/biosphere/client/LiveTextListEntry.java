package io.github.thatone0502.biosphere.client;

import io.github.thatone0502.biosphere.Biosphere;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 正文会随游戏状态变化的只读文本行。
 * <p>
 * Cloth 的 {@link TextListEntry} 把正文（{@code text}）和换行结果（{@code wrappedLines}，以
 * {@code savedWidth / savedX / savedY} 三个字段做缓存键）都存进私有字段，构造完就不再重算——所以
 * 直接用它写「功能开没开」这类状态行，玩家把开关拨过去之后，必须退出配置页再进来才看得到变化。
 * <p>
 * 这里只做两件事把它变成活的：正文交给一个 {@link Supplier}，每次绘制时重新求值；正文变了就把换行
 * 缓存的宽度置成 {@code -1}，让父类在下一帧重新换行。其余（高度、缩进、只读置灰、tooltip）全部照旧
 * 由父类处理，行为与普通文本行一致。
 * <p>
 * 用到的两个字段是 Cloth Config 自己的名字，不经过 Minecraft 的映射表，开发环境与发布环境同名；
 * 万一将来改名，这里只记一条警告并退化成静态文本行，不会崩。
 */
final class LiveTextListEntry extends TextListEntry {

    private static final Field TEXT = findField("text");
    private static final Field SAVED_WIDTH = findField("savedWidth");
    private static final boolean LIVE = TEXT != null && SAVED_WIDTH != null;

    /**
     * 正文颜色：必须是不透明白 {@code 0xFFFFFFFF}，<b>不能</b>写 {@code 0xFFFFFF}。
     * <p>
     * Cloth 的 {@code color} 是 ARGB，而 {@code Font.PreparedTextBuilder#getTextColor(Style)} 的规则是
     * 「RGB 取自正文自身的样式色，alpha 取自这个 color」：
     * <pre>styleColor != null ? ARGB.color(ARGB.alpha(this.color), styleColor.getValue()) : this.color</pre>
     * 于是 {@code 0xFFFFFF} 的 alpha = 0——即使正文带了 {@link ChatFormatting#GREEN} 之类的样式色，
     * 画出来仍是全透明：条目照样占着高度（布局会动），却一个字都看不见。
     * <p>
     * {@code -1} 正是 Cloth 自己的默认值（{@code TextListEntry} 的短构造器与
     * {@code TextDescriptionBuilder} 都写死 {@code iconst_m1}，{@code DISABLED_COLOR} 也走
     * {@code ARGB.opaque(...)}），也就是普通文本行用的那个值。
     */
    private static final int OPAQUE_WHITE = -1;

    private final Supplier<Component> supplier;
    /** 当前已经写进父类的正文，用来判断要不要换。 */
    private Component shown;

    // Cloth 把 TextListEntry 的构造器标成了 @Deprecated + @ApiStatus.Internal（它希望大家都走
    // entries.startTextDescription(...) 这个建造器）。但建造器收的是固定正文，没有能传 Supplier 的
    // 公开入口——「正文实时变化」只能靠继承这个类来做，所以这里显式压掉告警。
    @SuppressWarnings("deprecation")
    LiveTextListEntry(Component initial, Supplier<Component> supplier) {
        // 第一个参数是条目名（纯文本行没有名字），tooltip 留空。
        super(Component.empty(), initial, OPAQUE_WHITE, () -> Optional.empty());
        this.supplier = supplier;
        this.shown = initial;
    }

    @Override
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        if (LIVE) {
            Component now = supplier.get();
            if (!now.equals(shown)) {
                try {
                    TEXT.set(this, now);
                    // 换行缓存的键里有宽度，置成不可能出现的 -1 来强制父类重新换行。
                    SAVED_WIDTH.setInt(this, -1);
                    shown = now;
                } catch (ReflectiveOperationException e) {
                    Biosphere.LOGGER.warn("[biosphere] 配置页状态行无法刷新，退化为静态文本: {}", e.toString());
                }
            }
        }
        super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
    }

    private static Field findField(String name) {
        try {
            Field field = TextListEntry.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Biosphere.LOGGER.warn("[biosphere] Cloth 配置页字段 {} 不可用，状态行将不会实时刷新: {}",
                    name, e.toString());
            return null;
        }
    }
}

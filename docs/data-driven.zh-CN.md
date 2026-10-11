# 数据驱动教程 — TOMC-Biosphere

> 本页的一切都用 **JSON** 声明——只有一个例外，即 [§6](#6-公开的-java-api) 里那个可选的 Java API，它是写给别的模组作者的，不是写给数据包的。

原版数据包可以改写配方、战利品表、标签和进度。但它说不出「这只鸡想吃那只蠹虫」「这朵花该自己枯萎」「这块末地石该长出点什么」。这些行为在原版里根本没有对应的文件槽位——而把它们写死在代码里，正是一个模组变得无法调整的原因。

本模组为这些行为加了四个槽位，外加一个留给「确实需要代码」那类情况的公开入口。

| 资源 | 它回答的问题 | 位置 | 读取时机 |
| --- | --- | --- | --- |
| `biosphere_environmental_change` | 世界 tick 到它时，这个方块该怎么变？ | `data/<命名空间>/biosphere_environmental_change/` | 世界加载时，从数据包管理器 |
| `biosphere_food_chain` | 谁猎谁，谁又躲谁？ | `data/<命名空间>/biosphere_food_chain/` | 世界加载时，从数据包管理器 |
| `biosphere_spawns` | 这些生物还该在哪些地方刷新？ | `data/biosphere/biosphere_spawns/` | 模组初始化时，作为内置资源 |
| 方块标签 | 什么被排除在外，什么可以被搬走？ | `data/<命名空间>/tags/block/` | 随标签注册表一同加载 |

- [1. 环境演替](#1-环境演替-biosphere_environmental_change)
- [2. 食物链关系](#2-食物链关系-biosphere_food_chain)
- [3. 自然刷新](#3-自然刷新-biosphere_spawns)
- [4. 内置标签](#4-内置标签)
- [5. 末影人搬运白名单](#5-末影人搬运白名单)
- [6. 公开的 Java API](#6-公开的-java-api)
- [7. 加载、重载与失败行为](#7-加载重载与失败行为)

---

## 1. 环境演替（`biosphere_environmental_change`）

随机刻演替规则。每条规则在随机刻落到源方块上时，把它（或其邻近）变换成别的东西。

**位置** —— `data/<命名空间>/biosphere_environmental_change/`（任意子目录）。一个文件可含单条规则对象或规则对象数组。从数据包资源管理器加载，因此可用数据包扩展。

### 字段

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `change_type` | string | `"setblock"` | `setblock` 替换方块；`data_merge` 原地修改既有方块的状态/NBT。 |
| `origin` | string[] / `#标签` | — | 源方块。`#标签` 会展开为全部成员（经 `biosphere:natural_flower_blacklist` 过滤）。 |
| `result` | string[] / `#标签` | — | `setblock` 的结果方块。可含 `minecraft:air`。随机选取其一。 |
| `random_tick_chance` | int | `1` | 触发概率为每次随机刻 `1/N`。越大越稀有。 |
| `mode` | string | `"replace"` | `replace` 覆盖目标；`destroy` 先破坏目标再放；`keep` 目标非空则跳过。 |
| `position` | string | `"self"` | 施加位置：`self`、`positive_x`、`negative_x`、`positive_y`、`negative_y`、`positive_z`、`negative_z`。 |
| `offset` | object | `{x:0,y:0,z:0}` | 在 `position` 基础上叠加的偏移。 |
| `result_states` | object | — | 方块状态覆盖，如 `{"age":"7"}`。 |
| `result_nbt` | SNBT 字符串 | — | 方块实体数据（`setblock` 与 `data_merge` 均可用）。 |
| `conditions` | object | — | 单个 `LootItemCondition`（如 `location_check`），须通过才触发。 |
| `summon` | SNBT 字符串 | — | 变化后在目标位置生成的一个实体（含 `id`）。 |

### 语义

- `setblock` 必须提供 `result`；`data_merge` 禁止 `result`，且必须提供 `result_states` 或 `result_nbt`。
- **只有 `origin` 方块真的能被随机刻抽中，规则才会触发。** 加载器在读到一条命名了某方块的规则时，会把该源方块设为可随机刻，所以你不必只挑原版本来就会 tick 的方块——但一个根本不在世界里的方块依然永远不会被 tick 到。
- 双层植物（`DoublePlantBlock`）会自动处理两半格；作为 `origin` 的双层植物仅从下半格触发。当结果要替换掉 origin 自身时，两半格会在放置新方块之前先被移除，因此不会出现半截高花立在那里。
- `conditions` 使用标准谓词系统，`location_check` 支持 `biomes`、`can_see_sky`、`block` 等。被检查的方块按谓词自身的 `offsetX/Y/Z` 偏移。
- `setblock` 且 `result: ["minecraft:air"]` 即移除origin。配合 `summon`，这就是一朵花枯萎并掉落染料的做法。

### 示例

草方块在枯叶堆覆盖下变为灰化土：

```json
{
  "change_type": "setblock",
  "origin": ["minecraft:grass_block"],
  "result": ["minecraft:podzol"],
  "random_tick_chance": 250,
  "position": "self",
  "conditions": {
    "condition": "minecraft:location_check",
    "offsetY": 1,
    "predicate": { "block": { "blocks": ["minecraft:leaf_litter"] } }
  }
}
```

蒲公英凋谢并掉落黄色染料：

```json
{
  "change_type": "setblock",
  "origin": ["minecraft:dandelion"],
  "result": ["minecraft:air"],
  "position": "self",
  "random_tick_chance": 200,
  "summon": "{id:\"minecraft:item\", Item:{id:\"minecraft:yellow_dye\", count:1}}"
}
```

末地石在上方长出紫颂花，仅在末地高地的露天下：

```json
{
  "change_type": "setblock",
  "origin": ["minecraft:end_stone"],
  "result": ["minecraft:chorus_flower"],
  "random_tick_chance": 400,
  "position": "positive_y",
  "conditions": {
    "condition": "minecraft:location_check",
    "offsetY": 1,
    "predicate": { "biomes": ["minecraft:end_highlands"], "can_see_sky": true }
  }
}
```

---

## 2. 食物链关系（`biosphere_food_chain`）

生物之间的攻击与逃跑关系。

**位置** —— `data/<命名空间>/biosphere_food_chain/`（惯例拆分为 `attack/` 与 `avoid/` 子目录，加载器会递归扫描）。从数据包资源管理器加载。

### 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `subjects` | string[] / `#标签` | 行动方——攻击者或逃跑者。 |
| `objects` | string[] / `#标签` | 它们的目标或逃离对象。 |
| `behavior_hint` | string | `"attack"` 或 `"avoid"`。 |

`subjects` 与 `objects` 接受实体 ID 或 `#标签`。规则里写标签时，会为每个成员展开成一条关系。

### 示例

鸡猎杀蠹虫：

```json
{
  "subjects": ["minecraft:chicken"],
  "objects": ["minecraft:silverfish"],
  "behavior_hint": "attack"
}
```

基于标签的规避规则——牲畜逃离亡灵：

```json
{
  "subjects": ["#biosphere:livestock"],
  "objects": ["#biosphere:hostile_undead"],
  "behavior_hint": "avoid"
}
```

### 说明

- 规则是**叠加**的。两个数据包可以为同一种生物各写规则，谁都不会被覆盖。
- 缺乏原版近战攻击的生物（鸡、犰狳、豹猫等）会获得新的近战 AI，由 **被动生物攻击** 配置类别调节。`AiCapabilities.MELEE_CAPABLE` 中的生物已有近战，仅获得目标选择——那里的近战数值对它们从不生效。
- 两个方向是各自独立的规则。写了一条 `attack` 并不会让猎物开始逃跑；那需要在 `avoid/` 里再写一个文件。这是刻意的：并不是每个捕食者都该被它能打到的一切所畏惧。

---

## 3. 自然刷新（`biosphere_spawns`）

额外生物自然刷新的位置。

**位置** —— `data/biosphere/biosphere_spawns/`（每个生物一个文件）。与前两个系统不同，刷新是在模组初始化阶段通过 `BiomeModifications` 注册的，因此它是**模组内置资源**，而非运行时数据包。要改动它就得重新构建模组。

### 字段

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `biomes` | string[] | — | 刷新的生物群系 ID。 |
| `entity` | string | — | 实体 ID。 |
| `category` | string | `"monster"` | 刷新类别（`monster`、`creature` 等）。 |
| `weight` | int | `8` | 刷新权重。 |
| `min` | int | `2` | 最小群体数量。 |
| `max` | int | `4` | 最大群体数量。 |

### 示例

幻翼游荡于末地小岛：

```json
{
  "biomes": ["minecraft:small_end_islands", "minecraft:end_barrens"],
  "entity": "minecraft:phantom",
  "category": "monster",
  "weight": 8,
  "min": 2,
  "max": 4
}
```

---

## 4. 内置标签

模组附带若干可复用标签。实体标签是给你在自己的规则里引用的；方块标签由模组自身消费。

### 实体标签（`data/<命名空间>/tags/entity_type/`）

- `biosphere:passive_prey` —— `#biosphere:livestock` + 村民 + 流浪商人。
- `biosphere:livestock` —— 鸡、牛、驴、马、哞菇、骡、猪、兔、羊。
- `biosphere:hostile_undead` —— 溺尸、尸壳、沼骸（bogged）、干枯者（parched）、骷髅、流浪者、凋灵骷髅、僵尸、僵尸村民。
- `biosphere:skeleton_archers` —— 沼骸、干枯者、骷髅、流浪者、凋灵骷髅。

四个标签都使用 `"replace": false`，因此数据包可以向其中追加。想要另一套集合的数据包应当定义自己的标签并引用它。

### 方块标签（`data/<命名空间>/tags/block/`）

- `biosphere:natural_flower_blacklist` —— 凋灵玫瑰、眼眸花（闭合/张开两种状态）、火炬花、瓶子草。它们被排除在通配花朵规则之外，这样一个形如 `#minecraft:flowers` 的 `origin` 就不会吃掉不可再生的花。
- `biosphere:enderman_holdable_blacklist` —— 见下节。

---

## 5. 末影人搬运白名单

原版末影人会捡起 `minecraft:enderman_holdable` 里的东西，那是一个人工维护、约二十个方块的标签。它可以被整个替换，但从精神上无法被扩充：往里加十三个方块并不会让这份清单变得更灵活，它只是变得更长。

本模组走的是反过来的路：白名单是**推导**出来的——从创造模式分类推导，再由标签**过滤**。

### 配置键

| 键 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `enderman_holdable_override` | boolean | `true` | 为 false 时原样使用 `minecraft:enderman_holdable`，以下全部不适用。 |
| `enderman_holdable_tabs` | string[] | `["minecraft:natural_blocks"]` | 创造模式分类 ID。白名单是所列分类中所有方块的并集。 |

合法的十一个内容分类是：`building_blocks`、`colored_blocks`、`natural_blocks`、`functional_blocks`、`redstone_blocks`、`tools_and_utilities`、`combat`、`food_and_drinks`、`ingredients`、`spawn_eggs`、`op_blocks`。三个特殊分类（`hotbar`、`search`、`inventory`）不是方块，会被拒绝。

### 判定顺序

对一个候选方块状态，依次判断：

1. 它在 `#biosphere:enderman_holdable_blacklist` 里吗？→ **永远不会被搬走。** 黑名单优先于一切，包括包含该方块的分类。
2. 覆写关掉了吗？→ 退回 `minecraft:enderman_holdable`，判定结束。
3. 该方块属于内置的多方块类（`BedBlock`、`DoorBlock`、`DoublePlantBlock`）之一吗？→ 不搬走，因此永远不会只搬走半个结构。
4. 该方块在展开集合里吗？→ 搬走。

### 编写黑名单

该标签为 `"replace": false`，因此数据包可以向其中追加：

```json
{
  "replace": false,
  "values": [
    "minecraft:end_stone",
    "minecraft:bedrock",
    "minecraft:obsidian",
    "minecraft:crying_obsidian",
    "minecraft:end_portal_frame",
    "minecraft:end_gateway",
    "minecraft:command_block",
    "minecraft:chain_command_block",
    "minecraft:repeating_command_block",
    "minecraft:jigsaw",
    "minecraft:structure_block",
    "minecraft:structure_void",
    "minecraft:barrier",
    "minecraft:light"
  ]
}
```

展开发生在**服务端**，惰性进行——末影人第一次询问时展开，配置每次保存时重新展开。每次展开都会向日志打印一行：

```
[biosphere] 展开末影人可拾取白名单：1 个分类 → 243 个方块
```

如果那一行报告的是 0 个方块，说明分类 ID 写错了，末影人将什么都搬不动——这是一个合法的配置，但通常不是你想要的。

---

## 6. 公开的 Java API

有一种情况确实需要代码：某个模组新增了自己的两段式方块，希望末影人别去碰它。这是一次注册，不是一条规则。

```java
import io.github.thatone0502.biosphere.BiosphereApi;
import net.minecraft.world.level.block.Block;

BiosphereApi.registerMultiblockFilter(MyTwoPartBlock.class);
BiosphereApi.registerMultiblockFilter(state -> state.is(MyModBlocks.FRAGILE.get()));
```

两个重载都追加到上面第 3 步所查询的同一个过滤器列表。注册应当在模组初始化期间进行。该类位于本模组的 jar 内，因此使用它的模组应声明软依赖，并只在 `FabricLoader.isModLoaded("biosphere")` 成立时调用。

---

## 7. 加载、重载与失败行为

| 时机 | 发生什么 |
| --- | --- |
| 模组初始化 | 读取 `biosphere_spawns` 并注册为生物群系修改。加载配置。 |
| 世界加载 | 从数据包管理器读取 `biosphere_environmental_change` 与 `biosphere_food_chain`，并把源方块设为可随机刻。 |
| 游戏内存档配置 | 末影人白名单缓存失效，下次查询时重建。 |
| `/reload` | 未接入任何自有系统。原版标签的改动会被读到；两套规则系统不会重新读取。 |

**失败是本地的。** 一个解析不了的规则文件只会自己失败，并在日志里给出原因和文件名；其余规则照常加载。这是刻意设计——一套五十个文件的规则集里有一个拼写错误，不该让你损失另外四十九个。

空的 `origin` 或 `result`，或者一个展开后为空的 `#标签`，得到的是一条永不触发的规则。它不会抛异常，也不会让模组失效。

## 刻意不做成数据驱动的部分

- **进食互动。** 哪种生物吃哪种方块、以及配套的低头动画，都是代码。不存在 `biosphere_eating/` 目录；一种生物要么有这个 goal，要么没有。
- **近战数值。** 伤害、追击速度与冷却属于配置值，不是逐规则字段——这样数据包就无法在两只鸡猎杀同一猎物时，悄悄让其中一只打得更疼。

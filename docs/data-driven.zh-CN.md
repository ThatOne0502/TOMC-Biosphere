# 数据驱动教程 — TOMC-Biosphere

TOMC-Biosphere 添加的一切都是一条从资源管理器读取的 JSON 规则。本教程记录三种规则格式，并展示如何编写你自己的规则。

- [1. 环境演替](#1-环境演替-biosphere_environmental_change)
- [2. 食物链关系](#2-食物链关系-biosphere_food_chain)
- [3. 自然刷新](#3-自然刷新-biosphere_spawns)

---

## 1. 环境演替（`biosphere_environmental_change`）

随机刻演替规则。每条规则在随机刻触发时，把一个源方块（或其邻近）变换为另一个方块。

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
- 双层植物（`DoublePlantBlock`）会自动处理两半格；作为 `origin` 的双层植物仅从下半格触发。
- `conditions` 使用标准谓词系统，`location_check` 支持 `biomes`、`can_see_sky`、`block` 等。被检查的方块按谓词自身的 `offsetX/Y/Z` 偏移。

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

`subjects` 与 `objects` 接受实体 ID 或 `#标签`。

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

缺乏原版近战攻击的生物（鸡、犰狳、豹猫等）会获得新的近战 AI，由 **被动生物攻击** 配置类别调节。`AiCapabilities.MELEE_CAPABLE` 中的生物已有近战，仅获得目标选择。

---

## 3. 自然刷新（`biosphere_spawns`）

额外生物自然刷新的位置。

**位置** —— `data/biosphere/biosphere_spawns/`（每个生物一个文件）。与前两个系统不同，刷新是在模组初始化阶段通过 `BiomeModifications` 注册的，因此它是**模组内置资源**，而非运行时数据包。

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

## 内置标签

模组附带几个可复用标签：

- `biosphere:passive_prey` —— `#biosphere:livestock` + 村民 + 流浪商人。
- `biosphere:livestock` —— 鸡、牛、驴、马、哞菇、骡、猪、兔、羊。
- `biosphere:hostile_undead` —— 亡灵敌对生物（bogged、husk、parched、skeleton、stray、wither_skeleton、zombie、zombie_villager、drowned）。
- `biosphere:skeleton_archers` —— 骷髅射手变种（bogged、parched、skeleton、stray、wither_skeleton）。
- `biosphere:natural_flower_blacklist` —— 方块标签；凋灵玫瑰、灰目花（闭合/盛开）、火炬花、瓶子草（从通配花朵规则中排除）。

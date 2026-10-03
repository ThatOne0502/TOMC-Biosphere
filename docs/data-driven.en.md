# Data-Driven Guide — TOMC-Biosphere

Everything TOMC-Biosphere adds is a JSON rule read from the resource manager. This guide documents the three rule formats and shows how to author your own.

- [1. Environmental change](#1-environmental-change-biosphere_environmental_change)
- [2. Food-chain relationships](#2-food-chain-relationships-biosphere_food_chain)
- [3. Natural spawns](#3-natural-spawns-biosphere_spawns)

---

## 1. Environmental change (`biosphere_environmental_change`)

Random-tick succession rules. Each rule transforms a source block (or its neighbourhood) into another block when a random tick fires.

**Location** — `data/<namespace>/biosphere_environmental_change/` (any subdirectory). One file may hold a single rule object or an array of rule objects. Loaded from the data-pack resource manager, so data packs can extend them.

### Fields

| Field | Type | Default | Description |
| --- | --- | --- | --- |
| `change_type` | string | `"setblock"` | `setblock` replaces blocks; `data_merge` mutates an existing block's state/NBT in place. |
| `origin` | string[] / `#tag` | — | Source block(s). A `#tag` expands to all members (filtered by `biosphere:natural_flower_blacklist`). |
| `result` | string[] / `#tag` | — | Result block(s) for `setblock`. May include `minecraft:air`. One is picked at random. |
| `random_tick_chance` | int | `1` | Trigger chance is `1/N` per random tick. Larger = rarer. |
| `mode` | string | `"replace"` | `replace` overwrites the target; `destroy` breaks the existing target first; `keep` skips if the target is not air. |
| `position` | string | `"self"` | Where to apply: `self`, `positive_x`, `negative_x`, `positive_y`, `negative_y`, `positive_z`, `negative_z`. |
| `offset` | object | `{x:0,y:0,z:0}` | Extra offset added to `position`. |
| `result_states` | object | — | Block-state overrides, e.g. `{"age":"7"}`. |
| `result_nbt` | SNBT string | — | Block-entity data (for both `setblock` and `data_merge`). |
| `conditions` | object | — | A single `LootItemCondition` (e.g. `location_check`) that must pass. |
| `summon` | SNBT string | — | An entity (with `id`) to summon at the target after the change. |

### Semantics

- `setblock` requires `result`; `data_merge` forbids `result` and requires `result_states` or `result_nbt`.
- Double plants (`DoublePlantBlock`) are handled as both halves automatically; a double-plant `origin` only fires from the lower half.
- `conditions` use the standard predicate system, so `location_check` supports `biomes`, `can_see_sky`, `block`, etc. The inspected block is offset by the predicate's own `offsetX/Y/Z`.

### Examples

Grass turns to podzol when leaf litter sits on top:

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

Dandelions wither and drop yellow dye:

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

End stone sprouts a chorus flower above, only under the open sky in the end highlands:

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

## 2. Food-chain relationships (`biosphere_food_chain`)

Mob-vs-mob attack and flee relationships.

**Location** — `data/<namespace>/biosphere_food_chain/` (conventionally split into `attack/` and `avoid/` subdirectories, though the loader scans recursively). Loaded from the data-pack resource manager.

### Fields

| Field | Type | Description |
| --- | --- | --- |
| `subjects` | string[] / `#tag` | The mob(s) that act — attackers or fleers. |
| `objects` | string[] / `#tag` | The mob(s) they target or flee. |
| `behavior_hint` | string | `"attack"` or `"avoid"`. |

`subjects` and `objects` accept entity IDs or `#tags`.

### Examples

Chickens hunt silverfish:

```json
{
  "subjects": ["minecraft:chicken"],
  "objects": ["minecraft:silverfish"],
  "behavior_hint": "attack"
}
```

A tag-based avoid rule — livestock flee the undead:

```json
{
  "subjects": ["#biosphere:livestock"],
  "objects": ["#biosphere:hostile_undead"],
  "behavior_hint": "avoid"
}
```

Mobs that lack a vanilla melee attack (chickens, armadillos, ocelots, …) get a new melee AI, tuned by the **Passive Attack** config category. Mobs in `AiCapabilities.MELEE_CAPABLE` already have melee and only receive the targeting.

---

## 3. Natural spawns (`biosphere_spawns`)

Where additional mobs spawn naturally.

**Location** — `data/biosphere/biosphere_spawns/` (one file per mob). Unlike the two systems above, spawns are registered at mod init via `BiomeModifications`, so they are a **built-in mod resource**, not a runtime data pack.

### Fields

| Field | Type | Default | Description |
| --- | --- | --- | --- |
| `biomes` | string[] | — | Biome IDs to spawn in. |
| `entity` | string | — | Entity ID. |
| `category` | string | `"monster"` | Spawn category (`monster`, `creature`, …). |
| `weight` | int | `8` | Spawn weight. |
| `min` | int | `2` | Minimum group size. |
| `max` | int | `4` | Maximum group size. |

### Example

Phantoms haunt the End islands:

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

## Bundled tags

The mod ships a few reusable tags:

- `biosphere:passive_prey` — `#biosphere:livestock` + villagers + wandering traders.
- `biosphere:livestock` — chicken, cow, donkey, horse, mooshroom, mule, pig, rabbit, sheep.
- `biosphere:hostile_undead` — bogged, husk, parched, skeleton, stray, wither skeleton, zombie, zombie villager, drowned.
- `biosphere:skeleton_archers` — bogged, parched, skeleton, stray, wither skeleton.
- `biosphere:natural_flower_blacklist` — block tag; wither rose, closed/open eyeblossom, torchflower, pitcher plant (excluded from wildcard flower rules).

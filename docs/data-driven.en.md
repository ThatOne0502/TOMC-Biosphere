# Data-Driven Guide — TOMC-Biosphere

> Everything on this page is declared in **JSON** — with one exception, the optional Java API in [§6](#6-the-public-java-api), which exists for other mods' authors rather than for data packs.

A vanilla data pack can rewrite recipes, loot tables, tags and advancements. It cannot say *this chicken wants to eat that silverfish*, *this flower should wither on its own*, or *this end stone should sprout something*. Those are behaviours vanilla has no file for — and hard-coding them is how a mod becomes impossible to tune.

This mod adds four slots for exactly those behaviours, plus a public entry point for the fifth case that genuinely needs code.

| Asset | The question it answers | Lives under | Read |
| --- | --- | --- | --- |
| `biosphere_environmental_change` | How should a block change when the world ticks it? | `data/<ns>/biosphere_environmental_change/` | on world load, from the datapack manager |
| `biosphere_food_chain` | Who hunts whom, and who runs from whom? | `data/<ns>/biosphere_food_chain/` | on world load, from the datapack manager |
| `biosphere_spawns` | Where should these mobs also spawn? | `data/biosphere/biosphere_spawns/` | at mod init, as a built-in resource |
| block tags | What is excluded, and what may be carried? | `data/<ns>/tags/block/` | with the rest of the tag registry |

- [1. Environmental change](#1-environmental-change-biosphere_environmental_change)
- [2. Food-chain relationships](#2-food-chain-relationships-biosphere_food_chain)
- [3. Natural spawns](#3-natural-spawns-biosphere_spawns)
- [4. Bundled tags](#4-bundled-tags)
- [5. The Enderman carrying whitelist](#5-the-enderman-carrying-whitelist)
- [6. The public Java API](#6-the-public-java-api)
- [7. Loading, reloading and failure behaviour](#7-loading-reloading-and-failure-behaviour)

---

## 1. Environmental change (`biosphere_environmental_change`)

Random-tick succession rules. Each rule transforms a source block (or its neighbourhood) into something else when a random tick fires on it.

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
- **A rule only fires if its `origin` block is actually eligible for random ticks.** The loader makes a source block randomly tickable when it loads a rule that names it, so you do not have to pick blocks vanilla already ticks — but a block that is not in the world at all is still never ticked.
- Double plants (`DoublePlantBlock`) are handled as both halves automatically, and a double-plant `origin` only fires from the lower half. When the result replaces the origin itself, both halves are removed before the new block is placed, so a tall flower cannot be left half-standing.
- `conditions` use the standard predicate system, so `location_check` supports `biomes`, `can_see_sky`, `block`, etc. The inspected block is offset by the predicate's own `offsetX/Y/Z`.
- `setblock` with `result: ["minecraft:air"]` removes the origin. Combined with `summon`, this is how a withering flower drops its dye.

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

`subjects` and `objects` accept entity IDs or `#tags`. A rule naming a tag fans out to one relationship per member.

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

### Notes

- Rules are **additive**. Two packs may contribute rules for the same mob; nothing is overwritten.
- Mobs that lack a vanilla melee attack (chickens, armadillos, ocelots, …) get a new melee AI, tuned by the **Passive Attack** config category. Mobs in `AiCapabilities.MELEE_CAPABLE` already have melee and only receive the targeting — the melee numbers never apply to them.
- Both directions are separate rules. Writing an `attack` rule does not make the prey flee; that is a second file in `avoid/`, which is deliberate, because not every predator should be feared by everything it can hit.

---

## 3. Natural spawns (`biosphere_spawns`)

Where additional mobs spawn naturally.

**Location** — `data/biosphere/biosphere_spawns/` (one file per mob). Unlike the two systems above, spawns are registered at mod init via `BiomeModifications`, so they are a **built-in mod resource**, not a runtime data pack. Changing them requires rebuilding the mod.

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

## 4. Bundled tags

The mod ships reusable tags. Entity tags are meant to be referenced from your own rules; block tags are consumed by the mod itself.

### Entity tags (`data/<ns>/tags/entity_type/`)

- `biosphere:passive_prey` — `#biosphere:livestock` + villagers + wandering traders.
- `biosphere:livestock` — chicken, cow, donkey, horse, mooshroom, mule, pig, rabbit, sheep.
- `biosphere:hostile_undead` — drowned, husk, bogged, parched, skeleton, stray, wither skeleton, zombie, zombie villager.
- `biosphere:skeleton_archers` — bogged, parched, skeleton, stray, wither skeleton.

All four use `"replace": false`, so a data pack can append to them. A pack that wants a different set should define its own tag and reference that instead.

### Block tags (`data/<ns>/tags/block/`)

- `biosphere:natural_flower_blacklist` — wither rose, closed/open eyeblossom, torchflower, pitcher plant. Excluded from wildcard flower rules, so that a `#minecraft:flowers`-style `origin` never eats a flower that is not renewable.
- `biosphere:enderman_holdable_blacklist` — see below.

---

## 5. The Enderman carrying whitelist

Vanilla endermen pick up whatever is in `minecraft:enderman_holdable`, a hand-curated tag of about twenty blocks. It can be replaced, but not extended in spirit: adding thirteen blocks to it does not make the list behave differently, it just makes it longer.

This mod takes the opposite approach: the whitelist is **derived** from creative tabs, then **filtered** by a tag.

### Config keys

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `enderman_holdable_override` | boolean | `true` | When false, `minecraft:enderman_holdable` is used verbatim and nothing below applies. |
| `enderman_holdable_tabs` | string[] | `["minecraft:natural_blocks"]` | Creative tab IDs. The whitelist is the union of every block in every listed tab. |

The eleven valid content tabs are `building_blocks`, `colored_blocks`, `natural_blocks`, `functional_blocks`, `redstone_blocks`, `tools_and_utilities`, `combat`, `food_and_drinks`, `ingredients`, `spawn_eggs` and `op_blocks`. The three special tabs (`hotbar`, `search`, `inventory`) are not blocks and are rejected.

### Resolution order

For a candidate block state, in order:

1. Is it in `#biosphere:enderman_holdable_blacklist`? → **never carried.** The blacklist wins over everything, including a tab that contains the block.
2. Is the override off? → fall back to `minecraft:enderman_holdable`, and stop.
3. Is the block one of the built-in multiblock classes (`BedBlock`, `DoorBlock`, `DoublePlantBlock`)? → not carried, so half a structure is never taken.
4. Is the block in the expanded set? → carried.

### Authoring the blacklist

The tag is `"replace": false`, so a data pack can append to it:

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

Expansion happens **on the server**, lazily, the first time an enderman asks, and is re-expanded whenever the config is saved. It is emitted to the log once per expansion:

```
[biosphere] 展开末影人可拾取白名单：1 个分类 → 243 个方块
```

If that line reports 0 blocks, the tab IDs are wrong and endermen will carry nothing — which is a valid configuration, but usually not the intended one.

---

## 6. The public Java API

One case genuinely needs code: a mod that adds its own two-part block wants endermen to leave it alone. That is a registration, not a rule.

```java
import io.github.thatone0502.biosphere.BiosphereApi;
import net.minecraft.world.level.block.Block;

BiosphereApi.registerMultiblockFilter(MyTwoPartBlock.class);
BiosphereApi.registerMultiblockFilter(state -> state.is(MyModBlocks.FRAGILE.get()));
```

Both overloads append to the same filter list consulted at step 3 above. Registration is intended to run during mod initialisation. The class lives in the mod jar, so a mod using it should declare a `compileOnly`-style soft dependency and call it only when `FabricLoader.isModLoaded("biosphere")` says so.

---

## 7. Loading, reloading and failure behaviour

| Moment | What happens |
| --- | --- |
| Mod init | `biosphere_spawns` is read and registered as biome modifications. Config is loaded. |
| World load | `biosphere_environmental_change` and `biosphere_food_chain` are read from the datapack manager, and source blocks are made randomly tickable. |
| Config save (in-game) | The Enderman whitelist cache is invalidated and rebuilt on the next lookup. |
| `/reload` | Not wired to any custom system. Vanilla tag changes are picked up; the two rule systems are not re-read. |

**Failures are local.** A rule file that cannot be parsed fails on its own, with the reason and the file name in the log; the remaining rules still load. This is deliberate — one typo in a fifty-file rule set should not cost you the other forty-nine.

An empty `origin` or `result`, or a `#tag` that expands to nothing, produces a rule that simply never fires. It does not throw, and it does not disable the mod.

## What is deliberately not data-driven

- **Eating interactions.** Which mob eats which block, and the head-bob animation that goes with it, are code. There is no `biosphere_eating/` directory; a mob either has the goal or it does not.
- **The melee numbers.** Damage, chase speed and cooldown are config values, not per-rule fields, so that a pack cannot silently make one chicken hit harder than another when both are hunting the same prey.

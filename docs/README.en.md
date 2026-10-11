<p align="center">
  <img src="../src/main/resources/assets/biosphere/biosphere_logo.png" alt="TOMC-Biosphere" width="800">
</p>

<p align="center">
  <a href="../README.md">English</a> &nbsp;·&nbsp; <a href="README.zh-CN.md">简体中文</a>
</p>

# TOMC-Biosphere

> 📘 **Data-driven guide** — [data-driven.en.md](data-driven.en.md)

A world is not alive because of how many creatures are standing in it. It is alive because those creatures have something to do with each other.

Vanilla Minecraft has the creatures and none of the relationships. A chicken will stand next to a silverfish forever. A flower will stand in the same spot until someone breaks it. End stone has been sitting in the same configuration since the End was generated. **TOMC-Biosphere** is an attempt to put the missing half back — as data, not as hard-coded behaviour.

## What it does

Four systems, all of them driven by files you can edit without writing Java.

### 🍖 Food-chain relationships

Mobs form a real food web. Predators hunt, prey flee — and this includes passive mobs that normally never fight at all.

| Predator | Prey |
| --- | --- |
| Chicken | Silverfish |
| Armadillo | Spiders |
| Ocelot | Creeper |
| Dolphin | Squid |
| Polar bear | Fish (`#minecraft:fishes`) |
| Undead (`#biosphere:hostile_undead`) | Livestock |
| Skeleton archers (`#biosphere:skeleton_archers`) | `#biosphere:passive_prey` |
| Phantom | Endermite |
| Shulker | Endermite |
| Vex | Allay |

And the other direction, twenty subjects in total:

Allays flee vexes · livestock flees the undead · fish flee polar bears · squid flee dolphins · villagers flee skeletons · creepers flee ocelots · endermites flee everything that hunts them · silverfish flee chickens · spiders flee armadillos.

A mob that has no melee attack of its own — a chicken, an armadillo, an ocelot — receives a new one. Damage, chase speed and attack cooldown are all configurable, because "a chicken that can kill you" is a design decision, not a law of nature.

### 🥕 Eating interactions

Animals eat the world around them, each with a sheep-style head-bob animation, so you can see it happen rather than guess at it.

- **Cows** graze grass, and eat mycelium with a configurable chance of turning into a **mooshroom**.
- **Mooshrooms** nibble mushrooms and turn mycelium back into dirt — the reverse of the cow.
- **Pigs** seek out ripe crops, melons and pumpkins.
- **Camels** pathfind to a cactus and eat it from the base, so a cactus column is consumed bottom-up.
- **Horses, donkeys and mules** graze like sheep.

### 🌱 Random-tick succession

Thirty rules run on random ticks — the same mechanic vanilla uses for grass spreading. Nothing is polled, nothing is scheduled; a block changes when the world decides to tick it.

- Flowers wither and drop their dye: every one of the sixteen dye flowers, plus the four double plants (sunflower, lilac, rose bush, peony).
- Short grass grows tall, or dies; on sunflower plains it succeeds into taller forms; in flower forests it gives way to flowers.
- Dry grass succeeds into living plants; leaf litter turns the grass beneath it into **podzol**.
- End stone sprouts chorus flowers under the open sky; chorus plants wither again.
- Crimson and warped nylium spread their vegetation; nether plants wither.

Every rule may carry a **predicate** (`biomes`, `can_see_sky`, `block`, …), so a rule can be as narrow as "only in the end highlands, only where the sky is visible".

### 🧊 Enderman block-carrying whitelist

Endermen pick up whatever is in `minecraft:enderman_holdable` — a hand-curated list of about twenty blocks that has not changed in years, and that no data pack can extend without replacing the tag wholesale.

This mod lets you rebuild that list out of **whole creative inventory tabs** instead:

- Tick any combination of the eleven content tabs. The whitelist becomes every block in the ticked tabs — 243 blocks for a single Natural Blocks tab, if that is what you want.
- A hand-written **blacklist** is subtracted afterwards and always wins: `biosphere:enderman_holdable_blacklist` covers end stone, bedrock, obsidian and crying obsidian, the exit portal frame and gateway, and every block in the three operator tabs (command blocks, jigsaw, structure block, structure void, barrier, light). Those can never be carried, whatever else is ticked.
- Multiblock structures — beds, doors, double plants — are filtered out, so an enderman never walks off with half a structure and leaves the other half floating.
- Turning the override off restores the vanilla tag exactly. Nothing is patched in place.

### 🐚 Natural spawns

Two mobs are given new homes: **phantoms** haunt the End islands (`small_end_islands`, `end_barrens`), and **endermites** skitter across the End midlands and highlands.

## Design philosophy

1. **Everything is data.** Every behaviour above is a JSON file under `data/biosphere/`, read from the data-pack resource manager. Add a file to a data pack and the behaviour exists; remove it and it does not. Vanilla files are never replaced.
2. **Sensible defaults, no required configuration.** The mod ships a complete, playable set of rules. You are meant to be able to install it and forget it.
3. **The server is the authority.** Configuration lives on the server and is pushed to clients on join. A client can never disagree with the world it is standing in.
4. **Soft dependencies only.** Nothing hard-depends on anything except Fabric API. Creefern and Cloth Config are detected, not required.
5. **Rules fail loudly, alone.** A malformed rule file fails to load with a reason in the log, and takes nothing else down with it.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **0.19.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api) — required
- [Cloth Config](https://modrinth.com/mod/cloth-config) — required for the configuration screen
- [Mod Menu](https://modrinth.com/mod/modmenu) — optional; opens the configuration screen from the mod list

## Configuration

Server-authoritative, edited in-game through **Mod Menu → TOMC-Biosphere**:

| Category | What is in it |
| --- | --- |
| **Modules** | Toggles for food-chain AI, environmental succession and the spawn data pack; the cow→mooshroom conversion chance. |
| **Passive Attack** | Melee damage, chase speed, attack cooldown and flee speed for mobs that gained a new melee AI. |
| **Enderman** | The block-carrying override, and which creative tabs the whitelist is built from. |
| **Creefern** | Compatibility settings for the Creefern mod, including automatic detection. |

The whitelist is expanded **on the server**, lazily, the first time an enderman asks about a block, and re-expanded whenever the config is saved — so changing the tabs takes effect without a restart when the config is edited in-game. On a dedicated server, editing the file directly takes effect after a restart.

Because the config is server-authoritative, the screen is editable only where the world is actually running: in singleplayer, or before you have joined anything. As a guest on someone else's server every entry is greyed out, and saving does nothing — which is the point.

## Compatibility and limits

- 🧩 Everything except Fabric API is optional. Cloth Config missing means the screen does not exist; the mod still runs.
- 🌍 The environmental succession system adds `isRandomlyTicking` to source blocks dynamically. It changes when blocks tick, not whether the world loads — but a very complex rule set will cost tick time.
- 🥚 Food-chain AI is applied by adding goals to mobs, not by replacing them. Another mod that rewrites the same goal selectors may conflict.
- 🔁 Mobs in `AiCapabilities.MELEE_CAPABLE` already have melee attacks and only receive the new targeting; the Passive Attack numbers do not apply to them.
- 👀 The Enderman override intercepts the single `is(BlockTags.ENDERMAN_HOLDABLE)` check inside the vanilla goal. It does not change what endermen want, only what they are allowed to take.
- 🧪 OptiFine and shaders: **untested**. No claims either way.

## For modpack authors

- All content is reachable through ordinary Fabric registration. No mixin replaces a vanilla file, and no vanilla JSON is overwritten.
- The three data systems can be disabled wholesale from the **Modules** category if you want only part of the mod.
- Food-chain rules are additive and namespaced — a pack can ship its own `data/<pack>/biosphere_food_chain/` rules alongside ours.
- The Enderman whitelist has a public Java entry point (`BiosphereApi`) for other mods to register custom multiblock filters, so a mod that adds a new two-part block can keep endermen from taking half of it.

## Building from source

```bash
./gradlew build          # Windows: gradlew.bat build
./gradlew runClient      # a dev client
./gradlew runServer      # a dev server
```

Java 21 is required. The first build downloads Minecraft, mappings and dependencies, so it takes a few minutes; later builds are seconds.

Output lands in `build/libs/` as `tomc-biosphere-<version>.<build>-fabric+mc1.21.11.jar`. The build counter increments on every successful build, and the directory is pruned to keep only the newest generation.

## License

[GPL-3.0-or-later](../LICENSE)

---

> 📘 **Data-driven guide** — [data-driven.en.md](data-driven.en.md)

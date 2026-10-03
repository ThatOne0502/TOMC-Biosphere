<p align="center">
  <img src="../src/main/resources/assets/biosphere/biosphere_logo.png" alt="TOMC-Biosphere" width="800">
</p>

<p align="center">
  <a href="../README.md">English</a> &nbsp;·&nbsp; <a href="README.zh-CN.md">简体中文</a>
</p>

# TOMC-Biosphere

> 📘 **Data-driven guide** — [data-driven.en.md](data-driven.en.md)

**TOMC-Biosphere** is a data-driven ecology mod for **Fabric** on **Minecraft 1.21.11**. It breathes life into your world with three interlocking systems — **food-chain relationships**, **eating interactions** and **random-tick environmental succession** — all driven by plain JSON files that you can extend with a data pack, no Java code required.

## Features

### 🍖 Food-chain relationships

Mobs form a real food web. Predators hunt, prey flee — including passive mobs that normally never fight:

- **Chickens** hunt silverfish · **armadillos** hunt spiders · **ocelots** hunt creepers.
- **Dolphins** hunt squid · **polar bears** hunt fish.
- **Zombies** hunt livestock · **skeletons** hunt prey · **phantoms**, **shulkers** and **vexes** each gain prey.
- Prey flee their predators: allays flee vexes, livestock flee the undead, fish flee polar bears, villagers flee skeletons, creepers flee ocelots.

Passive mobs receive a new melee AI whose damage, chase speed and cooldown are all configurable.

### 🥕 Eating interactions

Animals eat the world around them, each with a sheep-style head-bob animation:

- **Cows** graze grass and eat mycelium, with a configurable chance to turn into a **mooshroom**.
- **Camels** pathfind to a cactus and eat it from the base.
- **Pigs** seek out ripe crops, melons and pumpkins.
- **Mooshrooms** nibble mushrooms and turn mycelium back into dirt.
- **Horses, donkeys and mules** graze like sheep.

### 🌱 Random-tick succession

Blocks evolve through random ticks — the same mechanic vanilla grass spreading uses:

- Flowers wither away, dropping their dye.
- Grass grows; dry grass succeeds into new plants; leaf litter turns grass into podzol.
- End stone sprouts chorus flowers under the open sky; chorus plants wither.
- Crimson/warped nylium spreads its vegetation; nether plants wither.
- Every rule can carry a **predicate** (`biomes`, `can_see_sky`, `block`, …) so changes happen only where you want.

### 🐚 Natural spawns

Extend where mobs spawn: **phantoms** haunt the End islands, **endermites** skitter across the End midlands.

## Design philosophy

Every behaviour above is a **JSON rule** under `data/biosphere/`. The mod ships sensible defaults and reads them from the data-pack resource manager, so you can add, tweak or remove any behaviour by dropping a file into a data pack — no code, no rebuild. The three rule formats (succession, relationships, spawns) are documented in the guide linked below.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **0.19.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config) (configuration UI)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional — opens the config screen)

## Configuration

The server-authoritative config is edited in-game via **Mod Menu → TOMC-Biosphere**:

- **Modules** — toggle food-chain AI, environmental succession and the spawn data pack; set the cow→mooshroom conversion chance.
- **Passive Attack** — melee damage, chase speed, attack cooldown and flee speed.
- **Creefern** — compatibility toggles for the Creefern mod.

Changes to loaded data apply after the world is reloaded.

## License

[GPL-3.0](../LICENSE)

---

> 📘 **Data-driven guide** — [data-driven.en.md](data-driven.en.md)

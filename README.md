# 2tenty

A large gameplay plugin for a **Paper 1.21.11** Minecraft "void survival" server. It bundles a lot
of independent features — custom recipes and enchantments, teams & alliances, a trade-chest
delivery system, player stats & leaderboards, a sidebar scoreboard, a multi-threaded world reset,
proximity chat, anti-drop/anti-pickup filters, and more.

This README is written for people **new to the codebase** (or new to plugin development). It
explains how the project is laid out and how to add your own features. If you just want to copy a
working example, look in [`src/main/java/threeadd/glimpsy/template/`](src/main/java/threeadd/glimpsy/template) —
those files are heavily commented, copy-me starting points for a recipe, a command, and a listener.

---

## Requirements

- **Java 21** (the plugin is compiled for Java 21).
- **Paper 1.21.11** server (it uses Paper-only APIs, so Spigot/Bukkit won't work).
- Optional plugins — the plugin runs fine without them, individual features just switch off:
  - **[FastAsyncWorldEdit](https://modrinth.com/plugin/fastasyncworldedit)** — needed for `/mapreset` / `/fullreset` (the world wipe).
  - **[WorldGuard](https://modrinth.com/plugin/worldguard/)** — enables the `bridge-egg` and `boat-fix` region flags.
  - **DiscordSRV** — the in-game ⇄ Discord chat-mute bridge.

## Recommended plugins

These aren't required, but the server 2tenty was built for runs them alongside it:

- **[WorldGuard](https://modrinth.com/plugin/worldguard/)** — region protection; also unlocks this
  plugin's `bridge-egg` and `boat-fix` region flags.
- **[LuckPerms](https://luckperms.net/)** — permissions; lets you revoke this plugin's per-command
  `2tenty.command.*` nodes (and the staff-only ones).
- **[LightningGrim](https://github.com/Axionize/LightningGrim)** — a performance-optimised fork of
  the GrimAC anti-cheat.
- **[KnockbackSync](https://modrinth.com/plugin/knockbacksync)** — makes knockback consistent for
  high-latency players.

## Building

```bash
git clone <this repo>
cd Glimpsy
./gradlew build       # (./gradlew.bat build on Windows)
```

The finished plugin jar ends up in `build/libs/2tenty-1.0.jar`. Drop it in your server's
`plugins/` folder and restart.

> There's also a `deploy.ps1` (Windows/PowerShell) that stops a local dev server over RCON,
> rebuilds, copies the jar in, and starts it again — handy while iterating.

## Updating to a newer Minecraft version

The Minecraft version this plugin targets lives in **one place: `gradle.properties`**. To build for
a newer version:

1. Open `gradle.properties` and edit the two values:
   ```properties
   minecraftVersion=1.21.11   # the exact Paper version (must be a real published dev bundle)
   paperApiVersion=1.21       # just major.minor - only changes on a new minor (1.21.x -> 1.22)
   ```
   `minecraftVersion` must match a Paper dev-bundle that actually exists — check
   <https://papermc.io/downloads/paper> for the latest. `paperApiVersion` is the API level Paper
   advertises and only needs bumping when the **minor** version changes (e.g. `1.21` → `1.22`).

2. Rebuild: `./gradlew build`. That's it — `build.gradle` reads both values, and `paper-plugin.yml`
   gets its `api-version` filled in automatically, so nothing else needs touching for a normal bump.

3. **If the build fails to compile**, Mojang changed an API. Read the compiler errors — they point
   at the exact lines. The places most likely to break on a big update are:
   - anything using **packetevents** (`feature/simple/TabManager.java`) — bump the
     `packetevents-spigot` version in `build.gradle` to one that supports the new Minecraft version.
   - the **optional soft-dependencies** (`compileOnly(...)` in `build.gradle`): WorldGuard,
     FastAsyncWorldEdit, DiscordSRV. Update their versions to releases that target the new
     Minecraft version.
   - **enchantment / recipe registration** (`GlimpsyBootstrap.java`, `feature/explosive`,
     `util/enchantments`) and **scoreboard** (`feature/scoreboard`), which use Paper APIs that
     occasionally change shape between versions.

Everything else (config, messages, saved data) is version-independent and carries over untouched.

> **Why it's only "mostly" future-proof:** no Minecraft plugin can be fully future-proof — Mojang
> and Paper change APIs every major version, so a big jump (say 1.21 → 1.23) will usually need a few
> code tweaks. What this setup guarantees is that a *routine* bump is a one-line change, and when a
> jump does break something the compiler tells you exactly where.

---

## How the project is organised

Everything lives under `src/main/java/threeadd/glimpsy/`:

```
Glimpsy.java            The main plugin class (onEnable / onDisable). Wires everything together.
GlimpsyBootstrap.java   Runs before the plugin loads - used to register custom enchantments.

feature/                One folder PER feature. This is where gameplay code lives.
  recipes/                custom crafting/smelting recipes
  teams/                  teams, alliances, shield skins, team chat
  tradechest/             the trade-chest delivery system
  stats/                  per-player kills/deaths/fishes/playtime + /top leaderboards
  scoreboard/             the sidebar scoreboard
  map/                    the world reset (manual + scheduled) and /whenismapreset
  timezone/               per-player /timezone used by the scoreboard clock
  antifilter/             /antidrop and /antipickup
  simple/                 lots of small self-contained listeners (one file each)
  ...                     (competition, nickname, powerskull, wind, spawn, help, ...)

util/                   Shared plumbing used by the features.
  registry/               the Registry<T> base (see below)
  manager/                Manager / ListeningManager + ManagerRegistry
  command/                CustomCommand + CustomCommandRegistry
  recipes/                CustomRecipe + CustomRecipeRegistry
  listener/               ListenerRegistry
  config/                 ConfigManager (config.yml) + Messages (messages.yml)
  persistence/            Repository / DataBase (SQLite storage)
  inventory/              helpers for building GUIs
  text/                   MiniMessage / component helpers
  ...

template/               Copy-me examples (NOT wired into the plugin).
```

**The golden rule: one feature = one package under `feature/`.** When you add something new, make a
new package for it rather than dumping files into an existing one.

---

## The core idea: registries

The plugin doesn't scan the classpath for your classes — you add them to a **registry** by hand.
There are four you'll use, and they all work the same way (a plain list you edit in a constructor):

| You wrote a...            | Base class                | Add it to                                   |
|---------------------------|---------------------------|---------------------------------------------|
| Recipe                    | `CustomRecipe`            | `util/recipes/CustomRecipeRegistry.java`    |
| Command                   | `CustomCommand`           | `util/command/CustomCommandRegistry.java`   |
| Event listener            | `Listener`                | `util/listener/ListenerRegistry.java`       |
| Feature with saved data   | `Manager`/`ListeningManager` | `util/manager/ManagerRegistry.java`      |

On startup, `Glimpsy.onEnable()` asks each registry for its list and enables everything. So the
**last step of adding any feature is always "add one line to a registry."**

### Manager vs ListeningManager vs plain Listener

- **`Listener`** (plain) — a class with `@EventHandler` methods, for stateless reactions to events.
  Register it in `ListenerRegistry`.
- **`ListeningManager`** — a `Listener` that also needs to **run code on startup** (usually to load
  saved data from the database). Override `onEnable()`; it auto-registers itself as a listener too.
  Register it in `ManagerRegistry`.
- **`Manager`** (plain) — needs startup code but has **no events**. Override `enable()`. Register in
  `ManagerRegistry`. (`TimezoneManager` is an example.)

---

## How to add things

### A recipe
1. Copy `template/ExampleRecipe.java` into `feature/recipes`, rename it, change the shape/result.
2. Add `new YourRecipe()` to `CustomRecipeRegistry`'s constructor.

### A command
1. Copy `template/ExampleCommand.java` into a feature package, rename it, change the command tree.
2. Add `new YourCommand()` to `CustomCommandRegistry`'s constructor.
3. **Permissions are automatic** — every command gets a `2tenty.command.<name>` node granted to
   everyone by default (so LuckPerms can revoke it). For a staff command, override
   `getPermissionDefault()` to `PermissionDefault.OP` **and** add a `.requires(...)` check
   (see `MapResetCommand`).

### A listener
1. Copy `template/ExampleListener.java`, rename it, change the `@EventHandler` method.
2. Add `new YourListener()` to `ListenerRegistry`'s constructor.

### A tunable number (config.yml)
Don't hard-code balance numbers — read them from config so `/2tentyreload` can change them live:
```java
private static double myChance() {
    return ConfigManager.getDouble("my-feature.chance", 0.05); // 0.05 is the default
}
```
Then add `my-feature: { chance: 0.05 }` to `src/main/resources/config.yml`. `ConfigManager` has
`getInt/getLong/getDouble/getBoolean`.

### A player-facing message (messages.yml)
Don't hard-code text either — it should be translatable/re-brandable:
```java
player.sendMessage(Messages.msg("my-feature.hello", "<success>Hello {name}!", "name", player.getName()));
```
- The **first** argument is the key in `messages.yml`; the **second** is the default if it's missing.
- `{name}` style placeholders are filled by the trailing `"key", value` pairs.
- Colour tags like `<red>` work, plus **branding tags** (`<primary>`, `<success>`, `<error>`,
  `<muted>`, `<title>`, ...) that resolve to the colours in the `branding:` section — change one
  colour there and it re-themes everywhere.

### Saving data (database)
Persistent data uses a small SQLite database via a `Repository` subclass — see
`feature/stats/StatsRepository.java` for the simplest example (a table, a `loadAllSync()`, and
`runAsyncSave(...)`). The convention is: **load once on startup** (sync) inside a manager's
`onEnable()`, and **save changes off the main thread** with the `runAsync*` helpers.

---

## Configuration files

Two files are generated in `plugins/2tenty/` on first run:

- **`config.yml`** — gameplay-balance numbers only (radii, chances, multipliers, cooldowns,
  interval lengths, feature on/off switches).
- **`messages.yml`** — all player-facing text and the branding colours.

After editing either, run **`/2tentyreload`** in-game to apply the changes without a restart. (The
one exception is anything read at server-boot time, such as whether a recipe is registered — those
need a full restart.)

---

## A note on the code style here

- Features are deliberately kept **small and self-contained** — most of `feature/simple/` is one
  short listener per file. Prefer adding a new small class over growing an existing one.
- Comments explain the **"why"**, especially non-obvious constraints (e.g. "this must run in
  `onLoad()` because WorldGuard's flag registry closes before `onEnable()`"). When you add
  something surprising, leave a comment saying why.
- Balance numbers live in `config.yml`, text lives in `messages.yml` — try to keep new hard-coded
  numbers and strings out of the Java.

---

## Credits

- The **Bridge Egg** feature is based on [TheVihara/BridgeEggs](https://github.com/TheVihara/BridgeEggs).
- Built on Paper, FastAsyncWorldEdit, packetevents, Adventure, HikariCP + SQLite, and Brigadier.

*Originally vibe-coded, now with a README.*

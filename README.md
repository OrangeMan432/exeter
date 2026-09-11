# Larp Client (Minecraft 26.2 — Fabric)

Larp Client is an anarchy-oriented Minecraft client for **Minecraft 26.2 on Fabric**. It is a
from-scratch continuation of the Exeter client (`evelyn-gosselin/Exeter-1.12.2-fork`),
rebuilt against the modern Fabric toolchain so it can finally hang with the 1.12.2 clients on
today's anarchy servers. PvP logic follows Future-style patterns, movement follows
RusherHack-style patterns, and the UI stays Exeter.

## Why this exists

- **Beat the 1.12.2 clients at their own game.** The 1.12.2 generation had years of polish; this
  port closes that gap on 26.2.
- **Inventory interactions that actually work.** Overstacked / 127-count items, shulkers, and
  container automation must behave correctly. All item movement goes through Minecraft's own
  stack-size-aware click path (`AbstractContainerMenu.clicked(...)` with `ContainerInput`), which
  respects the server-reported max stack size, so overstacked items are handled instead of silently
  dropped or corrupted.
- **No artificial delay.** Modules do the work every tick they can; there is no built-in throttling
  slowing you down for no reason.

## Modules

Registered in `ModuleManager`. Categories: Combat, Miscellaneous, Movement, Render, World.

### Combat

| Module | Notes |
| --- | --- |
| `KillAura` | Attacks players/hostiles in range. Weapon auto-switch, rotate, cooldown (full-charge or Future-style spam at 0), walls-range, AntiWeakness sword swap. |
| `CrystalAura` | BlackOut-ported damage engine: per-spot target/self damage with exposure raycasts, armor, toughness, resistance, blast protection. MinDamage/MaxSelf gates, lethal fast-path. |
| `AnchorAura` | BlackOut-pattern anchor engine: place, charge, detonate with MinPlace/MaxSelf/MinExplode gates. |
| `AntiRegear` | Lemon-pattern shulker and ender chest breaker. |
| `Surround` | Shoreline-pattern obsidian feet trap plus Lemon support blocks, Anti CEV head cover, and pause on eat. Center on enable, attacks blocking crystals, off-center extend, jump-disable, instant re-place on explosion sound. |
| `AutoTrap` | Cages targets in obsidian (feet + head + top). |
| `SelfTrap` | Fully cages yourself in obsidian (feet + head + top). |
| `HoleFill` | Fills holes near enemies with obsidian, proximity mode included. |
| `AutoWeb` | Webs targets feet-first. |
| `AutoArmor` | Shoreline-pattern armor manager for 26.2 component armor. Blast-priority scoring, durability gate, binding skip, elytra priority. |
| `BedAura` | Automatically places and breaks beds for combat. BlackOut-style damage gates (MinDamage/MaxSelf), rotate, auto-switch, place/break delays. |
| `CevBreaker` | Lemon-pattern head-cover miner with damage-scored crystal seating. |
| `SelfBed` | Places and uses beds at your position for self-combat. |
| `AutoTotem` | Shoreline-pattern offhand manager. Absorption-aware health, fall-lethal fast-path, gapple while holding use on a sword, live totem count tag. |
| `AutoEat` | Eats food when hungry, gapples first when low on HP. 6b6t-pattern saturation scoring. |
| `AutoPot` | Automatically throws healing, swiftness, and debuff splash potions. Friend-aware, packet cleanup on death. |
| `AutoXP` | Throws XP bottles looking down for mending. |
| `AutoGear` | Kit sorter: equips saved gear sets from chests/shulkers. See `.minecraft/LarpClient/AutoGear.json`. |
| `AutoCart` | Minecart-based combat automation. |
| `PistonPush` | Places pistons + redstone to push players. Clicks solid neighbor faces, guarded slot swapping. |
| `PistonCrystal` | BlackOut-pattern staged piston crystal pusher. |
| `HolePush` | Lemon-pattern piston shove into nearby holes. |
| `TNTAura` | Lemon-pattern TNT placement with flint-and-steel ignition. |
| `SelfProtect` | Lemon-pattern Surround auto-run while enemies approach. |
| `BlockLag` | Shoreline-pattern burrow: rubberbands you inside obsidian. |
| `Criticals` | Shoreline packet patterns (PACKET chain + GRIM mode) on 26.2 attack packets. |

### Miscellaneous

| Module | Notes |
| --- | --- |
| `AutoItemDupe` | Recipe-book wooden-button dupe. Resolves the `RecipeDisplayId` from the client recipe book and fires `ServerboundPlaceRecipePacket`. Server-specific, not magic. |
| `AutoLog` | Meteor-pattern disconnect on low HP, totem pops, or nearby players. |
| `Replenish` | Meteor-pattern hotbar refill from inventory via shift-click merge. |
| `ChestStealer` | Loots chests and shulkers on a delay with auto-close. |
| `AutoTool` | Swaps to the fastest hotbar tool while mining. |
| `PingSpoof` | Delays keepalives for fake low ping. |

### Movement

| Module | Notes |
| --- | --- |
| `Speed` | Strafe-based speed with damage/lava/water boosts. |
| `Sprint` | Auto-sprint with omnidirectional mode. |
| `Step` | Step height via the `STEP_HEIGHT` attribute. |
| `Velocity` | Scales knockback by Horizontal/Vertical percent with optional explosion cancel and jump reset. |
| `ElytraFly` | Combatant-pattern engine: Boost, Static, Vanilla, Firework modes with elytra gating. |
| `LongJump` | Shoreline NORMAL-mode staged longjump. |
| `NoFall` | Resets fall distance to prevent fall damage. |
| `NoSlow` | Removes eating/drinking slowdown via LocalPlayer mixin. |
| `PacketFly` | BlackOut-addon-pattern packet flight with bounds spoof and teleport confirm. |
| `HoleSnap` | Anarchy staple: pulls you into the nearest safe hole and centers you. |
| `TargetStrafe` | Orbits targets at range with radial correction. |
| `FakeLag` | Chokes movement packets then flushes for lag teleportation. |
| `FastFall` | Falls faster than vanilla for quick drops. |
| `Jesus` | Walk on water, sneak to dive. |
| `Parkour` | 6b6t-pattern auto ledge jumps. |
| `SafeWalk` | 6b6t-pattern edge stop. |
| `NoBedStep` | Step variant tuned for bed-PvP terrain. |

### World

| Module | Notes |
| --- | --- |
| `AutoShulker` | Automatically places and opens shulker boxes. Container blacklist honored, player-following target range. |
| `InstaMine` | Packet mine for instant soft-block breaks. |
| `AutoMine` | Auto-breaks ores in range with tool swap. |
| `SpeedMine` | Lemon-pattern targeted mining with pickaxe swap. |

### Render

| Module | Notes |
| --- | --- |
| `Hud`, `ClickGui`, `TabGui`, `Colors`, `HUDEditor`, `EatTimer` | Client UI (Exeter). |
| `HoleESP` | Nicotine-pattern 3D hole outlines on the 26.2 render pipeline. |
| `Tracers` | Crosshair lines to players. |
| `StorageESP` | Boxes chests, shulkers, ender chests, barrels. |
| `Nametags` | Floating health + distance tags. |

## Controls

Modules are toggleable, bindable, and configurable through the ClickGui (Right Shift to open). Each
module has a settings panel for its properties.

## Building

Requires **JDK 25** and a network connection (Minecraft mappings + Fabric deps are fetched on first build).

```bash
chmod +x gradlew
./gradlew build
```

The built jar lands in `build/libs/`. Drop it in your `.minecraft/mods` folder alongside Fabric API.

## Configuration

Settings persist per-module as Gson JSON in `.minecraft/config/larp/` (`friends.json` lives alongside the client config).

## Credits

- **Exeter** — original client by `evelyn-gosselin`, UI foundation.
- **Future** (2.9 patterns: AutoCrystal settings, AntiWeakness, Lethal) — PvP design reference.
- **RusherHack-style** movement design reference.
- **Meteor** (open source) — aura cross-check (walls-range, cooldown).
- **Nicotine** (`tranarchy/nicotine`, GPL-3.0) — 26.x 3D render pipeline reference.
- **6b6t AnarchyClient** (`6b6t/AnarchyClient`, MIT) — Parkour/SafeWalk patterns.
- **Combatant** (`pivosos2007/combatant-client`, GPL-3.0) — 26.2 ElytraFly modes reference.
- **Lemon Client** (`ov-4/lemon-client`) — CevBreaker/SpeedMine/PistonCrystal/TNTAura patterns, ShulkerDupe logic.
- **BlackOut Meteor addon** (`pierogiee/BlackOut`) — PacketFly pattern.

## Disclaimer

This is a cheat client. Use it where it is allowed; you are responsible for the consequences of
running it on any server.

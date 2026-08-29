# WeirdPvP

A [Meteor Client](https://meteorclient.com/) addon for anarchy PvP (2b2t / 5b5t). It bundles a set of combat and inventory modules under a single `WeirdPvP` category, plus an `AutoGear` kit command and an `Eat Timer` HUD element.

Targets Minecraft `26.2` (1.21.5-era) via Fabric + Meteor Client `26.2-SNAPSHOT`.

## Modules

All modules live in the `WeirdPvP` category.

| Module | Command/key | What it does |
|--------|-------------|--------------|
| `anti-hole-camper` | AntiHoleCamper | Pushes enemies out of holes using pistons + redstone. |
| `auto-cart` | AutoCart | Places rails and TNT minecarts at an opponent's feet; optional insta-light. |
| `auto-gear` | AutoGear | Sorts your inventory to match a saved kit from a chest/shulker. |
| `auto-pot` | AutoPot | Throws healing/swiftness splash potions and bad pots at enemies. |
| `auto-shulker` | AutoShulker | Places shulker boxes near you and auto-opens them. |
| `bed-aura` | BedAura | Places and uses/breaks beds near targets for crystal-style damage. |
| `self-bed` | SelfBed | Places and uses a bed behind you for explosion knockback boost. |
| `speed+` | SpeedPlus | Strafe speed with damage boost (ported from Lemon client). |

### Module settings

**AntiHoleCamper**
- `rotate` (true) – rotate to the placement target.
- `range` (6) – detection range for targets and placement range.
- `place-delay` (0) – ticks between placements.
- `swing-hand` (true) – swing hand when placing.
- `debug` (false) – print debug info.

**AutoCart**
- `tnt-carts` (5) – minecarts per cycle.
- `carts-per-tick` (1) – minecarts placed per tick.
- `range` (5) – max placement range.
- `pull-from-inventory` (true) – pull carts from main inventory into hotbar.
- `rotate-rail` (true) / `rotate-minecart` (false) – rotate when placing.
- `send-complete-message` (true) – chat message when done.
- `insta-light` (false) – break the rail and light the block underneath, then repeat.
- `break-delay` (1, visible if `insta-light`) – ticks between break and light.

**AutoGear**
- `tick-delay` (0) – ticks between sort actions.
- `switch-per-tick` (1) – items moved per tick.
- `ender-chest` (false) – also sort from an open ender chest.
- `confirm-sort` (true) – re-checks the inventory once after sorting.
- `invasive` (false) – overwrite non-matching slots.
- `close-after` (false) – close the container when done.
- `info-msgs` (true) / `debug-mode` (false) – logging.

**AutoPot** (pages: `General`, `BadPot`)
- General: `health-potion`, `health` (16), `equal`, `predict`, `time-seconds` (1), `predict-health-delay` (50), `health-slot` (1), `health-delay` (50), `swiftness`, `time-left` (5), `swiftness-slot` (1), `swiftness-delay` (50), `on-ground-only` (true), `packet-switch` (true).
- BadPot: `delay` (10), `factor` (0.75), `range` (4), `badpot-slot` (1), `weakness`, `jump-boost`, `poison`, `slowness`, `debug`.

**AutoShulker**
- `once` (false) – place/open one shulker then disable.
- `empty-slots` (6, hidden if `once`) – min empty slots before placing a new box.
- `disable-after-death` (true, hidden if `once`).
- `range` (5) / `y-range` (5) – placement box.
- `target-range` (8) – beyond this range from the target, positions are pushed to higher Y levels; the module always prefers the farthest valid spot from the target.
- `tick-delay` (5) / `open-delay` (5).
- `inventory` (true) – move shulkers into hotbar.
- `slot` (1).
- `packet-place` (true), `place-swing` (true), `packet-swing` (true), `packet-switch` (true).

**BedAura**
- `rotate` (true), `auto-switch` (true), `switch-back` (true).
- `place-range` (5), `target-range` (10).
- `place-delay` (0), `break-delay` (1), `swing-hand` (true).

**SelfBed**
- `rotate` (true), `place-range` (5), `place-delay` (1), `use-delay` (0).

**SpeedPlus**
- `damage-boost` (true) – speed up on received knockback.
- `use-timer` (true) + `timer-speed` (1.2).
- `jump` (true) – auto-jump while moving.
- `strict` (false) – stricter speed calc.
- `water-speed` (true) – apply speed in liquids.
- `random-boost` (false) – occasional random boost.

### HUD

**Eat Timer** (`eat-timer`) – shows eating progress as a percentage. Setting: `shadow` (true).

### Command

`gear` (aliases `gr`, `kit`):
- `gear save <name>` – save current inventory as a kit.
- `gear set <name>` – select the active kit (reloads AutoGear).
- `gear del <name>` – delete a kit.
- `gear list` – list saved kits.

Kits are stored in `WeirdPvP/AutoGear.json` inside the Minecraft instance folder.

## Development

- Use this addon with Meteor Client installed. To test, run the `Minecraft Client` run configuration in your IDE (it launches a client with Meteor + this addon loaded).
- To build, run `./gradlew build`. The JAR lands in `build/libs`. Drop it into your instance's `mods` folder alongside Meteor Client.

## Project layout

```
src/main/java/sh/orangeman/weirdpvp/
├── WeirdPvP.java            # addon entrypoint, registers modules/commands/hud
├── commands/AutoGearCommand.java
├── hud/EatTimerHud.java
└── modules/                 # AntiHoleCamper, AutoCart, AutoGear, AutoPot,
                             # AutoShulker, BedAura, SelfBed, SpeedPlus
```

## License

MIT (see LICENSE).

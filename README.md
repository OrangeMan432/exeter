# idk (Minecraft 26.2 — Fabric)

**idk** is an anarchy-oriented Minecraft client built for **5b5t** on **Minecraft 26.2 (Fabric)**.
It is a from-scratch continuation of the original Exeter client
(`evelyn-gosselin/Exeter-1.12.2-fork`), rebuilt against the modern Fabric toolchain and tuned
specifically for 5b5t's anticheat stack (AnarchyExploitFixes + LPX).

> Formerly "Exeter" — renamed per project owner. The client title/build string will follow in a
> later branding pass; module code keeps the `me.friendly.exeter` namespace for config compat.

## Why this exists

- **Beat the 1.12.2 clients at their own game.** The 1.12.2 generation had years of polish; this
  port closes that gap on 26.2.
- **Inventory interactions that actually work.** Overstacked / 127-count items, shulkers, and
  container automation must behave correctly. All item movement goes through Minecraft's own
  stack-size-aware click path (`AbstractContainerMenu.clicked(...)` with `ContainerInput`), which
  respects the server-reported max stack size.
- **No artificial delay.** Modules do the work every tick they can; no built-in throttling.
- **5b5t-tailored by evidence, not guesswork.** Movement/elytra/chat limits are tuned against the
  actual server-side protection config (AnarchyExploitFixes), not forum rumors.

## Module roster (54 registered)

### Combat (13)
| Module | What it does | Key 5b5t tuning |
|---|---|---|
| AntiAim | Scrambles reported rotation (spin/random yaw, pitch modes) | view untouched; packet-only |
| AntiCrystal | Crystal-defense toolkit (prevent attack, place timeout) | pop radius + timeout customizable |
| AutoCrystal | Places & detonates crystals (damage heuristic, LOS gate) | friends skip, min-dmg/max-self gates |
| AutoPot | Auto splash potions (health/swiftness/bad-pots) | canThrow fixed: never in liquid, ground check |
| AutoTotem | Keeps totem in offhand | delay/hp-threshold/skip-open props |
| BedAura | Places & detonates beds (nether/end) | fixed place→use→break state machine |
| NoRotate | Blocks server camera rotation snaps | rotation rewrite, Only-In-Combat opt |
| SelfBed | Places & uses a bed at your feet for self-damage | conditional slot swap |
| Surround | Auto-obsidian shell around your feet | old-chunk safe, center pull, block pref |
| HoleSnap | Finds the nearest safe 1x1/1x2 hole and snaps you into it | Motion/Teleport/Strict modes, center pin, occupancy check |
| AutoCity | Breaks the block under an enemy's feet (the "city" move) | vanilla dig path, predict next step, min-dist-to-self gate |
| Velocity | Knockback control | per-axis reduction % (default 0 = vanilla); full cancel opt-in |

### Movement (8)
| Module | What it does | Key 5b5t tuning |
|---|---|---|
| AirJump | Mid-air hops | legit single-hop default |
| AntiVoid | Void recovery (upward/teleport) | scan depth, trigger Y |
| AutoJump | Auto hop while moving | only-when-moving / skip-using props |
| ElytraFly | Boost/Control/Vanilla elytra flight | **cap 1.7 old chunks, burst 3.1, nether-roof 0.5 — matches AEF** |
| FastFall | Dive down (sneak-triggered) | capped ≤ 0.98/tick |
| NoFall | Fall-damage prevention | packet onGround spoof + zero fall distance |
| SafeWalk | Edge protection | input-record rebuild, only-ahead mode |
| SpeedPlus | Ground speed (damage/jump boost) | jump multiplier softened (1.433/1.35) |

### Render (11)
Colors, CustomCrosshair (4 styles + rainbow), ESP (players/mobs/animals/items/pearls/invis,
outline/fill/cross), Fullbright (gamma), Hud (watermark/arraylist/armor/potions/coords/time/direction
— all with **%placeholder% support**), HUDEditor (drag & drop), ItemHighlight (fresh drops glow),
NameTags (custom format: %name% %health% %hearts% %ping%), NoHurtCam (only-own-damage mode),
NoWeather (rain / rain+thunder), TabGui (keyboard navigable), Tracers (filters + width).

### World (4)
AutoMine (vanilla dig path), AutoShulker (place→open→loot automation), FastPlace (20 CPS placement,
blocks-only opt), Scaffold (bridge/tower, silent aim).

### Misc (19)
AntiAFK (idle-only actions), AutoArmor (durability floor, toughness weight), AutoEat (trigger/stop
levels, golden-apple opt, combat pause), AutoItemDupe (recipe-book dupe sequence), AutoRespawn,
AutoShulker, AutoTool (min-gain %, whole-inventory search), ChatSpam (**%placeholder% in messages**,
5s min delay), ClickGui, Colors, DonkeyDupe, FastEat (early release), HUDEditor, InventoryResync
(ghost-item cleanup), MidClickPearl (silent pearl clutch), NoSlow (0.2873 delta cap), Refill
(same-item restock, top-off), ShulkerDupe, Timer (**default 1.05, hard-clamped 1.15**).

## PlaceholderAPI

Any user-facing text (Hud watermark/coords, ChatSpam messages, NameTags format) resolves
`%placeholders%`: `%player% %health% %fps% %ping% %coords% %x% %y% %z% %facing% %server% %players%
%time% %date% %totem% %crystals% %pearls% %xpbottles% %dimension% %biome% %helditem% %durability%`.
Modules can register more via `PlaceholderAPI.register(name, supplier)`.

## 5b5t anticheat notes (evidence-based)

The server runs **AnarchyExploitFixes (AEF)** — its public config is the authoritative list of
what's blocked:

- **Packet elytra** (glide-toggle >25/8s) → detected. Our ElytraFly works on `deltaMovement` only,
  never toggles gliding.
- **Elytra speed**: old chunks ~1.81, new-chunk burst 3.12–5.0 at high TPS, nether roof 0.5.
- **Chat rate limits** → ChatSpam min delay 100 ticks (5s).
- **Movement checks** → Timer clamped ≤1.15; NoSlow delta ≤0.2873 (vanilla sprint).
- **PacketFly / BoatFly** → patched server-side; we ship neither.

## Build

```bash
./gradlew build -x test
# → build/libs/exeter-1.0.jar
```

Requires Java 25+ (compatibilityLevel JAVA_25). Drop the jar into your Fabric `mods/` folder with
Fabric Loader 0.19.3+ and Fabric API 0.158.0+26.2.

## In-game

- Right Shift — ClickGui
- TabGui — arrow keys navigate, Right/Enter toggle
- HUDEditor — drag & drop HUD components
- `.help` — command list; `.toggle <module>`; `.bind <module> <key>`

## Credits

Original client: Friendly (Exeter 1.8) · 1.12.2 fork: evelyn-gosselin · 26.2 port & 5b5t tuning:
Gopro336 + humza branch contributors.

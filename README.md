# Exeter (Minecraft 26.2 — Fabric)

Exeter is an anarchy-oriented Minecraft client ported to **Minecraft 26.2 on Fabric**. It is a
from-scratch continuation of the original Exeter client (`evelyn-gosselin/Exeter-1.12.2-fork`),
rebuilt against the modern Fabric toolchain so it can finally hang with the 1.12.2 clients on
today's anarchy servers.

## Why this exists

The goal, straight from the project owner's brief:

- **Beat the 1.12.2 clients at their own game.** The 1.12.2 generation had years of polish; this
  port closes that gap on 26.2.
- **Inventory interactions that actually work.** Overstacked / 127-count items, shulkers, and
  container automation must behave correctly. All item movement goes through Minecraft's own
  stack-size-aware click path (`AbstractContainerMenu.clicked(...)` with `ContainerInput`), which
  respects the server-reported max stack size, so overstacked items are handled instead of silently
  dropped or corrupted.
- **No artificial delay.** Modules do the work every tick they can; there is no built-in throttling
  slowing you down for no reason.
- **Lemon-style utility.** The good stuff from Lemon Client — dupes, QoL, combat helpers — ported
  in and kept working.

## Modules

Registered in `ModuleManager`. Categories: Combat, Miscellaneous, Movement, Render, World.

### Combat

| Module | Notes |
| --- | --- |
| `Anti Aim` | Scrambles the rotation sent to the server (local view untouched). **Yaw Mode:** Off / Spin / Random. **Pitch Mode:** Off / Up (90) / Down (-90) / Zero (0) / Custom. Plus Spin Speed and Pitch Value sliders. |

### Miscellaneous

| Module | Notes |
| --- | --- |
| `ShulkerDupe` | Shulker / container dupe (ported from **Lemon Client**). Vanilla/Forge/Fabric **≤ 1.19** only. |
| `AutoItemDupe` | Recipe-book item dupe (ported from **Lambda 5bDupes** by ToxicAven). **5b5t-specific**. Full sequence wired: throw + recipe-place trigger via `handlePlaceRecipe` with the server-assigned `RecipeDisplayId`. |
| `DonkeyDupe` | Chested-horse (donkey/llama) dupe helper. Auto-rides the nearest chested horse with a chest; the dupe itself is a server-side bug triggered on disconnect while mounted. |
| `Refill` | Keeps the hotbar filled from the main inventory. One stack moved per tick via the stack-size-aware click path, so overstacked (127) items are moved correctly. Only runs while no container screen is open. |
| `AutoTotem` | Keeps a Totem of Undying in your offhand, moved from the main inventory via the stack-size-aware click path. Skips a tick if you are holding an item in the cursor. |
| `AutoArmor` | Equips the best armor piece per slot from the inventory, replacing strictly worse equipped pieces. Scores by armor + toughness; skips nearly-broken and named pieces. |
| `AutoEat` | Auto-eats the best food in the hotbar when hunger drops. Picks highest-nutrition food, pauses in combat, respects a health floor. |
| `Sprint` | Keeps you sprinting whenever moving — no double-tap W, no holding the sprint key. Modes: Omni (all directions) / Forward. |
| `InventoryResync` | Clears client-side ghost items after container-desync dupes by forcing a server-authoritative inventory round-trip. |

### Render

| Module | Notes |
| --- | --- |
| `Hud`, `ClickGui`, `TabGui`, `Colors`, `HUDEditor` | Client UI. |

### Dupe compatibility (read this)

These are **server-version / server-specific exploits**, not magic:

- `ShulkerDupe` only duplicates on servers running the old container-desync dupe (vanilla/forge/fabric
  1.19 and below). On 1.21+ it will click slots and do nothing.
- `AutoItemDupe` is the 5b5t recipe-book dupe. The full sequence is wired: it throws the held
  stack, then sends the recipe-place trigger (`ServerboundPlaceRecipePacket`) via
  `handlePlaceRecipe`, using the server-assigned `RecipeDisplayId` resolved from the client
  recipe book. It does nothing on servers without that intentional dupe.
- `DonkeyDupe` only helps on servers where the chested-horse dupe is live. It rides the horse; you
  still trigger the dupe by disconnecting while mounted.

If a dupe stops working, the server patched it — that is expected, not a bug in this client.

## Controls

Modules are toggleable, bindable, and configurable through the ClickGui (Right Shift to open). Each
module has a settings panel for its properties (e.g. `Anti Aim`'s yaw/pitch modes, `DonkeyDupe`'s
ride range, `Refill`'s behavior).

## Building

Requires **JDK 25** and a network connection (Minecraft mappings + Fabric deps are fetched on first build).

```bash
chmod +x gradlew
./gradlew build
```

The built jar lands in `build/libs/`. Drop it in your `.minecraft/mods` folder alongside Fabric API
and a compatible Meteor/Exeter loader.

## Configuration

Settings persist to a Gson JSON config (`friends.json` lives alongside the client config) — no TOML,
no dead dependencies.

## Credits

- **Exeter** — original client by `evelyn-gosselin`.
- **Lemon Client** (`ov-4/lemon-client`) — `ShulkerDupe` logic.
- **Lambda 5bDupes** (`ToxicAven/5b-AutoDupes`) — `AutoItemDupe` logic.

## Disclaimer

This is a cheat client. Use it where it is allowed; you are responsible for the consequences of
running it on any server.

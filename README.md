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
  stack-size-aware `clickSlot` path (which respects the server-reported max stack size), so
  overstacked items are handled instead of silently dropped or corrupted.
- **No artificial delay.** Modules do the work every tick they can; there is no built-in
  throttling slowing you down for no reason.
- **Lemon-style utility.** The good stuff from Lemon Client — dupes, QoL, combat helpers — ported
  in and kept working.

## Modules

Registered in `ModuleManager`. Categories: Combat, Miscellaneous, Movement, Render, World.

| Module | Category | Notes |
| --- | --- | --- |
| `ShulkerDupe` | Miscellaneous | Shulker / container dupe (ported from **Lemon Client**). Vanilla/Forge/Fabric **≤ 1.19** only. |
| `AutoItemDupe` | Miscellaneous | Recipe-book item dupe (ported from **Lambda 5bDupes** by ToxicAven). **5b5t-specific** (intentional dupe). Throws the held stack; the recipe-place trigger is not wired — see note below. |
| `DonkeyDupe` | Miscellaneous | Chested-horse (donkey/llama) dupe helper. Auto-rides the nearest chested horse with a chest; the dupe itself is a server-side bug triggered on disconnect while mounted. |
| `Hud`, `ClickGui`, `TabGui`, `Colors`, `HUDEditor` | Render | Client UI. |
| `AntiAim` | Combat | Packet-level aim scrambler (event wired, logic to be filled in). |

Modules are toggleable, bindable, and configurable through the ClickGui. Enable a dupe, open the
right container / hold the right item, and it fires.

### Dupe compatibility (read this)

These are **server-version / server-specific exploits**, not magic:

- `ShulkerDupe` only duplicates on servers running the old container-desync dupe (vanilla/forge/fabric
  1.19 and below). On 1.21+ it will click slots and do nothing.
- `AutoItemDupe` is the 5b5t recipe-book dupe. It throws the held stack (the portable part); the
  recipe-place trigger (`ServerboundPlaceRecipePacket`) is **not wired** because in Minecraft 26.2
  that packet requires an `int` `RecipeDisplayId` that is only resolvable through internal
  recipe-manager state — there is no public API to map a recipe name to its display id. It does
  nothing on servers without that intentional dupe.
- `DonkeyDupe` only helps on servers where the chested-horse dupe is live. It rides the horse; you
  still trigger the dupe by disconnecting while mounted.

If a dupe stops working, the server patched it — that is expected, not a bug in this client.

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

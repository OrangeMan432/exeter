# AGENTS.md — Humza Client (`humza-client` branch, EarthHack base)

Forge 1.12.2 Minecraft client mod. Branch `humza-client` tracks
`origin/humza-client` on `OrangeMan432/exeter`. Base: 3arthh4ck-continued (MIT, LICENSE kept verbatim).
Root package is `me.earth.earthhack`. Mio 0.6.9 full source is held as a
**donor only** (same Forge 1.12 toolchain, direct ports), not as base.

## Build (GREEN, keep it that way)

```bash
export JAVA_HOME=<temurin-or-zulu-jdk8>
./gradlew build
```

- Toolchain is Gradle 4.9 + ForgeGradle **pinned to
  `2.3-20190910.005614-43`** (never float `2.3-SNAPSHOT`; current
  snapshots generate a malformed forgeBin that no javac 8 accepts),
  Forge 1.12.2-14.23.5.2768, MCP stable_39, Java 8 target (8u242
  verified, newer 8u builds also fine for compiling OUR code).
- Full build ~1min warm. Incremental rebuilds ~30-60s.
- `cabaletta:baritone-deobf-unoptimized-mcp-dev:1.2` is a
  **compileOnly** dep from the impactdevelopment maven (never shaded;
  the Baritone Forge mod provides runtime classes).
- Base quirk fixed in-tree: `AntiPackets` used a method ref the old
  javac rejects, now a lambda. Do not "clean it up" back.
- ECJ cross-check available: ecj 3.33 jar in opencode temp with the
  minimal 1.12 classpath in /tmp/mincp.txt. Expect only the 9
  raw-registry strictness hits in base files; any error in a port
  is a real bug, fix it.

## Adding / porting a module

EarthHack module anatomy (example: `impl/modules/combat/autocrystal/`):
1. `Xxx.java` extends `Module` (category package must match).
2. `XxxData.java` holds `Setting<?>` declarations when the module is big.
3. `ListenerXxx.java` classes extend the api `EventListener<E>` and are
   registered on enable / unregistered on disable (custom `api/event/bus`
   SimpleBus, not Forge EVENT_BUS).
4. Register: add `new Xxx()` in `impl/managers/client/ModuleManager.java`.
5. Settings live in `api/setting` (`Setting<T>`, `SettingContainer`).
   Complex settings hide behind the Settings module by design; keep it so.

## Porting from Mio donor

- Mio source: temp deob dir (`Real-Mio-0.6.9`, Forge 1.12, MCP, full src).
- Mio uses Forge EVENT_BUS (`MinecraftForge.EVENT_BUS.register`) + its own
  `Setting<T>`; EarthHack uses SimpleBus. Port logic, not wiring: move the
  behavior into Module/Data/Listeners and re-register settings.
- Priority donors: combat (Aura, AutoTrap, HoleFiller, Criticals), exploit
  (XCarry, TPCoordLog, PearlSpoof), render gaps (BreakingESP, VoidESP).
- Keep per-port commits small: one module per commit, message
  `1.12.2: port <Module> from Mio`.

## Direction (Exeter flavor, later)

- Command prefix stays EarthHack default (`+`); ClickGui/HUD styling converges
  toward Exeter taste only after parity work lands. No renames of
  `me.earth.earthhack` packages: keeps diffs reviewable against upstream.
- Config stays EarthHack JSON. Do not introduce TOML.
- Credits: every ported module keeps its origin note (Mio / Future-pattern /
  upstream EarthHack). Never strip LICENSE or author headers.

## Workflow

- Work on `1.12.2`; commit and push to `origin/1.12.2` freely as work lands.
  Messages look like `1.12.2: <what changed>`.
- In-game testing is impossible from here; flag behavior needing a human on a
  real 1.12.2 Forge instance (mixins, bypasses, crystal timing).

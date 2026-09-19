# AGENTS.md — Humza Client (`humza-client` branch, EarthHack base)

Forge 1.12.2 Minecraft client mod. Branch `humza-client` tracks
`origin/humza-client` on `OrangeMan432/exeter`. Base: 3arthh4ck-continued (MIT, LICENSE kept verbatim).
Root package is `me.earth.earthhack`. Mio 0.6.9 full source is held as a
**donor only** (same Forge 1.12 toolchain, direct ports), not as base.

## Build (BLOCKED on this machine, be honest about it)

```bash
chmod +x gradlew
./gradlew build
```

- Toolchain is Gradle 4.9 + ForgeGradle 2.3-SNAPSHOT, Forge
  1.12.2-14.23.5.2768, MCP stable_39, Java 8 target. Gradle 4.9 dist and
  most FG artifacts are warm in ~/.gradle from a prior setup.
- Temurin JDK 8 builds live in the opencode temp dir (8u504 + 8u312).
- BUILD STATUS 2026-09-19: `./gradlew compileJava` runs ~2min with JDK 8
  then FAILS IN BASE CODE, not in ports: ForgeGradle's generated
  forgeBin jar has a malformed RuntimeInvisibleParameterAnnotations
  attribute that javac rejects on both 8u504 and 8u312 (reproduced with a
  5-line file). A clean tree fails identically. Suspect: current FG
  2.3-SNAPSHOT vs the 2024 one upstream used. Do NOT claim green.
- The `cabaletta:baritone-api:1.2` dep (impactdevelopment maven) was added
  without a working build and is UNVERIFIED: confirm compile resolves it.
- To unblock: pin an older FG 2.3 snapshot or repair the forgeBin
  attribute (ASM strip), then verify. That toolchain fix is in-scope work.

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

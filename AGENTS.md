# AGENTS.md — Skid Client (Exeter fork)

Fabric 26.2 Minecraft client mod. Branch `skid` tracks `origin/skid` on
`orangeman432/exeter`. Display name is "Skid Client"; packages stay
`me.friendly.exeter` (do not rename packages).

## Build

```bash
./gradlew build -x test
```

- No tests exist in the repo; a green build is the verification gate.
- JDK 25 is provisioned automatically by the Gradle toolchain. System java is
  21 — do not touch `JAVA_HOME`, the toolchain handles it.
- First build needs network (mappings + Fabric deps). Incremental builds take
  ~10s; `clean` forces slow loom re-resolves, avoid it unless necessary.
- `compileJava` rewrites `HASH`/`BUILD`/`DIRTY` inside
  `src/main/java/me/friendly/exeter/core/Exeter.java` on every compile. A dirty
  `Exeter.java` after building is expected, not a mistake.
- CI (`.github/workflows/gradle.yml`) is stale (JDK 11, `main` branch). Do not
  trust it; verify locally.
- `format` task exists (google-java-format, downloads its own jar on first run).

## Adding a module

1. Extend `ToggleableModule` in the matching
   `module/impl/toggle/{combat,misc,movement,render,world}/` package.
2. Register in `ModuleManager`: add the import **and** the `register(...)` call.
3. Tick logic: `Listener<TickEvent>`, gate on `event.getStage() != Stage.PRE`.
4. Packets: `Listener<PacketEvent>`, cancel with `event.setCanceled(true)`.
   Inbound and outbound both flow through it.
5. Settings: `offerProperties(...)`. Always use `NumberProperty` with min/max,
   never raw `Property<Double/Integer>` (unclamped sliders).
6. Inventory clicks go through `minecraft.gameMode.handleContainerInput`
   with `ContainerInput` (`PICKUP`, `QUICK_MOVE`, `SWAP`). Player container
   slots: 9–35 main inventory, 36–44 hotbar, 45 offhand. Hotbar index `i`
   maps to container slot `i + 36`. Guard the cursor with
   `containerMenu.getCarried().isEmpty()` before clicking.
7. Block placement: `PlayerUtil.useItemOn` / `swapTo` / `swapBack`. Only swap
   when the slot differs from selected; click a solid neighbor face
   (`pos.relative(dir)` + `dir.getOpposite()`), never raw air.

## Mixins

- Registered in `src/main/resources/mixins.exeter.json`. Existing:
  `MixinClientPlayer` (tick/motion events, NoSlow), `MixinLevelRenderer`
  (dispatches `RenderWorldEvent` after translucent features),
  `MixinNetworkManager`, `MixinGuiIngame`, `MixinKeyboardHandler`.
- Mixin targets validate at **game launch, not compile time**. Before writing
  or retargeting a mixin, verify the exact method/field signature against the
  real jar, e.g.:
  `javap -classpath ~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar
  net.minecraft.client.player.LocalPlayer`
- 3D ESP goes through `RenderWorldEvent` + `util/Render3D` (vanilla `lines()`
  pipeline only — no custom pipelines). No other render hooks exist.

## 26.2 mapping gotchas (verified against the jar, do not re-derive)

- No `SwordItem`, `ArmorItem`, or `DiggerItem` classes. Detect weapons by
  registry path (`_sword`/`_axe` suffix) or `ItemTags`; armor via the
  `DataComponents.EQUIPPABLE` component (`.slot()` gives the `EquipmentSlot`).
- Attacks arrive as `ServerboundAttackPacket` (split from interact packets).
  `gameMode.attack(player, entity)` still exists for sending them.
- Effects are holders: `MobEffects.RESISTANCE` (not `DAMAGE_RESISTANCE`),
  `SPEED`/`SLOWNESS`/`WEAKNESS`/`BLINDNESS` as used in `Speed`/`KillAura`.
- Enchant levels need registry-resolved holders:
  `level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)`
  then `stack.getEnchantments().getLevel(holder)`.
- Food check is `stack.get(DataComponents.FOOD) != null`; tool speed is
  `stack.getDestroySpeed(state)`; `ItemStack.is(Item)` exists.
- `DimensionType` has no `bedWorks()`/`ultraWarm()` — compare
  `level.dimension() == Level.NETHER`. Anchor charge is
  `RespawnAnchorBlock.CHARGE`; end crystals live at
  `entity.boss.enderdragon.EndCrystal`.
- Totem-pop animation id is 35 on `ClientboundEntityEventPacket`
  (`packet.getEntity(level)`, `packet.getEventId()`).
- Disconnect via `minecraft.getConnection().getConnection().disconnect(...)`
  (double hop — the first getter returns the packet listener).

## Workflow

- Work on `skid`; commit + push only when asked. Commit messages look like
  `skid: <what changed>`.
- README.md documents the real module roster — update its tables when adding
  modules. Credits section lists every client code was ported from; extend it.
- Reference sources live outside the repo in the opencode temp dir
  (`future-2.9-deobf.jar`, `shoreline/`, `blackout/`, `lemon/`, `kami/`,
  `nicotine/`, `anarchyclient/`, `combatant/` clones). Re-clone if missing.
- No web-fetchable source exists for RusherHack, Pyro, or Opium — do not burn
  time searching again.
- In-game testing is impossible from here; say so when behavior (not just
  compilation) needs a human to verify, especially new mixins and ESP.

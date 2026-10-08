# AGENTS.md

Working agreements for AI agents on this repo. The user runs the client and the game;
the agent writes code. Ask before crossing that boundary.

## Test / commit / push flow

1. When a task is complete, request permission to run the client with `./gradlew runClient`.
   Never run the client without asking first.
2. After the user tests, confirm everything worked properly.
3. If it worked, commit the changes, then request permission to push. Never push without
   being asked or given explicit confirmation.
4. When pushing is approved, enumerate the commits being pushed first if not already done.

## Commits

- One commit per fix. Do not batch unrelated fixes into a single commit.
- Concise imperative messages matching repo style (e.g. `Fix Speed NPE during config load`).
- Before committing, inspect `git status`, the diff, and recent log. Stage only intended
  files. Never commit secrets.
- Ensure `./gradlew formatCheck` passes (or run `./gradlew format`) before committing.

## Branches and remotes

- Every set of new features gets its own branch, created for a PR (e.g. `packetmine`).
  Do not pile unrelated work onto shared branches; `newbase-orange` is retired for new work.
- Push to the `origin` remote.
- Push a branch and open its PR with `gh pr create` only when asked.
- Push to `newbase` only when explicitly asked; prefer PRs for merging into it.

## Environment

- Run Gradle with `JAVA_HOME` pointed at Temurin JDK 25, e.g.
  `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"`.
- Use the `workdir` tool parameter instead of `cd` in shell commands.
- Never kill background Java processes. The user runs long-lived bot instances
  (viaproxy) alongside dev work; only act on processes the user names.

## Code conventions

- Rotation: use `PlayerUtil.setRotation` / `restoreRotation`, not hand-rolled packets.
- Logging: use `DebugLogger` (`log` with a level, `logFile` for file-only detail).
  Do not spam chat with `sendSystemMessage`.
- Settings: expose tuning as `Property` / `NumberProperty` / `EnumProperty` via
  `offerProperties()` so config save/load picks them up. Plain fields are invisible
  to the config system.
- Setting nesting: child settings belong under their parent via `visibleWhen`
  (e.g. Fill Alpha / Outline Alpha only show when Custom Alpha is on; gradient stop
  sliders stay hidden behind their pickers). Keep visibility predicates to one
  condition per line; never leave dead settings visible.
- Alpha in new/ported modules: never invent per-module alpha plumbing. Add a
  `Custom Alpha` toggle plus `Fill Alpha` / `Outline Alpha` (gated as above) and
  fall back to the central ESP alphas otherwise:
  `custom ? local : EspRenderManager.getGlobalFillAlpha()` (same for outline).
  Flat fills, outlines and gizmo styles all go through these two values.
- Shared UI helpers live in `module/impl/toggle/render/clickgui/` (e.g. `SelectionPopup`).
  Do not duplicate popup logic across modules.
- Test-only hooks go in `me.friendly.exeter.test` and must be inert unless explicitly
  enabled (e.g. `-Dexeter.smokeTest=true`).
- If the user corrects you, treat it as a standing constraint until explicitly lifted.

## Logging in new modules

- Every new or ported module must log through `DebugLogger`: enable (with key settings),
  state transitions, and actions taken with their values (positions, slots, names,
  distances, durability numbers).
- Never log per-tick. Log events and transitions only; gate repeated messages on
  state-change flags.
- Disable and shutdown reasons log at WARN with the exact values behind the decision,
  not just the check name.
- Detail too long for chat goes through `DebugLogger.logFile` (file and console only).

## Formatting

- Run `./gradlew format` (or verify `./gradlew formatCheck`) once, right before
  committing. Do not run the formatter repeatedly during development.

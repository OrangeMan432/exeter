# AGENTS.md — WeirdPvP (Meteor Client addon)

Engineering rules for agents working in this repository. Follow these without being asked.

## What this project is
- A Minecraft **Fabric** mod / **Meteor Client** addon written in **Java** (toolchain JDK 21+, Gradle via Fabric Loom).
- Targets Minecraft `26.2` / Meteor `26.2-SNAPSHOT`. APIs (e.g. `ContainerInput`, `DataComponents`) are the modern 1.21.5+ surface — do not use pre-1.21 method names.
- Purpose: anarchy PvP helpers (crystal/bed damage, speed, auto-pot, kit sorting, shulker/cart automation). It is a cheat client addon; keep that context in mind for anti-cheat realism, but the code itself must be clean and correct.

## Completion discipline (most important)
- **Do not stop until the goal is actually done.** Never end a turn with a plan, a question, or "I'll do X next." Do the work with tool calls now.
- After any code change, **verify before reporting success**: run `./gradlew build` (or at least `./gradlew compileJava`). If the build can't run (no network for deps), say so plainly and state what was left unverified. Never claim "it works" without a real build/check.
- Report failures with their actual output. No silent truncation, no "fixed" without evidence.
- Prefer the smallest correct change. Don't widen scope or refactor unrelated code unless the task needs it.

## Workflow basics
- **Read before edit.** Read the file (or the relevant section) before changing it. Don't re-read after a successful edit.
- **Edit, don't rewrite,** unless the file is small (<100 lines) or the structure genuinely needs it.
- **Parallelize independent tool calls** (reads, greps, edits that don't depend on each other) in one message.
- **One task at a time** for sequential dependencies.

## Meteor addon conventions
- Modules extend `meteordevelopment.meteorclient.systems.modules.Module`; commands extend `Command`; HUD elements extend `HudElement`.
- Register everything in `WeirdPvP.onInitialize()` and add the category in `onRegisterCategories()`.
- Settings go through the `Setting`/`SettingGroup` API (`.name()`, `.description()`, `.defaultValue()`, `.range()`, `.visible(...)`). Always set `name` and `description`.
- Subscribe to events with `@EventHandler` on `private void` methods; guard with `if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;` at the top of tick handlers.
- Reset transient state in both `onActivate()` and `onDeactivate()`.
- Keep `getRepo()` / `fabric.mod.json` metadata pointing at the real repo (`orangeman432/meteor-5b5t-addon`).

## Known weak spots to avoid repeating
- Don't key items by `id + damageValue` only — that ignores NBT (enchanted vs plain). Use full item identity when matching.
- Don't duplicate `findTarget` / `inRange` / `canPlace` across modules; add a shared helper instead of copy-paste.
- Don't leave dead settings (declared but never read). If a setting isn't wired to behavior, remove it or implement it.
- Avoid hand-rolled index math over inventory/container slots; it is the source of the sort bugs. Prefer Meteor's `InvUtils` / `FindItemResult` helpers.
- Guard async callbacks (e.g. `Rotations.rotate(..., () -> ...)`) against inventory changes before they fire.

## Safety
- Never commit secrets, tokens, or `.git` history rewrites without explicit instruction.
- Keep `LICENSE` and attribution intact.
- Don't change `gradle/libs.versions.toml` Minecraft/Meteor versions unless the task is an update; mismatched versions break the build.

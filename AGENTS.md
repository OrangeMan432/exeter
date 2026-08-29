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

## Mistakes to never repeat (auto-learned from review)
These were found in this repo. Each is a rule, not a suggestion.

1. **Don't invert a nearest-target comparator.** AutoShulker used `.max` on a squared-distance score, so it placed shulkers as FAR from the enemy as possible. Rule: to pick the position NEAREST a target, use `.min` (or negate the score). Always check the comparator direction against the stated intent. `modules/AutoShulker.java` (`getWeight` + the `.stream().max(...)` call).
2. **Ignite a minecart on its own block, not the block beneath.** AutoCart lit `targetPos.below()` (the rail's support block) instead of the TNT minecart at `targetPos`, so carts never ignited. Rule: when you must interact with an entity/block at a position, target that position, not `pos.below()`. `modules/AutoCart.java` (`tickLighting`).
3. **Keep repo metadata pointing at the real repo.** `getRepo()` used `meteor-weirdpvp`, which doesn't exist (real: `meteor-5b5t-addon`), silently breaking the in-game update check. Rule: `getRepo()` and `fabric.mod.json` must match the actual GitHub `owner/repo`. `WeirdPvP.java`, `fabric.mod.json`.
4. **Match items by full identity, not `id+damageValue`.** AutoGear keys items by registry id + damage value, so an enchanted sword == a plain one and kits grab the wrong stack. Rule: include NBT/components (use Meteor's item-comparison helpers) when identity matters. `commands/AutoGearCommand.java` (`getItemKey`), `modules/AutoGear.java` (sort/match).
5. **Never hand-roll inventory/container slot math.** AutoGear's sort mutates its input map while iterating and decrements the loop index by hand (`i--`) with a dynamic bound — fragile on stacks >1 and already-correct slots. Rule: use Meteor `InvUtils` / `FindItemResult`; never mutate a collection during iteration and never decrement a for-loop index manually. `modules/AutoGear.java` (`getInventorySort`).
6. **Every setting must be wired to behavior.** `lavaBoost` in SpeedPlus was declared and described but never read — only `waterSpeed` gated liquids. Rule: if you add a toggle, use it or delete it. No orphan settings. `modules/SpeedPlus.java`.
7. **Reset transient movement state when the player stops.** SpeedPlus left `level` at 2/3 when movement stopped, causing a speed burst on resume. Rule: when `!moving`, reset `level`/`moveSpeed` (and timer override) so the next move starts clean. `modules/SpeedPlus.java` (`onMove`).
8. **Don't trust inventory across an async rotation callback.** AutoPot scheduled `throwPotion(slot)` ~100ms later via `Rotations.rotate(..., () -> ...)`; if the inventory shifted in that window it throws the wrong item, and it hardcodes pitch 90. Rule: capture/re-validate slot state synchronously, or re-check the slot inside the async callback. `modules/AutoPot.java`.
9. **Clean up template leftovers.** The README shipped as the unmodified Meteor template and a dead `assets/template/icon.png` was included. Rule: after scaffolding from a template, replace README with real module docs and delete unused template assets. (Done: README rewritten, template icon removed.)
10. **Keep repo metadata consistent with the real location.** Owner case/name drift (`OrangeMan432` vs `orangeman432`) is benign on GitHub but a wrong repo *name* is not. Rule: one source of truth for the repo path; verify it resolves.

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
- Don't duplicate `findTarget` / `inRange` / `canPlace` across modules; add a shared helper instead of copy-paste.
- Guard async callbacks (e.g. `Rotations.rotate(..., () -> ...)`) against inventory changes before they fire.

## Safety
- Never commit secrets, tokens, or rewrite `.git` history without explicit instruction.
- Keep `LICENSE` and attribution intact.
- Don't change `gradle/libs.versions.toml` Minecraft/Meteor versions unless the task is an update; mismatched versions break the build.

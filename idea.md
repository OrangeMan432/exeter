# Idea / Goals — what orangeman is building

Notes distilled from planning chat (orange + sirhumza). This is the direction, not a changelog.

## North star
Build a modern-version Minecraft client (**Exeter**, a fork of the 1.12.2 Exeter) that can **compete with the old 1.12.2 clients** on feel and responsiveness. Currently targeting **Minecraft 26.2** (1.21.5-era), after an earlier 1.21.11 port.

## The core blocker: inventory interactions
The thing that made 1.12.2 clients good was tight, low-latency inventory handling. Modern MC broke that:
- **Overstacked items are the problem.** AutoGear (this addon) is the proving ground and it "doesn't work well" today.
- **1.21 changed how stacks work** — stacks of 127 items (and similar overstacked counts) **can't be interacted with correctly right now**. Fixing click/swap/pickup on overstacked slots is the make-or-break task.
- Until inventory clicks are reliable on overstacked stacks, the kit/gear features can't match 1.12.2 behavior.

## Current work in progress
- Rewriting the **Gradle build script** because it "isn't working right on 26.2" (hence the loom/libs refresh).
- Adding a **TOML config system** to Exeter (replacing the old JSON approach that "never worked") and cleaning up the base code.
- Plan is to push the reworked base to a **new branch** (not straight to master).

## agents.md directive (from sirhumza)
To stop the AI from bailing out early: the project `AGENTS.md` must state plainly that the agent should **always finish the job it's asked for** — never stop at a plan or "I'll do it next." (This repo's `AGENTS.md` already encodes that as the "Completion discipline" rule.)

## Open questions / threads
- AnyDesk alternative for remote control — leaning toward **SSH over Tailscale** (orange's setup).
- Whether to adopt sirhumza's `agents.md` (originally lifted from "nuraad") as a base.
- General "what makes this unique vs other modern clients" — still open.

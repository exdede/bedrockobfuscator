# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Bedrock Obfuscator: a client-side Fabric mod for Minecraft 1.21.11 (official Mojang mappings, not Yarn). It visually replaces the bottom five Overworld bedrock layers (Y -64 to -60) with a configurable fill block, purely at render time. It can also hide ores and/or a fixed list of stone-variant blocks (andesite, granite, diorite, tuff, gravel, dirt) anywhere in a configurable Y range, replacing them with deepslate below Y 0 or stone above it, again purely at render time. The real world, collision, mining, and server state are never touched.

## Build

Requires JDK 21. If the system default `java` is newer, point Gradle at 21 explicitly:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

Output jar: `build/libs/bedrockobfuscator-1.0.0.jar`.

`./gradlew runClient` launches a dev client but needs a display (X11/Wayland) — it can't run headless in this environment.

There is no test suite; verification is manual in-game (see README's feature list for what to check: toggle, fill-block picker, void-shaft holes, player-overlap exemption, dimension changes).

## Architecture

The entire mod is one mechanism: a mixin swaps the `BlockState` a chunk mesher reads at a given position, without touching the live `Level`. Collision, mining, and gameplay all read the real world; only the baked terrain geometry differs.

Two renderers must be hooked separately because Minecraft's vanilla mesher and Sodium's mesher are different code paths, and Sodium (used by default under Lunar Client) completely bypasses vanilla's:

- `mixin/RenderSectionRegionMixin.java` — hooks vanilla's `RenderSectionRegion.getBlockState(BlockPos)`.
- `mixin/LevelSliceMixin.java` — hooks Sodium's `LevelSlice.getBlockState(int,int,int)`. Targeted by string (`@Mixin(targets = "...")`, `remap = false`) since there's no compile-time Sodium dependency. Lives in a separate `bedrockobfuscator.sodium.mixins.json` with `required: false` so it silently no-ops when Sodium isn't present.

Both mixins implement `render/MeshTarget.java` and delegate all actual logic to `RenderState.decide(...)` — that's the one place the swap decision (dimension check, Y-range check, player-overlap exemption, void-shaft exemption) lives. Never duplicate that logic in a mixin; add a new render-path hook by implementing `MeshTarget` and calling `decide`.

`render/RenderState.java` holds all state the mesher threads read: `volatile` fields written from the client thread (`enabled`, `overworld`, `fillState`, `overlap` positions) so worker threads see updates without locking. `render/MeshProbe.java` provides thread-local scratch state (a reentrancy guard and a mutable `BlockPos`) used when probing a whole column to detect a void shaft — a column that's entirely air down through Y -64..-60 is left unpainted rather than given a fake floor.

`decide(...)` also carries a second, independent decision path for underground hiding: if the position falls outside the bedrock Y range but inside the configurable `undergroundMinY..undergroundMaxY` range, and either `hideOres` or `hideStoneVariants` is on, the block is checked against one of two fixed `Set<Block>` lookups (`ORE_BLOCKS`, `STONE_VARIANT_BLOCKS`) and swapped for a plain deepslate or stone state depending on whether `y < 0`. This path never touches air (it only replaces solid target blocks) so it needs neither the player-overlap exemption nor the void-shaft probe that the bedrock path uses.

`BedrockObfuscatorClient.java` (the `ClientModInitializer`) runs the per-tick loop: reconciles `RenderState` with the MaLiLib-backed config, tracks the player's dimension and hitbox overlap (`render/OverlapTracker.java`), and triggers re-meshes via `render/RerenderHelper.java` (`LevelRenderer.setSectionDirtyWithNeighbors` / `allChanged()`) so config or overlap changes show up immediately instead of waiting for the next natural re-mesh.

Three independent points of entry for the three user-facing actions (toggle bedrock hiding, open settings, toggle underground hiding): MaLiLib chord-capable hotkeys declared in `config/Configs.java` and wired up in `init/BedrockObfInitHandler.java`/`init/BedrockObfKeybindProvider.java`, and mirrored single-key vanilla `KeyMapping`s registered in code in `BedrockObfuscatorClient.onInitializeClient()` (unbound by default; `fabric.mod.json` has no Controls section of its own). Both call into the same config/GUI-open logic in `BedrockObfuscatorClient`.

`config/FillBlockFilter.java` filters `BuiltInRegistries.BLOCK` down to full opaque solid cubes only (`isSolidRender()` + `isCollisionShapeFullBlock()`), excluding block entities and a manual denylist (barrier, light, structure void, moving piston) — this is what populates the fill-block picker in `gui/ConfigScreen.java`. The advanced text field bypasses this filter entirely.

## Conventions specific to this repo

- User-facing text (README, in-game messages, GUI labels, Modrinth/store copy) must be plain language a non-technical viewer can follow — no implementation jargon (renderer, mesher, snapshot, hook, substitutes), no em dashes, no AI-sounding filler. See mixin/README comments for the technical explanation; keep that out of anything a player reads.
- Mixin method names inside third-party classes (Sodium) are prefixed `bedrockobfuscator$` to avoid collisions.

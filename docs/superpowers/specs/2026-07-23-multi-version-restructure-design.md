# Multi-version restructure: ports to MC 1.26, 1.26.1, 1.26.2

## Context

Bedrock Obfuscator currently targets only Minecraft 1.21.11, as a single
Gradle project at the repo root. The user wants the mod available for MC
1.26, 1.26.1, and 1.26.2 as well, and wants the repo laid out so each
supported Minecraft version has its own folder, rather than juggling
branches or a single multi-target build. This is a repo restructure plus
three new ports, not a change to the mod's features — the current feature
set (bedrock hiding + underground ore/stone-variant hiding, both shipped in
1.1.0) is what gets ported, unchanged.

The author has said outright they rely on AI for this project and isn't a
Minecraft-modding expert themselves, so the chosen approach favors whatever
is simplest to reason about and safest to hand-edit later, over what's most
"clever" or DRY.

Note on unknowns: the assistant's knowledge cutoff is January 2026; MC
1.26.x may postdate that. The exact API/mapping differences for the three
new versions are not knowable in advance and will only surface once each
version is actually compiled against.

## Design

### Repo layout

```
bedrock obfuscator/
  README.md, CLAUDE.md, LICENSE, icon.png, image.psd.png   (repo-wide, unchanged location)
  .gitignore                                                (unchanged; patterns already match inside nested folders)
  versions/
    1.21.11/   <- current root Gradle project, moved here verbatim
    26/        <- new, targets MC 1.26
    26.1/      <- new, targets MC 1.26.1
    26.2/      <- new, targets MC 1.26.2
```

Each `versions/<x>/` folder is a fully independent Fabric Loom project: its
own `gradlew`/`gradlew.bat`, `gradle/wrapper/`, `build.gradle`,
`gradle.properties`, `settings.gradle`, and `src/`. No shared source set, no
cross-folder Gradle includes. Building one version is `cd versions/26 &&
./gradlew build` and nothing else is touched. This was chosen over a shared
common-module layout or Stonecutter specifically for that isolation: a
change or a Mojang mapping shift in one version's build can never break
another version's build, and there's no multi-project Gradle wiring to
misconfigure.

Trade-off, stated plainly: a bugfix that applies to all four versions has to
be applied four times. Given how small this mod is (about a dozen files)
and how infrequently it changes, that's an acceptable cost for the
simplicity gained.

### What moves vs. what's new

1. **1.21.11**: the entire current root project (`build.gradle`,
   `gradle.properties`, `gradlew`, `gradle/`, `settings.gradle`, `src/`)
   moves into `versions/1.21.11/` with `git mv`, preserving history.
2. **26, 26.1, 26.2**: each starts as a copy of the freshly-moved
   `versions/1.21.11/` folder (same source, same mixins, same MaLiLib usage)
   and then gets its `gradle.properties` updated to point at the real
   dependency versions for that Minecraft release.

### Jar naming

Every version's `build.gradle` sets the output jar name to include the game
version: `bedrockobfuscator-<mod_version>-mc<game_version>.jar`, e.g.
`bedrockobfuscator-1.1.0-mc1.21.11.jar`. Applied retroactively to the
1.21.11 folder too, so naming is consistent across all four from this point
on.

### Dependency resolution per new version

For each of 26 / 26.1 / 26.2, before attempting a build:

- Look up the matching Fabric Loader and Fabric API versions for that exact
  game version (Fabric's meta API: `meta.fabricmc.net`).
- Look up matching MaLiLib and Mod Menu versions that declare support for
  that game version (Modrinth's API, since both are Modrinth-hosted per the
  existing `gradle.properties` maven config).
- Write those into that version's `gradle.properties`.

If no compatible MaLiLib/Mod Menu build exists yet for a given version, that
version gets flagged back to the user rather than silently shipped broken or
skipped without mention.

### Build / fix loop (the real work)

For each new version folder:

1. `./gradlew build` and read the compile errors.
2. Fix renamed classes/methods (Mojang official mappings churn between
   versions — this project already hit this once, see the "1.21.11 mojmap
   renames" pattern from the initial port).
3. Re-check both mixin targets specifically: `RenderSectionRegionMixin`
   (vanilla `RenderSectionRegion.getBlockState`) and `LevelSliceMixin`
   (Sodium's `LevelSlice.getBlockState`, matched by string target since
   there's no compile-time Sodium dependency) are the two places most likely
   to break silently — a mixin whose target method no longer exists/matches
   fails at runtime, not always at compile time, so these get a deliberate
   manual check even after a clean compile.
4. Repeat until `./gradlew build` succeeds and produces the correctly-named
   jar.

### Docs

Root `README.md` gets a short section explaining the `versions/` layout and
how to build a specific one, so a non-expert visiting the repo (including
future-the-user) isn't confused by four Gradle projects in one repo.
`CLAUDE.md`'s "Build" section gets the same update.

### Out of scope for this pass

- In-game verification of the three new versions (no display in this
  environment; same limitation that already applies to 1.21.11).
- Any new mod features — this is a pure port of the existing 1.1.0 feature
  set.
- GitHub Release / Modrinth / CurseForge uploads for the new versions —
  handled after the user has verified each build in-game, same as 1.1.0.

## Files touched

- Repo restructure: `git mv` of the entire current root project into
  `versions/1.21.11/`.
- Three new folders: `versions/26/`, `versions/26.1/`, `versions/26.2/`,
  each a full copy with version-specific `gradle.properties` and whatever
  source fixes each version's compile/mixin check requires.
- `versions/1.21.11/build.gradle` (and the three new ones): jar-naming
  change.
- `README.md`, `CLAUDE.md`: layout/build instructions.

## Verification

1. `cd versions/1.21.11 && ./gradlew build` still succeeds post-move, jar
   name matches the new convention.
2. For each of `versions/26`, `versions/26.1`, `versions/26.2`:
   `./gradlew build` succeeds, output jar is
   `bedrockobfuscator-1.1.0-mc<version>.jar`.
3. Report back anything that had to be guessed at or couldn't be resolved
   (e.g. no MaLiLib build yet for a given version) rather than silently
   shipping a best-effort jar.
4. In-game testing of 26/26.1/26.2 is manual, on the user's end, same as
   the existing CLAUDE.md testing note for 1.21.11.

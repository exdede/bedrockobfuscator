# Multi-Version Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the repo into `versions/<x>/` folders (one independent Fabric Loom project per Minecraft version) and port the existing 1.1.0 feature set to Minecraft 26.1, 26.1.1, 26.1.2, and 26.2, alongside the existing 1.21.11.

**Architecture:** Each `versions/<x>/` folder is a fully independent Gradle project (own `gradlew`, `build.gradle`, `gradle.properties`, `settings.gradle`, `src/`) — no shared source set, no multi-project wiring. 1.21.11 keeps its current obfuscated-build setup (Mojang mappings resolved via Loom). 26.1+ uses Minecraft's new unobfuscated jars, which removes the mappings step entirely and requires Java 25 instead of 21.

**Tech Stack:** Fabric Loom (`fabric-loom` plugin for 1.21.11, `net.fabricmc.fabric-loom` non-remap plugin for 26.1+), Gradle, MaLiLib, Mod Menu, Mixin.

## Global Constraints

- No test suite exists for this project. "Testing" a task means `./gradlew build` succeeds and produces the correctly-named jar — matches CLAUDE.md's existing "verification is manual in-game" convention. Do not write unit tests; there is no test framework in this repo.
- JDK 21 builds `versions/1.21.11` (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`). JDK 25 builds `versions/26.1`, `versions/26.1.1`, `versions/26.1.2`, `versions/26.2` — already staged at `/home/exdede/.local/jdks/jdk-25.0.3+9` (no sudo needed, verified `javac 25.0.3` works).
- Jar naming, all five folders: `bedrockobfuscator-<mod_version>-mc<game_version>.jar`, e.g. `bedrockobfuscator-1.1.0-mc1.21.11.jar`, `bedrockobfuscator-1.1.0-mc26.1.jar`.
- `mod_version` stays `1.1.0` in every folder — this is the same release, ported, not a new feature set.
- Root-level files stay at the repo root, not duplicated per folder: `README.md`, `CLAUDE.md`, `LICENSE`, `icon.png`, `image.psd.png`, `.gitignore`. `.gitignore`'s existing patterns (`build/`, `.gradle/`) already match inside nested folders — no change needed there.
- Verified real dependency versions for the four new folders (checked live against Fabric's meta API and Modrinth's API on 2026-07-23 — do not substitute guessed versions):
  - `loader_version=0.19.3` (same as 1.21.11, Fabric Loader is not Minecraft-version-specific)
  - 26.1 / 26.1.1 / 26.1.2: `fabric_version=0.155.2+26.1.2`, `malilib_version=0.28.9`, `modmenu_version=18.0.0`
  - 26.2: `fabric_version=0.155.2+26.2`, `malilib_version=0.29.3`, `modmenu_version=20.0.1`
- 26.1+ build.gradle differences from 1.21.11, confirmed against `FabricMC/fabric-example-mod`'s own `26.1`/`26.1.1`/`26.1.2`/`26.2` branches (ground truth, not guessed):
  - Plugin id: `net.fabricmc.fabric-loom` (not `fabric-loom`, and not the `-remap` variant — Minecraft itself is unobfuscated from 26.1 on).
  - No `mappings` line in `dependencies` at all — there is no separate mappings artifact anymore.
  - `fabric-loader` and `fabric-api` change from `modImplementation` to plain `implementation`.
  - `sourceCompatibility`/`targetCompatibility` = `JavaVersion.VERSION_25`, `options.release = 25`.
  - `fabric.mod.json`: `"java": ">=25"` instead of `">=21"`.
  - These are genuinely new for this codebase; if the exact Loom plugin version pinned below fails to resolve, that's expected iteration territory (see Task 3, step on build/fix loop) — not a sign the plan is wrong.
- `LICENSE` now lives two directories above each version folder (`<repo-root>/LICENSE`, not `<repo-root>/versions/<x>/LICENSE`). Every folder's `jar { from(...) }` block must reference `"../../LICENSE"`, not `"LICENSE"`.

---

### Task 1: Move the current project into `versions/1.21.11/` and fix jar naming

**Files:**
- Move (via `git mv`): `build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `settings.gradle`, `gradle/` → `versions/1.21.11/`
- Move (via `git mv`): `src/` → `versions/1.21.11/src/`
- Modify: `versions/1.21.11/build.gradle` (jar naming, LICENSE path)

**Interfaces:** N/A (build tooling only, no code interfaces).

- [ ] **Step 1: Create the target directory and move files with history preserved**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
mkdir -p versions/1.21.11
git mv build.gradle gradle.properties gradlew gradlew.bat settings.gradle gradle src versions/1.21.11/
```

- [ ] **Step 2: Fix jar naming in `versions/1.21.11/build.gradle`**

Change:
```groovy
version = project.mod_version
```
to:
```groovy
version = "${project.mod_version}-mc${project.minecraft_version}"
```

Change:
```groovy
processResources {
    inputs.property "version", project.version

    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}
```
to:
```groovy
processResources {
    inputs.property "version", project.mod_version

    filesMatching("fabric.mod.json") {
        expand "version": project.mod_version
    }
}
```
(This keeps the mod's *displayed* version in `fabric.mod.json` as plain `1.1.0`, while the jar filename gets the `-mc1.21.11` suffix via Gradle's normal `archivesName-version.jar` convention — no per-task `archiveFileName` overrides needed.)

- [ ] **Step 3: Fix the LICENSE path in the same file**

Change:
```groovy
jar {
    from("LICENSE") {
        rename { "${it}_${project.archives_base_name}" }
    }
}
```
to:
```groovy
jar {
    from("../../LICENSE") {
        rename { "${it}_${project.archives_base_name}" }
    }
}
```
(`LICENSE` now lives at the repo root, two directories above `versions/1.21.11/build.gradle`.)

- [ ] **Step 4: Build and verify**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator/versions/1.21.11"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
ls build/libs/
```
Expected: `BUILD SUCCESSFUL`, and `build/libs/bedrockobfuscator-1.1.0-mc1.21.11.jar` exists (plus a matching `-sources.jar`).

- [ ] **Step 5: Commit**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git add -A versions/1.21.11
git commit -m "Move 1.21.11 project into versions/ folder, add game version to jar name"
```

---

### Task 2: Update root docs for the new layout

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

**Interfaces:** N/A (docs only).

- [ ] **Step 1: Add a "Versions" section to `README.md`**, after the existing "## Building" section:

```markdown
## Versions

This repo has one folder per supported Minecraft version, each a complete,
independent build:

- `versions/1.21.11/`
- `versions/26.1/`
- `versions/26.1.1/`
- `versions/26.1.2/`
- `versions/26.2/`

To build a specific version:

```bash
cd versions/26.2
./gradlew build
```

The finished jar will be in that folder's own `build/libs/`, named
`bedrockobfuscator-<mod version>-mc<game version>.jar`.
```

- [ ] **Step 2: Update `CLAUDE.md`'s "Build" section** to reflect the per-version folders. Replace:

```markdown
## Build

Requires JDK 21. If the system default `java` is newer, point Gradle at 21 explicitly:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

Output jar: `build/libs/bedrockobfuscator-1.0.0.jar`.
```

with:

```markdown
## Build

The repo is split into `versions/<x>/` folders, one fully independent Gradle
project per supported Minecraft version (`1.21.11`, `26.1`, `26.1.1`,
`26.1.2`, `26.2`). Build one version by `cd`-ing into its folder first.

`versions/1.21.11` still targets an obfuscated Minecraft jar and needs JDK 21:

```bash
cd versions/1.21.11
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

`versions/26.1`, `versions/26.1.1`, `versions/26.1.2`, and `versions/26.2`
target Minecraft's newer unobfuscated jars and need JDK 25:

```bash
cd versions/26.2
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

Output jar per folder: `build/libs/bedrockobfuscator-<mod_version>-mc<game_version>.jar`.
```

- [ ] **Step 3: Commit**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git add README.md CLAUDE.md
git commit -m "Document the versions/ folder layout"
```

---

### Task 3: Port to Minecraft 26.1

**Files:**
- Create: `versions/26.1/` (copied from `versions/1.21.11/`)
- Modify: `versions/26.1/build.gradle`
- Modify: `versions/26.1/gradle.properties`
- Modify: `versions/26.1/src/main/resources/fabric.mod.json`
- Modify (as needed, discovered during build): any `.java` files under `versions/26.1/src/` that don't compile against 26.1's API

**Interfaces:** N/A (build tooling + source-compat fixes only; no interfaces cross into other tasks).

- [ ] **Step 1: Copy the 1.21.11 folder as a starting point**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
cp -r versions/1.21.11 versions/26.1
rm -rf versions/26.1/build versions/26.1/.gradle
```

- [ ] **Step 2: Replace `versions/26.1/gradle.properties`** with:

```properties
# Done with Gradle
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false

# Fabric / Minecraft
# https://fabricmc.net/develop
minecraft_version=26.1
loader_version=0.19.3
fabric_version=0.155.2+26.1.2

# Mod
mod_version=1.1.0
maven_group=com.bedrockobfuscator
archives_base_name=bedrockobfuscator

# Dependencies (Modrinth maven)
malilib_version=0.28.9
modmenu_version=18.0.0
```

- [ ] **Step 3: Replace `versions/26.1/build.gradle`** with:

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version '1.17.13'
    id 'maven-publish'
}

version = "${project.mod_version}-mc${project.minecraft_version}"
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    // Modrinth maven, used to pull MaLiLib (hard dep) and Mod Menu (soft dep).
    maven {
        name = 'Modrinth'
        url = 'https://api.modrinth.com/maven'
        content {
            includeGroup 'maven.modrinth'
        }
    }
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    // Minecraft ${project.minecraft_version} ships unobfuscated with Mojang's
    // real names built in, so there is no separate mappings artifact to pull.

    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

    // Hard dependency: config/GUI/keybind system.
    implementation "maven.modrinth:malilib:${project.malilib_version}"
    // Soft dependency: only used to hook a config button into the mod list.
    implementation "maven.modrinth:modmenu:${project.modmenu_version}"
}

processResources {
    inputs.property "version", project.mod_version

    filesMatching("fabric.mod.json") {
        expand "version": project.mod_version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 25
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

jar {
    from("../../LICENSE") {
        rename { "${it}_${project.archives_base_name}" }
    }
}
```

- [ ] **Step 4: Update `versions/26.1/src/main/resources/fabric.mod.json`**

Change:
```json
  "depends": {
    "fabricloader": ">=0.19.0",
    "minecraft": "~1.21.11",
    "java": ">=21",
```
to:
```json
  "depends": {
    "fabricloader": ">=0.19.0",
    "minecraft": "~26.1",
    "java": ">=25",
```

- [ ] **Step 5: Attempt the build**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator/versions/26.1"
JAVA_HOME=/home/exdede/.local/jdks/jdk-25.0.3+9 ./gradlew build
```

- [ ] **Step 6: Build/fix loop until it succeeds**

This is expected to fail at least once — Minecraft's internal classes get
renamed/restructured between versions independent of the obfuscation
change, same as the existing "1.21.11 mojmap renames" this project already
hit once. For each failure:

1. Read the exact compiler or Gradle error (class/method not found, plugin
   resolution failure, etc.) — do not guess at a fix from a different error.
2. If it's a Gradle plugin/dependency resolution failure (e.g. the pinned
   `1.17.13` Loom version doesn't publish the `net.fabricmc.fabric-loom` id):
   check `https://github.com/FabricMC/fabric-loom/releases` for the latest
   stable tag and try that version instead.
3. If it's a Java compile error (renamed/moved class or method): search the
   decompiled Minecraft sources Loom just downloaded
   (`versions/26.1/.gradle` / Loom's cache, or `./gradlew genSources` if
   needed) for the new name, and update the import/reference in the
   `.java` file that failed.
4. Re-run `./gradlew build`. Repeat.
5. If more than ~10 iterations pass without a successful build, stop and
   report back exactly what's failing rather than continuing to guess —
   this means something structural changed that needs a human decision,
   not another mechanical rename.

- [ ] **Step 7: Manually re-check both mixin targets after a clean compile**

A clean compile does not guarantee the mixins still attach — mixin target
resolution happens at runtime, and this project already relies on that
(`LevelSliceMixin` targets Sodium by string with `required: false`, so a
broken target fails silently). Read
`versions/26.1/src/main/java/com/bedrockobfuscator/mixin/RenderSectionRegionMixin.java`
and confirm `RenderSectionRegion` and `getBlockState(BlockPos)` still exist
with that exact shape in the 26.1 Minecraft jar (via the same decompiled
source lookup as step 6.3). Sodium's `LevelSliceMixin` cannot be verified
this way at compile time (no compile-time Sodium dependency, by design) —
note it as untested rather than silently assuming it works.

- [ ] **Step 8: Confirm the output**

```bash
ls build/libs/
```
Expected: `bedrockobfuscator-1.1.0-mc26.1.jar` and a matching `-sources.jar`.

- [ ] **Step 9: Commit**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git add -A versions/26.1
git commit -m "Port to Minecraft 26.1"
```

---

### Task 4: Port to Minecraft 26.1.1

**Files:**
- Create: `versions/26.1.1/` (copied from `versions/26.1/`, once Task 3 is done and building)
- Modify: `versions/26.1.1/gradle.properties`
- Modify: `versions/26.1.1/src/main/resources/fabric.mod.json`
- Modify (as needed): any `.java` files that don't compile against 26.1.1's API

**Interfaces:** N/A.

- [ ] **Step 1: Copy the working 26.1 folder**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
cp -r versions/26.1 versions/26.1.1
rm -rf versions/26.1.1/build versions/26.1.1/.gradle
```

- [ ] **Step 2: Update `versions/26.1.1/gradle.properties`** — only `minecraft_version` changes (Fabric API's `0.155.2+26.1.2` build already declares support for 26.1.1, confirmed live against Modrinth):

```properties
minecraft_version=26.1.1
```
(leave every other line — `loader_version`, `fabric_version`, `malilib_version`, `modmenu_version`, `mod_version`, `maven_group`, `archives_base_name` — exactly as copied from 26.1)

- [ ] **Step 3: Update `versions/26.1.1/src/main/resources/fabric.mod.json`**

Change `"minecraft": "~26.1"` to `"minecraft": "~26.1.1"`.

- [ ] **Step 4: Build**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator/versions/26.1.1"
JAVA_HOME=/home/exdede/.local/jdks/jdk-25.0.3+9 ./gradlew build
```

- [ ] **Step 5: Build/fix loop if needed**

Same procedure as Task 3, Step 6. A patch release (26.1 → 26.1.1) is far
less likely to rename engine classes than a minor release, but check
instead of assuming.

- [ ] **Step 6: Confirm the output**

```bash
ls build/libs/
```
Expected: `bedrockobfuscator-1.1.0-mc26.1.1.jar`.

- [ ] **Step 7: Commit**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git add -A versions/26.1.1
git commit -m "Port to Minecraft 26.1.1"
```

---

### Task 5: Port to Minecraft 26.1.2

**Files:**
- Create: `versions/26.1.2/` (copied from `versions/26.1.1/`)
- Modify: `versions/26.1.2/gradle.properties`
- Modify: `versions/26.1.2/src/main/resources/fabric.mod.json`
- Modify (as needed): any `.java` files that don't compile against 26.1.2's API

**Interfaces:** N/A.

- [ ] **Step 1: Copy**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
cp -r versions/26.1.1 versions/26.1.2
rm -rf versions/26.1.2/build versions/26.1.2/.gradle
```

- [ ] **Step 2: Update `versions/26.1.2/gradle.properties`**

```properties
minecraft_version=26.1.2
```
(everything else unchanged from 26.1.1 — `fabric_version=0.155.2+26.1.2` already matches this exact patch)

- [ ] **Step 3: Update `versions/26.1.2/src/main/resources/fabric.mod.json`**

Change `"minecraft": "~26.1.1"` to `"minecraft": "~26.1.2"`.

- [ ] **Step 4: Build**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator/versions/26.1.2"
JAVA_HOME=/home/exdede/.local/jdks/jdk-25.0.3+9 ./gradlew build
```

- [ ] **Step 5: Build/fix loop if needed** (same procedure as Task 3, Step 6)

- [ ] **Step 6: Confirm the output**

```bash
ls build/libs/
```
Expected: `bedrockobfuscator-1.1.0-mc26.1.2.jar`.

- [ ] **Step 7: Commit**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git add -A versions/26.1.2
git commit -m "Port to Minecraft 26.1.2"
```

---

### Task 6: Port to Minecraft 26.2

**Files:**
- Create: `versions/26.2/` (copied from `versions/26.1.2/`)
- Modify: `versions/26.2/gradle.properties`
- Modify: `versions/26.2/src/main/resources/fabric.mod.json`
- Modify (as needed): any `.java` files that don't compile against 26.2's API

**Interfaces:** N/A.

- [ ] **Step 1: Copy**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
cp -r versions/26.1.2 versions/26.2
rm -rf versions/26.2/build versions/26.2/.gradle
```

- [ ] **Step 2: Update `versions/26.2/gradle.properties`** — this is a minor version bump (26.1.x → 26.2), so `fabric_version`, `malilib_version`, and `modmenu_version` all change too, not just `minecraft_version`:

```properties
minecraft_version=26.2
fabric_version=0.155.2+26.2
malilib_version=0.29.3
modmenu_version=20.0.1
```
(`loader_version=0.19.3`, `mod_version=1.1.0`, `maven_group`, `archives_base_name` stay the same)

- [ ] **Step 3: Update `versions/26.2/src/main/resources/fabric.mod.json`**

Change `"minecraft": "~26.1.2"` to `"minecraft": "~26.2"`.

- [ ] **Step 4: Build**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator/versions/26.2"
JAVA_HOME=/home/exdede/.local/jdks/jdk-25.0.3+9 ./gradlew build
```

- [ ] **Step 5: Build/fix loop if needed** (same procedure as Task 3, Step 6 — this one is the most likely of the four to need real fixes, since it's a minor version bump, not just a patch)

- [ ] **Step 6: Manually re-check both mixin targets** (same procedure as Task 3, Step 7)

- [ ] **Step 7: Confirm the output**

```bash
ls build/libs/
```
Expected: `bedrockobfuscator-1.1.0-mc26.2.jar`.

- [ ] **Step 8: Commit**

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git add -A versions/26.2
git commit -m "Port to Minecraft 26.2"
```

---

## Final step: push

Once all six tasks are committed locally:

```bash
cd "/home/exdede/Desktop/bedrock obfuscator"
git push origin main
```

## Out of scope (per the design spec)

- In-game verification of 26.1/26.1.1/26.1.2/26.2 — no display in this
  environment, same limitation as 1.21.11 already has.
- GitHub Release / Modrinth / CurseForge uploads for the new versions —
  after the user verifies each build in-game.

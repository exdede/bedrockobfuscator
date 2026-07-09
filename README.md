# Bedrock Obfuscator

A client-side Fabric mod for Minecraft **1.21.11** that visually hides the
bottom bedrock layers of the Overworld, so viewers of your footage cannot
reverse-engineer your real world coordinates from bedrock-pattern analysis
(a known deanonymization trick used against streamers and technical players).

It is rendering only. It never touches real block data, collision, or server
state. Mining, building, and physics all work normally underneath the overlay.
Only what gets drawn on your screen changes, and only on your own screen. Other
players see nothing different.

## What it does

- Replaces every position from **Y -64 to -60** in the Overworld with a
  configurable fill block (default: bedrock) at mesh-build time.
- Leaves the real block wherever your own hitbox overlaps it, so being knocked
  into a hollowed-out layer does not leave you looking entombed.
- Leaves genuine holes alone: if the whole -64 to -60 column is air (a shaft
  straight down to the void), it stays visible instead of getting a fake floor
  painted over it.
- Off by default. Toggle it with a hotkey or in the settings screen.
- Works with the vanilla renderer and with Sodium, so it works under Lunar
  Client.

## How it works

The mod hooks the client chunk-meshing snapshot and substitutes the fill block
only while terrain geometry is being baked. It hooks both the vanilla snapshot
(`RenderSectionRegion`) and Sodium's (`LevelSlice`), since Sodium replaces the
vanilla mesher. Gameplay reads the live world, never the snapshot, so nothing
about the actual world changes.

## Requirements

- Minecraft 1.21.11 (Fabric)
- Fabric API
- [MaLiLib](https://modrinth.com/mod/malilib) (hard dependency: config and GUI)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional: adds a config button)

## Controls

- **Toggle overlay:** Left Shift + B
- **Open settings:** U + I

Both hotkeys are set in the mod's settings screen (MaLiLib) and are rebindable
there. The same two actions are also registered as vanilla keybinds (unbound by
default), so you can bind them from the vanilla Controls menu instead.

## Building

Requires a JDK 21:

```
./gradlew build
```

The built jar lands in `build/libs/`.

## License

MIT. See [LICENSE](LICENSE).

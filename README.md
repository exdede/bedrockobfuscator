# Bedrock Obfuscator

A simple client-side Fabric mod that hides the bottom bedrock layers in the Overworld.

At the very bottom of every Minecraft world there's a layer of bedrock with a
random bumpy pattern. That pattern is different in every spot, so people watching
your videos or streams can find your coords using server seed and figure out 
where you are. This mod hides that pattern so they can't.

It only changes what you see on your own screen. It doesn't touch the real world,
your collisions, or anything on the server. Everyone else sees the normal game.

## Features

- Hides bedrock from Y -64 to -60
- Pick any block to show instead (bedrock by default)
- Leaves real holes down to the void alone, so it won't cover a gap you could fall through
- Won't replace a block you're standing inside, so you never look stuck in the floor
- Turn it on and off with a hotkey

## How it works

The mod only changes the picture drawn on your screen. The real blocks never
change, so mining, walking, falling and multiplayer all work exactly like normal.
Turn the mod off and the real bedrock shows again right away.

## Requirements

- Minecraft 1.21.11
- Fabric Loader
- Fabric API
- MaLiLib
- Mod Menu (optional, adds a settings button in the mod list)

## Controls

Default hotkeys:

- Toggle on/off: Left Shift + B
- Open settings: U + I

You can change both in the mod's settings screen or in Minecraft's own Controls menu.

## Building

```bash
./gradlew build
```

The finished jar will be in `build/libs/`.

## License

MIT

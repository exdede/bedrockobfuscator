package com.bedrockobfuscator.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;

/**
 * Nudges the terrain renderer to rebuild geometry so a change shows up
 * immediately instead of whenever the chunk next happens to re-mesh. This is a
 * re-render, not a reload of anything to do with the actual world.
 *
 * <p>The bottom five bedrock layers (-64..-60) all live in a single vertical
 * chunk section, index -4.
 */
public final class RerenderHelper {

    /** Section Y for world Y -64..-60 ( -64 >> 4 == -60 >> 4 == -4 ). */
    private static final int SECTION_Y = -4;

    private RerenderHelper() {}

    /**
     * Rebuild all terrain geometry. Used for the big, infrequent changes
     * (toggle, fill-block swap, entering the Overworld). It is the blunt but
     * guaranteed option: every section that is in view gets re-meshed, so the
     * overlay always appears at once even if you are standing perfectly still.
     */
    public static void rerenderAll(Minecraft mc) {
        LevelExtractor le = mc.levelExtractor;
        if (le != null) {
            le.allChanged();
        }
    }

    /**
     * Re-mesh just the target section around a chunk, using the same path
     * vanilla uses for a block update. Used for the frequent, cheap player
     * overlap transitions so they do not trigger a full reload.
     */
    public static void rerenderAround(Minecraft mc, int chunkX, int chunkZ) {
        LevelExtractor le = mc.levelExtractor;
        if (le == null) {
            return;
        }
        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                le.setSectionDirtyWithNeighbors(x, SECTION_Y, z);
            }
        }
    }
}

package com.bedrockobfuscator.render;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Works out which block positions in the target Y range the local player's
 * hitbox currently overlaps, packed as {@link BlockPos} longs.
 *
 * <p>The player is small and the Y range is five layers, so this is always a
 * handful of positions. The array is rebuilt fresh each tick and compared to
 * the previous one to decide whether a re-render is needed.
 */
public final class OverlapTracker {

    private static final long[] EMPTY = new long[0];

    private OverlapTracker() {}

    public static long[] compute(LocalPlayer player) {
        AABB box = player.getBoundingBox();

        int minY = Math.max(RenderState.MIN_Y, Mth.floor(box.minY));
        int maxY = Math.min(RenderState.MAX_Y, Mth.floor(box.maxY));
        if (minY > maxY) {
            return EMPTY;
        }

        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);

        int count = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        long[] result = new long[count];
        int i = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    result[i++] = BlockPos.asLong(x, y, z);
                }
            }
        }
        return result;
    }
}

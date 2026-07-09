package com.bedrockobfuscator.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The tiny bundle of state the chunk-mesh mixin reads while baking terrain.
 *
 * <p>The mesh is baked on worker threads, so everything here is a plain
 * {@code volatile} field that the client thread writes and the workers read.
 * No locks, no allocation on the hot path. The fields are deliberately dumb:
 * all the "should this position be hidden" logic that needs world/config
 * context happens on the client thread once per tick and is boiled down to
 * these primitives.
 */
public final class RenderState {
    /** Bottom five bedrock layers of the Overworld, matching vanilla generation. */
    public static final int MIN_Y = -64;
    public static final int MAX_Y = -60;

    /** Master switch. When false the mixin does nothing at all. */
    public static volatile boolean enabled = false;

    /** True only while the client is currently viewing the Overworld. */
    public static volatile boolean overworld = false;

    /**
     * Set by the mixin the moment it actually processes a target block. If this
     * never flips true shortly after enabling, the client's chunk renderer is
     * not going through the vanilla mesh path we hook, and we can say so instead
     * of leaving the user staring at unchanged bedrock.
     */
    public static volatile boolean hookRan = false;

    /** The block state drawn in place of the real bottom layers. */
    public static volatile BlockState fillState = Blocks.BEDROCK.defaultBlockState();

    /**
     * Packed {@link net.minecraft.core.BlockPos} longs currently overlapped by
     * the local player's hitbox within the target Y range. Those positions are
     * drawn as their real block so the streamer is never visually entombed.
     * Swapped atomically as a whole array; never mutated in place.
     */
    public static volatile long[] overlap = new long[0];

    private RenderState() {}

    /**
     * The one decision, shared by every renderer we hook: given the real block
     * a mesh is about to draw at {@code (x, y, z)}, return either that block
     * untouched or the fill block. Ordered cheapest-check-first because this is
     * called for every block and face during meshing.
     *
     * @param target the render snapshot, used to probe the column for a void
     *               shaft; never touches the live world.
     */
    public static BlockState decide(BlockState original, int x, int y, int z, MeshTarget target) {
        if (!enabled) {
            return original;
        }
        if (y < MIN_Y || y > MAX_Y) {
            return original;
        }
        if (!overworld) {
            return original;
        }

        // We are genuinely meshing a target block through a hooked renderer.
        hookRan = true;

        // Keep the real block wherever the player is standing/wedged, so a
        // knockback into a hollowed layer never leaves them looking entombed.
        long key = BlockPos.asLong(x, y, z);
        long[] ov = overlap;
        for (int i = 0; i < ov.length; i++) {
            if (ov[i] == key) {
                return original;
            }
        }

        // A solid block here means this is not a shaft to the void, so paint it.
        // (This is the common case: most of -64..-60 is real bedrock/stone.)
        if (!original.isAir()) {
            return fillState;
        }

        // The block here is air. If the entire -64..-60 column is air, this is a
        // hole straight down to the void; leave it visible rather than paint a
        // fake floor over a very real way to fall out of the world. Otherwise it
        // is just an air pocket with a floor below, so paint it like the rest.
        if (isVoidShaft(x, z, target)) {
            return original;
        }
        return fillState;
    }

    private static boolean isVoidShaft(int x, int z, MeshTarget target) {
        // Guard against recursing back into our own hook while we probe.
        MeshProbe.PROBING.set(Boolean.TRUE);
        try {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                if (!target.bedrockobfuscator$isAirAt(x, y, z)) {
                    return false;
                }
            }
            return true;
        } finally {
            MeshProbe.PROBING.set(Boolean.FALSE);
        }
    }
}

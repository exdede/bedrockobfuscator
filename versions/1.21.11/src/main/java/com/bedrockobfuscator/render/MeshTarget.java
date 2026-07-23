package com.bedrockobfuscator.render;

/**
 * Implemented (via mixin) by every render snapshot we hook — vanilla's
 * {@code RenderSectionRegion} and Sodium's {@code LevelSlice}. It lets the
 * shared decision logic in {@link RenderState#decide} peek at the real block
 * states of a whole column without knowing which renderer it is running under.
 *
 * <p>The odd method name carries the mod prefix on purpose: it is merged into
 * third-party classes, so it must never collide with a method they already have.
 */
public interface MeshTarget {

    /** True if the real (snapshot) block at these coordinates is air. */
    boolean bedrockobfuscator$isAirAt(int x, int y, int z);
}

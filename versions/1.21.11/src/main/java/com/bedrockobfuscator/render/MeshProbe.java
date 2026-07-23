package com.bedrockobfuscator.render;

import net.minecraft.core.BlockPos;

/**
 * Per-thread scratch state used by the chunk-mesh mixin when it needs to peek
 * at a whole column of real block states (to spot a shaft straight to the
 * void). Meshing runs on several worker threads, so both fields are thread
 * local. Kept out of the mixin class so there is no static initializer to merge
 * into the target.
 */
public final class MeshProbe {

    /**
     * Set while we are probing a column's real states. It makes the mixin hand
     * back the untouched block instead of recursing into itself.
     */
    public static final ThreadLocal<Boolean> PROBING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Reused mutable position so the column probe allocates nothing. */
    public static final ThreadLocal<BlockPos.MutableBlockPos> SCRATCH =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private MeshProbe() {}
}

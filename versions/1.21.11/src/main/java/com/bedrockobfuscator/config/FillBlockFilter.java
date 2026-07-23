package com.bedrockobfuscator.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides which blocks are sensible fill candidates, and turns a stored block
 * id back into a {@link BlockState}.
 *
 * <p>The candidate list is derived from registry properties rather than a
 * hand-written list, so it stays correct as blocks come and go between
 * versions. A good fill block is a plain, opaque, full cube that draws itself
 * from a normal model with no live per-frame data.
 */
public final class FillBlockFilter {

    private FillBlockFilter() {}

    /** Resolve a stored block id to a state, falling back to bedrock. */
    public static BlockState resolve(String id) {
        if (id != null && !id.isBlank()) {
            try {
                Identifier rl = Identifier.parse(id.trim());
                Block block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
                if (block != null) {
                    return block.defaultBlockState();
                }
            } catch (Exception ignored) {
                // Bad id typed into the advanced field; fall through to bedrock.
            }
        }
        return Blocks.BEDROCK.defaultBlockState();
    }

    /** The friendly, filtered list of block ids for the picker, sorted. */
    public static List<String> getCandidateIds() {
        List<String> ids = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (isSuitable(block)) {
                ids.add(BuiltInRegistries.BLOCK.getKey(block).toString());
            }
        }
        Collections.sort(ids);
        return ids;
    }

    /**
     * True only for blocks that are a completely full, opaque, self-occluding
     * cube — the only kind that fills the layer cleanly with no gaps to see
     * through and no odd model. This rejects, all at once:
     * <ul>
     *   <li>see-through / translucent blocks (glass, ice, slime, honey, leaves)
     *       and anything with a non-cube model (flowers, torches, rails) via
     *       {@link BlockState#isSolidRender()};</li>
     *   <li>blocks that are not a full cube physically (soul sand and dirt path,
     *       which sit a notch low, plus slabs, stairs, cactus, snow layers…) via
     *       the full-collision-cube check;</li>
     *   <li>block entities (chests, shulkers, signs…) via {@link EntityBlock}.</li>
     * </ul>
     * Barrier and friends are excluded on purpose (barrier renders invisible,
     * defeating the point) but can still be forced in via the advanced text
     * field, which does not go through this filter.
     */
    public static boolean isSuitable(Block block) {
        if (block instanceof EntityBlock) {
            return false;
        }
        if (block == Blocks.BARRIER
                || block == Blocks.LIGHT
                || block == Blocks.STRUCTURE_VOID
                || block == Blocks.MOVING_PISTON) {
            return false;
        }

        BlockState state = block.defaultBlockState();
        // Full, opaque, self-occluding cube render: kills glass, slime, honey,
        // leaves, flowers, torches and every other see-through or partial model.
        if (!state.isSolidRender()) {
            return false;
        }
        // Full cube physically: kills soul sand and dirt path (they sit low),
        // slabs, stairs, cactus, snow layers and anything else not a whole block.
        if (!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return false;
        }
        return true;
    }
}

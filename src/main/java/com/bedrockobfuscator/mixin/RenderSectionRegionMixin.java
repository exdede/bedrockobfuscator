package com.bedrockobfuscator.mixin;

import com.bedrockobfuscator.render.MeshProbe;
import com.bedrockobfuscator.render.MeshTarget;
import com.bedrockobfuscator.render.RenderState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The mod, on the vanilla terrain renderer.
 *
 * <p>{@link RenderSectionRegion} is the read-only snapshot the client bakes
 * terrain geometry from on its worker threads. Collision, raycasting, block
 * breaking and every other bit of gameplay read the live {@code Level} instead,
 * never this. So swapping the block reported here changes only what gets drawn.
 *
 * <p>All the actual logic lives in {@link RenderState#decide}, shared with the
 * Sodium hook ({@code LevelSliceMixin}) so both renderers behave identically.
 */
@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin implements MeshTarget {

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState bedrockobfuscator$hideBedrock(BlockState original, BlockPos pos) {
        // We are mid-probe of a column's real states; do not touch anything.
        if (MeshProbe.PROBING.get()) {
            return original;
        }
        return RenderState.decide(original, pos.getX(), pos.getY(), pos.getZ(), this);
    }

    @Override
    public boolean bedrockobfuscator$isAirAt(int x, int y, int z) {
        BlockPos.MutableBlockPos scratch = MeshProbe.SCRATCH.get();
        return ((BlockAndTintGetter) (Object) this).getBlockState(scratch.set(x, y, z)).isAir();
    }
}

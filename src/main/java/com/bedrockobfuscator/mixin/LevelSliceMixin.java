package com.bedrockobfuscator.mixin;

import com.bedrockobfuscator.render.MeshProbe;
import com.bedrockobfuscator.render.MeshTarget;
import com.bedrockobfuscator.render.RenderState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The same hook as {@link RenderSectionRegionMixin}, but for Sodium.
 *
 * <p>Sodium replaces the vanilla terrain mesher entirely and reads blocks from
 * its own render snapshot, {@code LevelSlice}, via {@code getBlockState(int,
 * int, int)} (called per block from {@code ChunkBuilderMeshingTask}). Without
 * this the whole mod is a no-op under Sodium — which is exactly the case on
 * Lunar Client, whose renderer is Sodium-based.
 *
 * <p>Targeted by name so we need no compile-time dependency on Sodium; the
 * config that lists this mixin is gated to only apply when Sodium is present.
 * {@code remap = false} keeps the {@code getBlockState} selector literal (it is
 * Sodium's own method name, not a Minecraft-mapped one).
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class LevelSliceMixin implements MeshTarget {

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"), remap = false)
    private BlockState bedrockobfuscator$hideBedrock(BlockState original, int x, int y, int z) {
        if (MeshProbe.PROBING.get()) {
            return original;
        }
        return RenderState.decide(original, x, y, z, this);
    }

    @Override
    public boolean bedrockobfuscator$isAirAt(int x, int y, int z) {
        // Reads the real snapshot state through the BlockPos accessor, which on
        // LevelSlice is a different method than the (int,int,int) one we hook,
        // so it returns the untouched block.
        BlockPos.MutableBlockPos scratch = MeshProbe.SCRATCH.get();
        return ((BlockAndTintGetter) (Object) this).getBlockState(scratch.set(x, y, z)).isAir();
    }
}

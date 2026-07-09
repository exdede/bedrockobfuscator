package com.bedrockobfuscator;

import com.bedrockobfuscator.config.Configs;
import com.bedrockobfuscator.config.FillBlockFilter;
import com.bedrockobfuscator.gui.ConfigScreen;
import com.bedrockobfuscator.hud.HudFeedback;
import com.bedrockobfuscator.init.BedrockObfInitHandler;
import com.bedrockobfuscator.render.OverlapTracker;
import com.bedrockobfuscator.render.RenderState;
import com.bedrockobfuscator.render.RerenderHelper;
import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.Arrays;

/**
 * Client entry point. Registers the MaLiLib init handler, two vanilla
 * Controls-menu keybinds (unbound by default, a layout-friendly place to bind),
 * and the per-tick loop that keeps {@link RenderState} in sync with the config
 * and the player's position. The mod's own settings GUI carries the same two
 * actions as chord-capable hotkeys.
 */
public class BedrockObfuscatorClient implements ClientModInitializer {

    private static KeyMapping toggleKey;
    private static KeyMapping guiKey;

    private static String lastFill = null;
    private static long[] lastOverlap = new long[0];
    private static boolean overlapActive = false;
    private static int overlapMessageTimer = 0;
    private static boolean lastOverworld = false;
    private static int hookCheckTimer = 0;

    @Override
    public void onInitializeClient() {
        // MaLiLib calls registerModHandlers once it is itself initialised; that
        // is where the (chord-capable) hotkeys and config persistence get wired.
        InitializationHandler.getInstance().registerInitializationHandler(new BedrockObfInitHandler());

        // Vanilla Controls-menu keybinds, unbound by default. They mirror the two
        // GUI hotkeys and give a familiar, layout-aware place to bind a key that
        // works on non-QWERTY layouts.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.parse(Reference.MOD_ID + ":main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bedrockobfuscator.toggle",
                InputConstants.UNKNOWN.getValue(),
                category));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bedrockobfuscator.open_gui",
                InputConstants.UNKNOWN.getValue(),
                category));

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
    }

    /** Seed the worker-thread-visible state from the loaded config, at startup. */
    public static void applyConfigToRenderState() {
        RenderState.enabled = Configs.ENABLED.getBooleanValue();
        lastFill = Configs.FILL_BLOCK.getStringValue();
        RenderState.fillState = FillBlockFilter.resolve(lastFill);
    }

    private void onEndClientTick(Minecraft mc) {
        if (toggleKey != null) {
            while (toggleKey.consumeClick()) {
                Configs.ENABLED.toggleBooleanValue();
            }
        }
        if (guiKey != null) {
            while (guiKey.consumeClick()) {
                GuiBase.openGui(new ConfigScreen(null));
            }
        }

        reconcileConfig(mc);
        updateDimensionAndOverlap(mc);
    }

    /** Catch config changes from any source (hotkey, GUI, text field). */
    private void reconcileConfig(Minecraft mc) {
        boolean enabled = Configs.ENABLED.getBooleanValue();
        if (enabled != RenderState.enabled) {
            RenderState.enabled = enabled;
            RerenderHelper.rerenderAll(mc);
            HudFeedback.toggle(mc, enabled);
            Configs.INSTANCE.save();
            // Arm the render-hook self-check whenever we turn on.
            if (enabled) {
                RenderState.hookRan = false;
                hookCheckTimer = 60;
            } else {
                hookCheckTimer = 0;
            }
        }

        // If we turned on but the mixin never processed a block, tell the user
        // rather than leaving them puzzled by unchanged bedrock. We only start
        // the countdown once the player is actually down at bedrock depth in the
        // Overworld: that is the only time we can be sure the target sections are
        // being meshed around us. Toggling on up at the surface leaves the Y=-4
        // sections culled and un-remeshed, which would otherwise false-alarm.
        if (hookCheckTimer > 0 && RenderState.enabled && !RenderState.hookRan) {
            LocalPlayer probe = mc.player;
            boolean atBedrockDepth = probe != null
                    && mc.level != null
                    && mc.level.dimension() == Level.OVERWORLD
                    && probe.getY() <= RenderState.MAX_Y + 8;
            if (atBedrockDepth) {
                hookCheckTimer--;
                if (hookCheckTimer == 0) {
                    HudFeedback.hookMissing(mc);
                }
            }
        }

        String fill = Configs.FILL_BLOCK.getStringValue();
        if (lastFill == null || !lastFill.equals(fill)) {
            lastFill = fill;
            RenderState.fillState = FillBlockFilter.resolve(fill);
            if (RenderState.enabled) {
                RerenderHelper.rerenderAll(mc);
            }
            Configs.INSTANCE.save();
        }
    }

    /** Track dimension and the player-overlap exception. */
    private void updateDimensionAndOverlap(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            RenderState.overworld = false;
            lastOverworld = false;
            if (RenderState.overlap.length != 0) {
                RenderState.overlap = new long[0];
                lastOverlap = RenderState.overlap;
            }
            overlapActive = false;
            overlapMessageTimer = 0;
            return;
        }

        boolean overworld = mc.level.dimension() == Level.OVERWORLD;
        RenderState.overworld = overworld;

        // Sections meshed before we first learned the dimension (or right after
        // walking through a portal into the Overworld) were baked with the real
        // bedrock showing. Force those back through the mixin once on entry.
        if (overworld && !lastOverworld && RenderState.enabled) {
            RerenderHelper.rerenderAll(mc);
        }
        lastOverworld = overworld;

        long[] overlap = (RenderState.enabled && overworld)
                ? OverlapTracker.compute(player)
                : new long[0];

        if (!Arrays.equals(overlap, lastOverlap)) {
            lastOverlap = overlap;
            RenderState.overlap = overlap;
            RerenderHelper.rerenderAround(mc, player.chunkPosition().x, player.chunkPosition().z);
        }

        boolean nowActive = overlap.length > 0;
        if (nowActive) {
            // Keep the "this is intentional" blurb visible while it lasts by
            // refreshing it before the action bar fades.
            if (!overlapActive || overlapMessageTimer <= 0) {
                HudFeedback.overlapActive(mc);
                overlapMessageTimer = 40;
            } else {
                overlapMessageTimer--;
            }
        } else {
            overlapMessageTimer = 0;
        }
        overlapActive = nowActive;
    }
}

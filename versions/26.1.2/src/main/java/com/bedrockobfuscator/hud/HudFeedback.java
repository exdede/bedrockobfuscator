package com.bedrockobfuscator.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Status messages shown in the action-bar area (the small text above the
 * hotbar).
 */
public final class HudFeedback {

    private HudFeedback() {}

    public static void toggle(Minecraft mc, boolean on) {
        if (mc.player == null) {
            return;
        }
        String message = on
                ? "Bedrock Obfuscator: ON"
                : "Bedrock Obfuscator: OFF";
        mc.player.sendOverlayMessage(Component.literal(message));
    }

    public static void hideOresToggle(Minecraft mc, boolean on) {
        if (mc.player == null) {
            return;
        }
        String message = on ? "Hide ores: ON" : "Hide ores: OFF";
        mc.player.sendOverlayMessage(Component.literal(message));
    }

    public static void hideStoneVariantsToggle(Minecraft mc, boolean on) {
        if (mc.player == null) {
            return;
        }
        String message = on ? "Hide other underground blocks: ON" : "Hide other underground blocks: OFF";
        mc.player.sendOverlayMessage(Component.literal(message));
    }

    public static void overlapActive(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        mc.player.sendOverlayMessage(
                Component.literal("Bedrock Obfuscator: showing real blocks where you stand"));
    }

    public static void hookMissing(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        mc.player.sendOverlayMessage(Component.literal(
                "Bedrock Obfuscator is ON but the render hook never fired. Your client is likely "
                        + "using a custom chunk renderer this mod cannot paint over."));
    }
}

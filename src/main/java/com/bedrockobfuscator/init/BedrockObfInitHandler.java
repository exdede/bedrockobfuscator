package com.bedrockobfuscator.init;

import com.bedrockobfuscator.BedrockObfuscatorClient;
import com.bedrockobfuscator.Reference;
import com.bedrockobfuscator.config.Configs;
import com.bedrockobfuscator.gui.ConfigScreen;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

/**
 * Runs once MaLiLib is ready. Registers config persistence and hotkeys, wires
 * the hotkey callbacks, and seeds the render state from the loaded config.
 */
public class BedrockObfInitHandler implements IInitializationHandler {

    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, Configs.INSTANCE);
        Configs.INSTANCE.load();

        InputEventHandler.getKeybindManager().registerKeybindProvider(new BedrockObfKeybindProvider());

        // The toggle hotkey just flips the config value; the per-tick reconcile
        // in the client handles the re-render, the HUD blurb and saving.
        Configs.ENABLED.getKeybind().setCallback((action, keybind) -> {
            Configs.ENABLED.toggleBooleanValue();
            return true;
        });

        Configs.OPEN_GUI.getKeybind().setCallback((action, keybind) -> {
            GuiBase.openGui(new ConfigScreen(null));
            return true;
        });

        // Flip both underground settings together, as a pair, independent of
        // the master toggle. The per-tick reconcile handles the rest.
        Configs.TOGGLE_UNDERGROUND.getKeybind().setCallback((action, keybind) -> {
            boolean newState = !(Configs.HIDE_ORES.getBooleanValue()
                    || Configs.HIDE_STONE_VARIANTS.getBooleanValue());
            Configs.HIDE_ORES.setBooleanValue(newState);
            Configs.HIDE_STONE_VARIANTS.setBooleanValue(newState);
            return true;
        });

        BedrockObfuscatorClient.applyConfigToRenderState();

        // Write the config out once on startup so the file always exists (and
        // regenerates if deleted), rather than only appearing after the first
        // in-game change.
        Configs.INSTANCE.save();
    }
}

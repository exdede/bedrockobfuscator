package com.bedrockobfuscator.init;

import com.bedrockobfuscator.Reference;
import com.bedrockobfuscator.config.Configs;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

import java.util.List;

/**
 * Tells MaLiLib about our two chord-capable hotkeys so they get polled every
 * tick and show up on MaLiLib's own hotkey configuration list.
 */
public class BedrockObfKeybindProvider implements IKeybindProvider {

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        manager.addKeybindToMap(Configs.ENABLED.getKeybind());
        manager.addKeybindToMap(Configs.OPEN_GUI.getKeybind());
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        List<IHotkey> hotkeys = List.of(Configs.ENABLED, Configs.OPEN_GUI);
        manager.addHotkeysForCategory(Reference.MOD_ID, Reference.MOD_NAME, hotkeys);
    }
}

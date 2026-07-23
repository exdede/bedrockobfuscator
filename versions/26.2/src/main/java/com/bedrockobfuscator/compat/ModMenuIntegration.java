package com.bedrockobfuscator.compat;

import com.bedrockobfuscator.gui.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional Mod Menu hook: adds a config button to the mod list entry. Mod Menu
 * only ever calls this when it is installed, so it is safe as a soft dependency.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}

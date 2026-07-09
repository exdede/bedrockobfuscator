package com.bedrockobfuscator.gui;

import com.bedrockobfuscator.Reference;
import com.bedrockobfuscator.config.Configs;
import com.bedrockobfuscator.config.FillBlockFilter;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * The settings screen. It leans on MaLiLib's config widgets for the toggle, the
 * two rebindable hotkeys and the advanced fill-block text field, and adds one
 * custom button: the filtered fill-block picker.
 */
public class ConfigScreen extends GuiConfigsBase {

    public ConfigScreen(Screen parent) {
        super(10, 50, Reference.MOD_ID, parent, Reference.MOD_NAME);
        this.setTitle(Reference.MOD_NAME);
    }

    @Override
    public void initGui() {
        super.initGui();

        ButtonGeneric pick = new ButtonGeneric(
                12, this.height - 26, 150, 20, "Pick fill block");
        this.addButton(pick, (button, mouseButton) -> openFillBlockPicker());
    }

    private void openFillBlockPicker() {
        List<String> candidates = FillBlockFilter.getCandidateIds();
        GuiStringListSelection picker = new GuiStringListSelection(candidates, selected -> {
            if (!selected.isEmpty()) {
                Configs.FILL_BLOCK.setStringValue(selected.iterator().next());
            }
            return true;
        });
        picker.setParent(this);
        picker.setTitle("Pick a fill block (full opaque cubes only)");
        GuiBase.openGui(picker);
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(Configs.OPTIONS);
    }
}

package com.bedrockobfuscator.config;

import com.bedrockobfuscator.Reference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * All persisted settings, plus their save/load wiring.
 *
 * <p>Config option names double as their on-screen labels and their JSON keys,
 * so they are written the way a human should read them. The comments are the
 * hover tooltips and are allowed to have a personality.
 */
public class Configs implements IConfigHandler {

    public static final Configs INSTANCE = new Configs();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CATEGORY = "Bedrock Obfuscator";

    // Master switch, doubles as the "toggle overlay" hotkey. Default OFF, default
    // key is Left Shift + B (a chord, since single keys clash with everything).
    public static final ConfigBooleanHotkeyed ENABLED = new ConfigBooleanHotkeyed(
            "Hide the bedrock", false, "KEY_LEFT_SHIFT,KEY_B",
            "When ON, the bottom five bedrock layers of the Overworld are drawn as the fill block. "
                    + "Rendering only; the real world, mining and physics are unchanged.");

    // Action hotkey that opens this settings screen. Default U + I.
    public static final ConfigHotkey OPEN_GUI = new ConfigHotkey(
            "Open settings", "KEY_U,KEY_I",
            "Opens the Bedrock Obfuscator settings screen.");

    public static final ConfigString FILL_BLOCK = new ConfigString(
            "Fill block", "minecraft:bedrock",
            "The block the bottom layers are drawn as. Type any block id here (including barrier), "
                    + "or use the Pick fill block button for the filtered list.");

    // Underground hiding: a second, independent effect. Ores default ON, the
    // other blocks default OFF, both share one height range.
    public static final ConfigBoolean HIDE_ORES = new ConfigBoolean(
            "Hide ores", true,
            "When ON, ore blocks in the height range below are drawn as stone or deepslate.");

    public static final ConfigBoolean HIDE_STONE_VARIANTS = new ConfigBoolean(
            "Hide other underground blocks", false,
            "When ON, andesite, granite, diorite, tuff, gravel, and dirt in the height range "
                    + "below are drawn as stone or deepslate.");

    public static final ConfigInteger UNDERGROUND_MIN_Y = new ConfigInteger(
            "Underground hiding - minimum height", -64, -64, 320,
            "The lowest height the two settings above apply to.");

    public static final ConfigInteger UNDERGROUND_MAX_Y = new ConfigInteger(
            "Underground hiding - maximum height", 80, -64, 320,
            "The highest height the two settings above apply to.");

    // Optional pair toggle: flips HIDE_ORES and HIDE_STONE_VARIANTS together,
    // without touching the master switch above. Unbound by default.
    public static final ConfigHotkey TOGGLE_UNDERGROUND = new ConfigHotkey(
            "Toggle underground hiding", "",
            "Turns both of the underground-hiding settings above on or off together, "
                    + "without touching the main bedrock toggle.");

    public static final List<IConfigBase> OPTIONS = List.of(
            ENABLED, OPEN_GUI, FILL_BLOCK,
            HIDE_ORES, HIDE_STONE_VARIANTS, UNDERGROUND_MIN_Y, UNDERGROUND_MAX_Y, TOGGLE_UNDERGROUND);

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(Reference.MOD_ID + ".json");
    }

    @Override
    public void load() {
        Path path = configFile();
        if (!Files.isReadable(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(reader);
            if (el != null && el.isJsonObject()) {
                ConfigUtils.readConfigBase(el.getAsJsonObject(), CATEGORY, OPTIONS);
            }
        } catch (Exception e) {
            // A corrupt config should never crash the game; just keep defaults.
        }
    }

    @Override
    public void save() {
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, CATEGORY, OPTIONS);
        try {
            Path path = configFile();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Nothing sensible to do here; settings just will not persist.
        }
    }
}

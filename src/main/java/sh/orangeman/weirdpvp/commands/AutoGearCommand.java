package sh.orangeman.weirdpvp.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import sh.orangeman.weirdpvp.modules.AutoGear;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class AutoGearCommand extends Command {
    public AutoGearCommand() {
        super("gear", "Saves and loads AutoGear kits.", "gr", "kit");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("save").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            save(context.getArgument("name", String.class));
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("set").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            set(context.getArgument("name", String.class));
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("del").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            delete(context.getArgument("name", String.class));
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("list").executes(context -> {
            list();
            return SINGLE_SUCCESS;
        }));
    }

    private static File getFile() {
        return new File(mc.gameDirectory, "WeirdPvP/AutoGear.json");
    }

    private void list() {
        try {
            JsonObject completeJson = new JsonParser().parse(new FileReader(getFile())).getAsJsonObject();
            StringBuilder kits = new StringBuilder();
            for (Map.Entry<String, JsonElement> entry : completeJson.entrySet()) {
                String item = entry.getKey();
                if (!item.equals("pointer")) {
                    if (kits.length() == 0) {
                        kits.append(item);
                    } else {
                        kits.append(", ").append(item);
                    }
                }
            }
            info("Kits available: %s", kits.length() == 0 ? "(none)" : kits);
        } catch (Exception e) {
            error("No kits found.");
        }
    }

    private void delete(String name) {
        try {
            JsonObject completeJson = new JsonParser().parse(new FileReader(getFile())).getAsJsonObject();
            if (completeJson.get(name) != null && !name.equals("pointer")) {
                completeJson.remove(name);
                if (completeJson.get("pointer").getAsString().equals(name)) {
                    completeJson.addProperty("pointer", "none");
                }
                saveFile(completeJson, name, "deleted");
            } else {
                error("Kit not found.");
            }
        } catch (Exception e) {
            error("Kit not found.");
        }
    }

    private void set(String name) {
        try {
            JsonObject completeJson = new JsonParser().parse(new FileReader(getFile())).getAsJsonObject();
            if (completeJson.get(name) != null && !name.equals("pointer")) {
                completeJson.addProperty("pointer", name);
                saveFile(completeJson, name, "selected");
                AutoGear module = Modules.get().get(AutoGear.class);
                if (module.isActive()) {
                    module.reload();
                } else {
                    module.toggle();
                }
            } else {
                error("Kit not found.");
            }
        } catch (Exception e) {
            error("Kit not found.");
        }
    }

    private void save(String name) {
        JsonObject completeJson;
        try {
            completeJson = new JsonParser().parse(new FileReader(getFile())).getAsJsonObject();
            if (completeJson.get(name) != null && !name.equals("pointer")) {
                error("This kit already exists.");
                return;
            }
        } catch (Exception e) {
            completeJson = new JsonObject();
            completeJson.addProperty("pointer", "none");
        }

        StringBuilder jsonInventory = new StringBuilder();
        for (ItemStack item : mc.player.getInventory().getNonEquipmentItems()) {
            jsonInventory.append(getItemKey(item)).append(" ");
        }

        completeJson.addProperty(name, jsonInventory.toString());
        saveFile(completeJson, name, "saved");
    }

    private void saveFile(JsonObject completeJson, String name, String operation) {
        try {
            File file = getFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file);
            writer.write(completeJson.toString());
            writer.close();
            info("Kit %s %s.", name, operation);
        } catch (IOException e) {
            error("Error saving the file.");
        }
    }

    public static String getCurrentSet() {
        try {
            JsonObject completeJson = new JsonParser().parse(new FileReader(getFile())).getAsJsonObject();
            String pointer = completeJson.get("pointer").getAsString();
            if (!pointer.equals("none")) {
                return pointer;
            }
        } catch (Exception e) {
            // no config yet
        }
        return "";
    }

    public static String getInventoryKit(String kit) {
        try {
            JsonObject completeJson = new JsonParser().parse(new FileReader(getFile())).getAsJsonObject();
            return completeJson.get(kit).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getItemKey(ItemStack item) {
        if (item.isEmpty()) {
            return "minecraft:air0";
        }
        // Include the stack count so overstacked stacks (e.g. 127) are distinct from
        // a normal stack of the same item. 1.21 changed how overstacked stacks behave,
        // so the sort must not treat a 127-stack as identical to a 64-stack.
        return BuiltInRegistries.ITEM.getKey(item.getItem()).toString() + item.getDamageValue() + ":" + item.getCount();
    }
}
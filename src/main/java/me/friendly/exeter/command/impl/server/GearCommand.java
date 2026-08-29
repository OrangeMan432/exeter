package me.friendly.exeter.command.impl.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.misc.AutoGear;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class GearCommand extends Command {
    public GearCommand() {
        super(new String[]{"gear", "kit"}, new Argument("sub"), new Argument("name"));
    }

    @Override
    public String dispatch(String[] input) {
        if (input.length < 2) {
            return getSyntax();
        }

        String sub = input[1].toLowerCase();

        switch (sub) {
            case "save": {
                if (input.length < 3) return "Usage: .gear save &e[name]";
                String name = joinArgs(input, 2);
                return save(name);
            }
            case "set": {
                if (input.length < 3) return "Usage: .gear set &e[name]";
                String name = joinArgs(input, 2);
                return set(name);
            }
            case "del":
            case "delete": {
                if (input.length < 3) return "Usage: .gear del &e[name]";
                String name = joinArgs(input, 2);
                return delete(name);
            }
            case "list": {
                return list();
            }
            default:
                return getSyntax();
        }
    }

    @Override
    public String dispatch() {
        return getSyntax();
    }

    private String joinArgs(String[] input, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < input.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(input[i]);
        }
        return sb.toString();
    }

    private String save(String name) {
        JsonObject completeJson;
        try {
            completeJson = JsonParser.parseReader(new FileReader(AutoGear.getFile())).getAsJsonObject();
            if (completeJson.get(name) != null) {
                return "Kit &e" + name + "&7 already exists.";
            }
        } catch (Exception e) {
            completeJson = new JsonObject();
            completeJson.addProperty("pointer", "none");
        }

        StringBuilder jsonInventory = new StringBuilder();
        for (ItemStack item : minecraft.player.getInventory().getNonEquipmentItems()) {
            jsonInventory.append(AutoGear.getItemKey(item)).append(" ");
        }

        completeJson.addProperty(name, jsonInventory.toString());
        AutoGear.writeJson(completeJson);
        return "Kit &e" + name + "&7 saved.";
    }

    private String set(String name) {
        try {
            JsonObject completeJson = JsonParser.parseReader(new FileReader(AutoGear.getFile())).getAsJsonObject();
            if (completeJson.get(name) == null) {
                return "Kit &e" + name + "&7 not found.";
            }
            completeJson.addProperty("pointer", name);
            AutoGear.writeJson(completeJson);

            AutoGear module = (AutoGear) Exeter.getInstance().getModuleManager().getModuleByAlias("autogear");
            if (module != null) {
                if (module.isRunning()) {
                    module.reload();
                } else {
                    module.toggle();
                }
            }
            return "Kit &e" + name + "&7 selected.";
        } catch (Exception e) {
            return "Kit &e" + name + "&7 not found.";
        }
    }

    private String delete(String name) {
        try {
            JsonObject completeJson = JsonParser.parseReader(new FileReader(AutoGear.getFile())).getAsJsonObject();
            if (completeJson.get(name) == null) {
                return "Kit &e" + name + "&7 not found.";
            }
            completeJson.remove(name);
            if (completeJson.has("pointer") && completeJson.get("pointer").getAsString().equals(name)) {
                completeJson.addProperty("pointer", "none");
            }
            AutoGear.writeJson(completeJson);
            return "Kit &e" + name + "&7 deleted.";
        } catch (Exception e) {
            return "Kit &e" + name + "&7 not found.";
        }
    }

    private String list() {
        try {
            JsonObject completeJson = JsonParser.parseReader(new FileReader(AutoGear.getFile())).getAsJsonObject();
            StringBuilder kits = new StringBuilder();
            for (Map.Entry<String, JsonElement> entry : completeJson.entrySet()) {
                String item = entry.getKey();
                if (!item.equals("pointer")) {
                    if (kits.length() == 0) {
                        kits.append("&e").append(item);
                    } else {
                        kits.append("&7, &e").append(item);
                    }
                }
            }
            return kits.length() == 0 ? "No kits found." : "Kits: " + kits;
        } catch (Exception e) {
            return "No kits found.";
        }
    }

    public static String getCurrentSet() {
        try {
            JsonObject completeJson = JsonParser.parseReader(new FileReader(AutoGear.getFile())).getAsJsonObject();
            String pointer = completeJson.get("pointer").getAsString();
            if (!pointer.equals("none")) {
                return pointer;
            }
        } catch (Exception e) {
        }
        return "";
    }

    public static String getInventoryKit(String kit) {
        try {
            JsonObject completeJson = JsonParser.parseReader(new FileReader(AutoGear.getFile())).getAsJsonObject();
            return completeJson.get(kit).getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}

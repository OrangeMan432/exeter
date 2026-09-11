package me.larp.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;

/**
 * Raven-style profiles: named snapshots of the whole config directory plus a
 * curated 5b5t setup applied in code.
 */
public final class ProfileManager {

  private ProfileManager() {}

  public static Path profilesDir() {
    return LarpConfig.getInstance().getConfigDir().resolve("profiles");
  }

  public static List<String> list() {
    List<String> names = new ArrayList<>();
    try {
      Files.createDirectories(profilesDir());
      try (Stream<Path> stream = Files.list(profilesDir())) {
        stream.filter(Files::isDirectory).forEach(p -> names.add(p.getFileName().toString()));
      }
    } catch (IOException e) {
      System.err.println("[Larp] Failed to list profiles: " + e.getMessage());
    }
    names.sort(String::compareToIgnoreCase);
    return names;
  }

  public static boolean save(String name) {
    if (!validName(name)) return false;
    try {
      LarpConfig.getInstance().saveAll();
      Path dest = profilesDir().resolve(name);
      deleteDir(dest);
      Files.createDirectories(dest);
      copyJson(LarpConfig.getInstance().getConfigDir(), dest);
      return true;
    } catch (IOException e) {
      System.err.println("[Larp] Failed to save profile: " + e.getMessage());
      return false;
    }
  }

  public static boolean load(String name) {
    if (!validName(name)) return false;
    Path src = profilesDir().resolve(name);
    if (!Files.isDirectory(src)) return false;
    try {
      copyJson(src, LarpConfig.getInstance().getConfigDir());
      LarpConfig.getInstance().loadAll();
      return true;
    } catch (IOException e) {
      System.err.println("[Larp] Failed to load profile: " + e.getMessage());
      return false;
    }
  }

  public static boolean delete(String name) {
    if (!validName(name) || name.equalsIgnoreCase("5b5t")) return false;
    try {
      deleteDir(profilesDir().resolve(name));
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /** Curated 5b5t setup: safe reaches, full-charge hits, survival core on. */
  public static void apply5b5t() {
    enable("Surround");
    enable("KillAura");
    enable("CrystalAura");
    enable("AutoTotem");
    enable("AutoArmor");
    enable("AutoEat");
    enable("Velocity");
    enable("Criticals");
    enable("NoFall");
    enable("Sprint");
    enable("Hud");
    // KillAura: legit reach, full charge, single target, tight walls.
    set("KillAura", "Range", 4.5);
    set("KillAura", "Cooldown", 1.0);
    set("KillAura", "Delay", 1);
    set("KillAura", "Multi", 1);
    set("KillAura", "Walls Range", 3.5);
    set("KillAura", "Rotate", true);
    // CrystalAura: patient placements, gated damage, single burst/break.
    set("CrystalAura", "Place Delay", 4);
    set("CrystalAura", "Break Delay", 2);
    set("CrystalAura", "Min Damage", 6.0);
    set("CrystalAura", "Max Self", 8.0);
    set("CrystalAura", "Burst Packets", 1);
    set("CrystalAura", "Max Breaks", 1);
    set("CrystalAura", "Exist Ticks", 2);
    set("CrystalAura", "Support", true);
    // Velocity: full cancel + explosions.
    set("Velocity", "Horizontal", 0.0);
    set("Velocity", "Vertical", 0.0);
    set("Velocity", "Explosions", true);
    // Survival core.
    set("AutoTotem", "Min Health", 14.0);
    set("AutoEat", "Hunger", 16);
    set("AutoEat", "Health", 14.0);
    LarpConfig.getInstance().saveAll();
  }

  private static void enable(String label) {
    Module module = Larp.getInstance().getModuleManager().getModuleByAlias(label);
    if (module instanceof ToggleableModule toggleable && !toggleable.isRunning()) {
      toggleable.setRunning(true);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void set(String moduleLabel, String propertyAlias, Object value) {
    Module module = Larp.getInstance().getModuleManager().getModuleByAlias(moduleLabel);
    if (module == null) return;
    for (Property<?> property : module.getProperties()) {
      if (!property.getAliases()[0].equalsIgnoreCase(propertyAlias)) continue;
      try {
        if (property instanceof EnumProperty) {
          ((EnumProperty) property).setValue(value.toString());
        } else if (value instanceof Boolean && property.getValue() instanceof Boolean) {
          ((Property<Boolean>) property).setValue((Boolean) value);
        } else if (value instanceof Number && property instanceof NumberProperty) {
          Object current = property.getValue();
          if (current instanceof Integer) {
            ((NumberProperty) property).setValue(((Number) value).intValue());
          } else if (current instanceof Double) {
            ((NumberProperty) property).setValue(((Number) value).doubleValue());
          } else if (current instanceof Float) {
            ((NumberProperty) property).setValue(((Number) value).floatValue());
          } else if (current instanceof Long) {
            ((NumberProperty) property).setValue(((Number) value).longValue());
          }
        }
      } catch (Exception e) {
        System.err.println("[Larp] 5b5t preset skipped " + moduleLabel + "." + propertyAlias);
      }
      return;
    }
  }

  private static boolean validName(String name) {
    return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
  }

  private static void copyJson(Path from, Path to) throws IOException {
    try (Stream<Path> stream = Files.list(from)) {
      for (Path file : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
        Files.copy(file, to.resolve(file.getFileName()));
      }
    }
  }

  private static void deleteDir(Path dir) throws IOException {
    if (!Files.exists(dir)) return;
    try (Stream<Path> stream = Files.walk(dir)) {
      for (Path p : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
        Files.deleteIfExists(p);
      }
    }
  }
}

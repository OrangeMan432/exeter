package me.friendly.exeter.plugin;

/** Display metadata read from exeter-plugin.json inside a plugin jar. */
public record PluginInfo(String name, String author, String version) {
  public static PluginInfo unknown(String name) {
    return new PluginInfo(name, "?", "?");
  }
}

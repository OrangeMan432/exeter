package me.friendly.exeter.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The community plugin list (name, author, repo, version, updated). Entries point at GitHub repos;
 * jars come from their releases.
 */
public final class PluginList {
  /** Raw plugins.json in the plugin-list repo. */
  public static final String LIST_URL =
      "https://raw.githubusercontent.com/OrangeMan432/exeter-plugin-list/main/plugins.json";

  public record Entry(String name, String author, String repo, String version, String updated) {}

  private PluginList() {}

  /** Fetches and parses the list. Runs on the caller's thread; call off-thread. */
  public static List<Entry> fetch(String url) throws Exception {
    List<Entry> entries = new ArrayList<>();
    try (Reader reader = new InputStreamReader(Github.open(url), StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!(root instanceof JsonArray array)) {
        return entries;
      }
      for (JsonElement node : array) {
        if (!(node instanceof JsonObject entry)) {
          continue;
        }
        entries.add(
            new Entry(
                string(entry, "name"),
                string(entry, "author"),
                string(entry, "repo"),
                string(entry, "version"),
                string(entry, "updated")));
      }
    }
    return entries;
  }

  public static List<Entry> fetch() throws Exception {
    return fetch(LIST_URL);
  }

  private static String string(JsonObject entry, String key) {
    return entry.has(key) ? entry.get(key).getAsString() : "";
  }
}

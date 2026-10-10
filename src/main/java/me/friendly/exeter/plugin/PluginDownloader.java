package me.friendly.exeter.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

/** Downloads plugin jars from GitHub releases into the plugins folder. */
public final class PluginDownloader {
  private PluginDownloader() {}

  /**
   * Resolves the latest release jar and downloads it, all on the caller's thread. Reports back
   * through result, which runs on the same background thread.
   */
  public static void install(
      PluginList.Entry entry, File pluginsDir, Consumer<Optional<File>> result) {
    try {
      String assetUrl = latestJarUrl(entry.repo());
      if (assetUrl == null) {
        result.accept(Optional.empty());
        return;
      }
      String fileName = assetUrl.substring(assetUrl.lastIndexOf('/') + 1);
      File out = new File(pluginsDir, fileName);
      download(assetUrl, out);
      result.accept(Optional.of(out));
    } catch (Exception e) {
      e.printStackTrace();
      result.accept(Optional.empty());
    }
  }

  /** First .jar asset of the latest release that is not a sources jar. */
  private static String latestJarUrl(String repo) throws Exception {
    String api = "https://api.github.com/repos/" + repo + "/releases/latest";
    try (Reader reader =
        new InputStreamReader(URI.create(api).toURL().openStream(), StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!(root instanceof JsonObject release)) {
        return null;
      }
      if (!release.has("assets") || !(release.get("assets") instanceof JsonArray assets)) {
        return null;
      }
      String fallback = null;
      for (JsonElement node : assets) {
        if (!(node instanceof JsonObject asset) || !asset.has("browser_download_url")) {
          continue;
        }
        String url = asset.get("browser_download_url").getAsString();
        if (!url.endsWith(".jar")) {
          continue;
        }
        if (url.contains("sources")) {
          if (fallback == null) fallback = url;
          continue;
        }
        return url;
      }
      return fallback;
    }
  }

  private static void download(String url, File out) throws Exception {
    if (out.getParentFile() != null) {
      out.getParentFile().mkdirs();
    }
    try (InputStream in = URI.create(url).toURL().openStream();
        OutputStream fileOut = new FileOutputStream(out)) {
      in.transferTo(fileOut);
    }
  }
}

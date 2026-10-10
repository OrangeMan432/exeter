package me.friendly.exeter.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** GitHub HTTP with the headers the API demands; bare openStream() gets 403s. */
public final class Github {
  private Github() {}

  public static InputStream open(String url) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
    connection.setRequestProperty("User-Agent", "Exeter-Client");
    connection.setRequestProperty("Accept", "application/vnd.github+json");
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(30000);
    int status = connection.getResponseCode();
    if (status < 200 || status >= 300) {
      String body = "";
      try (InputStream err = connection.getErrorStream()) {
        if (err != null) {
          body = new String(err.readAllBytes(), StandardCharsets.UTF_8);
        }
      } catch (IOException ignored) {
      }
      throw new IOException("HTTP " + status + " from " + url + ": " + body);
    }
    return connection.getInputStream();
  }
}

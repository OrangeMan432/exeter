package me.friendly.exeter.proxy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.core.Exeter;

/**
 * Manages SOCKS5/HTTP proxies for all client traffic: game connections go through a
 * Netty proxy handler (see MixinProxyInitializer), and JDK HTTP clients (account
 * manager auth) use a proxy selector. Passwords are stored in plaintext in
 * proxies.json like the rest of the client config.
 */
public final class ProxyManager extends ListRegistry<ProxyEntry> {

  public enum PingState {
    UNTESTED,
    TESTING,
    OK,
    FAIL
  }

  public static final class PingResult {
    public final PingState state;
    public final long ms;
    public final long checkedAt;

    PingResult(PingState state, long ms, long checkedAt) {
      this.state = state;
      this.ms = ms;
      this.checkedAt = checkedAt;
    }
  }

  private static final long PING_INTERVAL_MS = 30000L;
  private static final int PING_TIMEOUT_MS = 5000;

  private final java.util.concurrent.ExecutorService pingPool =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "Exeter-ProxyPing");
            thread.setDaemon(true);
            return thread;
          });
  private final java.util.Map<String, PingResult> pings = new java.util.HashMap<>();

  private final Config config;
  private boolean enabled;
  private String activeName = "";

  public ProxyManager() {
    this.registry = new ArrayList();
    this.config =
        new Config("proxies.json") {
          @Override
          public void load(Object... source) {
            loadProxies();
          }

          @Override
          public void save(Object... destination) {
            saveProxies();
          }
        };
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    applyAuthenticator();
  }

  public ProxyEntry getActive() {
    if (!enabled) {
      return null;
    }
    for (ProxyEntry entry : getRegistry()) {
      if (entry.getName().equalsIgnoreCase(activeName)) {
        return entry;
      }
    }
    return null;
  }

  public void setActive(String name) {
    this.activeName = name == null ? "" : name;
    applyAuthenticator();
  }

  public String getActiveName() {
    return activeName;
  }

  /** Cached ping, refreshing in the background when stale. Never blocks. */
  public PingResult ping(ProxyEntry entry) {
    long now = System.currentTimeMillis();
    PingResult cached = pings.get(entry.getName());
    if (cached == null || now - cached.checkedAt > PING_INTERVAL_MS) {
      if (cached == null || cached.state != PingState.TESTING) {
        pings.put(entry.getName(), new PingResult(PingState.TESTING, -1, now));
        pingPool.submit(() -> test(entry));
      }
      return cached == null
          ? new PingResult(PingState.TESTING, -1, now)
          : cached;
    }
    return cached;
  }

  private void test(ProxyEntry entry) {
    long start = System.currentTimeMillis();
    try (java.net.Socket socket = new java.net.Socket()) {
      socket.connect(entry.address(), PING_TIMEOUT_MS);
      pings.put(
          entry.getName(),
          new PingResult(PingState.OK, System.currentTimeMillis() - start, start));
    } catch (Exception e) {
      pings.put(entry.getName(), new PingResult(PingState.FAIL, -1, start));
    }
  }

  private void loadProxies() {
    File file = configFile();
    if (!file.exists()) {
      return;
    }
    try (FileReader reader = new FileReader(file)) {
      JsonElement root = new JsonParser().parse(reader);
      if (!(root instanceof JsonObject)) {
        return;
      }
      JsonObject object = (JsonObject) root;
      enabled = object.has("enabled") && object.get("enabled").getAsBoolean();
      if (object.has("active") && object.get("active").isJsonPrimitive()) {
        activeName = object.get("active").getAsString();
      }
      if (object.has("proxies") && object.get("proxies").isJsonArray()) {
        JsonArray array = object.getAsJsonArray("proxies");
        for (int i = 0; i < array.size(); i++) {
          if (!array.get(i).isJsonObject()) continue;
          JsonObject entry = array.get(i).getAsJsonObject();
          String name = stringOr(entry, "name", "");
          String host = stringOr(entry, "host", "");
          int port = entry.has("port") ? entry.get("port").getAsInt() : 0;
          ProxyEntry.Type type = ProxyEntry.Type.SOCKS5;
          if (entry.has("type")) {
            try {
              type = ProxyEntry.Type.valueOf(entry.get("type").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
          }
          if (name.isEmpty() || host.isEmpty() || port <= 0) continue;
          getRegistry()
              .add(
                  new ProxyEntry(
                      name,
                      host,
                      port,
                      type,
                      stringOr(entry, "username", ""),
                      stringOr(entry, "password", "")));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    applyAuthenticator();
  }

  private static String stringOr(JsonObject object, String key, String fallback) {
    if (object.has(key) && object.get(key).isJsonPrimitive()) {
      return object.get(key).getAsString();
    }
    return fallback;
  }

  private File configFile() {
    return config.getFile();
  }

  public void saveProxies() {
    JsonObject object = new JsonObject();
    object.addProperty("enabled", enabled);
    object.addProperty("active", activeName);
    JsonArray array = new JsonArray();
    for (ProxyEntry entry : getRegistry()) {
      JsonObject e = new JsonObject();
      e.addProperty("name", entry.getName());
      e.addProperty("host", entry.getHost());
      e.addProperty("port", entry.getPort());
      e.addProperty("type", entry.getType().name());
      e.addProperty("username", entry.getUsername());
      e.addProperty("password", entry.getPassword());
      array.add(e);
    }
    object.add("proxies", array);
    File file = configFile();
    try {
      if (file.getParentFile() != null) {
        file.getParentFile().mkdirs();
      }
      try (FileWriter writer = new FileWriter(file)) {
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(object));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void applyAuthenticator() {
    ProxyEntry active = getActive();
    if (active != null && active.hasAuth()) {
      String user = active.getUsername();
      char[] pass = active.getPassword().toCharArray();
      String host = active.getHost();
      Authenticator.setDefault(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              if (getRequestingHost() != null
                  && getRequestingHost().equalsIgnoreCase(host)) {
                return new PasswordAuthentication(user, pass);
              }
              return null;
            }
          });
    }
  }

  /** JDK HTTP client honoring the active proxy, for account manager auth traffic. */
  public static HttpClient httpClient() {
    HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30));
    ProxyEntry active = null;
    if (Exeter.getInstance() != null) {
      active = Exeter.getInstance().getProxyManager().getActive();
    }
    if (active != null) {
      InetSocketAddress address = active.address();
      if (active.getType() == ProxyEntry.Type.HTTP) {
        builder.proxy(ProxySelector.of(address));
      } else {
        builder.proxy(
            new ProxySelector() {
              private final List<Proxy> proxies =
                  java.util.Collections.singletonList(new Proxy(Proxy.Type.SOCKS, address));

              @Override
              public List<Proxy> select(URI uri) {
                return proxies;
              }

              @Override
              public void connectFailed(URI uri, java.net.SocketAddress sa, IOException ioe) {}
            });
      }
    }
    return builder.build();
  }
}

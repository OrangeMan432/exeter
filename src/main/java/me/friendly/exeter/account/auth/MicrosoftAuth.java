package me.friendly.exeter.account.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Microsoft/Xbox/Minecraft authentication chain.
 *
 * <p>Ported from OpenMyau's {@code me.ksyz.accountmanager.auth.MicrosoftAuth} (derived from
 * https://github.com/ksyzov/AccountManager, originally LGPL, modified version GPL v3; itself based
 * on Auth Me by axieum). The Apache HttpClient dependency was replaced with the JDK's {@link
 * HttpClient}, and the custom {@code ssl.jks} trust store was dropped in favor of the JVM defaults,
 * which are complete on modern Java.
 */
public final class MicrosoftAuth {
  public static String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
  public static String SCOPE = "XboxLive.signin XboxLive.offline_access";

  /** Token-exchange identity used by the "Add Token" flow. */
  public static final String TOKEN_CLIENT_ID = "00000000402b5328";

  public static final String TOKEN_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

  // 25565 + 10
  private static final int PORT = 25575;

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  private MicrosoftAuth() {}

  /** A Minecraft profile resolved from a Mojang access token. */
  public record MinecraftProfile(String username, UUID uuid) {}

  public static URI getMSAuthLink(String state) {
    try {
      String query =
          "client_id="
              + encode(CLIENT_ID)
              + "&response_type=code"
              + "&redirect_uri="
              + encode("http://localhost:" + PORT + "/callback")
              + "&scope="
              + encode(SCOPE)
              + "&state="
              + encode(state)
              + "&prompt=select_account";
      return new URI("https://login.live.com/oauth20_authorize.srf?" + query);
    } catch (Exception e) {
      return null;
    }
  }

  public static CompletableFuture<String> acquireMSAuthCode(String state, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> authCode = new AtomicReference<>(null);
            AtomicReference<String> errorMsg = new AtomicReference<>(null);

            server.createContext(
                "/callback",
                exchange -> {
                  try {
                    Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                    if (!state.equals(query.get("state"))) {
                      errorMsg.set(
                          String.format(
                              "State mismatch! Expected '%s' but got '%s'.",
                              state, query.get("state")));
                    } else if (query.containsKey("code")) {
                      authCode.set(query.get("code"));
                    } else if (query.containsKey("error")) {
                      errorMsg.set(
                          String.format(
                              "%s: %s", query.get("error"), query.get("error_description")));
                    }
                    byte[] response =
                        "<html><body><h1>You may now close this page.</h1></body></html>"
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, response.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                      out.write(response);
                    }
                  } finally {
                    latch.countDown();
                  }
                });

            try {
              server.start();
              latch.await();
              String code = authCode.get();
              if (code != null && !code.isBlank()) {
                return code;
              }
              throw new Exception(
                  Optional.ofNullable(errorMsg.get())
                      .orElse("There was no auth code or error description present."));
            } finally {
              server.stop(2);
            }
          } catch (InterruptedException e) {
            throw new CancellationException("Microsoft auth code acquisition was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to acquire Microsoft auth code!", e);
          }
        },
        executor);
  }

  public static CompletableFuture<Map<String, String>> acquireMSAccessTokens(
      String authCode, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Map<String, String> form = new HashMap<>();
            form.put("client_id", CLIENT_ID);
            form.put("grant_type", "authorization_code");
            form.put("code", authCode);
            form.put("redirect_uri", "http://localhost:" + PORT + "/callback");
            JsonObject json = postForm("https://login.live.com/oauth20_token.srf", form);
            return extractTokens(json);
          } catch (InterruptedException e) {
            throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
          }
        },
        executor);
  }

  public static CompletableFuture<Map<String, String>> refreshMSAccessTokens(
      String refreshToken, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Map<String, String> form = new HashMap<>();
            form.put("client_id", CLIENT_ID);
            form.put("grant_type", "refresh_token");
            form.put("refresh_token", refreshToken);
            if (CLIENT_ID.equals(TOKEN_CLIENT_ID)) {
              form.put("scope", SCOPE);
            } else {
              form.put("redirect_uri", "http://localhost:" + PORT + "/callback");
            }
            JsonObject json = postForm("https://login.live.com/oauth20_token.srf", form);
            return extractTokens(json);
          } catch (InterruptedException e) {
            throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
          }
        },
        executor);
  }

  public static CompletableFuture<String> acquireXboxAccessToken(
      String accessToken, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            JsonObject properties = new JsonObject();
            properties.addProperty("AuthMethod", "RPS");
            properties.addProperty("SiteName", "user.auth.xboxlive.com");
            properties.addProperty(
                "RpsTicket", CLIENT_ID.equals(TOKEN_CLIENT_ID) ? accessToken : "d=" + accessToken);
            JsonObject entity = new JsonObject();
            entity.add("Properties", properties);
            entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
            entity.addProperty("TokenType", "JWT");
            JsonObject json =
                postJson("https://user.auth.xboxlive.com/user/authenticate", entity.toString());
            return Optional.ofNullable(json.get("Token"))
                .map(JsonElement::getAsString)
                .filter(token -> !token.isBlank())
                .orElseThrow(
                    () ->
                        new Exception(
                            json.has("XErr")
                                ? json.get("XErr").getAsString()
                                    + ": "
                                    + json.get("Message").getAsString()
                                : "There was no access token or error description present."));
          } catch (InterruptedException e) {
            throw new CancellationException("Xbox Live access token acquisition was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to acquire Xbox Live access token!", e);
          }
        },
        executor);
  }

  public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(
      String accessToken, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            JsonArray userTokens = new JsonArray();
            userTokens.add(accessToken);
            JsonObject properties = new JsonObject();
            properties.addProperty("SandboxId", "RETAIL");
            properties.add("UserTokens", userTokens);
            JsonObject entity = new JsonObject();
            entity.add("Properties", properties);
            entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            entity.addProperty("TokenType", "JWT");
            JsonObject json =
                postJson("https://xsts.auth.xboxlive.com/xsts/authorize", entity.toString());
            String token =
                Optional.ofNullable(json.get("Token"))
                    .map(JsonElement::getAsString)
                    .filter(value -> !value.isBlank())
                    .orElseThrow(
                        () ->
                            new Exception(
                                json.has("XErr")
                                    ? json.get("XErr").getAsString()
                                        + ": "
                                        + json.get("Message").getAsString()
                                    : "There was no access token or error description present."));
            String uhs =
                json.get("DisplayClaims")
                    .getAsJsonObject()
                    .get("xui")
                    .getAsJsonArray()
                    .get(0)
                    .getAsJsonObject()
                    .get("uhs")
                    .getAsString();
            Map<String, String> result = new HashMap<>();
            result.put("Token", token);
            result.put("uhs", uhs);
            return result;
          } catch (InterruptedException e) {
            throw new CancellationException("Xbox Live XSTS token acquisition was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
          }
        },
        executor);
  }

  public static CompletableFuture<String> acquireMCAccessToken(
      String xstsToken, String userHash, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            String body = "{\"identityToken\": \"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}";
            JsonObject json =
                postJson("https://api.minecraftservices.com/authentication/login_with_xbox", body);
            return Optional.ofNullable(json.get("access_token"))
                .map(JsonElement::getAsString)
                .filter(token -> !token.isBlank())
                .orElseThrow(
                    () ->
                        new Exception(
                            json.has("error")
                                ? json.get("error").getAsString()
                                    + ": "
                                    + json.get("errorMessage").getAsString()
                                : "There was no access token or error description present."));
          } catch (InterruptedException e) {
            throw new CancellationException("Minecraft access token acquisition was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to acquire Minecraft access token!", e);
          }
        },
        executor);
  }

  public static CompletableFuture<MinecraftProfile> fetchProfile(
      String mcToken, Executor executor) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + mcToken)
                    .GET()
                    .build();
            HttpResponse<String> response =
                HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = new JsonParser().parse(response.body()).getAsJsonObject();
            String id =
                Optional.ofNullable(json.get("id"))
                    .map(JsonElement::getAsString)
                    .filter(value -> !value.isBlank())
                    .orElseThrow(
                        () ->
                            new Exception(
                                json.has("error")
                                    ? json.get("error").getAsString()
                                        + ": "
                                        + json.get("errorMessage").getAsString()
                                    : "There was no profile or error description present."));
            String name = json.get("name").getAsString();
            return new MinecraftProfile(name, parseUndashedUuid(id));
          } catch (InterruptedException e) {
            throw new CancellationException("Minecraft profile fetching was cancelled!");
          } catch (Exception e) {
            throw new CompletionException("Unable to fetch Minecraft profile!", e);
          }
        },
        executor);
  }

  private static Map<String, String> extractTokens(JsonObject json) throws Exception {
    String accessToken =
        Optional.ofNullable(json.get("access_token"))
            .map(JsonElement::getAsString)
            .filter(token -> !token.isBlank())
            .orElseThrow(
                () ->
                    new Exception(
                        json.has("error")
                            ? json.get("error").getAsString()
                                + ": "
                                + json.get("error_description").getAsString()
                            : "There was no Microsoft access token or error description present."));
    String refreshToken =
        Optional.ofNullable(json.get("refresh_token"))
            .map(JsonElement::getAsString)
            .filter(token -> !token.isBlank())
            .orElseThrow(
                () ->
                    new Exception(
                        json.has("error")
                            ? json.get("error").getAsString()
                                + ": "
                                + json.get("error_description").getAsString()
                            : "There was no Microsoft refresh token or error description"
                                + " present."));
    Map<String, String> result = new HashMap<>();
    result.put("access_token", accessToken);
    result.put("refresh_token", refreshToken);
    return result;
  }

  private static JsonObject postForm(String url, Map<String, String> form)
      throws IOException, InterruptedException {
    String body =
        form.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .collect(Collectors.joining("&"));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    return new JsonParser().parse(response.body()).getAsJsonObject();
  }

  private static JsonObject postJson(String url, String body)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    return new JsonParser().parse(response.body()).getAsJsonObject();
  }

  private static Map<String, String> parseQuery(String rawQuery) {
    Map<String, String> result = new HashMap<>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return result;
    }
    for (String pair : rawQuery.split("&")) {
      int index = pair.indexOf('=');
      if (index < 0) {
        continue;
      }
      result.put(decode(pair.substring(0, index)), decode(pair.substring(index + 1)));
    }
    return result;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String decode(String value) {
    try {
      return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return value;
    }
  }

  /** Mojang profile ids are undashed hex; insert dashes to build a {@link UUID}. */
  public static UUID parseUndashedUuid(String id) {
    String dashed =
        id.substring(0, 8)
            + "-"
            + id.substring(8, 12)
            + "-"
            + id.substring(12, 16)
            + "-"
            + id.substring(16, 20)
            + "-"
            + id.substring(20);
    return UUID.fromString(dashed);
  }
}

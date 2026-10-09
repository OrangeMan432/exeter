package me.friendly.exeter.command.impl.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.proxy.ProxyManager;
import net.minecraft.client.Screenshot;

public final class ScreenShot extends Command {
  private static final String API_URL = "https://catbox.moe/user/api.php";
  private static final DateTimeFormatter NAME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss-SSS");

  public ScreenShot() {
    super(new String[] {"screenshot"}, new Argument[0]);
    setDescription("Take a screenshot and upload it to catbox.moe");
  }

  @Override
  public String dispatch() {
    if (minecraft.gameRenderer == null) return "Renderer not ready.";
    Screenshot.takeScreenshot(
        minecraft.gameRenderer.mainRenderTarget(),
        image -> {
          Thread thread = new Thread(() -> saveAndUpload(image), "Screenshot Upload Thread");
          thread.start();
        });
    return "Capturing screenshot...";
  }

  private void saveAndUpload(NativeImage image) {
    try (image) {
      File dir = new File(minecraft.gameDirectory, "screenshots");
      dir.mkdirs();
      File file = new File(dir, LocalDateTime.now().format(NAME_FORMAT) + ".png");
      image.writeToFile(file.toPath());
      String url = upload(file);
      if (url == null) {
        Logger.getLogger().printToChat("Upload failed: catbox returned an error.");
        return;
      }
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
      Logger.getLogger().printToChat("Screenshot URL copied to clipboard: " + url);
    } catch (IOException e) {
      Logger.getLogger().printToChat("Unable to save screenshot.");
      e.printStackTrace();
    }
  }

  /**
   * POSTs the file as multipart/form-data via the shared proxied HTTP client, so uploads honor the
   * active proxy like all other client traffic. Returns the file URL, or null on error.
   */
  private static String upload(File file) {
    String boundary = "Exeter" + System.currentTimeMillis();
    try {
      byte[] body = multipartBody(boundary, file);
      HttpClient client = ProxyManager.httpClient();
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(API_URL))
              .timeout(Duration.ofSeconds(60))
              .header("Content-Type", "multipart/form-data; boundary=" + boundary)
              .header("User-Agent", "exeter")
              .POST(HttpRequest.BodyPublishers.ofByteArray(body))
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String result = response.body() == null ? "" : response.body().trim();
      if (response.statusCode() != 200 || !result.startsWith("https://")) {
        DebugLogger.get()
            .logFile(
                "ScreenShot",
                "upload failed: status="
                    + response.statusCode()
                    + " body="
                    + result.substring(0, Math.min(result.length(), 200)));
        return null;
      }
      return result;
    } catch (IOException | InterruptedException e) {
      DebugLogger.get().logFile("ScreenShot", "upload exception: " + e);
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      e.printStackTrace();
      return null;
    }
  }

  private static byte[] multipartBody(String boundary, File file) throws IOException {
    String head =
        "--"
            + boundary
            + "\r\n"
            + "Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n"
            + "fileupload\r\n"
            + "--"
            + boundary
            + "\r\n"
            + "Content-Disposition: form-data; name=\"fileToUpload\"; filename=\""
            + file.getName()
            + "\"\r\n"
            + "Content-Type: image/png\r\n\r\n";
    String tail = "\r\n--" + boundary + "--\r\n";
    byte[] headBytes = head.getBytes(StandardCharsets.UTF_8);
    byte[] data = Files.readAllBytes(file.toPath());
    byte[] tailBytes = tail.getBytes(StandardCharsets.UTF_8);
    byte[] body = new byte[headBytes.length + data.length + tailBytes.length];
    System.arraycopy(headBytes, 0, body, 0, headBytes.length);
    System.arraycopy(data, 0, body, headBytes.length, data.length);
    System.arraycopy(tailBytes, 0, body, headBytes.length + data.length, tailBytes.length);
    return body;
  }
}

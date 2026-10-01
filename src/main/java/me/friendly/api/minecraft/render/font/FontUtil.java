package me.friendly.api.minecraft.render.font;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.module.impl.toggle.client.CustomFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;

public class FontUtil {

  private static final float TTF_OVERSAMPLE = 4.0F;
  private static final int DEFAULT_SIZE = 9;

  private static final Map<String, String> SYSTEM_FILES = new HashMap<String, String>();

  static {
    SYSTEM_FILES.put("arial", "arial.ttf");
    SYSTEM_FILES.put("arial black", "ariblk.ttf");
    SYSTEM_FILES.put("comic sans ms", "comic.ttf");
    SYSTEM_FILES.put("courier new", "cour.ttf");
    SYSTEM_FILES.put("georgia", "georgia.ttf");
    SYSTEM_FILES.put("impact", "impact.ttf");
    SYSTEM_FILES.put("lucida console", "lucon.ttf");
    SYSTEM_FILES.put("tahoma", "tahoma.ttf");
    SYSTEM_FILES.put("times new roman", "times.ttf");
    SYSTEM_FILES.put("trebuchet ms", "trebuc.ttf");
    SYSTEM_FILES.put("verdana", "verdana.ttf");
    SYSTEM_FILES.put("calibri", "calibri.ttf");
    SYSTEM_FILES.put("cambria", "cambria.ttc");
    SYSTEM_FILES.put("consolas", "consola.ttf");
    SYSTEM_FILES.put("segoe ui", "segoeui.ttf");
  }

  private static final Map<String, Font> CACHE = new HashMap<String, Font>();
  private static Font systemFont;
  private static String systemKey;
  private static final java.util.Set<String> LOGGED = new java.util.HashSet<String>();

  private static void logOnce(String key, Exception e) {
    if (LOGGED.add(key)) {
      System.err.println("[Exeter] TTF font failed (" + key + "): " + e);
    }
  }

  public static void drawString(String text, float x, float y, int color) {
    if (RenderMethods.guiGraphics != null) {
      Font font = activeFont();
      if (font != null) {
        RenderMethods.guiGraphics.text(font, text, (int) x, (int) y, color, true);
      } else {
        RenderMethods.guiGraphics.text(
            Minecraft.getInstance().font, text, (int) x, (int) y, color, true);
      }
    }
  }

  public static int getStringWidth(String text) {
    Font font = activeFont();
    if (font != null) {
      return font.width(text);
    }
    return Minecraft.getInstance().font.width(text);
  }

  private static int fontSize() {
    CustomFont module = CustomFont.get();
    if (module == null) {
      return DEFAULT_SIZE;
    }
    try {
      int size = module.size.getValue().intValue();
      if (size < 6) size = 6;
      if (size > 32) size = 32;
      return size;
    } catch (Exception e) {
      return DEFAULT_SIZE;
    }
  }

  private static Font activeFont() {
    CustomFont module = CustomFont.get();
    if (module == null || !module.isRunning()) {
      return null;
    }
    CustomFont.Face face;
    try {
      face = module.face.getValue();
    } catch (Exception e) {
      logOnce("face", e);
      return null;
    }
    if (face == null || face == CustomFont.Face.VANILLA) {
      return null;
    }
    if (face == CustomFont.Face.SYSTEM) {
      return systemFont();
    }
    Font cached = CACHE.get(face.name() + fontSize());
    if (cached != null) {
      return cached;
    }
    try {
      Identifier location =
          face == CustomFont.Face.LEXEND_DECA
              ? Identifier.fromNamespaceAndPath("exeter", "lexenddeca.ttf")
              : Identifier.fromNamespaceAndPath("exeter", "jetbrainsmono-regular.ttf");
      Font built = TtfFont.fromResource(location, (float) fontSize(), TTF_OVERSAMPLE);
      CACHE.put(face.name() + fontSize(), built);
      return built;
    } catch (Exception e) {
      logOnce(face.name(), e);
      return null;
    }
  }

  private static Font systemFont() {
    CustomFont module = CustomFont.get();
    if (module == null) {
      return null;
    }
    String key = module.family.getValue();
    if (key == null || key.isEmpty()) {
      return null;
    }
    String cacheKey = key.toLowerCase() + fontSize();
    if (systemFont != null && cacheKey.equals(systemKey)) {
      return systemFont;
    }
    File file = resolveSystemFont(key);
    if (file == null || !file.isFile()) {
      logOnce("system-missing-" + key, new IllegalStateException("no file"));
      return null;
    }
    try {
      Font built = TtfFont.fromFile(file, (float) fontSize(), TTF_OVERSAMPLE);
      systemFont = built;
      systemKey = cacheKey;
      return built;
    } catch (Exception e) {
      logOnce("system-" + key, e);
      return null;
    }
  }

  private static File resolveSystemFont(String key) {
    File direct = new File(key);
    if (direct.isFile()) {
      return direct;
    }
    String mapped = SYSTEM_FILES.get(key.toLowerCase());
    if (mapped != null) {
      String windir = System.getenv("WINDIR");
      if (windir == null || windir.isEmpty()) {
        windir = "C:\\Windows";
      }
      File candidate = new File(windir + "\\Fonts\\" + mapped);
      if (candidate.isFile()) {
        return candidate;
      }
    }
    return null;
  }
}

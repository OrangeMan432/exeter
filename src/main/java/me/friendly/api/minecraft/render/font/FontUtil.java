package me.friendly.api.minecraft.render.font;

import java.awt.Font;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.module.impl.toggle.client.CustomFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class FontUtil {

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

  private static final int[] SECTION_COLORS = {
    0xFF000000,
    0xFF0000AA,
    0xFF00AA00,
    0xFF00AAAA,
    0xFFAA0000,
    0xFFAA00AA,
    0xFFFFAA00,
    0xFFAAAAAA,
    0xFF555555,
    0xFF5555FF,
    0xFF55FF55,
    0xFF55FFFF,
    0xFFFF5555,
    0xFFFF55FF,
    0xFFFFFF55,
    0xFFFFFFFF
  };

  private static final Map<String, AtlasFont> CACHE = new HashMap<String, AtlasFont>();
  private static AtlasFont lastServed;
  private static String lastServedKey;
  private static final java.util.Set<String> LOGGED = new java.util.HashSet<String>();

  // Rebuilds are throttled: the slider must settle before a new atlas bakes.
  // Replaced atlases are intentionally never closed (bounded leak, no use-after-free).
  private static final long REBUILD_SETTLE_MS = 750L;
  private static String pendingKey;
  private static long pendingSince;

  private static void logOnce(String key, Exception e) {
    if (LOGGED.add(key)) {
      System.err.println("[Exeter] Atlas font failed (" + key + "): " + e);
    }
  }

  public static void drawString(String text, float x, float y, int color) {
    if (RenderMethods.guiGraphics == null || text == null) {
      return;
    }
    AtlasFont atlas = activeFont();
    if (atlas == null) {
      RenderMethods.guiGraphics.text(
          Minecraft.getInstance().font, text, (int) x, (int) y, color, true);
      return;
    }
    float scale = (float) fontSize() / (float) AtlasFont.BAKE_HEIGHT;
    int shadow = shadowColor(color);
    renderRun(text, x + 1.0F, y + 1.0F, shadow, scale, atlas);
    renderRun(text, x, y, color, scale, atlas);
  }

  public static int getStringWidth(String text) {
    AtlasFont atlas = activeFont();
    if (atlas == null || text == null) {
      return Minecraft.getInstance().font.width(text);
    }
    float scale = (float) fontSize() / (float) AtlasFont.BAKE_HEIGHT;
    float width = 0.0F;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\u00a7' && i + 1 < text.length()) {
        i++;
        continue;
      }
      width += atlas.advance(c) * scale;
    }
    return Math.round(width);
  }

  private static int shadowColor(int color) {
    int alpha = color & 0xFF000000;
    int rgb = (color & 0x00FCFCFC) >> 2;
    return alpha | rgb;
  }

  private static void renderRun(
      String text, float x, float y, int color, float scale, AtlasFont atlas) {
    float cx = x;
    int current = color;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\u00a7' && i + 1 < text.length()) {
        char code = Character.toLowerCase(text.charAt(i + 1));
        i++;
        if (code == 'r') {
          current = color;
        } else if (sectionIndex(code) != -1) {
          current = applyAlpha(SECTION_COLORS[sectionIndex(code)], color);
        }
        continue;
      }
      AtlasFont.Glyph glyph = atlas.glyph(c);
      if (glyph == null) {
        continue;
      }
      int w = Math.max(1, Math.round(glyph.width * scale));
      int h = Math.max(1, Math.round(atlas.cellHeight * scale));
      RenderMethods.guiGraphics.blit(
          RenderPipelines.GUI_TEXTURED,
          atlas.textureId,
          Math.round(cx),
          Math.round(y),
          glyph.u,
          glyph.v,
          w,
          h,
          glyph.width,
          atlas.cellHeight,
          1024,
          1024,
          current);
      cx += glyph.advance * scale;
    }
  }

  private static int sectionIndex(char code) {
    if (code >= '0' && code <= '9') return code - '0';
    if (code >= 'a' && code <= 'f') return 10 + (code - 'a');
    return -1;
  }

  private static int applyAlpha(int section, int base) {
    return (base & 0xFF000000) | (section & 0x00FFFFFF);
  }

  private static int fontSize() {
    CustomFont module = CustomFont.get();
    if (module == null) {
      return DEFAULT_SIZE;
    }
    try {
      int size = module.size.getValue().intValue();
      if (size < 8) size = 8;
      if (size > 32) size = 32;
      return size;
    } catch (Exception e) {
      return DEFAULT_SIZE;
    }
  }

  private static Font activeAwt(String kind, String family) {
    try {
      if ("lexend".equals(kind)) {
        return AtlasFont.awtFromResource("/assets/exeter/font/lexenddeca.ttf");
      }
      if ("jetbrains".equals(kind)) {
        return AtlasFont.awtFromResource("/assets/exeter/font/jetbrainsmono-regular.ttf");
      }
      if ("system".equals(kind)) {
        if (family == null || family.isEmpty()) {
          return null;
        }
        File direct = new File(family);
        if (direct.isFile()) {
          return AtlasFont.awtFromFile(direct);
        }
        String mapped = SYSTEM_FILES.get(family.toLowerCase());
        if (mapped != null) {
          String windir = System.getenv("WINDIR");
          if (windir == null || windir.isEmpty()) {
            windir = "C:\\Windows";
          }
          File candidate = new File(windir + "\\Fonts\\" + mapped);
          if (candidate.isFile()) {
            return AtlasFont.awtFromFile(candidate);
          }
          return AtlasFont.awtSystem(family);
        }
        return AtlasFont.awtSystem(family);
      }
    } catch (Exception e) {
      logOnce("awt-" + kind, e);
    }
    return null;
  }

  private static AtlasFont request(String key, String kind, String family) {
    long now = System.currentTimeMillis();
    AtlasFont hit = CACHE.get(key);
    if (hit != null) {
      pendingKey = null;
      lastServed = hit;
      lastServedKey = key;
      return hit;
    }
    if (!key.equals(pendingKey)) {
      pendingKey = key;
      pendingSince = now;
      return staleFor(key);
    }
    if (now - pendingSince < REBUILD_SETTLE_MS) {
      return staleFor(key);
    }
    pendingKey = null;
    try {
      Font awt = activeAwt(kind, family);
      if (awt == null) {
        return staleFor(key);
      }
      Identifier textureId =
          Identifier.fromNamespaceAndPath(
              "exeter", "font/" + key.toLowerCase().replaceAll("[^a-z0-9]", ""));
      AtlasFont built = AtlasFont.bake(awt, textureId);
      CACHE.put(key, built);
      lastServed = built;
      lastServedKey = key;
      return built;
    } catch (Exception e) {
      logOnce(key, e);
      return staleFor(key);
    }
  }

  private static String keyPrefix(String key) {
    int end = key.length();
    while (end > 0 && Character.isDigit(key.charAt(end - 1))) {
      end--;
    }
    return key.substring(0, end);
  }

  private static AtlasFont staleFor(String key) {
    if (lastServed != null
        && lastServedKey != null
        && keyPrefix(key).equals(keyPrefix(lastServedKey))) {
      return lastServed;
    }
    return null;
  }

  private static AtlasFont activeFont() {
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
      String family = module.family.getValue();
      if (family == null || family.isEmpty()) {
        return null;
      }
      return request("system-" + family.toLowerCase() + fontSize(), "system", family);
    }
    String kind = face == CustomFont.Face.LEXEND_DECA ? "lexend" : "jetbrains";
    return request(face.name() + fontSize(), kind, null);
  }
}

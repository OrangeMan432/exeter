package me.friendly.exeter.render;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

/**
 * Nametag-style world tags: a semi-transparent plate behind centered billboard lines. Glyph metrics
 * follow the engine text pipeline (font pixels at scale/16, 9px lines).
 */
public final class TagRenderer {
  private TagRenderer() {}

  /** Visible length without legacy formatting codes. */
  private static int displayLength(String line) {
    int length = 0;
    for (int i = 0; i < line.length(); i++) {
      if (line.charAt(i) == '§' && i + 1 < line.length()) {
        i++;
        continue;
      }
      length++;
    }
    return length;
  }

  public static void drawTag(Vec3 center, List<String> lines, List<Integer> colors, float scale) {
    if (lines.isEmpty()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;

    float yaw = (float) Math.toRadians(mc.player.getYRot());
    float pitch = (float) Math.toRadians(mc.player.getXRot());
    float sinY = (float) Math.sin(yaw);
    float cosY = (float) Math.cos(yaw);
    float cosP = (float) Math.cos(pitch);
    Vec3 forward = new Vec3(-sinY * cosP, -(float) Math.sin(pitch), cosY * cosP);
    Vec3 right = new Vec3(-cosY, 0f, -sinY);
    Vec3 up = right.cross(forward).normalize();

    float lineH = 0.625f * scale;
    float charW = 0.375f * scale;
    int maxLen = 1;
    for (String line : lines) {
      maxLen = Math.max(maxLen, displayLength(line));
    }
    float halfW = maxLen * charW / 2.0f + 0.15f * scale;
    float halfH = (lines.size() * lineH) / 2.0f + 0.1f * scale;
    // Glyphs extend up from their baseline, so the plate sits a quarter line lower.
    Vec3 plateCenter = center.subtract(up.scale(lineH * 0.25f));
    // Text rides slightly toward the viewer so translucent sorting stays stable.
    Vec3 textPush = forward.scale(-0.05);
    Vec3 rightScaled = right.scale(halfW);
    Vec3 upScaled = up.scale(halfH);
    Vec3 topLeft = plateCenter.add(upScaled).subtract(rightScaled);
    Vec3 topRight = plateCenter.add(upScaled).add(rightScaled);
    Vec3 bottomRight = plateCenter.subtract(upScaled).add(rightScaled);
    Vec3 bottomLeft = plateCenter.subtract(upScaled).subtract(rightScaled);
    var plate = GizmoStyle.fill(0x40000000);
    Gizmos.rect(topLeft, topRight, bottomRight, bottomLeft, plate).setAlwaysOnTop();
    Gizmos.rect(topLeft, bottomLeft, bottomRight, topRight, plate).setAlwaysOnTop();

    for (int i = 0; i < lines.size(); i++) {
      float dy = ((lines.size() - 1) / 2.0f - i) * lineH;
      int color = i < colors.size() ? colors.get(i) : 0xFFFFFFFF;
      var lineStyle = TextGizmo.Style.forColorAndCentered(color).withScale(scale);
      Vec3 anchor = new Vec3(center.x, center.y + dy, center.z).add(textPush);
      Gizmos.billboardText(lines.get(i), anchor, lineStyle).setAlwaysOnTop();
    }
  }
}

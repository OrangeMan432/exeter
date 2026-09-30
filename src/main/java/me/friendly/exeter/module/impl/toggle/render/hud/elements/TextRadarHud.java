package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

public final class TextRadarHud extends ListHudModule {

  private static final class PlayerEntry {
    final String name;
    final int health;
    final int distance;

    PlayerEntry(String name, int health, int distance) {
      this.name = name;
      this.health = health;
      this.distance = distance;
    }
  }

  public TextRadarHud() {
    super("TextRadar", new String[] {"textradar", "radar", "tr"}, Corner.TOP_LEFT);
    setDescription("Lists nearby players with health and distance.");
    offerProperties();
  }

  private static int healthColor(int health) {
    if (health > 16) return 0xFF00FF00;
    if (health > 12) return 0xFFFFA500;
    if (health > 8) return 0xFFFFFF00;
    return 0xFFFF0000;
  }

  private List<PlayerEntry> collect() {
    List<PlayerEntry> result = new ArrayList<PlayerEntry>();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return result;
    for (PlayerEntity player : PlayerUtil.players()) {
      if (player == null || player == mc.player || player.dead) continue;
      int dist = (int) Math.round(PlayerUtil.distanceTo(player));
      result.add(new PlayerEntry(player.name, player.health, dist));
    }
    Collections.sort(
        result,
        new Comparator<PlayerEntry>() {
          @Override
          public int compare(PlayerEntry a, PlayerEntry b) {
            return a.distance - b.distance;
          }
        });
    return result;
  }

  private String format(PlayerEntry e) {
    return e.health + " " + e.name + " " + e.distance + "m";
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<TextEntry> entries = new ArrayList<TextEntry>();
    for (PlayerEntry e : collect()) {
      entries.add(new TextEntry(format(e), healthColor(e.health)));
    }
    return entries;
  }

  @Override
  public int getWidth() {
    int max = 0;
    for (PlayerEntry e : collect()) {
      int w = FontUtil.getStringWidth(format(e));
      if (w > max) max = w;
    }
    if (max == 0 && isInEditor()) {
      max = FontUtil.getStringWidth(getLabel() + " dummy");
    }
    return max;
  }

  @Override
  public int getHeight() {
    int size = collect().size();
    if (size == 0 && isInEditor()) size = 1;
    return size * FontUtil.getFontHeight();
  }
}

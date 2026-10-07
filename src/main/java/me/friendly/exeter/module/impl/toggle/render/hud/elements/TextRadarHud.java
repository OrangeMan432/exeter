package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.util.TotemPopTracker;
import net.minecraft.world.entity.player.Player;

public final class TextRadarHud extends ListHudModule {

  private record PlayerEntry(Player player, float health, int pops, int distance) {}

  public TextRadarHud() {
    super("TextRadar", new String[] {"textradar", "radar", "tr"}, Corner.TOP_LEFT);
    setDescription("Shows totem pops per player in render distance.");
    this.offerProperties();
  }

  private static int healthColor(float health) {
    if (health > 16.0f) return 0xFF00FF00;
    if (health > 12.0f) return 0xFFFFA500;
    if (health > 8.0f) return 0xFFFFFF00;
    return 0xFFFF0000;
  }

  private List<PlayerEntry> collect() {
    List<PlayerEntry> result = new ArrayList<>();
    if (minecraft.player == null || minecraft.level == null) return result;
    TotemPopTracker tracker = TotemPopTracker.getInstance();
    for (Player player : minecraft.level.players()) {
      if (player == null || player == minecraft.player) continue;
      if (player.isRemoved()) continue;
      float health = player.getHealth() + player.getAbsorptionAmount();
      int pops = tracker.getPops(player);
      int dist = (int) Math.round(minecraft.player.distanceTo(player));
      result.add(new PlayerEntry(player, health, pops, dist));
    }
    if (result.isEmpty() && isInEditor()) {
      float health = minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount();
      int pops = tracker.getPops(minecraft.player);
      result.add(new PlayerEntry(minecraft.player, health, pops, 0));
    }
    result.sort(Comparator.comparingInt(PlayerEntry::distance));
    return result;
  }

  private String format(PlayerEntry e) {
    String hp = String.format("%.1f", e.health());
    return hp + " " + e.pops() + " " + e.player().getName().getString() + " " + e.distance() + "m";
  }

  @Override
  public int getWidth() {
    int max = 0;
    for (PlayerEntry e : collect()) {
      int w = FontUtil.getStringWidth(format(e));
      if (w > max) max = w;
    }
    return max;
  }

  @Override
  public int getHeight() {
    return collect().size() * 9;
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<TextEntry> entries = new ArrayList<>();
    for (PlayerEntry e : collect()) {
      entries.add(new TextEntry(format(e), healthColor(e.health())));
    }
    return entries;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<PlayerEntry> entries = collect();
    if (entries.isEmpty()) return;

    boolean top = isTop();
    boolean right = isRight();
    int width = getWidth();

    int py = top ? getY() : getY() + getHeight() - 9;
    for (PlayerEntry e : entries) {
      String hpStr = String.format("%.1f", e.health());
      String popsStr = String.valueOf(e.pops());
      String nameStr = e.player().getName().getString();
      String distStr = e.distance() + "m";

      String hpPart = hpStr + " ";
      String popsPart = popsStr + " ";
      String namePart = nameStr + " ";
      // widths
      int hpW = FontUtil.getStringWidth(hpPart);
      int popsW = FontUtil.getStringWidth(popsPart);
      int nameW = FontUtil.getStringWidth(namePart);
      int distW = FontUtil.getStringWidth(distStr);
      int totalW = hpW + popsW + nameW + distW;

      int px = right ? getX() + width - totalW : getX();

      int hpColor = healthColor(e.health());
      FontUtil.drawString(hpPart, px, py, hpColor);
      px += hpW;
      FontUtil.drawString(popsPart, px, py, 0xFFFFFFFF);
      px += popsW;
      FontUtil.drawString(namePart, px, py, 0xFFFFFFFF);
      px += nameW;
      FontUtil.drawString(distStr, px, py, 0xFFAAAAAA);

      py += top ? 9 : -9;
    }
  }
}

package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayDeque;
import java.util.Deque;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.NumberProperty;

public final class SpeedHud extends HudModule {

  private final NumberProperty<Double> averagingTime =
      new NumberProperty<>(0.0, 0.0, 10.0, "Averaging Time");
  private final Deque<Double> samples = new ArrayDeque<>();

  public SpeedHud() {
    super("Speed", new String[] {"speedhud", "speed"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays your current movement speed.");
    this.offerProperties(averagingTime);
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth("Speed: ") + FontUtil.getStringWidth("0.000");
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    if (minecraft.player == null) return;
    double dx = minecraft.player.getX() - minecraft.player.xOld;
    double dz = minecraft.player.getZ() - minecraft.player.zOld;
    double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;

    double display;
    double time = averagingTime.getValue();
    if (time <= 0.0) {
      display = bps;
      samples.clear();
    } else {
      samples.addLast(bps);
      int maxSamples = (int) Math.ceil(time * 20.0);
      while (samples.size() > maxSamples) {
        samples.removeFirst();
      }
      double avg = 0;
      for (double s : samples) avg += s;
      avg /= samples.size();
      display = avg;
    }

    String value = String.format("%.3f", display);
    FontUtil.drawString("Speed: ", getX(), getY(), Colors.getHudAccent());
    FontUtil.drawString(
        value, getX() + FontUtil.getStringWidth("Speed: "), getY(), Colors.getHudMain());
  }
}

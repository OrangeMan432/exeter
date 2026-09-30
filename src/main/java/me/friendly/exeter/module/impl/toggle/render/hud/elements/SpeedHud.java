package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayDeque;
import java.util.Deque;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.client.Minecraft;

public final class SpeedHud extends HudModule {

  private final NumberProperty<Double> averagingTime =
      new NumberProperty<Double>(0.0, 0.0, 10.0, "Averaging Time", "averagingtime");
  private final Deque<Double> samples = new ArrayDeque<Double>();

  public SpeedHud() {
    super("Speed", new String[] {"speedhud", "speed"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays your current movement speed.");
    offerProperties(averagingTime);
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth("\u00a77Speed: \u00a7f0.000");
  }

  @Override
  public int getHeight() {
    return FontUtil.getFontHeight();
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return;
    double dx = mc.player.x - mc.player.prevX;
    double dz = mc.player.z - mc.player.prevZ;
    double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;

    double display;
    double time = averagingTime.getValue().doubleValue();
    if (time <= 0.0) {
      display = bps;
      samples.clear();
    } else {
      samples.addLast(Double.valueOf(bps));
      int maxSamples = (int) Math.ceil(time * 20.0);
      while (samples.size() > maxSamples) {
        samples.removeFirst();
      }
      double avg = 0.0;
      for (Double s : samples) avg += s.doubleValue();
      avg /= samples.size();
      display = avg;
    }

    String text = String.format("\u00a77Speed: \u00a7f%.3f", Double.valueOf(display));
    FontUtil.drawString(text, (float) getX(), (float) getY(), 0xFFFFFFFF);
  }
}

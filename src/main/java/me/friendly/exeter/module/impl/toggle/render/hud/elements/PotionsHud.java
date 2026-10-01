package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.properties.EnumProperty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public final class PotionsHud extends ListHudModule {

  public enum ColorMode {
    DEFAULT,
    CLIENT
  }

  public final EnumProperty<ColorMode> colorMode =
      new EnumProperty<>(ColorMode.DEFAULT, "Color", "color");

  public PotionsHud() {
    super("Potions", new String[] {"potions", "pots", "p"}, Corner.BOTTOM_LEFT);
    setDescription("Displays active potion effects.");
    this.offerProperties(colorMode);
  }

  @Override
  public int getWidth() {
    if (getEntries().isEmpty() && isInEditor()) {
      return FontUtil.getStringWidth(getDummyEntries().get(0).text());
    }
    return 100;
  }

  @Override
  protected List<TextEntry> getDummyEntries() {
    int color =
        colorMode.getValue() == ColorMode.CLIENT ? Colors.getHudMain() : 0xFF7CAFC6;
    return List.of(new TextEntry("Speed II (01:23)", color));
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<TextEntry> entries = new ArrayList<>();
    if (minecraft.player == null) return entries;
    Collection<MobEffectInstance> effects = minecraft.player.getActiveEffects();
    if (effects == null || effects.isEmpty()) return entries;
    boolean syncClient = colorMode.getValue() == ColorMode.CLIENT;
    float tickRate = minecraft.level.tickRateManager().tickrate();
    for (MobEffectInstance effect : effects) {
      if (effect == null) continue;
      MobEffect mobEffect = effect.getEffect().value();
      if (mobEffect == null) continue;
      String name = net.minecraft.client.resources.language.I18n.get(mobEffect.getDescriptionId());
      String duration = MobEffectUtil.formatDuration(effect, 1.0f, tickRate).getString();
      String text = String.format("%s %d (%s)", name, effect.getAmplifier() + 1, duration);
      int color = syncClient ? Colors.getHudMain() : 0xFF000000 | mobEffect.getColor();
      entries.add(new TextEntry(text, color));
    }
    return entries;
  }
}

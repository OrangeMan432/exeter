package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public final class PotionsHud extends ListHudModule {

  public PotionsHud() {
    super("Potions", new String[] {"potions", "pots", "p"}, Corner.BOTTOM_LEFT);
    setDescription("Displays active potion effects.");
    this.offerProperties();
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
    return List.of(new TextEntry("Speed II (01:23)", 0xFF7CAFC6));
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<TextEntry> entries = new ArrayList<>();
    if (minecraft.player == null) return entries;
    Collection<MobEffectInstance> effects = minecraft.player.getActiveEffects();
    if (effects == null || effects.isEmpty()) return entries;
    float tickRate = minecraft.level.tickRateManager().tickrate();
    for (MobEffectInstance effect : effects) {
      if (effect == null) continue;
      MobEffect mobEffect = effect.getEffect().value();
      if (mobEffect == null) continue;
      String name =
          net.minecraft.client.resources.language.I18n.get(mobEffect.getDescriptionId());
      String duration = MobEffectUtil.formatDuration(effect, 1.0f, tickRate).getString();
      String text = String.format("%s %d (%s)", name, effect.getAmplifier() + 1, duration);
      entries.add(new TextEntry(text, 0xFF000000 | mobEffect.getColor()));
    }
    return entries;
  }
}

package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public final class PotionsHud extends HudModule {

  public PotionsHud() {
    super("Potions", new String[] {"potions", "pots", "p"}, Corner.BOTTOM_LEFT);
    setDescription("Displays active potion effects.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    return 100;
  }

  @Override
  public int getHeight() {
    if (minecraft.player == null) return 9;
    Collection<MobEffectInstance> effects = minecraft.player.getActiveEffects();
    if (effects == null || effects.isEmpty()) return 0;
    return effects.size() * 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    try {
      Collection<MobEffectInstance> effects = minecraft.player.getActiveEffects();
      if (effects == null || effects.isEmpty()) return;
      float tickRate = minecraft.level.tickRateManager().tickrate();
      List<MobEffectInstance> sortedEffects = new ArrayList<>(effects);
      boolean bottom = getY() > scaledHeight / 2;
      if (bottom) Collections.reverse(sortedEffects);
      int py = getY();
      for (MobEffectInstance effect : sortedEffects) {
        if (effect == null) continue;
        MobEffect mobEffect = effect.getEffect().value();
        if (mobEffect == null) continue;
        String name = net.minecraft.client.resources.language.I18n.get(mobEffect.getDescriptionId());
        String duration = MobEffectUtil.formatDuration(effect, 1.0f, tickRate).getString();
        String text = String.format("%s %d (%s)", name, effect.getAmplifier() + 1, duration);
        RenderMethods.guiGraphics.text(minecraft.font, text, getX(), py, 0xFF000000 | mobEffect.getColor(), true);
        py += bottom ? -9 : 9;
      }
    } catch (Exception e) {
      System.out.println("[PotionsHud] render error: " + e.getMessage());
    }
  }
}

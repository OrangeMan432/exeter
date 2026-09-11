package me.larp.client.module.impl.active.render;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Collection;
import me.larp.api.event.Listener;
import me.larp.api.interfaces.Toggleable;
import me.larp.api.minecraft.helper.PlayerHelper;
import me.larp.api.minecraft.render.RenderMethods;
import me.larp.api.minecraft.render.font.FontUtil;
import me.larp.client.core.Larp;
import me.larp.client.events.RenderGameOverlayEvent;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;
import me.larp.client.module.impl.toggle.render.hud.HudComponent;
import me.larp.client.module.impl.toggle.render.hud.HudEditorScreen;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.Property;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;

public final class Hud extends Module {
  public static final Property<Boolean> customFont =
      new Property<Boolean>(false, "CustomFont", "cf", "font");
  private final Property<Boolean> watermark =
      new Property<Boolean>(false, "Watermark", "wm", "water");
  private final Property<Boolean> transparent = new Property<Boolean>(true, "Transparent", "trans");
  private final Property<Boolean> direction =
      new Property<Boolean>(true, "Direction", "facing", "d");
  private final Property<Boolean> armor = new Property<Boolean>(true, "Armor", "a");
  private final Property<Boolean> potions = new Property<Boolean>(true, "Potions", "pots");
  private final Property<Boolean> time = new Property<Boolean>(true, "Time", "t");
  private final Property<Boolean> coords =
      new Property<Boolean>(true, "Coords", "coord", "c", "cord");
  private final Property<Boolean> arraylist =
      new Property<Boolean>(true, "ArrayList", "array", "al");
  private final Property<Boolean> targetHud =
      new Property<Boolean>(true, "TargetHUD", "target", "thud");
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
  private final EnumProperty<Organize> organize =
      new EnumProperty<Organize>(Organize.LENGTH, "Organize", "o");
  private final EnumProperty<Look> look = new EnumProperty<Look>(Look.DEFAULT, "Casing", "c");

  private final List<HudComponent> hudComponents = new ArrayList<>();
  private boolean defaultPositionsInitialized = false;

  public Hud() {
    super("Hud", new String[] {"textgui", "hud", "overlay"});
    setDescription("On-screen overlay showing coords, armor, potions, and more.");
    this.offerProperties(
        this.customFont,
        this.look,
        this.watermark,
        this.organize,
        this.transparent,
        this.potions,
        this.armor,
        this.time,
        this.direction,
        this.arraylist,
        this.targetHud,
        this.coords);

    hudComponents.add(
        new HudComponent("Watermark", 2, 2, 100, 9) {
          @Override
          public void render(int sw, int sh) {
            String dirty = Larp.DIRTY ? " (dirty)" : "";
            String text =
                String.format(
                    "%s \u00a77%s.%s%s \u00a78[\u00a7f%d fps\u00a78] [\u00a7f%dms\u00a78]",
                    Larp.TITLE, Larp.BUILD, Larp.HASH, dirty, fps(), ping());
            FontUtil.drawString(
                text, getX(), getY(), transparent.getValue() != false ? -1711276033 : -1);
          }

          private int fps() {
            try {
              return net.minecraft.client.Minecraft.getInstance().getFps();
            } catch (Exception e) {
              return 0;
            }
          }

          private int ping() {
            try {
              var mc = net.minecraft.client.Minecraft.getInstance();
              if (mc.getConnection() == null || mc.player == null) return 0;
              var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
              return info != null ? info.getLatency() : 0;
            } catch (Exception e) {
              return 0;
            }
          }
        });
    hudComponents.add(
        new HudComponent("ArrayList", 2, 2, 100, 9) {
          @Override
          public void render(int sw, int sh) {
            List<Module> modules =
                new ArrayList<>(Larp.getInstance().getModuleManager().getRegistry());
            Organize org = (Organize) ((Object) organize.getValue());
            if (org == Organize.ABC) {
              modules.sort((mod1, mod2) -> mod1.getLabel().compareTo(mod2.getLabel()));
            } else {
              modules.sort(
                  (mod1, mod2) ->
                      FontUtil.getStringWidth(mod2.getLabel())
                          - FontUtil.getStringWidth(mod1.getLabel()));
            }
            boolean bottom = getY() > sh / 2;
            if (bottom) Collections.reverse(modules);
            boolean left = getX() < sw / 2;
            int py = getY();
            for (Module module : modules) {
              if (!(module instanceof Toggleable)) continue;
              ToggleableModule tm = (ToggleableModule) module;
              if (!tm.isRunning() || !tm.isDrawn()) continue;
              int color = tm.getColor() | 0xFF000000;
              String label = getTag(tm.getLabel());
              int lw = FontUtil.getStringWidth(label);
              int tx = left ? getX() : getX() + getWidth() - lw;
              // Sidebar accent bar, left or right edge.
              int barX = left ? getX() - 2 : getX() + getWidth() + 1;
              me.larp.api.minecraft.render.RenderMethods.drawRect(
                  barX, py - 1, barX + 1, py + 8, color);
              FontUtil.drawString(label, tx, py, color);
              py += bottom ? -9 : 9;
            }
          }
        });
    hudComponents.add(
        new HudComponent("Armor", 0, 0, 72, 16) {
          @Override
          public void render(int sw, int sh) {
            int x = getX();
            net.minecraft.client.renderer.state.gui.GuiRenderState state =
                new net.minecraft.client.renderer.state.gui.GuiRenderState();
            net.minecraft.client.gui.GuiGraphicsExtractor localGui =
                new net.minecraft.client.gui.GuiGraphicsExtractor(minecraft, state, sw, sh);
            for (int index = 3; index >= 0; --index) {
              net.minecraft.world.entity.EquipmentSlot slot =
                  index == 3
                      ? net.minecraft.world.entity.EquipmentSlot.HEAD
                      : (index == 2
                          ? net.minecraft.world.entity.EquipmentSlot.CHEST
                          : (index == 1
                              ? net.minecraft.world.entity.EquipmentSlot.LEGS
                              : net.minecraft.world.entity.EquipmentSlot.FEET));
              ItemStack stack = minecraft.player.getItemBySlot(slot);
              if (stack == null || stack.isEmpty()) continue;
              int y = getY();
              localGui.item(stack, x, y);
              localGui.itemDecorations(minecraft.font, stack, x, y);
              x += 18;
            }
          }
        });
    hudComponents.add(
        new HudComponent("Potions", 0, 0, 100, 9) {
          @Override
          public void render(int sw, int sh) {
            try {
              Collection<MobEffectInstance> effects = minecraft.player.getActiveEffects();
              if (effects == null || effects.isEmpty()) return;
              float tickRate = minecraft.level.tickRateManager().tickrate();
              List<MobEffectInstance> sortedEffects = new ArrayList<>(effects);
              boolean bottom = getY() > sh / 2;
              if (bottom) Collections.reverse(sortedEffects);
              boolean left = getX() < sw / 2;
              int py = getY();
              for (MobEffectInstance effect : sortedEffects) {
                if (effect == null) continue;
                MobEffect mobEffect = effect.getEffect().value();
                if (mobEffect == null) continue;
                String name = I18n.get(mobEffect.getDescriptionId());
                String duration = MobEffectUtil.formatDuration(effect, 1.0f, tickRate).getString();
                String text =
                    String.format("%s %d (%s)", name, effect.getAmplifier() + 1, duration);
                int tw = minecraft.font.width(text);
                int tx = left ? getX() : getX() + getWidth() - tw;
                RenderMethods.guiGraphics.text(
                    minecraft.font, text, tx, py, 0xFF000000 | mobEffect.getColor(), true);
                py += bottom ? -9 : 9;
              }
            } catch (Exception e) {
              System.out.println("[Potions] render error: " + e.getMessage());
            }
          }
        });
    hudComponents.add(
        new HudComponent("Coords", 0, 0, 100, 9) {
          @Override
          public void render(int sw, int sh) {
            String text =
                String.format(
                    "\u00a7f%s, %s, %s \u00a77XYZ",
                    (int) minecraft.player.getX(),
                    (int) minecraft.player.getY(),
                    (int) minecraft.player.getZ());
            FontUtil.drawString(text, getX(), getY(), -1);
          }
        });
    hudComponents.add(
        new HudComponent("Time", 0, 0, 100, 9) {
          @Override
          public void render(int sw, int sh) {
            String text = String.format("\u00a77%s", dateFormat.format(new Date()));
            FontUtil.drawString(text, getX(), getY(), -1);
          }
        });
    hudComponents.add(
        new HudComponent("TargetHUD", 0, 0, 120, 27) {
          @Override
          public void render(int sw, int sh) {
            net.minecraft.world.entity.player.Player target = findTarget();
            if (target == null) return;
            String name = target.getGameProfile().name();
            float hp = target.getHealth() + target.getAbsorptionAmount();
            String hpText = String.format("%.0f HP", hp);
            String distText =
                String.format("%.0fm", minecraft.player.distanceTo(target));
            int nameColor = hp > 12 ? 0xFF55FF55 : hp > 6 ? 0xFFFFAA00 : 0xFFFF5555;
            FontUtil.drawString(name, getX() + 2, getY() + 2, -1);
            FontUtil.drawString(hpText, getX() + 2, getY() + 12, nameColor);
            FontUtil.drawString(
                distText,
                getX() + getWidth() - FontUtil.getStringWidth(distText) - 2,
                getY() + 12,
                0xFF888888);
          }

          private net.minecraft.world.entity.player.Player findTarget() {
            if (minecraft.level == null || minecraft.player == null) return null;
            net.minecraft.world.entity.player.Player best = null;
            double bestDist = 12.0;
            for (var entity : minecraft.level.players()) {
              if (entity == minecraft.player || !entity.isAlive()) continue;
              double d = minecraft.player.distanceTo(entity);
              if (d < bestDist) {
                bestDist = d;
                best = entity;
              }
            }
            return best;
          }
        });
    hudComponents.add(
        new HudComponent("Direction", 0, 0, 100, 9) {
          @Override
          public void render(int sw, int sh) {
            String text =
                String.format(
                    "\u00a77%s", PlayerHelper.getFacingWithProperCapitals().toUpperCase());
            FontUtil.drawString(text, getX(), getY(), -1);
          }
        });

    Larp.getInstance()
        .getEventManager()
        .register(
            new Listener<RenderGameOverlayEvent>("text_gui_render_game_overlay_listener") {
              @Override
              public void call(RenderGameOverlayEvent event) {
                if (minecraft.gui.hud.getDebugOverlay().showDebugScreen()
                    || event.getType() != RenderGameOverlayEvent.Type.IN_GAME) {
                  return;
                }
                int sw = minecraft.getWindow().getGuiScaledWidth();
                int sh = minecraft.getWindow().getGuiScaledHeight();
                if (!defaultPositionsInitialized) {
                  initDefaultPositions(sw, sh);
                  defaultPositionsInitialized = true;
                }
                for (HudComponent comp : hudComponents) {
                  comp.setVisible(isComponentEnabled(comp));
                  if (!comp.isVisible()) continue;
                  comp.render(sw, sh);
                }
              }
            });

    HudEditorScreen.getInstance().setComponents(hudComponents);
  }

  private boolean isComponentEnabled(HudComponent comp) {
    switch (comp.getLabel()) {
      case "Watermark":
        return watermark.getValue();
      case "ArrayList":
        return arraylist.getValue();
      case "Armor":
        return armor.getValue();
      case "Potions":
        return potions.getValue();
      case "Coords":
        return coords.getValue();
      case "Time":
        return time.getValue();
      case "Direction":
        return direction.getValue();
      case "TargetHUD":
        return targetHud.getValue();
      default:
        return false;
    }
  }

  private void initDefaultPositions(int sw, int sh) {
    for (HudComponent c : hudComponents) {
      switch (c.getLabel()) {
        case "Watermark":
          if (c.getX() == 0 && c.getY() == 0) {
            c.setX(2);
            c.setY(2);
          }
          break;
        case "ArrayList":
          c.setX(sw - c.getWidth() - 5);
          c.setY(5);
          break;
        case "Armor":
          c.setX(sw / 2 - 36);
          c.setY(sh - 55);
          break;
        case "Potions":
          if (c.getX() == 0 && c.getY() == 0) {
            c.setX(sw - 100);
            c.setY(sh / 2);
          }
          break;
        case "Coords":
          if (c.getX() == 0 && c.getY() == 0) {
            c.setX(sw - 100);
            c.setY(sh / 2 + 20);
          }
          break;
        case "Time":
          if (c.getX() == 0 && c.getY() == 0) {
            c.setX(sw - 100);
            c.setY(sh / 2 + 30);
          }
          break;
        case "Direction":
          if (c.getX() == 0 && c.getY() == 0) {
            c.setX(sw - 100);
            c.setY(sh / 2 + 40);
          }
          break;
        case "TargetHUD":
          if (c.getX() == 0 && c.getY() == 0) {
            c.setX(sw / 2 - 60);
            c.setY(sh / 2 + 40);
          }
          break;
      }
    }
  }

  public List<HudComponent> getHudComponents() {
    return hudComponents;
  }

  private String getTag(String tag) {
    switch (look.getValue()) {
      case UPPER:
        tag = tag.toUpperCase();
        break;
      case LOWER:
        tag = tag.toLowerCase();
        break;
      case CUB:
        tag = String.format("[%s]", tag.toLowerCase());
        break;
    }
    return tag;
  }

  private enum Look {
    DEFAULT,
    LOWER,
    UPPER,
    CUB;
  }

  private enum Organize {
    ABC,
    LENGTH;
  }
}

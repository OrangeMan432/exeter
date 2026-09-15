package me.friendly.exeter.module;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.client.Debug;
import me.friendly.exeter.module.impl.toggle.client.DiscordRPC;
import me.friendly.exeter.module.impl.toggle.client.Notifier;
import me.friendly.exeter.module.impl.toggle.client.TestModule;
import me.friendly.exeter.module.impl.toggle.combat.AutoCart;
import me.friendly.exeter.module.impl.toggle.combat.AutoPot;
import me.friendly.exeter.module.impl.toggle.combat.AutoTotem;
import me.friendly.exeter.module.impl.toggle.combat.BedAura;
import me.friendly.exeter.module.impl.toggle.combat.PistonPush;
import me.friendly.exeter.module.impl.toggle.combat.SelfBed;
import me.friendly.exeter.module.impl.toggle.misc.AutoGear;
import me.friendly.exeter.module.impl.toggle.misc.AutoItemDupe;
import me.friendly.exeter.module.impl.toggle.movement.NoBedStep;
import me.friendly.exeter.module.impl.toggle.movement.NoFall;
import me.friendly.exeter.module.impl.toggle.movement.Speed;
import me.friendly.exeter.module.impl.toggle.movement.Sprint;
import me.friendly.exeter.module.impl.toggle.movement.Step;
import me.friendly.exeter.module.impl.toggle.movement.Velocity;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.EatTimer;
import me.friendly.exeter.module.impl.toggle.render.HUDEditor;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TabGui;
import me.friendly.exeter.module.impl.toggle.render.hud.HudRenderer;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.ArrayListHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.ArmorHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.CoordsHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.DirectionHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.PotionsHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TimeHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TextRadarHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.NotificationHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.WatermarkHud;
import me.friendly.exeter.module.impl.toggle.world.AutoShulker;
import com.mojang.blaze3d.platform.InputConstants;

/** Manages {@link Module}s for Exeter. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList();

    register(new HudRenderer());
    register(new ClickGui());
    register(new TabGui());
    register(new EatTimer());
    register(new Colors());
    register(new HUDEditor());
    register(new DiscordRPC());
    register(new AutoItemDupe());
    register(new AutoTotem());
    register(new AutoGear());
    register(new SelfBed());
    register(new BedAura());
    register(new Speed());
    register(new PistonPush());
    register(new AutoCart());
    register(new AutoPot());
    register(new AutoShulker());
    register(new Velocity());
    register(new NoBedStep());
    register(new NoFall());
    register(new Step());
    register(new Sprint());
    register(new TestModule());
    register(new Debug());
    register(new Notifier());

    register(new WatermarkHud());
    register(new ArrayListHud());
    register(new ArmorHud());
    register(new PotionsHud());
    register(new CoordsHud());
    register(new TimeHud());
    register(new DirectionHud());
    register(new TextRadarHud());
    register(new NotificationHud());

    for (Module m : registry) {
      if (m instanceof Debug debug) {
        debug.initModuleToggles(registry);
        break;
      }
    }

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("ClickGui")
        .setKey(InputConstants.KEY_RSHIFT);

    new Config("module_configurations") {

      @Override
      public void load(Object... source) {
        ExeterConfig.getInstance().loadAll();
      }

      @Override
      public void save(Object... destination) {
        ExeterConfig.getInstance().saveAll();
      }
    };
  }

  public Module getModuleByAlias(String alias) {
    for (Module module : registry) {
      for (String moduleAlias : module.getAliases()) {
        if (!alias.equalsIgnoreCase(moduleAlias)) continue;
        return module;
      }
    }
    return null;
  }
}

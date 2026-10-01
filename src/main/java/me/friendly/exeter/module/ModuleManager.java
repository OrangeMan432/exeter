package me.friendly.exeter.module;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.client.Debug;
import me.friendly.exeter.module.impl.toggle.client.HUDEditor;
import me.friendly.exeter.module.impl.toggle.client.Notifier;
import me.friendly.exeter.module.impl.toggle.client.WindowsModule;
import me.friendly.exeter.module.impl.toggle.combat.AutoArmor;
import me.friendly.exeter.module.impl.toggle.combat.KillAura;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.movement.FastFall;
import me.friendly.exeter.module.impl.toggle.movement.NoFall;
import me.friendly.exeter.module.impl.toggle.movement.Speed;
import me.friendly.exeter.module.impl.toggle.movement.Step;
import me.friendly.exeter.module.impl.toggle.movement.Velocity;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.EntityEsp;
import me.friendly.exeter.module.impl.toggle.render.FreeLook;
import me.friendly.exeter.module.impl.toggle.render.hud.HudRenderer;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.ArrayListHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.ArmorHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.CoordsHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.DirectionHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.NotificationHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.SpeedHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TextRadarHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.TimeHud;
import me.friendly.exeter.module.impl.toggle.render.hud.elements.WatermarkHud;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.FakePlayerModule;
import org.lwjgl.input.Keyboard;

/** Manages {@link Module}s for Exeter. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList<Module>();

    register(new ClickGui());
    register(new EntityEsp());
    register(new FreeLook());
    register(new HudRenderer());
    register(new WatermarkHud());
    register(new CoordsHud());
    register(new DirectionHud());
    register(new SpeedHud());
    register(new TimeHud());
    register(new TextRadarHud());
    register(new ArmorHud());
    register(new ArrayListHud());
    register(new NotificationHud());
    register(new Debug());
    register(new Notifier());
    register(new WindowsModule());
    register(new HUDEditor());
    register(new Colors());
    register(new Speed());
    register(new FastFall());
    register(new NoFall());
    register(new Step());
    register(new Velocity());
    register(new KillAura());
    register(new AutoArmor());
    register(new FakePlayerModule());

    for (Module m : registry) {
      if (m instanceof Debug) {
        ((Debug) m).initModuleToggles(registry);
        break;
      }
    }

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("ClickGui")
        .setKey(Keyboard.KEY_RSHIFT);

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("Windows")
        .setKey(Keyboard.KEY_GRAVE);

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("HUDEditor")
        .setKey(Keyboard.KEY_COMMA);

    new Config("module_configurations") {

      @Override
      public void load(Object... source) {
        Exeter.getInstance().getExeterConfig().loadAll();
      }

      @Override
      public void save(Object... destination) {
        Exeter.getInstance().getExeterConfig().saveAll();
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

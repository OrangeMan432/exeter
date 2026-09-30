package me.friendly.exeter.module;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.combat.KillAura;
import me.friendly.exeter.module.impl.toggle.movement.Speed;
import me.friendly.exeter.module.impl.toggle.render.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import org.lwjgl.input.Keyboard;

/** Manages {@link Module}s for Exeter. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList<Module>();

    register(new ClickGui());
    register(new Speed());
    register(new KillAura());
    register(new HudModule());

    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel("ClickGui")
        .setKey(Keyboard.KEY_RSHIFT);
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

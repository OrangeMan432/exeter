package me.friendly.exeter.module;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;

/** Manages {@link Module}s for Exeter. Modules are registered by later port waves. */
public final class ModuleManager extends ListRegistry<Module> {

  public ModuleManager() {
    this.registry = new ArrayList<Module>();
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

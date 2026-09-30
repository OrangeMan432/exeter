package me.friendly.exeter.config;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;

/** Registry for {@link Config} objects, loaded at startup and saved on shutdown. */
public final class ConfigManager extends ListRegistry<Config> {
  public ConfigManager() {
    this.registry = new ArrayList<Config>();
  }
}

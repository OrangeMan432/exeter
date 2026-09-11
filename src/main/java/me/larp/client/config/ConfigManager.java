package me.larp.client.config;

import java.util.ArrayList;
import me.larp.api.registry.ListRegistry;

public final class ConfigManager extends ListRegistry<Config> {

  /** Instantiate registry */
  public ConfigManager() {
    this.registry = new ArrayList();
  }
}

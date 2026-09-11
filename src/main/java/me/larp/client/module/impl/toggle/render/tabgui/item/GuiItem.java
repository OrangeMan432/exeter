package me.larp.client.module.impl.toggle.render.tabgui.item;

import me.larp.client.module.ToggleableModule;

public class GuiItem {
  private final ToggleableModule toggleableModule;

  public GuiItem(ToggleableModule toggleableModule) {
    this.toggleableModule = toggleableModule;
  }

  public ToggleableModule getToggleableModule() {
    return this.toggleableModule;
  }
}

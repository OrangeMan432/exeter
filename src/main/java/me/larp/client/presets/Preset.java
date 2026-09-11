package me.larp.client.presets;

import me.larp.api.interfaces.Labeled;

public abstract class Preset implements Labeled {
  private final String label;

  protected Preset(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  public abstract void onSet();
}

package me.friendly.exeter.module;

import java.util.*;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.presets.Preset;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;

/** Client Module. An implementation of Labeled */
public class Module implements Labeled {
  private final String label;
  private String tag;
  private final String[] aliases;
  private String description = "";

  /** Properties for the Module. */
  protected final List<Property<?>> properties = new ArrayList<>();

  private final List<Preset> presets = new ArrayList<Preset>();
  protected Minecraft minecraft = Minecraft.getInstance();

  protected Module(String label, String[] aliases) {
    this.label = this.tag = label;
    this.aliases = aliases;
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  public String[] getAliases() {
    return this.aliases;
  }

  public String getTag() {
    return this.tag;
  }

  protected void setTag(String tag) {
    this.tag = tag;
  }

  public String getDescription() {
    return this.description;
  }

  protected void setDescription(String description) {
    this.description = description;
  }

  public List<Property<?>> getProperties() {
    return this.properties;
  }

  protected void offerProperties(Property<?>... properties) {
    this.properties.addAll(Arrays.asList(properties));
  }

  public Property<?> getPropertyByAlias(String alias) {
    for (Property<?> property : properties) {
      for (String propertyAlias : property.getAliases()) {
        if (!alias.equalsIgnoreCase(propertyAlias)) continue;
        return property;
      }
    }
    return null;
  }

  public List<Preset> getPresets() {
    return this.presets;
  }

  protected void offsetPresets(Preset... presets) {
    for (Preset preset : presets) {
      this.presets.add(preset);
    }
    this.presets.sort((p1, p2) -> p1.getLabel().compareTo(p2.getLabel()));
  }

  public Preset getPresetByLabel(String label) {
    for (Preset preset : presets) {
      if (!label.equalsIgnoreCase(preset.getLabel())) continue;
      return preset;
    }
    return null;
  }
}

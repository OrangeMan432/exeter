package me.friendly.exeter.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.friendly.api.interfaces.Labeled;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;

/** Client Module. An implementation of Labeled */
public class Module implements Labeled {
  private final String label;
  private String tag;
  private final String[] aliases;
  private String description = "";

  /** Properties for the Module. */
  protected final List<Property<?>> properties = new ArrayList<Property<?>>();

  protected Module(String label, String[] aliases) {
    this.label = this.tag = label;
    this.aliases = aliases;
  }

  /**
   * Timing-proof accessor: entrypoints run before the game boots, so a cached field would stay
   * null forever. Always resolves live.
   */
  protected Minecraft minecraft() {
    return MinecraftAccessor.getMinecraft();
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
}

package me.friendly.exeter.properties;

import java.util.ArrayList;
import java.util.List;

public class Property<T> {
  private final String[] aliases;
  protected T value;
  private final List<Property<?>> children = new ArrayList<Property<?>>();

  public Property(T value, String... aliases) {
    this.value = value;
    this.aliases = aliases;
  }

  public String[] getAliases() {
    return this.aliases;
  }

  public T getValue() {
    return this.value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public List<Property<?>> getChildren() {
    return this.children;
  }

  public Property<T> addChild(Property<?> child) {
    this.children.add(child);
    return this;
  }
}

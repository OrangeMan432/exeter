package me.friendly.exeter.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Property<T> {
  private final String[] aliases;
  protected T value;
  private final List<Property<?>> children = new ArrayList<>();
  private BooleanSupplier visibleWhen = () -> true;

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

  public Property<T> visibleWhen(BooleanSupplier visibleWhen) {
    this.visibleWhen = visibleWhen;
    return this;
  }

  public boolean isVisible() {
    try {
      return visibleWhen.getAsBoolean();
    } catch (Exception e) {
      return true;
    }
  }
}

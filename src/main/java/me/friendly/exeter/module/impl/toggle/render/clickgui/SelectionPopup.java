package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import me.friendly.exeter.properties.Property;

/**
 * Shared building blocks for modules that configure selections through a {@link SearchSelectPopup}.
 */
public final class SelectionPopup {

  private SelectionPopup() {}

  /** A popup row bound to membership of {@code selected}. Displayed as {@code name [id]}. */
  public static SearchSelectPopup.ToggleItem idItem(
      String id, String displayName, Set<String> selected) {
    return toggle(
        displayName + " [" + id + "]",
        new BooleanSupplier() {
          @Override
          public boolean getAsBoolean() {
            return selected.contains(id);
          }
        },
        new Consumer<Boolean>() {
          @Override
          public void accept(Boolean enabled) {
            if (enabled.booleanValue()) {
              selected.add(id);
            } else {
              selected.remove(id);
            }
          }
        });
  }

  /** A popup row with a custom label bound to arbitrary state. */
  public static SearchSelectPopup.ToggleItem toggle(
      String label, final BooleanSupplier isEnabled, final Consumer<Boolean> setEnabled) {
    return new SearchSelectPopup.ToggleItem() {
      @Override
      public String getLabel() {
        return label;
      }

      @Override
      public boolean isEnabled() {
        return isEnabled.getAsBoolean();
      }

      @Override
      public void setEnabled(boolean enabled) {
        setEnabled.accept(Boolean.valueOf(enabled));
      }
    };
  }

  /**
   * Opens the popup. {@code onDone} runs when Done is clicked (use it to persist and apply side
   * effects); the popup is closed afterwards in both cases.
   */
  public static void open(String title, List<SearchSelectPopup.ToggleItem> items, Runnable onDone) {
    ClickGuiScreen.getInstance()
        .openPopup(
            new SearchSelectPopup(
                title,
                items,
                new Runnable() {
                  @Override
                  public void run() {
                    onDone.run();
                    ClickGuiScreen.getInstance().closePopup();
                  }
                },
                new Runnable() {
                  @Override
                  public void run() {
                    ClickGuiScreen.getInstance().closePopup();
                  }
                }));
  }

  /**
   * A set of selection ids persisted as a comma-separated config property. The runtime set is the
   * source of truth while playing; call {@link #save()} when the popup closes and {@link #load()}
   * when the module enables.
   */
  public static final class Ids {
    private final Property<String> property;
    private final Set<String> selected = new HashSet<String>();

    public Ids(String alias) {
      this.property = new Property<String>("", alias);
    }

    public Property<String> getProperty() {
      return property;
    }

    public Set<String> getSelected() {
      return selected;
    }

    public void load() {
      selected.clear();
      String raw = property.getValue();
      if (raw != null && !raw.isEmpty()) {
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
          String trimmed = parts[i].trim();
          if (!trimmed.isEmpty()) {
            selected.add(trimmed);
          }
        }
      }
    }

    public void save() {
      StringBuilder sb = new StringBuilder();
      for (String id : selected) {
        if (sb.length() > 0) sb.append(",");
        sb.append(id);
      }
      property.setValue(sb.toString());
    }
  }

  /**
   * A name-to-boolean toggle map persisted as comma-separated {@code name:true/false} pairs. {@link
   * #load()} merges over existing entries so defaults registered via putIfAbsent survive.
   */
  public static final class Toggles {
    private final Property<String> property;
    private final Map<String, Boolean> toggles = new LinkedHashMap<String, Boolean>();

    public Toggles(String alias) {
      this.property = new Property<String>("", alias);
    }

    public Property<String> getProperty() {
      return property;
    }

    public Map<String, Boolean> getToggles() {
      return toggles;
    }

    public void load() {
      String raw = property.getValue();
      if (raw == null || raw.isEmpty()) return;
      String[] pairs = raw.split(",");
      for (int i = 0; i < pairs.length; i++) {
        String trimmed = pairs[i].trim();
        if (trimmed.isEmpty()) continue;
        int eq = trimmed.indexOf(':');
        if (eq > 0) {
          toggles.put(
              trimmed.substring(0, eq).trim(),
              Boolean.valueOf(Boolean.parseBoolean(trimmed.substring(eq + 1).trim())));
        }
      }
    }

    public void save() {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, Boolean> entry : toggles.entrySet()) {
        if (sb.length() > 0) sb.append(",");
        sb.append(entry.getKey()).append(":").append(entry.getValue());
      }
      property.setValue(sb.toString());
    }
  }
}

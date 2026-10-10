package me.friendly.exeter.macro;

import java.util.ArrayList;
import java.util.List;

/**
 * A key-bound bundle of commands. Instant macros fire their commands on each press; toggle
 * ("flowy") macros run one set on enable and another on disable, like a module. Command lines are
 * stored with or without the chat prefix.
 */
public class Macro {
  public enum Mode {
    INSTANT,
    TOGGLE
  }

  private String name;
  private Mode mode = Mode.INSTANT;
  private int key;
  private final List<String> commands = new ArrayList<>();
  private final List<String> onEnable = new ArrayList<>();
  private final List<String> onDisable = new ArrayList<>();
  private boolean enabled;

  public Macro(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  /** SDL keycode, 0 means unbound. */
  public int getKey() {
    return key;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public List<String> getCommands() {
    return commands;
  }

  public List<String> getOnEnable() {
    return onEnable;
  }

  public List<String> getOnDisable() {
    return onDisable;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** Flips the flag without running anything; the manager runs the commands. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** Active command lines for the window preview. */
  public List<String> previewLines() {
    if (mode == Mode.TOGGLE) {
      List<String> both = new ArrayList<>(onEnable);
      both.addAll(onDisable);
      return both;
    }
    return commands;
  }
}

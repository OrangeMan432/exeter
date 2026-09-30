package me.friendly.exeter.command;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.command.impl.client.Bind;
import me.friendly.exeter.command.impl.client.Friends;
import me.friendly.exeter.command.impl.client.Help;
import me.friendly.exeter.command.impl.client.Modules;
import me.friendly.exeter.command.impl.client.Prefix;
import me.friendly.exeter.command.impl.client.Toggle;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;

public final class CommandManager extends ListRegistry<Command> {
  private String prefix = ".";

  public CommandManager() {
    this.registry = new ArrayList<Command>();
    this.register(new Toggle());
    this.register(new Help());
    this.register(new Modules());
    this.register(new Prefix());
    this.register(new Friends.Add());
    this.register(new Friends.Remove());
    this.register(new Bind());
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  /** Dispatches raw input with no prefix required (console usage). */
  public String dispatchDirect(String input) {
    String trimmed = input.trim();
    if (trimmed.isEmpty()) {
      return "No command entered.";
    }
    String[] arguments = trimmed.split(" ");
    String execute = arguments[0];

    for (Command command : getRegistry()) {
      String[] aliases = command.getAliases();
      for (int i = 0; i < aliases.length; i++) {
        if (!execute.equalsIgnoreCase(aliases[i].replaceAll(" ", ""))) continue;
        try {
          return command.dispatch(arguments);
        } catch (Exception e) {
          return aliases[i] + " " + command.getSyntax();
        }
      }
    }

    for (Module mod : Exeter.getInstance().getModuleManager().getRegistry()) {
      String[] aliases = mod.getAliases();
      for (int i = 0; i < aliases.length; i++) {
        if (!execute.equalsIgnoreCase(aliases[i].replace(" ", ""))) continue;
        if (arguments.length > 1) {
          String valueName = arguments[1];
          if (arguments[1].equalsIgnoreCase("list")) {
            if (mod.getProperties().size() > 0) {
              StringBuilder list = new StringBuilder();
              for (Property<?> property : mod.getProperties()) {
                if (list.length() > 0) list.append(", ");
                Object value = property.getValue();
                String shown;
                if (value instanceof Enum && property instanceof EnumProperty) {
                  shown = ((EnumProperty<?>) property).getFixedValue();
                } else {
                  shown = String.valueOf(value);
                }
                list.append(property.getAliases()[0]).append(" [").append(shown).append("]");
              }
              return "Properties (" + mod.getProperties().size() + ") " + list.toString() + ".";
            }
            return mod.getLabel() + " has no properties.";
          }
          Property<?> property = mod.getPropertyByAlias(valueName);
          if (property == null) {
            return "Property '" + valueName + "' not found.";
          }
          if (arguments.length < 3) {
            return execute + " [list|valuename] [list|get]";
          }
          Object current = property.getValue();
          if (current instanceof Number) {
            if (!arguments[2].equalsIgnoreCase("get")) {
              setNumberProperty(property, arguments[2]);
            }
            return property.getAliases()[0]
                + " current value is "
                + property.getValue()
                + " for "
                + mod.getLabel()
                + ".";
          }
          if (current instanceof Enum && property instanceof EnumProperty) {
            if (!arguments[2].equalsIgnoreCase("list")) {
              ((EnumProperty<?>) property).setValue(arguments[2]);
              return property.getAliases()[0]
                  + " has been set to "
                  + ((EnumProperty<?>) property).getFixedValue()
                  + " for "
                  + mod.getLabel()
                  + ".";
            }
            return "Modes: " + enumModes((EnumProperty<?>) property) + ".";
          }
          if (current instanceof String) {
            ((Property<Object>) property).setValue(arguments[2]);
            return property.getAliases()[0]
                + " has been set to \""
                + property.getValue()
                + "\" for "
                + mod.getLabel()
                + ".";
          }
          if (current instanceof Boolean) {
            ((Property<Boolean>) property)
                .setValue(Boolean.valueOf(!((Boolean) current).booleanValue()));
            return property.getAliases()[0]
                + " has been "
                + (((Boolean) property.getValue()).booleanValue() ? "enabled" : "disabled")
                + " for "
                + mod.getLabel()
                + ".";
          }
          return execute + " [list|valuename] [list|get]";
        }
        return execute + " [list|valuename] [list|get]";
      }
    }

    return "Unknown command: " + execute;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void setNumberProperty(Property<?> property, String raw) {
    Object current = property.getValue();
    if (current instanceof Integer) {
      ((NumberProperty) property).setValue(Integer.valueOf(Integer.parseInt(raw)));
    } else if (current instanceof Float) {
      ((NumberProperty) property).setValue(Float.valueOf(Float.parseFloat(raw)));
    } else if (current instanceof Double) {
      ((NumberProperty) property).setValue(Double.valueOf(Double.parseDouble(raw)));
    } else if (current instanceof Long) {
      ((NumberProperty) property).setValue(Long.valueOf(Long.parseLong(raw)));
    }
  }

  private String enumModes(EnumProperty<?> property) {
    Object current = property.getValue();
    if (!(current instanceof Enum)) {
      return "";
    }
    Enum[] array = (Enum[]) current.getClass().getEnumConstants();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < array.length; i++) {
      if (i > 0) builder.append(", ");
      builder.append(array[i].name());
    }
    return builder.toString();
  }

  /** Dispatches a chat message, printing the response to chat. Returns true if handled. */
  public boolean dispatch(String message) {
    if (message == null || !message.trim().startsWith(prefix)) {
      return false;
    }
    String response = dispatchDirect(message);
    if (response != null && !response.isEmpty()) {
      Logger.getLogger().printToChat(response.replace("&", "\u00a7"));
    }
    return true;
  }
}

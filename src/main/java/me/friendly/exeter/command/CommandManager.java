package me.friendly.exeter.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringJoiner;
import me.friendly.api.event.Listener;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.command.impl.client.Bind;
import me.friendly.exeter.command.impl.client.Friends;
import me.friendly.exeter.command.impl.client.Help;
import me.friendly.exeter.command.impl.client.Modules;
import me.friendly.exeter.command.impl.client.Prefix;
import me.friendly.exeter.command.impl.client.Presets;
import me.friendly.exeter.command.impl.client.Runtime;
import me.friendly.exeter.command.impl.client.ScreenShot;
import me.friendly.exeter.command.impl.client.Toggle;
import me.friendly.exeter.command.impl.client.Waypoints;
import me.friendly.exeter.command.impl.player.Grab;
import me.friendly.exeter.command.impl.player.HClip;
import me.friendly.exeter.command.impl.player.VClip;
import me.friendly.exeter.command.impl.server.Connect;
import me.friendly.exeter.command.impl.server.GearCommand;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

public final class CommandManager extends ListRegistry<Command> {
  private String prefix = ".";

  public CommandManager() {
    this.registry = new ArrayList();
    this.register(new Toggle());
    this.register(new Runtime());
    this.register(new Grab());
    this.register(new Help());
    this.register(new Modules());
    this.register(new Prefix());
    this.register(new Connect());
    this.register(new GearCommand());
    this.register(new Presets());
    this.register(new HClip());
    this.register(new VClip());
    this.register(new Friends.Add());
    this.register(new Friends.Remove());
    this.register(new Bind());
    this.register(new ScreenShot());
    this.register(new Waypoints.Add());
    this.register(new Waypoints.Here());
    this.register(new Waypoints.Remove());
    this.register(new Waypoints.List());
    this.registry.sort((cmd1, cmd2) -> cmd1.getAliases()[0].compareTo(cmd2.getAliases()[0]));
    Exeter.getInstance()
        .getEventManager()
        .register(
            new Listener<PacketEvent>("commands_packet_listener") {

              @Override
              public void call(PacketEvent event) {
                ServerboundChatPacket packet;
                String message;
                if (event.getPacket() instanceof ServerboundChatPacket
                    && (message =
                            (packet = (ServerboundChatPacket) event.getPacket()).message().trim())
                        .startsWith(getPrefix())) {
                  event.setCanceled(true);
                  boolean exists = false;
                  String[] arguments = message.split(" ");
                  if (message.length() < 1) {
                    Logger.getLogger().printToChat("No command was entered.");
                    return;
                  }
                  String execute = message.contains(" ") ? arguments[0] : message;
                  for (Command command : getRegistry()) {
                    for (String alias : command.getAliases()) {
                      if (!execute
                          .replace(getPrefix(), "")
                          .equalsIgnoreCase(alias.replaceAll(" ", ""))) continue;
                      exists = true;
                      try {
                        Logger.getLogger().printToChat(command.dispatch(arguments));
                      } catch (Exception e) {
                        Logger.getLogger()
                            .printToChat(
                                String.format("%s%s %s", getPrefix(), alias, command.getSyntax()));
                      }
                    }
                  }
                  String[] argz = message.split(" ");
                  for (Module mod : Exeter.getInstance().getModuleManager().getRegistry()) {
                    for (String alias : mod.getAliases()) {
                      try {
                        if (!argz[0].equalsIgnoreCase(getPrefix() + alias.replace(" ", "")))
                          continue;
                        exists = true;
                        if (argz.length > 1) {
                          String valueName = argz[1];
                          if (argz[1].equalsIgnoreCase("list")) {
                            if (mod.getProperties().size() > 0) {
                              StringJoiner stringJoiner = new StringJoiner(", ");
                              for (Property property : mod.getProperties()) {
                                stringJoiner.add(
                                    String.format(
                                        "%s&e[%s]&7",
                                        property.getAliases()[0],
                                        property.getValue() instanceof Enum
                                            ? ((EnumProperty) property).getFixedValue()
                                            : property.getValue()));
                              }
                              Logger.getLogger()
                                  .printToChat(
                                      String.format(
                                          "Properties (%s) %s.",
                                          mod.getProperties().size(), stringJoiner.toString()));
                              continue;
                            }
                            Logger.getLogger()
                                .printToChat(
                                    String.format("&e%s&7 has no properties.", mod.getLabel()));
                            continue;
                          }
                          Property property = mod.getPropertyByAlias(valueName);
                          if (property == null) continue;
                          if (property.getValue() instanceof Number) {
                            if (!argz[2].equalsIgnoreCase("get")) {
                              if (property.getValue() instanceof Double) {
                                property.setValue(Double.parseDouble(argz[2]));
                              }
                              if (property.getValue() instanceof Integer) {
                                property.setValue(Integer.parseInt(argz[2]));
                              }
                              if (property.getValue() instanceof Float) {
                                property.setValue(Float.valueOf(Float.parseFloat(argz[2])));
                              }
                              if (property.getValue() instanceof Long) {
                                property.setValue(Long.parseLong(argz[2]));
                              }
                              Logger.getLogger()
                                  .printToChat(
                                      String.format(
                                          "&e%s&7 has been set to &e%s&7 for &e%s&7.",
                                          property.getAliases()[0],
                                          property.getValue(),
                                          mod.getLabel()));
                              continue;
                            }
                            Logger.getLogger()
                                .printToChat(
                                    String.format(
                                        "&e%s&7 current value is &e%s&7 for &e%s&7.",
                                        property.getAliases()[0],
                                        property.getValue(),
                                        mod.getLabel()));
                            continue;
                          }
                          if (property.getValue() instanceof Enum) {
                            if (!argz[2].equalsIgnoreCase("list")) {
                              ((EnumProperty) property).setValue(argz[2]);
                              Logger.getLogger()
                                  .printToChat(
                                      String.format(
                                          "&e%s&7 has been set to &e%s&7 for &e%s&7.",
                                          property.getAliases()[0],
                                          ((EnumProperty) property).getFixedValue(),
                                          mod.getLabel()));
                              continue;
                            }
                            StringJoiner stringJoiner = new StringJoiner(", ");
                            Enum[] array =
                                (Enum[]) property.getValue().getClass().getEnumConstants();
                            int length = array.length;
                            for (int i = 0; i < length; ++i) {
                              stringJoiner.add(
                                  String.format(
                                      "%s%s&7",
                                      array[i]
                                              .name()
                                              .equalsIgnoreCase(property.getValue().toString())
                                          ? "&a"
                                          : "&c",
                                      getFixedValue(array[i])));
                            }
                            Logger.getLogger()
                                .printToChat(
                                    String.format(
                                        "Modes (%s) %s.", array.length, stringJoiner.toString()));
                            continue;
                          }
                          if (property.getValue() instanceof String) {
                            property.setValue(argz[2]);
                            Logger.getLogger()
                                .printToChat(
                                    String.format(
                                        "&e%s&7 has been set to &e\"%s\"&7 for &e%s&7.",
                                        property.getAliases()[0],
                                        property.getValue(),
                                        mod.getLabel()));
                            continue;
                          }
                          if (!(property.getValue() instanceof Boolean)) continue;
                          property.setValue((Boolean) property.getValue() == false);
                          Logger.getLogger()
                              .printToChat(
                                  String.format(
                                      "&e%s&7 has been %s&7 for &e%s&7.",
                                      property.getAliases()[0],
                                      (Boolean) property.getValue() != false
                                          ? "&aenabled"
                                          : "&cdisabled",
                                      mod.getLabel()));
                          continue;
                        }
                        Logger.getLogger()
                            .printToChat(
                                String.format("%s &e[list|valuename] [list|get]", argz[0]));
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    }
                  }
                  if (!exists) {
                    Logger.getLogger().printToChat("Invalid command entered.");
                  }
                }
              }
            });
    new Config("command_prefix.txt") {

      @Override
      public void load(Object... source) {
        try {
          if (!this.getFile().exists()) {
            this.getFile().createNewFile();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (!this.getFile().exists()) {
          return;
        }
        try {
          String readLine;
          BufferedReader br = new BufferedReader(new FileReader(this.getFile()));
          while ((readLine = br.readLine()) != null) {
            try {
              String[] split = readLine.split(":");
              prefix = split[0];
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          br.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void save(Object... destination) {
        try {
          if (!this.getFile().exists()) {
            this.getFile().createNewFile();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          BufferedWriter bw = new BufferedWriter(new FileWriter(this.getFile()));
          bw.write(prefix);
          bw.newLine();
          bw.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

  /**
   * Dispatches a command without requiring the chat prefix. Used by the Console window.
   *
   * @param input the raw command input (e.g. "toggle speed" instead of ".toggle speed")
   * @return the command output message
   */
  public String dispatchDirect(String input) {
    String trimmed = input.trim();
    if (trimmed.isEmpty()) return "No command entered.";

    String[] arguments = trimmed.split(" ");
    String execute = arguments[0];

    for (Command command : getRegistry()) {
      for (String alias : command.getAliases()) {
        if (!execute.equalsIgnoreCase(alias.replaceAll(" ", ""))) continue;
        try {
          return command.dispatch(arguments);
        } catch (Exception e) {
          return String.format("%s %s", alias, command.getSyntax());
        }
      }
    }

    for (Module mod : Exeter.getInstance().getModuleManager().getRegistry()) {
      for (String alias : mod.getAliases()) {
        if (!execute.equalsIgnoreCase(alias.replace(" ", ""))) continue;
        if (arguments.length > 1) {
          String valueName = arguments[1];
          if (arguments[1].equalsIgnoreCase("list")) {
            if (mod.getProperties().size() > 0) {
              StringJoiner stringJoiner = new StringJoiner(", ");
              for (Property property : mod.getProperties()) {
                stringJoiner.add(
                    String.format(
                        "%s [%s]",
                        property.getAliases()[0],
                        property.getValue() instanceof Enum
                            ? ((EnumProperty) property).getFixedValue()
                            : property.getValue()));
              }
              return String.format(
                  "Properties (%s) %s.", mod.getProperties().size(), stringJoiner.toString());
            }
            return String.format("%s has no properties.", mod.getLabel());
          }
          Property property = mod.getPropertyByAlias(valueName);
          if (property == null) return String.format("Property '%s' not found.", valueName);
          if (property.getValue() instanceof Number) {
            if (!arguments[2].equalsIgnoreCase("get")) {
              if (property.getValue() instanceof Double) {
                property.setValue(Double.parseDouble(arguments[2]));
              }
              if (property.getValue() instanceof Integer) {
                property.setValue(Integer.parseInt(arguments[2]));
              }
              if (property.getValue() instanceof Float) {
                property.setValue(Float.parseFloat(arguments[2]));
              }
              if (property.getValue() instanceof Long) {
                property.setValue(Long.parseLong(arguments[2]));
              }
              return String.format(
                  "%s has been set to %s for %s.",
                  property.getAliases()[0], property.getValue(), mod.getLabel());
            }
            return String.format(
                "%s current value is %s for %s.",
                property.getAliases()[0], property.getValue(), mod.getLabel());
          }
          if (property.getValue() instanceof Enum) {
            if (!arguments[2].equalsIgnoreCase("list")) {
              ((EnumProperty) property).setValue(arguments[2]);
              return String.format(
                  "%s has been set to %s for %s.",
                  property.getAliases()[0],
                  ((EnumProperty) property).getFixedValue(),
                  mod.getLabel());
            }
            StringJoiner stringJoiner = new StringJoiner(", ");
            Enum[] array = (Enum[]) property.getValue().getClass().getEnumConstants();
            for (Enum e : array) {
              stringJoiner.add(
                  String.format(
                      "%s%s",
                      e.name().equalsIgnoreCase(property.getValue().toString()) ? "" : "",
                      getFixedValue(e)));
            }
            return String.format("Modes (%s) %s.", array.length, stringJoiner.toString());
          }
          if (property.getValue() instanceof String) {
            property.setValue(arguments[2]);
            return String.format(
                "%s has been set to \"%s\" for %s.",
                property.getAliases()[0], property.getValue(), mod.getLabel());
          }
          if (property.getValue() instanceof Boolean) {
            property.setValue(!(Boolean) property.getValue());
            return String.format(
                "%s has been %s for %s.",
                property.getAliases()[0],
                (Boolean) property.getValue() ? "enabled" : "disabled",
                mod.getLabel());
          }
        }
        return String.format("%s [list|valuename] [list|get]", execute);
      }
    }

    return "Unknown command: " + execute;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  private String getFixedValue(Enum enumd) {
    return Character.toString(enumd.name().charAt(0))
        + enumd
            .name()
            .toLowerCase()
            .replace(Character.toString(enumd.name().charAt(0)).toLowerCase(), "");
  }
}

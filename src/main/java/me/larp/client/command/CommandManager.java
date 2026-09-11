package me.larp.client.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringJoiner;
import me.larp.api.event.Listener;
import me.larp.api.registry.ListRegistry;
import me.larp.client.command.impl.client.Bind;
import me.larp.client.command.impl.client.Friends;
import me.larp.client.command.impl.client.Help;
import me.larp.client.command.impl.client.Modules;
import me.larp.client.command.impl.client.Prefix;
import me.larp.client.command.impl.client.Presets;
import me.larp.client.command.impl.client.Profile;
import me.larp.client.command.impl.client.Runtime;
import me.larp.client.command.impl.client.ScreenShot;
import me.larp.client.command.impl.client.Spam;
import me.larp.client.command.impl.client.Toggle;
import me.larp.client.command.impl.client.Wp;
import me.larp.client.command.impl.player.Grab;
import me.larp.client.command.impl.player.HClip;
import me.larp.client.command.impl.player.VClip;
import me.larp.client.command.impl.server.Connect;
import me.larp.client.command.impl.server.GearCommand;
import me.larp.client.config.Config;
import me.larp.client.core.Larp;
import me.larp.client.events.PacketEvent;
import me.larp.client.logging.Logger;
import me.larp.client.module.Module;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.Property;
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
    this.register(new Profile());
    this.register(new Wp());
    this.register(new Spam());
    this.registry.sort((cmd1, cmd2) -> cmd1.getAliases()[0].compareTo(cmd2.getAliases()[0]));
    Larp.getInstance()
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
                  for (Module mod : Larp.getInstance().getModuleManager().getRegistry()) {
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

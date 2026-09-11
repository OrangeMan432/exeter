package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.client.events.PacketEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.Property;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

/** Appends a Larp signature to your public chat messages. */
public class ChatSuffix extends ToggleableModule {

  public enum Suffix {
    LARP,
    LARP_CLIENT,
    NONE
  }

  private final EnumProperty<Suffix> suffix =
      new EnumProperty<Suffix>(Suffix.LARP, "Suffix");
  private final Property<Boolean> greenText =
      new Property<Boolean>(false, "Green Text");

  public ChatSuffix() {
    super("ChatSuffix", new String[] {"chatsuffix", "suffix"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Signs your chat messages.");
    offerProperties(suffix, greenText);
    this.listeners.add(
        new Listener<PacketEvent>("chatsuffix_packet") {
          @Override
          public void call(PacketEvent event) {
            ChatSuffix.this.onPacket(event);
          }
        });
  }

  private void onPacket(PacketEvent event) {
    if (!(event.getPacket() instanceof ServerboundChatPacket chat)) return;
    if (minecraft.getConnection() == null) return;
    String original = extractMessage(chat);
    if (original == null || original.startsWith("/") || original.startsWith(".")) return;
    String tag =
        switch (suffix.getValue()) {
          case LARP -> " | Larp";
          case LARP_CLIENT -> " | Larp Client";
          case NONE -> "";
        };
    String message = (greenText.getValue() ? "> " : "") + original + tag;
    if (message.length() > 256) {
      message = message.substring(0, 256);
    }
    event.setCanceled(true);
    minecraft.getConnection().sendChat(message);
  }

  private String extractMessage(ServerboundChatPacket packet) {
    try {
      return packet.message();
    } catch (Exception e) {
      return null;
    }
  }
}

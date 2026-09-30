package me.friendly.exeter.account.auth;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.logging.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.Session;

/** Swaps the active session. Offline only: username with a dummy session id. */
public final class SessionManager {
  private static final String TAG = "AccountManager";

  private SessionManager() {}

  public static String currentName() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.session == null) {
      return "?";
    }
    return mc.session.username;
  }

  public static void setOffline(String username) {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null) {
      return;
    }
    mc.session = new Session(username, "-");
    DebugLogger.get().log(TAG, DebugLogger.Level.INFO, "Offline login " + username);
  }
}

package me.friendly.exeter.account.auth;

import java.util.Optional;
import java.util.UUID;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.mixin.MixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * Swaps the active Minecraft session.
 *
 * <p>Ported from OpenMyau's {@code me.ksyz.accountmanager.auth.SessionManager} (derived from
 * https://github.com/ksyzov/AccountManager, originally LGPL, modified version GPL v3). The
 * reflection scan for a {@code Session} field was replaced with a Fabric {@code @Accessor} mixin,
 * since modern Minecraft stores an immutable {@code User} instead.
 */
public final class SessionManager {
  private static final String TAG = "AccountManager";

  private SessionManager() {}

  public static User get() {
    return Minecraft.getInstance().getUser();
  }

  public static void set(String username, UUID uuid, String accessToken) {
    User user = new User(username, uuid, accessToken, Optional.empty(), Optional.empty());
    ((MixinMinecraft) (Object) Minecraft.getInstance()).exeter$setUser(user);
    DebugLogger.get().logFile(TAG, "Session set to " + username);
  }

  public static void set(MicrosoftAuth.MinecraftProfile profile, String accessToken) {
    set(profile.username(), profile.uuid(), accessToken);
  }

  /** Swaps to an offline (cracked) session with a deterministic offline-mode UUID. */
  public static void setOffline(String username) {
    UUID uuid =
        UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    set(username, uuid, "");
  }
}

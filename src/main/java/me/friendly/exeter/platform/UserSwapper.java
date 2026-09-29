package me.friendly.exeter.platform;

import net.minecraft.client.User;

/**
 * Replaces the active game session.
 *
 * <p>Modern Minecraft stores its {@link User} in a final field, so swapping sessions means editing
 * game code. On Java Edition that is done with a mixin; on Eaglercraft there is no mixin runtime,
 * and Eaglercraft runs offline sessions anyway, so the default is a no-op.
 *
 * <p>Kept as a seam so {@link me.friendly.exeter.account.auth.SessionManager} stays portable: a
 * host that can support session swapping installs a real implementation during bootstrap.
 */
@FunctionalInterface
public interface UserSwapper {

  /** Swaps in {@code user} as the active session. */
  void setUser(User user);

  /** Discards the active session. */
  default void clear() {}

  /** No-op swapper used when the host cannot (or does not need to) replace the session. */
  UserSwapper NOOP = user -> {};
}

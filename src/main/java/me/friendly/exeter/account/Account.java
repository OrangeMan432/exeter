package me.friendly.exeter.account;

/**
 * A stored Minecraft account.
 *
 * <p>Ported from OpenMyau's {@code me.ksyz.accountmanager.auth.Account} (derived from
 * https://github.com/ksyzov/AccountManager, originally LGPL, modified version GPL v3). Adds a
 * {@code uuid} field, which modern Minecraft needs to build its {@code User} session object, and a
 * {@code type} marking how the account logs in.
 */
public class Account {
  /** How this account authenticates. */
  public enum Type {
    /** Full Microsoft OAuth account with refreshable tokens. */
    MICROSOFT,
    /** A raw Minecraft session token with no refresh path. */
    SESSION,
    /** Cracked/offline account, no network auth involved. */
    OFFLINE
  }

  private String refreshToken;
  private String accessToken;
  private String username;
  private String uuid;
  private long unban;
  private long expiresAt;
  private boolean tokenDead;
  private transient String lastAuthError;
  private String clientId;
  private String scope;
  private Type type;

  public Account(
      String refreshToken,
      String accessToken,
      String username,
      String uuid,
      long unban,
      String clientId,
      String scope,
      Type type) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.username = username;
    this.uuid = uuid;
    this.unban = unban;
    this.clientId = clientId;
    this.scope = scope;
    this.type = type == null ? Type.MICROSOFT : type;
  }

  public String getClientId() {
    return clientId;
  }

  public String getScope() {
    return scope;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getUsername() {
    return username;
  }

  public String getUuid() {
    return uuid;
  }

  public long getUnban() {
    return unban;
  }

  /** Epoch millis when the access token expires, 0 when unknown. */
  public long getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(long expiresAt) {
    this.expiresAt = expiresAt;
  }

  /** Offline accounts never expire; unknown expiries are treated as valid. */
  public boolean isTokenExpired() {
    if (type == Type.OFFLINE || expiresAt <= 0) {
      return false;
    }
    return System.currentTimeMillis() > expiresAt;
  }

  /** True once a login or validation attempt has proven the token dead. */
  public boolean isTokenDead() {
    return tokenDead;
  }

  public void setTokenDead(boolean tokenDead) {
    this.tokenDead = tokenDead;
  }

  /** Last auth attempt failure, cleared on success. Session-only, never persisted. */
  public String getLastAuthError() {
    return lastAuthError;
  }

  public void setLastAuthError(String lastAuthError) {
    this.lastAuthError = lastAuthError;
  }

  public Type getType() {
    return type;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public void setUnban(long unban) {
    this.unban = unban;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public void setType(Type type) {
    this.type = type == null ? Type.MICROSOFT : type;
  }

  /** Short colored tag shown next to the username in the account list. */
  public String getTypeTag() {
    return switch (type) {
      case MICROSOFT -> "§9[MS]";
      case SESSION -> "§e[Token]";
      case OFFLINE -> "§7[Offline]";
    };
  }
}

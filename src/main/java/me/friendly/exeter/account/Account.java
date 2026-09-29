package me.friendly.exeter.account;

/**
 * A stored offline username.
 *
 * <p>Originally this held Microsoft OAuth credentials, a Minecraft access/refresh token, a client
 * id and a scope, and could represent three account types. Those flows all require outbound HTTPS
 * to login.microsoftonline.com and Mojang endpoints, which a browser build cannot perform: those
 * APIs send no CORS headers, so the requests are blocked before they leave the page. Only the
 * offline (cracked) form is reachable from a browser, so this is now a name and the UUID derived
 * from it.
 *
 * <p>The {@code unban} timestamp survives because it is filled in from server disconnect packets,
 * not from any auth call.
 */
public class Account {

  private String username;
  private String uuid;
  private long unban;

  public Account(String username, String uuid, long unban) {
    this.username = username;
    this.uuid = uuid;
    this.unban = unban;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public long getUnban() {
    return unban;
  }

  public void setUnban(long unban) {
    this.unban = unban;
  }
}

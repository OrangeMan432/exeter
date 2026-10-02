package me.friendly.exeter.proxy;

public class ProxyEntry {
  public enum Type {
    SOCKS5,
    HTTP
  }

  private final String name;
  private final String host;
  private final int port;
  private final Type type;
  private final String username;
  private final String password;

  public ProxyEntry(
      String name, String host, int port, Type type, String username, String password) {
    this.name = name;
    this.host = host;
    this.port = port;
    this.type = type;
    this.username = username == null ? "" : username;
    this.password = password == null ? "" : password;
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public Type getType() {
    return type;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public boolean hasAuth() {
    return !username.isEmpty();
  }

  public java.net.InetSocketAddress address() {
    return new java.net.InetSocketAddress(host, port);
  }

  /** Parses "host:port", "[user:pass@]host:port" or "name=..." prefixed variants. */
  public static ProxyEntry parse(String input, Type type) {
    String text = input.trim();
    String name = text;
    String username = "";
    String password = "";
    String hostPort = text;
    int eq = text.indexOf('=');
    if (eq > 0) {
      name = text.substring(0, eq).trim();
      hostPort = text.substring(eq + 1).trim();
    }
    int at = hostPort.lastIndexOf('@');
    if (at > 0) {
      String userinfo = hostPort.substring(0, at);
      hostPort = hostPort.substring(at + 1);
      int colon = userinfo.indexOf(':');
      if (colon >= 0) {
        username = userinfo.substring(0, colon);
        password = userinfo.substring(colon + 1);
      } else {
        username = userinfo;
      }
    }
    int colon = hostPort.lastIndexOf(':');
    if (colon < 0) {
      return null;
    }
    String host = hostPort.substring(0, colon).trim();
    int port;
    try {
      port = Integer.parseInt(hostPort.substring(colon + 1).trim());
    } catch (NumberFormatException e) {
      return null;
    }
    if (host.isEmpty() || port <= 0 || port > 65535) {
      return null;
    }
    if (name.equals(text)) {
      name = host + ":" + port;
    }
    return new ProxyEntry(name, host, port, type, username, password);
  }
}

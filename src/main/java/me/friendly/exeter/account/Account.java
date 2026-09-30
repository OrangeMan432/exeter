package me.friendly.exeter.account;

import me.friendly.api.interfaces.Labeled;

/** A stored offline account. Beta has no UUIDs; the username is the identity. */
public class Account implements Labeled {
  private String username;

  public Account(String username) {
    this.username = username;
  }

  @Override
  public String getLabel() {
    return username;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}

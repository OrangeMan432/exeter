package me.friendly.exeter.friend;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.core.Exeter;

/** a ListRegistry registered with Friend, that handles Exeter's friend system */
public final class FriendManager extends ListRegistry<Friend> {

  private final Config config;
  private boolean overrideAttack = false;

  /**
   * Creates a new Config instance, and overrides load, and save methods, to setup the friend
   * config.
   */
  public FriendManager() {
    this.registry = new ArrayList();
    this.config =
        new Config("friends.json") {

          @Override
          public void load(Object... source) {
            JsonElement root;
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
            try (FileReader reader = new FileReader(this.getFile()); ) {
              root = new JsonParser().parse((Reader) reader);
            } catch (IOException e) {
              e.printStackTrace();
              return;
            }
            if (!(root instanceof JsonArray) && !(root instanceof JsonObject)) {
              return;
            }
            JsonArray friends;
            if (root instanceof JsonObject object) {
              FriendManager.this.overrideAttack =
                  object.has("overrideAttack") && object.get("overrideAttack").getAsBoolean();
              friends =
                  object.has("friends") && object.get("friends") instanceof JsonArray array
                      ? array
                      : new JsonArray();
            } else {
              friends = (JsonArray) root;
            }
            friends.forEach(
                node -> {
                  if (!(node instanceof JsonObject)) {
                    return;
                  }
                  try {
                    JsonObject friendNode = (JsonObject) node;
                    Exeter.getInstance()
                        .getFriendManager()
                        .getRegistry()
                        .add(
                            new Friend(
                                friendNode.get("friend-label").getAsString(),
                                friendNode.get("friend-alias").getAsString()));
                  } catch (Throwable e) {
                    e.printStackTrace();
                  }
                });
          }

          @Override
          public void save(Object... destination) {
            if (this.getFile().exists()) {
              this.getFile().delete();
            }
            JsonArray friends = new JsonArray();
            Exeter.getInstance()
                .getFriendManager()
                .getRegistry()
                .forEach(
                    friend -> {
                      try {
                        JsonObject friendObject;
                        JsonObject properties = friendObject = new JsonObject();
                        properties.addProperty("friend-label", friend.getLabel());
                        properties.addProperty("friend-alias", friend.getAlias());
                        friends.add((JsonElement) properties);
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    });
            try (FileWriter writer = new FileWriter(this.getFile()); ) {
              JsonObject root = new JsonObject();
              root.add("friends", friends);
              root.addProperty(
                  "overrideAttack", Exeter.getInstance().getFriendManager().isOverride());
              writer.write(
                  new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement) root));
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
  }

  /**
   * Searches friend registry, for matches to aliasOrLabel param. If a match is found it returns
   * that friend instance, else, null it returned.
   *
   * @param aliasOrLabel the alias, or label to search for
   * @return Friend, or null if no matches ar found
   */
  public Friend getFriendByAliasOrLabel(String aliasOrLabel) {
    for (Friend friend : registry) {
      if (!aliasOrLabel.equalsIgnoreCase(friend.getLabel())
          && !aliasOrLabel.equalsIgnoreCase(friend.getAlias())) continue;
      return friend;
    }
    return null;
  }

  /**
   * Searches friend registry, for matches to aliasOrLabel param. If a match is found true is
   * returned else, false it returned.
   *
   * @param aliasOrLabel the alias, or label to search for
   * @return true, or false
   */
  public boolean isFriend(String aliasOrLabel) {
    for (Friend friend : registry) {
      if (!aliasOrLabel.equalsIgnoreCase(friend.getLabel())
          && !aliasOrLabel.equalsIgnoreCase(friend.getAlias())) continue;
      return true;
    }
    return false;
  }

  /** Persists the registry immediately, so window and command edits survive crashes. */
  public void save() {
    config.save(new Object[0]);
  }

  /** When true, combat modules treat friends as valid targets. */
  public boolean isOverride() {
    return overrideAttack;
  }

  public void setOverride(boolean override) {
    this.overrideAttack = override;
    save();
  }

  /**
   * True when the given player may be targeted: anyone when override is on, non-friends otherwise.
   */
  public boolean isTargetable(String aliasOrLabel) {
    return overrideAttack || !isFriend(aliasOrLabel);
  }
}

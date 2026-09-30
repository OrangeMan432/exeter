package me.friendly.exeter.friend;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;

/** a ListRegistry registered with Friend, that handles Exeter's friend system */
public final class FriendManager extends ListRegistry<Friend> {

  private final Config config;
  private boolean overrideAttack = false;

  /**
   * Creates a new Config instance, and overrides load, and save methods, to setup the friend
   * config.
   */
  public FriendManager() {
    this.registry = new ArrayList<Friend>();
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
              return;
            }
            try {
              FileReader reader = new FileReader(this.getFile());
              try {
                root = JsonParser.parseReader(reader);
              } finally {
                reader.close();
              }
            } catch (Exception e) {
              e.printStackTrace();
              return;
            }
            if (!(root instanceof JsonArray) && !(root instanceof JsonObject)) {
              return;
            }
            JsonArray friends;
            if (root instanceof JsonObject) {
              JsonObject object = (JsonObject) root;
              JsonElement flag = object.get("overrideAttack");
              FriendManager.this.overrideAttack =
                  flag != null && !flag.isJsonNull() && flag.getAsBoolean();
              JsonElement list = object.get("friends");
              if (!(list instanceof JsonArray)) {
                return;
              }
              friends = (JsonArray) list;
            } else {
              friends = (JsonArray) root;
            }
            for (int i = 0; i < friends.size(); i++) {
              JsonElement node = friends.get(i);
              if (!(node instanceof JsonObject)) {
                continue;
              }
              try {
                JsonObject friendNode = (JsonObject) node;
                FriendManager.this.register(
                    new Friend(
                        friendNode.get("friend-label").getAsString(),
                        friendNode.get("friend-alias").getAsString()));
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }

          @Override
          public void save(Object... destination) {
            if (this.getFile().exists()) {
              this.getFile().delete();
            }
            JsonArray friends = new JsonArray();
            for (Friend friend : FriendManager.this.getRegistry()) {
              try {
                JsonObject properties = new JsonObject();
                properties.addProperty("friend-label", friend.getLabel());
                properties.addProperty("friend-alias", friend.getAlias());
                friends.add(properties);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            try {
              FileWriter writer = new FileWriter(this.getFile());
              try {
                JsonObject root = new JsonObject();
                root.add("friends", friends);
                root.addProperty(
                    "overrideAttack", FriendManager.this.overrideAttack);
                writer.write(
                    new GsonBuilder().setPrettyPrinting().create().toJson(root));
              } finally {
                writer.close();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
  }

  /** Searches friend registry, for matches to aliasOrLabel param. */
  public Friend getFriendByAliasOrLabel(String aliasOrLabel) {
    for (Friend friend : registry) {
      if (!aliasOrLabel.equalsIgnoreCase(friend.getLabel())
          && !aliasOrLabel.equalsIgnoreCase(friend.getAlias())) continue;
      return friend;
    }
    return null;
  }

  /** Returns true when aliasOrLabel matches a friend's alias or label. */
  public boolean isFriend(String aliasOrLabel) {
    for (Friend friend : registry) {
      if (!aliasOrLabel.equalsIgnoreCase(friend.getLabel())
          && !aliasOrLabel.equalsIgnoreCase(friend.getAlias())) continue;
      return true;
    }
    return false;
  }

  /** Persists the registry immediately, so edits survive crashes. */
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
   * True when the given player may be targeted: anyone when override is on, non-friends
   * otherwise.
   */
  public boolean isTargetable(String aliasOrLabel) {
    return overrideAttack || !isFriend(aliasOrLabel);
  }
}

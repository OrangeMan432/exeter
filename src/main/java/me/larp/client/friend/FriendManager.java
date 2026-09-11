package me.larp.client.friend;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.util.ArrayList;
import me.larp.api.registry.ListRegistry;
import me.larp.client.config.Config;
import me.larp.client.core.Larp;

/** a ListRegistry registered with Friend, that handles Larp's friend system */
public final class FriendManager extends ListRegistry<Friend> {

  /**
   * Creates a new Config instance, and overrides load, and save methods, to setup the friend
   * config.
   */
  public FriendManager() {
    this.registry = new ArrayList();
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
        if (!(root instanceof JsonArray)) {
          return;
        }
        JsonArray friends = (JsonArray) root;
        friends.forEach(
            node -> {
              if (!(node instanceof JsonObject)) {
                return;
              }
              try {
                JsonObject friendNode = (JsonObject) node;
                Larp.getInstance()
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
        if (Larp.getInstance().getFriendManager().getRegistry().isEmpty()) {
          return;
        }
        JsonArray friends = new JsonArray();
        Larp.getInstance()
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
          writer.write(
              new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement) friends));
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
}

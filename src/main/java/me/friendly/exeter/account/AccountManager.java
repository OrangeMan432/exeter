package me.friendly.exeter.account;

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

/** Stores offline accounts in accounts.json. No tokens on beta; Betacraft auth may come later. */
public final class AccountManager extends ListRegistry<Account> {

  private final Config config;

  public AccountManager() {
    this.registry = new ArrayList<Account>();
    this.config =
        new Config("accounts.json") {

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
            if (!(root instanceof JsonArray)) {
              return;
            }
            AccountManager.this.getRegistry().clear();
            JsonArray accounts = (JsonArray) root;
            for (int i = 0; i < accounts.size(); i++) {
              JsonElement element = accounts.get(i);
              if (!(element instanceof JsonObject)) {
                continue;
              }
              try {
                JsonObject object = (JsonObject) element;
                JsonElement name = object.get("username");
                if (name == null || name.isJsonNull()) {
                  continue;
                }
                AccountManager.this.register(new Account(name.getAsString()));
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
            JsonArray accounts = new JsonArray();
            for (Account account : AccountManager.this.getRegistry()) {
              JsonObject object = new JsonObject();
              object.addProperty("username", account.getUsername());
              accounts.add(object);
            }
            try {
              FileWriter writer = new FileWriter(this.getFile());
              try {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(accounts));
              } finally {
                writer.close();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
  }

  public void save() {
    config.save(new Object[0]);
  }
}

package me.larp.client.command.impl.client;

import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.friend.Friend;

public final class Friends {

  public static final class Remove extends Command {
    public Remove() {
      super(new String[] {"remove", "rem"}, new Argument("username/alias"));
    }

    @Override
    public String dispatch() {
      String name = this.getArgument("username/alias").getValue();
      if (!Larp.getInstance().getFriendManager().isFriend(name)) {
        return "That user is not a friend.";
      }
      Friend friend = Larp.getInstance().getFriendManager().getFriendByAliasOrLabel(name);
      String oldAlias = friend.getAlias();
      Larp.getInstance().getFriendManager().unregister(friend);
      return String.format("Removed friend with alias %s.", oldAlias);
    }
  }

  public static final class Add extends Command {
    public Add() {
      super(new String[] {"add", "a"}, new Argument("username"), new Argument("alias"));
    }

    @Override
    public String dispatch() {
      String username = this.getArgument("username").getValue();
      String alias = this.getArgument("alias").getValue();
      if (Larp.getInstance().getFriendManager().isFriend(username)) {
        return "That user is already a friend.";
      }
      Larp.getInstance().getFriendManager().register(new Friend(username, alias));
      return String.format("Added friend with alias %s.", alias);
    }
  }
}

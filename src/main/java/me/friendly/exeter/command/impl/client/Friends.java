package me.friendly.exeter.command.impl.client;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.friend.Friend;

public final class Friends {

  public static final class Remove extends Command {
    public Remove() {
      super(new String[] {"remove", "rem", "unfriend", "del"}, new Argument("username/alias"));
      setDescription("Remove a friend");
    }

    @Override
    public String dispatch() {
      String name = this.getArgument("username/alias").getValue();
      if (!Exeter.getInstance().getFriendManager().isFriend(name)) {
        return "That user is not a friend.";
      }
      Friend friend = Exeter.getInstance().getFriendManager().getFriendByAliasOrLabel(name);
      String oldAlias = friend.getAlias();
      Exeter.getInstance().getFriendManager().unregister(friend);
      Exeter.getInstance().getFriendManager().save();
      return "Removed friend with alias " + oldAlias + ".";
    }
  }

  public static final class Add extends Command {
    public Add() {
      super(
          new String[] {"add", "a", "friend"},
          new Argument("username"),
          new Argument("alias"));
      setDescription("Add a friend");
    }

    @Override
    public String dispatch() {
      String username = this.getArgument("username").getValue();
      String alias = this.getArgument("alias").getValue();
      if (Exeter.getInstance().getFriendManager().isFriend(username)) {
        return "That user is already a friend.";
      }
      Exeter.getInstance().getFriendManager().register(new Friend(username, alias));
      Exeter.getInstance().getFriendManager().save();
      return "Added friend with alias " + alias + ".";
    }
  }
}

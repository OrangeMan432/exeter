package me.larp.api.event.filter;

import me.larp.api.event.Event;
import me.larp.api.event.Listener;

public interface Filter {
  boolean filter(Listener var1, Event var2);
}

package me.larp.client;

import me.larp.client.core.Larp;
import net.fabricmc.api.ModInitializer;

public class LarpInitializer implements ModInitializer {

  @Override
  public void onInitialize() {
    new Larp();
  }
}

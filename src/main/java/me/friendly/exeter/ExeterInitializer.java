package me.friendly.exeter;

import me.friendly.exeter.core.Exeter;
import net.fabricmc.api.ModInitializer;

public class ExeterInitializer implements ModInitializer {

  @Override
  public void onInitialize() {
    new Exeter();
  }
}

package me.friendly.exeter.beta;

import me.friendly.exeter.core.Exeter;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Exeter client entrypoint for Beta 1.7.3 (Babric). */
public class ExeterBeta implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("exeter");

  @Override
  public void onInitialize() {
    LOGGER.info("Exeter beta port initializing.");
    new Exeter();
  }
}

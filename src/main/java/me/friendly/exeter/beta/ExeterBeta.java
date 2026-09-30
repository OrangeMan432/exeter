package me.friendly.exeter.beta;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Exeter client entrypoint for Beta 1.7.3 (Babric). Framework port in progress. */
public class ExeterBeta implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("exeter");

  @Override
  public void onInitialize() {
    LOGGER.info("Exeter beta port initializing.");
  }
}

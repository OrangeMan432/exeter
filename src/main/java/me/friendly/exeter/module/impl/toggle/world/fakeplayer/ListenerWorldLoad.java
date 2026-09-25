package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import net.minecraft.client.Minecraft;

public class ListenerWorldLoad extends Listener<TickEvent> {
  private static final Minecraft mc = Minecraft.getInstance();
  private final FakePlayerModule module;
  private boolean wasInWorld;

  public ListenerWorldLoad(FakePlayerModule module) {
    super("fakeplayer_worldload");
    this.module = module;
  }

  @Override
  public void call(TickEvent event) {
    if (event.getStage() != Stage.POST) return;

    boolean inWorld = mc.level != null && mc.player != null;

    if (wasInWorld && !inWorld) {
      module.getPositions().clear();
      module.setIndex(0);
      module.setPlayingRecording(false);
    }

    wasInWorld = inWorld;
  }
}

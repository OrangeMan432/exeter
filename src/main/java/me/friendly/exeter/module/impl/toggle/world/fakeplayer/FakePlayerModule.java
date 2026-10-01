package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.Position;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FakePlayerModule extends ToggleableModule {

  private final Property<Boolean> record = new Property<Boolean>(false, "Record", "record");
  private final Property<Boolean> playRecording =
      new Property<Boolean>(false, "Play Recording", "play");
  private final Property<Boolean> loop = new Property<Boolean>(false, "Loop", "loop");
  private final Property<Boolean> damage = new Property<Boolean>(true, "Damage", "damage");
  private final Property<Boolean> copyArmor =
      new Property<Boolean>(true, "Copy Armor", "copyarmor");
  private final NumberProperty<Integer> respawnDelay =
      new NumberProperty<Integer>(2, 1, 10, "Respawn Delay", "respawndelay");

  private FakePlayerEntity fakePlayer;
  private final List<Position> positions = new ArrayList<Position>();
  private int index;
  private long diedAt;
  private boolean pendingRespawn;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("fakeplayer_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          FakePlayerModule.this.onTick();
        }
      };

  public FakePlayerModule() {
    super("FakePlayer", new String[] {"fakeplayer", "fp", "bot"}, 0xFFAA00, ModuleType.WORLD);
    setDescription("Spawns a fake player for testing combat and movement.");
    offerProperties(record, playRecording, loop, damage, copyArmor, respawnDelay);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    spawnFakePlayer();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    pendingRespawn = false;
    removeFakePlayer();
    positions.clear();
    index = 0;
    record.setValue(Boolean.valueOf(false));
    playRecording.setValue(Boolean.valueOf(false));
  }

  @Override
  public String getTag() {
    if (record.getValue().booleanValue()) return "Recording";
    if (playRecording.getValue().booleanValue()) return "Playing";
    return null;
  }

  public FakePlayerEntity getFakePlayer() {
    return fakePlayer;
  }

  public boolean isDamageEnabled() {
    return damage.getValue().booleanValue();
  }

  private void spawnFakePlayer() {
    if (minecraft() == null || minecraft().world == null || minecraft().player == null) {
      setRunning(false);
      return;
    }
    fakePlayer = new FakePlayerEntity(minecraft().world);
    int entityId = -1000;
    fakePlayer.id = entityId;
    PlayerEntity player = minecraft().player;
    fakePlayer.method_1338(player.x, player.y, player.z, player.yaw, player.pitch);
    fakePlayer.health = player.health;
    if (copyArmor.getValue().booleanValue()) {
      for (int i = 0; i < 4 && i < player.inventory.armor.length; i++) {
        ItemStack stack = player.inventory.armor[i];
        fakePlayer.inventory.armor[i] = stack;
      }
    }
    minecraft().world.method_184(fakePlayer);
    pendingRespawn = false;
  }

  private void removeFakePlayer() {
    if (fakePlayer != null) {
      fakePlayer.markDead();
      fakePlayer = null;
    }
  }

  private void onTick() {
    if (minecraft() == null || minecraft().world == null || minecraft().player == null) {
      return;
    }
    if (pendingRespawn) {
      long waitMs = respawnDelay.getValue().intValue() * 1000L;
      if (System.currentTimeMillis() - diedAt >= waitMs) {
        spawnFakePlayer();
      }
      return;
    }
    if (fakePlayer == null) {
      return;
    }
    if (fakePlayer.dead) {
      pendingRespawn = true;
      diedAt = System.currentTimeMillis();
      removeFakePlayer();
      return;
    }
    if (record.getValue().booleanValue()) {
      PlayerEntity player = minecraft().player;
      positions.add(new Position(player.x, player.y, player.z, player.yaw, player.pitch));
      return;
    }
    if (playRecording.getValue().booleanValue()) {
      if (positions.isEmpty()) {
        playRecording.setValue(Boolean.valueOf(false));
        return;
      }
      if (index >= positions.size()) {
        if (loop.getValue().booleanValue()) {
          index = 0;
        } else {
          playRecording.setValue(Boolean.valueOf(false));
          index = 0;
          return;
        }
      }
      Position pos = positions.get(index);
      fakePlayer.method_1338(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
      index++;
    }
  }
}

package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.Position;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.StopWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FakePlayerModule extends ToggleableModule {
  private static final Minecraft mc = Minecraft.getInstance();

  private final Property<Boolean> record = new Property<>(false, "Record");
  private final Property<Boolean> playRecording = new Property<>(false, "Play Recording");
  private final Property<Boolean> loop = new Property<>(false, "Loop");
  private final Property<Boolean> gapple = new Property<>(true, "Gapple");
  private final NumberProperty<Integer> gappleDelay =
      new NumberProperty<>(1600, 1500, 2000, "Gapple Delay");
  private final Property<Boolean> damage = new Property<>(true, "Damage");
  private final Property<Boolean> copyArmor = new Property<>(true, "Copy Armor");
  private final NumberProperty<Integer> respawnDelay =
      new NumberProperty<>(2, 1, 10, "Respawn Delay");

  private FakePlayerEntity fakePlayer;
  private final List<Position> positions = new ArrayList<>();
  private final StopWatch timer = new StopWatch();
  private final StopWatch respawnTimer = new StopWatch();
  private int index;
  private boolean pendingRespawn;

  public FakePlayerModule() {
    super("FakePlayer", new String[] {"fakeplayer", "fp", "bot"}, 0xFFAA00, ModuleType.WORLD);
    setDescription("Spawns a fake player for testing combat and movement.");

    offerProperties(
        record, playRecording, loop, gapple, gappleDelay, damage, copyArmor, respawnDelay);
    record.setDescription("Record your movement for playback.");
    playRecording.setDescription("Play back the recorded movement.");
    loop.setDescription("Loop the recorded movement.");
    gapple.setDescription("Apply golden apple effects to the fake player.");
    gappleDelay.setDescription("Delay between golden apple effects in milliseconds.");
    damage.setDescription("Let explosions and fireworks hurt the fake player.");
    copyArmor.setDescription("Copy your armor onto the fake player.");
    respawnDelay.setDescription("Seconds before respawning after death.");

    listeners.add(new ListenerTick(this));
    listeners.add(new ListenerAttack(this));
    listeners.add(new ListenerExplosion(this));
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
    record.setValue(false);
    playRecording.setValue(false);
  }

  @Override
  public String getTag() {
    if (record.getValue()) return "Recording";
    if (playRecording.getValue()) return "Playing";
    return null;
  }

  private void spawnFakePlayer() {
    if (mc.level == null || mc.player == null) {
      setRunning(false);
      return;
    }

    GameProfile profile = new GameProfile(UUID.randomUUID(), "FakePlayer");
    fakePlayer = new FakePlayerEntity(mc.level, profile);

    int entityId = -1000;
    while (mc.level.getEntity(entityId) != null) {
      entityId = ThreadLocalRandom.current().nextInt(-100000, -100);
    }
    fakePlayer.setId(entityId);

    fakePlayer.snapTo(
        mc.player.getX(),
        mc.player.getY(),
        mc.player.getZ(),
        mc.player.getYRot(),
        mc.player.getXRot());
    fakePlayer.setHealth(mc.player.getHealth());
    fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());

    fakePlayer.setItemSlot(
        EquipmentSlot.MAINHAND, mc.player.getItemBySlot(EquipmentSlot.MAINHAND).copy());

    if (copyArmor.getValue()) {
      fakePlayer.setItemSlot(
          EquipmentSlot.HEAD, mc.player.getItemBySlot(EquipmentSlot.HEAD).copy());
      fakePlayer.setItemSlot(
          EquipmentSlot.CHEST, mc.player.getItemBySlot(EquipmentSlot.CHEST).copy());
      fakePlayer.setItemSlot(
          EquipmentSlot.LEGS, mc.player.getItemBySlot(EquipmentSlot.LEGS).copy());
      fakePlayer.setItemSlot(
          EquipmentSlot.FEET, mc.player.getItemBySlot(EquipmentSlot.FEET).copy());
    }

    ItemStack playerOffhand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
    if (!playerOffhand.isEmpty()) {
      fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, playerOffhand.copy());
    } else {
      fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
    }

    mc.level.addEntity(fakePlayer);

    index = 0;
    timer.reset();
    pendingRespawn = false;

    mc.player.sendSystemMessage(Component.literal("[FakePlayer] Spawned fake player."));
  }

  public void checkRespawn() {
    if (!pendingRespawn || fakePlayer != null) return;
    if (respawnTimer.hasPassed((long) respawnDelay.getValue() * 1000)) {
      spawnFakePlayer();
    }
  }

  public void onFakePlayerDied() {
    removeFakePlayer();
    pendingRespawn = true;
    respawnTimer.reset();
  }

  private void removeFakePlayer() {
    if (fakePlayer == null) return;
    Entity entity = fakePlayer;
    mc.execute(
        () -> {
          entity.setRemoved(Entity.RemovalReason.DISCARDED);
          if (mc.level != null) {
            mc.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
          }
        });
    fakePlayer = null;
  }

  public FakePlayerEntity getFakePlayer() {
    return fakePlayer;
  }

  public List<Position> getPositions() {
    return positions;
  }

  public StopWatch getTimer() {
    return timer;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public boolean isRecording() {
    return record.getValue();
  }

  public boolean isPlayingRecording() {
    return playRecording.getValue();
  }

  public void setPlayingRecording(boolean playing) {
    playRecording.setValue(playing);
  }

  public boolean isLooping() {
    return loop.getValue();
  }

  public boolean isGappleEnabled() {
    return gapple.getValue();
  }

  public int getGappleDelay() {
    return gappleDelay.getValue();
  }

  public boolean isDamageEnabled() {
    return damage.getValue();
  }
}

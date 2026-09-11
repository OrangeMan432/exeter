package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Shoreline-pattern surround: center on enable, attack crystals blocking slots,
 * jump-disable, instant re-place on explosion sound.
 */
public class Surround extends ToggleableModule {

  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> center = new Property<Boolean>(true, "Center");
  private final Property<Boolean> attack = new Property<Boolean>(true, "Attack");
  private final Property<Boolean> jumpDisable =
      new Property<Boolean>(true, "Jump Disable");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(4, 1, 8, "Blocks Per Tick");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private static final int[][] OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

  private final List<BlockPos> surround = new ArrayList<>();
  private double prevY;

  public Surround() {
    super("Surround", new String[] {"surround", "self-trap-feet"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Shoreline-pattern obsidian feet trap.");
    offerProperties(
        rotate, autoSwitch, center, attack, jumpDisable, blocksPerTick, placeRange, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("surround_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Surround.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("surround_packet") {
          @Override
          public void call(PacketEvent event) {
            Surround.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    surround.clear();
    if (minecraft.player == null) return;
    prevY = minecraft.player.getY();
    if (center.getValue()) {
      double x = Math.floor(minecraft.player.getX()) + 0.5;
      double z = Math.floor(minecraft.player.getZ()) + 0.5;
      minecraft.player.setDeltaMovement(
          (x - minecraft.player.getX()) / 2.0,
          minecraft.player.getDeltaMovement().y,
          (z - minecraft.player.getZ()) / 2.0);
    }
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (jumpDisable.getValue() && minecraft.player.getY() > prevY + 0.5) {
      setRunning(false);
      return;
    }

    int obbySlot = PlayerUtil.findInHotbar(Surround::isObsidian);
    if (obbySlot == -1) return;

    surround.clear();
    surround.addAll(collectPositions());

    int placed = 0;
    for (BlockPos pos : surround) {
      if (placed >= blocksPerTick.getValue()) break;
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;
      attackPlace(pos, obbySlot);
      placed++;
    }
  }

  /** Shoreline-pattern: instant re-place when an explosion breaks a surround slot. */
  private void onPacket(PacketEvent event) {
    if (!(event.getPacket() instanceof ClientboundSoundPacket packet)) return;
    if (minecraft.player == null || minecraft.gameMode == null) return;
    String sound = packet.getSound().value().location().toString();
    if (!sound.endsWith("entity.generic.explode")) return;
    BlockPos boom =
        new BlockPos(
            (int) Math.floor(packet.getX()),
            (int) Math.floor(packet.getY()),
            (int) Math.floor(packet.getZ()));
    int obbySlot = PlayerUtil.findInHotbar(Surround::isObsidian);
    if (obbySlot == -1) return;
    // Inbound packets arrive on the netty thread: touch the game on the client thread.
    List<BlockPos> targets = new ArrayList<>(surround);
    minecraft.execute(
        () -> {
          if (minecraft.player == null || minecraft.gameMode == null) return;
          for (BlockPos pos : targets) {
            if (pos.distSqr(boom) <= 36 && PlayerUtil.isAirOrReplaceable(pos)) {
              attackPlace(pos, obbySlot);
            }
          }
        });
  }

  private List<BlockPos> collectPositions() {
    List<BlockPos> out = new ArrayList<>();
    BlockPos base = minecraft.player.blockPosition();
    for (int[] o : OFFSETS) {
      out.add(base.offset(o[0], 0, o[1]));
    }
    // Shoreline extend: off-center players straddle two blocks, cover both.
    double fx = minecraft.player.getX() - Math.floor(minecraft.player.getX());
    double fz = minecraft.player.getZ() - Math.floor(minecraft.player.getZ());
    int ex = fx < 0.3 ? -1 : (fx > 0.7 ? 1 : 0);
    int ez = fz < 0.3 ? -1 : (fz > 0.7 ? 1 : 0);
    if (ex != 0 || ez != 0) {
      BlockPos second = base.offset(ex, 0, ez);
      for (int[] o : OFFSETS) {
        BlockPos extra = second.offset(o[0], 0, o[1]);
        if (!out.contains(extra)) {
          out.add(extra);
        }
      }
    }
    return out;
  }

  private void attackPlace(BlockPos pos, int slot) {
    if (attack.getValue()) {
      AABB box = new AABB(pos);
      for (Entity entity : minecraft.level.entitiesForRendering()) {
        if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
        if (!crystal.getBoundingBox().intersects(box)) continue;
        minecraft.gameMode.attack(minecraft.player, crystal);
        minecraft.player.swing(InteractionHand.MAIN_HAND);
      }
    }
    placeObby(pos, slot);
  }

  private void placeObby(BlockPos pos, int slot) {
    if (!PlayerUtil.isSolid(pos.below()) && !PlayerUtil.isAirOrReplaceable(pos.below())) return;
    boolean needSwitch =
        autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    }
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
  }

  private static boolean isObsidian(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}

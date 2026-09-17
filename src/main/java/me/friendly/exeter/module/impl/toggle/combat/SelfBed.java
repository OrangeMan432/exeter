package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;

public class SelfBed extends ToggleableModule {

  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Double> range = new Property<Double>(5.0, "Place Range");
  private final Property<Integer> placeDelay = new Property<Integer>(1, "Place Delay");
  private final Property<Integer> useDelay = new Property<Integer>(0, "Use Delay");

  private int tickCounter;
  private int lastPlaceTick;
  private int lastUseTick;
  private int phase;
  private boolean hasBed;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("self_bed_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public SelfBed() {
    super("SelfBed", new String[] {"selfbed", "self-bed"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Places and uses beds at your position for self-combat.");
    this.offerProperties(rotate, range, placeDelay, useDelay);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    tickCounter = 0;
    lastPlaceTick = -100;
    lastUseTick = -100;
    phase = 0;
    hasBed = false;
    super.onEnable();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    if (!minecraft.player.isSprinting() || minecraft.player.onGround()) return;

    tickCounter++;

    int slot = PlayerUtil.findBed();
    if (slot == -1) return;

    Direction facing = minecraft.player.getDirection().getOpposite();
    BlockPos playerPos = minecraft.player.blockPosition();

    BlockPos supportPos = findSupport(playerPos);
    if (supportPos == null) return;

    BlockPos footPos = supportPos.above();
    BlockPos headPos = footPos.relative(facing);

    if (!PlayerUtil.inRange(footPos, range.getValue())) return;

    boolean bedAtFoot = minecraft.level.getBlockState(footPos).getBlock() instanceof BedBlock;
    boolean bedAtHead = minecraft.level.getBlockState(headPos).getBlock() instanceof BedBlock;
    hasBed = bedAtFoot || bedAtHead;

    BlockPos usePos = bedAtFoot ? footPos : (bedAtHead ? headPos : null);

    switch (phase) {
      case 0:
        if (hasBed) {
          phase = 1;
          return;
        }

        if (!PlayerUtil.isAirOrReplaceable(footPos) || !PlayerUtil.isAirOrReplaceable(headPos))
          return;
        if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;

        placeBed(supportPos, footPos, slot);
        lastPlaceTick = tickCounter;
        phase = 1;
        break;

      case 1:
        if (!hasBed) {
          phase = 0;
          return;
        }

        if (tickCounter - lastPlaceTick < useDelay.getValue()) return;

        useBed(usePos, slot);
        lastUseTick = tickCounter;
        phase = 2;
        break;

      case 2:
        if (hasBed) {
          if (tickCounter - lastUseTick >= 10) {
            phase = 0;
          }
          return;
        }

        phase = 0;
        break;
    }
  }

  private void placeBed(BlockPos supportPos, BlockPos footPos, int slot) {
    PlayerUtil.swapTo(slot);

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(footPos), PlayerUtil.getPitch(footPos));
    }

    PlayerUtil.useItemOn(supportPos, Direction.UP);
    PlayerUtil.swingHand();

    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    PlayerUtil.swapBack();
  }

  private void useBed(BlockPos usePos, int slot) {
    PlayerUtil.swapTo(slot);

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(usePos), PlayerUtil.getPitch(usePos));
    }

    PlayerUtil.useItemOn(usePos, Direction.UP);
    PlayerUtil.swingHand();

    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    PlayerUtil.swapBack();
  }

  private BlockPos findSupport(BlockPos pos) {
    if (PlayerUtil.isSolid(pos)) return pos;
    if (PlayerUtil.isSolid(pos.below())) return pos.below();
    if (PlayerUtil.isSolid(pos.below(2))) return pos.below(2);
    return null;
  }
}

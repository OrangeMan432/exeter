package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.Vec3;

/**
 * City miner ported from Lemon's AutoHoleMine (1.12.2 Forge).
 *
 * <p>Finds the nearest player's hole/surround and mines into it via the game controller (picked up
 * by PacketMine when enabled). The Lemon original also gated on sibling modules (AntiBurrow,
 * AntiRegear, CevBreaker, BedCevBreaker) which do not exist here, so those guards are omitted.
 */
public final class AutoHoleMine extends ToggleableModule {

  private static final int[][] SIDES = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};

  private final Property<Boolean> breakTrap = new Property<>(false, "Break Trap");
  private final Property<Boolean> doubleMine = new Property<>(true, "Double Mine");
  private final Property<Boolean> ignoreBed = new Property<>(false, "Ignore Bed");
  private final Property<Boolean> ignorePiston = new Property<>(false, "Ignore Piston");
  private final Property<Boolean> ignoreWeb = new Property<>(false, "Ignore Web");
  private final Property<Boolean> fire = new Property<>(false, "Fire");
  private final Property<Boolean> fallingBlocks = new Property<>(false, "Falling Blocks");

  public boolean working;

  private String lastTarget;
  private boolean wasWorking;

  public AutoHoleMine() {
    super("AutoHoleMine", new String[] {"autoholemine", "holemine"}, 0xFFAA00, ModuleType.COMBAT);
    setDescription("Mines into the nearest enemy hole.");
    offerProperties(breakTrap, doubleMine, ignoreBed, ignorePiston, ignoreWeb, fire, fallingBlocks);
    listeners.add(
        new Listener<TickEvent>("autoholemine_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onUpdate();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    working = false;
    wasWorking = false;
    lastTarget = null;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
    DebugLogger.get()
        .logFile(
            getLabel(),
            "settings: breakTrap="
                + breakTrap.getValue()
                + " doubleMine="
                + doubleMine.getValue()
                + " ignoreBed="
                + ignoreBed.getValue()
                + " ignorePiston="
                + ignorePiston.getValue()
                + " ignoreWeb="
                + ignoreWeb.getValue()
                + " fire="
                + fire.getValue()
                + " fallingBlocks="
                + fallingBlocks.getValue());
  }

  private void debug(String message) {
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, message);
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    working = false;
    if (minecraft.gameMode != null) {
      minecraft.gameMode.stopDestroyBlock();
    }
  }

  @Override
  public String getTag() {
    return working ? "Working" : null;
  }

  private void onUpdate() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) {
      if (wasWorking) {
        wasWorking = false;
        debug("idle");
      }
      return;
    }
    update();
    if (working && !wasWorking) {
      wasWorking = true;
      debug("started working" + (lastTarget != null ? " on " + lastTarget : ""));
    } else if (!working && wasWorking) {
      wasWorking = false;
      debug("idle");
    }
  }

  private void update() {
    working = false;

    PacketMine packetMine = packetMine();
    BlockPos instantPos = packetMine != null ? packetMine.getPacketPos() : null;
    if (instantPos != null) {
      BlockPos ownFeet = feet(minecraft.player);
      if (instantPos.equals(ownFeet.above().above()) || instantPos.equals(ownFeet.below())) {
        return;
      }
      if (minecraft.level.getBlockState(instantPos).getBlock() == Blocks.COBWEB) {
        return;
      }
    }

    Player target = nearestPlayer(8.0);
    if (target == null) {
      lastTarget = null;
      return;
    }
    String targetName = target.getName().getString();
    if (!targetName.equals(lastTarget)) {
      lastTarget = targetName;
      debug(
          "targeting "
              + targetName
              + " ("
              + String.format("%.1f", target.distanceTo(minecraft.player))
              + "m)");
    }
    BlockPos feet = feet(target);
    double breakRange = 0.0;
    BlockPos doublePosition = null;
    if (packetMine != null) {
      doublePosition = packetMine.getDoublePos();
      breakRange = packetMine.getBreakRange();
    }

    BlockPos pos = null;
    for (int[] side : SIDES) {
      BlockPos surround = offset(feet, side);
      BlockPos crystal = offset(surround, side);
      if (isAir(surround)) {
        if (isAir(surround.above())) {
          return;
        }
        if (isAir(crystal) && isAir(crystal.above())) {
          if (!breakTrap.getValue()) {
            return;
          }
          pos = surround.above();
        }
      }
    }

    if (pos != null) {
      surroundMine(pos);
    } else {
      List<BlockPos> posList = new ArrayList<>();
      for (int[] side : SIDES) {
        BlockPos surround = offset(feet, side);
        BlockPos crystal = offset(surround, side);
        if (isAir(crystal) && isAir(crystal.above())) {
          if (checkMine(surround, breakRange)) {
            posList.add(surround);
          }
        } else if (isAir(surround) && isAir(crystal.above())) {
          if (checkMine(crystal, breakRange)) {
            posList.add(crystal);
          }
        } else if (isAir(surround) && isAir(crystal) && checkMine(crystal.above(), breakRange)) {
          posList.add(crystal.above());
        }
      }

      if (!posList.isEmpty()) {
        surroundMine(nearest(posList));
      } else if (doubleMine.getValue()) {
        List<DoubleBreak> breakList = new ArrayList<>();
        for (int[] side : SIDES) {
          BlockPos surround = offset(feet, side);
          BlockPos crystal = offset(surround, side);
          if (!isAir(surround) && !isAir(crystal) && isAir(crystal.above())) {
            if (checkMine(surround, breakRange) && checkMine(crystal, breakRange)) {
              breakList.add(new DoubleBreak(surround, crystal));
            }
          } else if (!isAir(surround) && !isAir(crystal.above()) && isAir(crystal)) {
            if (checkMine(surround, breakRange) && checkMine(crystal.above(), breakRange)) {
              breakList.add(new DoubleBreak(surround, crystal.above()));
            }
          } else if (isAir(surround)
              && !isAir(crystal)
              && !isAir(crystal.above())
              && checkMine(crystal, breakRange)
              && checkMine(crystal.above(), breakRange)) {
            breakList.add(new DoubleBreak(crystal, crystal.above()));
          }
        }

        if (breakList.isEmpty()) {
          for (int[] side : SIDES) {
            BlockPos surround = offset(feet, side);
            BlockPos crystal = offset(surround, side);
            if (checkMine(surround, breakRange)
                && checkMine(crystal, breakRange)
                && checkMine(crystal.above(), breakRange)) {
              breakList.add(new DoubleBreak(crystal, crystal.above()));
            }
          }
        }

        if (breakList.isEmpty()) {
          for (int[] side : SIDES) {
            BlockPos surround = offset(feet, side);
            BlockPos crystal = offset(surround, side);
            if (!isAir(crystal)
                && !isAir(crystal.above())
                && !checkMine(crystal)
                && !checkMine(crystal.above())
                && checkMine(surround, breakRange)
                && checkMine(surround.above(), breakRange)) {
              breakList.add(new DoubleBreak(surround, surround.above()));
            }
          }
        }

        if (!breakList.isEmpty()) {
          DoubleBreak doubleBreak =
              breakList.stream()
                  .min(Comparator.comparingDouble(DoubleBreak::maxRange))
                  .orElse(null);
          if (doubleBreak != null) {
            surroundMine(doubleBreak.doublePos);
            if (doublePosition == null) {
              surroundMine(doubleBreak.packetPos);
            }
          }
          return;
        }
      } else {
        for (int[] side : SIDES) {
          BlockPos surround = offset(feet, side);
          BlockPos crystal = offset(surround, side);
          if (!isAir(surround) && checkMine(surround, breakRange)) {
            if (isAir(crystal) && checkMine(crystal, breakRange)
                || isAir(crystal.above()) && checkMine(crystal.above(), breakRange)) {
              posList.add(surround);
            }
          } else if (!isAir(crystal) && checkMine(crystal, breakRange)) {
            if (isAir(surround) && checkMine(surround, breakRange)
                || isAir(crystal.above()) && checkMine(crystal.above(), breakRange)) {
              posList.add(crystal);
            }
          } else if (!isAir(crystal.above())
              && checkMine(crystal.above(), breakRange)
              && (isAir(surround) && checkMine(surround, breakRange)
                  || isAir(crystal) && checkMine(crystal, breakRange))) {
            posList.add(crystal.above());
          }
        }

        if (posList.isEmpty()) {
          for (int[] side : SIDES) {
            BlockPos surround = offset(feet, side);
            BlockPos crystal = offset(surround, side);
            if (checkMine(surround, breakRange)
                && checkMine(crystal, breakRange)
                && checkMine(crystal.above(), breakRange)) {
              posList.add(crystal.above());
            }
          }
        }

        if (!posList.isEmpty()) {
          surroundMine(nearest(posList));
          return;
        }
      }

      boolean hole = true;
      for (int[] side : SIDES) {
        if (isAir(offset(feet, side)) && isAir(offset(feet, side).above())) {
          hole = false;
        }
      }
      if (hole) {
        for (int[] side : SIDES) {
          BlockPos surround = offset(feet, side);
          if (checkMine(surround, breakRange)) {
            posList.add(surround);
          }
        }
        if (!posList.isEmpty()) {
          surroundMine(nearest(posList));
        }
      }
    }
  }

  private void surroundMine(BlockPos pos) {
    if (pos == null || !checkMine(pos)) return;
    working = true;
    PacketMine packetMine = packetMine();
    BlockPos instantPos = packetMine != null ? packetMine.getPacketPos() : null;
    BlockPos doublePosition = packetMine != null ? packetMine.getDoublePos() : null;
    if (instantPos == null || !instantPos.equals(pos)) {
      if (doublePosition == null || !doublePosition.equals(pos)) {
        minecraft.gameMode.startDestroyBlock(pos, closestFace(pos));
      }
    }
  }

  private PacketMine packetMine() {
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("packetmine");
    return module instanceof PacketMine pm && pm.isRunning() ? pm : null;
  }

  private Player nearestPlayer(double range) {
    Player nearest = null;
    double best = range * range;
    for (Player player : minecraft.level.players()) {
      if (player == minecraft.player || player.isDeadOrDying()) continue;
      if (player instanceof FakePlayerEntity) continue;
      double dist = player.distanceToSqr(minecraft.player);
      if (dist < best) {
        best = dist;
        nearest = player;
      }
    }
    return nearest;
  }

  private boolean checkMine(BlockPos pos) {
    if (minecraft.level == null || pos == null) return false;
    if (isAir(pos)) return false;
    if (minecraft.level.getBlockState(pos).getDestroySpeed(minecraft.level, pos) < 0.0f) {
      return false;
    }
    return can(pos);
  }

  private boolean checkMine(BlockPos pos, double range) {
    return checkMine(pos) && (range == 0.0 || getDistance(pos) <= range);
  }

  private boolean can(BlockPos pos) {
    var state = minecraft.level.getBlockState(pos);
    var block = state.getBlock();
    return (!ignoreBed.getValue() || !state.is(BlockTags.BEDS))
        && (!ignorePiston.getValue() || block != Blocks.PISTON_HEAD)
        && (!ignoreWeb.getValue() || block != Blocks.COBWEB)
        && (fire.getValue() || block != Blocks.FIRE)
        && (fallingBlocks.getValue() || !(block instanceof FallingBlock));
  }

  private double getDistance(BlockPos pos) {
    Vec3 center = Vec3.atCenterOf(pos);
    return Math.sqrt(minecraft.player.distanceToSqr(center.x, center.y, center.z));
  }

  private BlockPos nearest(List<BlockPos> positions) {
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (BlockPos pos : positions) {
      double dist = getDistance(pos) * getDistance(pos);
      if (dist < bestDist) {
        bestDist = dist;
        best = pos;
      }
    }
    return best;
  }

  private static BlockPos feet(Player player) {
    return BlockPos.containing(player.getX(), player.getY() + 0.2, player.getZ());
  }

  private static BlockPos offset(BlockPos pos, int[] side) {
    return new BlockPos(pos.getX() + side[0], pos.getY() + side[1], pos.getZ() + side[2]);
  }

  private boolean isAir(BlockPos pos) {
    return minecraft.level != null && minecraft.level.getBlockState(pos).isAir();
  }

  private Direction closestFace(BlockPos pos) {
    Vec3 eye = minecraft.player.getEyePosition();
    Vec3 center = Vec3.atCenterOf(pos);
    double dx = center.x - eye.x;
    double dy = center.y - eye.y;
    double dz = center.z - eye.z;
    double ax = Math.abs(dx);
    double ay = Math.abs(dy);
    double az = Math.abs(dz);
    if (ax >= ay && ax >= az) return dx > 0.0 ? Direction.EAST : Direction.WEST;
    if (az >= ay) return dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
    return dy > 0.0 ? Direction.UP : Direction.DOWN;
  }

  private final class DoubleBreak {
    private final BlockPos packetPos;
    private final BlockPos doublePos;

    private DoubleBreak(BlockPos packetPos, BlockPos doublePos) {
      this.packetPos = packetPos;
      this.doublePos = doublePos;
    }

    private double maxRange() {
      return Math.max(getDistance(packetPos), getDistance(doublePos));
    }
  }
}

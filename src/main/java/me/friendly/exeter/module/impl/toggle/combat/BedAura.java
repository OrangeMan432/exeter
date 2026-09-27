package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BedAura extends ToggleableModule {
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 0.0, 10.0, "Place Range");
  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(10.0, 0.0, 20.0, "Target Range");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(0, 0, 20, "Place Delay");
  private final NumberProperty<Integer> breakDelay =
      new NumberProperty<Integer>(1, 0, 20, "Break Delay");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> anarchyServer = new Property<Boolean>(false, "5b5t", "5b5t");
  private final Property<Boolean> showEsp = new Property<Boolean>(true, "Show ESP", "ESP");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(60f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");

  private int tickCounter;
  private int lastPlaceTick;
  private int lastBreakTick;
  private Player target;
  private BlockPos lastBedPos;
  private boolean waitingToBreak;
  private boolean waitingToPlace;

  public BedAura() {
    super("BedAura", new String[] {"bedaura", "bed-aura"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Automatically places and breaks beds for combat.");

    this.offerProperties(
        rotate,
        autoSwitch,
        switchBack,
        placeRange,
        targetRange,
        placeDelay,
        breakDelay,
        swingHand,
        anarchyServer,
        showEsp,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha);

    this.listeners.add(
        new Listener<TickEvent>("bed_aura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            BedAura.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
    lastBreakTick = -100;
    target = null;
    lastBedPos = null;
    waitingToBreak = false;
    waitingToPlace = false;
  }

  @Override
  protected void onDisable() {
    super.onDisable();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    tickCounter++;

    int bedSlot = PlayerUtil.findBed();
    if (bedSlot == -1) return;

    target = findTarget();
    if (target == null) return;

    if (waitingToBreak) {
      if (tickCounter - lastBreakTick >= 2) {
        minecraft.gameMode.stopDestroyBlock();
        waitingToBreak = false;
        lastPlaceTick = tickCounter;
      }
      return;
    }

    if (waitingToPlace) {
      if (tickCounter - lastPlaceTick >= breakDelay.getValue()) {
        if (lastBedPos != null) {
          boolean bedExists =
              minecraft.level.getBlockState(lastBedPos).getBlock() instanceof BedBlock;
          if (bedExists) {
            useBed(lastBedPos, bedSlot);
          }
        }
        waitingToPlace = false;
        waitingToBreak = false;
        lastBreakTick = tickCounter;
      }
      return;
    }

    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;

    BlockPos placePos = findBestPlacePos();
    if (placePos == null) return;

    placeBed(placePos, bedSlot);
    lastBedPos = placePos;
    lastPlaceTick = tickCounter;
    waitingToPlace = true;
  }

  private void placeBed(BlockPos pos, int slot) {
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
    if (needSwitch && switchBack.getValue()) {
      PlayerUtil.swapBack();
    }

    if (showEsp.getValue()) {
      int overrideFill = useCustomAlpha.getValue() ? Math.round(fillAlpha.getValue()) : -1;
      int overrideOutline = useCustomAlpha.getValue() ? Math.round(outlineAlpha.getValue()) : -1;
      EspRenderManager.getInstance()
          .addBoxEsp(new AABB(pos), 3.0f, true, true, overrideFill, overrideOutline);
    }
  }

  private void useBed(BlockPos pos, int slot) {
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

    PlayerUtil.useItemOn(pos, Direction.UP);

    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }

    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch && switchBack.getValue()) {
      PlayerUtil.swapBack();
    }
  }

  private BlockPos findBestPlacePos() {
    List<BlockPos> positions = new ArrayList<>();

    int range = (int) Math.ceil(placeRange.getValue());
    for (int x = -range; x <= range; x++) {
      for (int y = -range; y <= range; y++) {
        for (int z = -range; z <= range; z++) {
          BlockPos pos = minecraft.player.blockPosition().offset(x, y, z);
          if (canPlaceBed(pos)) {
            positions.add(pos);
          }
        }
      }
    }

    return positions.stream()
        .min(Comparator.comparingDouble(pos -> pos.distSqr(target.blockPosition())))
        .orElse(null);
  }

  private boolean canPlaceBed(BlockPos pos) {
    if (!anarchyServer.getValue()) {
      if (!PlayerUtil.isAirOrReplaceable(pos)) return false;
    }

    BlockPos supporting = pos.below();
    BlockState supportingState = minecraft.level.getBlockState(supporting);
    if (supportingState.isAir()) return false;
    if (supportingState.getMenuProvider(minecraft.level, supporting) != null) return false;

    Direction facing = minecraft.player.getDirection().getOpposite();
    BlockPos headPos = pos.relative(facing);
    if (!anarchyServer.getValue()) {
      if (!PlayerUtil.isAirOrReplaceable(headPos)) return false;
    }

    if (target != null) {
      double distToTarget = pos.distSqr(target.blockPosition());
      if (distToTarget > targetRange.getValue() * targetRange.getValue()) return false;
    }

    return true;
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();

    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }

      double distance = minecraft.player.distanceTo(entity);
      if (distance <= targetRange.getValue()) {
        players.add((Player) entity);
      }
    }

    return players.stream()
        .min(Comparator.comparingDouble(p -> minecraft.player.distanceTo(p)))
        .orElse(null);
  }
}

package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.util.PlayerUtil;
import me.friendly.exeter.util.StopWatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Packet-based block miner ported from Lemon's PacketMine (1.12.2 Forge).
 *
 * <p>Intercepts the vanilla START_DESTROY_BLOCK packet and immediately follows it with a STOP,
 * tracking break progress by calculated break time instead of holding attack. Supports
 * double-mining, auto tool switching and cev-breaking.
 *
 * <p>Simplifications vs the original: a single interception path (our event bus carries both packet
 * directions, so the SendPacket/DamageBlockEvent mode split collapses), no raytrace-based strict
 * facing, no packet-swing (26.4 has no serverbound swing packet), and cosmetic render options
 * reduced to mode + line width with the global client color.
 */
public final class PacketMine extends ToggleableModule {

  public enum CreativeMode {
    AUTO,
    ALWAYS,
    NEVER
  }

  public enum BreakCrystal {
    VANILLA,
    PACKET,
    NONE
  }

  public enum RenderMode {
    FILLED,
    OUTLINED,
    BOTH
  }

  private final NumberProperty<Integer> delay = new NumberProperty<>(0, 0, 1000, "Delay");
  private final NumberProperty<Double> breakRange =
      new NumberProperty<>(5.0, 0.0, 10.0, "Break Range");
  private final EnumProperty<CreativeMode> creativeMode =
      new EnumProperty<>(CreativeMode.AUTO, "Creative Mode");
  private final NumberProperty<Integer> breakTime = new NumberProperty<>(70, 50, 120, "Break Time");
  private final Property<Boolean> instant = new Property<>(true, "Instant");
  private final Property<Boolean> checkUnbreakable = new Property<>(true, "Check Unbreakable");
  private final Property<Boolean> strict = new Property<>(false, "Strict");
  private final Property<Boolean> ignoreChecks = new Property<>(true, "Ignore Checks");
  private final Property<Boolean> swing = new Property<>(false, "Swing");
  private final Property<Boolean> forceRotation = new Property<>(false, "Force Rotation");
  private final NumberProperty<Integer> rotateTime =
      new NumberProperty<>(100, 0, 1000, "Rotate Time");
  private final NumberProperty<Integer> sendRange = new NumberProperty<>(16, 0, 256, "Send Range");
  private final NumberProperty<Integer> removeRange =
      new NumberProperty<>(16, 0, 256, "Remove Range");
  private final Property<Boolean> doubleMineEnabled = new Property<>(true, "Double Mine");
  private final NumberProperty<Integer> doubleCalc =
      new NumberProperty<>(70, 50, 120, "DoubleMine Calc");
  private final NumberProperty<Integer> maxTick = new NumberProperty<>(20, 0, 100, "MaxTick");
  private final NumberProperty<Double> minHealth =
      new NumberProperty<>(16.0, 0.0, 36.0, "Min Health");
  private final Property<Boolean> pauseMending = new Property<>(true, "Pause When Mending");
  private final Property<Boolean> autoSwitch = new Property<>(true, "Auto Switch");
  private final Property<Boolean> waitDouble = new Property<>(false, "Wait DoubleMine");
  private final Property<Boolean> bypassSwitch = new Property<>(false, "Bypass Switch");
  private final Property<Boolean> inventoryOnly = new Property<>(false, "Inventory Only");
  private final Property<Boolean> fastSwitch = new Property<>(false, "DoubleMine FastSwitch");
  private final Property<Boolean> switchBack = new Property<>(true, "Switch Back");
  private final Property<Boolean> packetSwitch = new Property<>(false, "Packet Switch");
  private final Property<Boolean> placeCrystal = new Property<>(false, "Place Crystal");
  private final EnumProperty<BreakCrystal> breakCrystal =
      new EnumProperty<>(BreakCrystal.PACKET, "Break Crystal");
  private final Property<Boolean> antiWeakness = new Property<>(true, "AntiWeakness");
  private final NumberProperty<Integer> renderRange =
      new NumberProperty<>(16, 0, 256, "Render Range");
  private final Property<Boolean> display = new Property<>(true, "Display");
  private final EnumProperty<RenderMode> renderMode = new EnumProperty<>(RenderMode.BOTH, "Render");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<>(2.0f, 0.5f, 5.0f, "Line Width");

  private final StopWatch timer = new StopWatch();
  private final StopWatch doubleTimer = new StopWatch();

  private BreakPos lastBlock;
  private BreakPos doubleMine;
  private BlockPos packetPos;
  private BlockPos doublePos;
  private boolean hadDouble;
  private boolean allow;
  private boolean cev;
  private boolean isMending;
  private boolean healthAbove = true;
  private boolean instantly;
  private boolean different;
  private int bypass = -1;
  private int lastSlot;
  private boolean should;

  public PacketMine() {
    super("PacketMine", new String[] {"packetmine", "packet-mine"}, 0xFF8800, ModuleType.COMBAT);
    setDescription("Mines blocks with packets instead of holding attack.");
    offerProperties(
        delay,
        breakRange,
        creativeMode,
        breakTime,
        instant,
        checkUnbreakable,
        strict,
        ignoreChecks,
        swing,
        forceRotation,
        rotateTime,
        sendRange,
        removeRange,
        doubleMineEnabled,
        doubleCalc,
        maxTick,
        minHealth,
        pauseMending,
        autoSwitch,
        waitDouble,
        bypassSwitch,
        inventoryOnly,
        fastSwitch,
        switchBack,
        packetSwitch,
        placeCrystal,
        breakCrystal,
        antiWeakness,
        renderRange,
        display,
        renderMode,
        lineWidth);
    delay.setDescription("Pause between mining attempts.");
    breakRange.setDescription("Maximum reach for breaking, in blocks.");
    creativeMode.setDescription("Control when blocks break instantly without mining time.");
    breakTime.setDescription("Scales how long each block takes to mine.");
    instant.setDescription("Keep mining the same spot without starting a new attempt.");
    checkUnbreakable.setDescription("Skip blocks that cannot be broken.");
    strict.setDescription("Restart mining after switching held items.");
    ignoreChecks.setDescription("Finish the break even if the block already looks broken.");
    swing.setDescription("Swing the hand while mining.");
    forceRotation.setDescription("Face the block while it finishes mining.");
    rotateTime.setDescription("How early to face the block before it breaks.");
    sendRange.setDescription("Only finish breaks within this many blocks.");
    removeRange.setDescription("Drop mining targets past this many blocks.");
    doubleMineEnabled.setDescription("Mine a second block alongside the main one.");
    doubleCalc.setDescription("Mining time scaling for the second block.");
    maxTick.setDescription("Ticks before the second block attempt expires.");
    minHealth.setDescription("Pause double-mining below this health.");
    pauseMending.setDescription("Pause double-mining while mending armor.");
    autoSwitch.setDescription("Switch to the needed item before acting.");
    waitDouble.setDescription("Wait for the second block before finishing the first.");
    bypassSwitch.setDescription("Swap tools from the full inventory, not just the hotbar.");
    inventoryOnly.setDescription("Limit tool swaps to inventory slots.");
    fastSwitch.setDescription("Swap back immediately after the double-mine hit.");
    switchBack.setDescription("Return to the previous slot afterwards.");
    packetSwitch.setDescription("Switch tools without changing the visible held slot.");
    placeCrystal.setDescription("Place a crystal on the mined block.");
    breakCrystal.setDescription("Choose how crystals on mined blocks are broken.");
    antiWeakness.setDescription("Swap to a weapon before hitting crystals while weakened.");
    renderRange.setDescription("Only draw mining progress within this many blocks.");
    display.setDescription("Draw the mining progress box.");
    renderMode.setDescription("Whether the progress box is filled, outlined, or both.");
    lineWidth.setDescription("Outline thickness in pixels.");
    listeners.add(
        new Listener<TickEvent>("packetmine_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    listeners.add(
        new Listener<PacketEvent>("packetmine_packet") {
          @Override
          public void call(PacketEvent event) {
            onPacket(event);
          }
        });
    listeners.add(
        new Listener<WorldRenderEvent>("packetmine_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    reset();
    if (minecraft.player != null) {
      lastSlot = minecraft.player.getInventory().getSelectedSlot();
    }
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
    DebugLogger.get()
        .logFile(
            getLabel(),
            "settings: delay="
                + delay.getValue()
                + " breakRange="
                + breakRange.getValue()
                + " creativeMode="
                + creativeMode.getValue()
                + " breakTime="
                + breakTime.getValue()
                + " instant="
                + instant.getValue()
                + " doubleMine="
                + doubleMineEnabled.getValue()
                + " autoSwitch="
                + autoSwitch.getValue()
                + " placeCrystal="
                + placeCrystal.getValue()
                + " breakCrystal="
                + breakCrystal.getValue());
  }

  private void debug(String message) {
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, message);
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    if (minecraft.gameMode != null) {
      minecraft.gameMode.stopDestroyBlock();
    }
    reset();
  }

  private void reset() {
    lastBlock = null;
    resetDouble();
  }

  private void resetDouble() {
    doubleMine = null;
    doubleTimer.reset();
    if (bypass != -1 && minecraft.level != null && minecraft.player != null) {
      switchDouble(bypass, true);
    }
    bypass = -1;
  }

  public BlockPos getPacketPos() {
    return packetPos;
  }

  public BlockPos getDoublePos() {
    return doublePos;
  }

  public double getBreakRange() {
    return breakRange.getValue();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) {
      return;
    }

    int selected = minecraft.player.getInventory().getSelectedSlot();
    if (selected != lastSlot) {
      lastSlot = selected;
      different = true;
    }

    float origYaw = minecraft.player.getYRot();
    float origPitch = minecraft.player.getXRot();
    boolean rotated = false;
    if (forceRotation.getValue()
        && lastBlock != null
        && lastBlock.getEnd() - rotateTime.getValue() <= System.currentTimeMillis()
        && !isAirLike(lastBlock.pos)) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(lastBlock.pos), PlayerUtil.getPitch(lastBlock.pos));
      rotated = true;
    }

    mine();

    if (rotated) {
      PlayerUtil.restoreRotation(origYaw, origPitch);
    }

    if (minecraft.level != null && minecraft.player != null && !minecraft.player.isDeadOrDying()) {
      if (lastBlock != null && doubleMine != null) {
        hadDouble = true;
      }
      packetPos = lastBlock != null ? lastBlock.pos : null;
      doublePos = doubleMine != null ? doubleMine.pos : null;
    }
  }

  private void onPacket(PacketEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;

    if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
      if (packet.getAction() != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return;
      BlockPos pos = packet.getPos();
      Direction facing = packet.getDirection();

      if (allow) {
        allow = false;
        return;
      }

      if (!inRange(pos, breakRange.getValue())) {
        event.setCanceled(true);
        return;
      }

      if (calcBreakTime(pos) < 0 && !isPistonMoving(pos)) {
        if (!checkUnbreakable.getValue()) {
          newDouble();
          lastBlock = null;
        }
        event.setCanceled(true);
        return;
      }

      if (isSamePos(lastBlockPos(), pos)) {
        event.setCanceled(true);
        return;
      }

      newDouble();
      lastBlock = new BreakPos(pos, facing, System.currentTimeMillis(), calcBreakTime(pos));
      DebugLogger.get()
          .log(getLabel(), DebugLogger.Level.INFO, "mining " + pos + " (" + lastBlock.time + "ms)");
      instantly = lastBlock.time == 0 && !isAirLike(pos);
      allow = true;
      sendDigging(
          ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
          lastBlock.pos,
          lastBlock.facing);
      sendDigging(
          ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, lastBlock.pos, lastBlock.facing);
      if (lastBlock.time == 0) {
        minecraft.gameMode.startDestroyBlock(lastBlock.pos, lastBlock.facing);
      }
      doSwing();

      if (doubleMine != null && lastBlock != null && isSamePos(doubleMine.pos, lastBlock.pos)) {
        lastBlock =
            new BreakPos(lastBlock.pos, lastBlock.facing, doubleMine.start, calcBreakTime(pos));
        resetDouble();
      }
      if (doubleMine == null) {
        hadDouble = false;
      }
    } else if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
      if (lastBlock != null && packet.getPos().equals(lastBlock.pos)) {
        lastBlock.update();
        should = !isAirLike(packet.getBlockState());
      }
      if (doubleMine != null && packet.getPos().equals(doubleMine.pos)) {
        if (isAirLike(packet.getBlockState())) {
          resetDouble();
        } else {
          doubleMine.updateDouble();
        }
      }
    }
  }

  private void mine() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) {
      reset();
      return;
    }

    if (!doubleMineEnabled.getValue()) {
      resetDouble();
    }

    if (lastBlock != null
        && (calcBreakTime(lastBlock.pos) < 0 && !isPistonMoving(lastBlock.pos)
            || !inRange(lastBlock.pos, removeRange.getValue()))) {
      debug("clearing target " + lastBlock.pos);
      lastBlock = null;
    }

    if (strict.getValue()
        && lastBlock != null
        && different
        && inRange(lastBlock.pos, breakRange.getValue())) {
      different = false;
      allow = true;
      lastBlock =
          new BreakPos(
              lastBlock.pos,
              lastBlock.facing,
              System.currentTimeMillis(),
              calcBreakTime(lastBlock.pos));
      sendDigging(
          ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
          lastBlock.pos,
          lastBlock.facing);
      sendDigging(
          ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, lastBlock.pos, lastBlock.facing);
      doSwing();
      debug("strict resend on " + lastBlock.pos);
    }

    if (doubleMine != null && doubleMine.getEnd() <= System.currentTimeMillis()) {
      if (doubleTimer.hasPassed((long) maxTick.getValue() * 50)) {
        debug("double-mine expired on " + doubleMine.pos);
        resetDouble();
      }
    } else {
      doubleTimer.reset();
    }

    if (lastBlock != null) {
      lastBlock.update();
      if (instantly && (isAirLike(lastBlock.pos) || lastBlock.time != 0)) {
        allow = true;
        sendDigging(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
            lastBlock.pos,
            lastBlock.facing);
        sendDigging(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
            lastBlock.pos,
            lastBlock.facing);
        instantly = false;
        debug("instant break on " + lastBlock.pos);
      }
    }

    if (doubleMine != null) {
      doubleMine.updateDouble();
    }

    isMending = isAutoMendRunning();
    healthAbove =
        minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount()
            >= minHealth.getValue();
    boolean breakDoubleMine =
        doubleMine != null
            && doubleMine.getEnd() <= System.currentTimeMillis()
            && (!isMending || !pauseMending.getValue())
            && healthAbove;
    if (!breakDoubleMine && bypass != -1) {
      switchDouble(bypass, true);
      bypass = -1;
    }

    if (!timer.hasPassed(delay.getValue())) return;
    timer.reset();

    if (lastBlock != null && isAirLike(lastBlock.pos) && cev) {
      Entity crystal = findCrystal(lastBlock.pos);
      breakCrystal(crystal);
      if (crystal != null) {
        cev = false;
      }
    }

    if (creative()) {
      if (lastBlock != null && (ignoreChecks.getValue() || !isAirLike(lastBlock.pos) || should)) {
        minecraft.gameMode.startDestroyBlock(lastBlock.pos, lastBlock.facing);
      }
      if (doubleMine != null) {
        minecraft.gameMode.startDestroyBlock(doubleMine.pos, doubleMine.facing);
      }
      doSwing();
      return;
    }

    if (lastBlock != null
        && lastBlock.getEnd() <= System.currentTimeMillis()
        && inRange(lastBlock.pos, sendRange.getValue())
        && (!waitDouble.getValue() || doubleMine == null || breakDoubleMine)) {
      boolean breakBlock = !isAirLike(lastBlock.pos) || should;
      if (ignoreChecks.getValue() || breakBlock) {
        boolean civ = false;
        if (placeCrystal.getValue() && canPlaceCrystal(lastBlock.pos)) {
          placeCrystalOn(lastBlock.pos);
          civ = true;
          debug("placed crystal on " + lastBlock.pos);
        }

        if (instantly) {
          allow = true;
          sendDigging(
              ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
              lastBlock.pos,
              lastBlock.facing);
        }

        int slot = findTool(lastBlock.pos);
        if (autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot()) {
          debug("switched to tool slot " + slot + " for " + lastBlock.pos);
          int oldSlot =
              !bypassSwitch.getValue() || inventoryOnly.getValue() && slot <= 8
                  ? minecraft.player.getInventory().getSelectedSlot()
                  : slot;
          switchTo(slot);
          sendDigging(
              ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
              lastBlock.pos,
              lastBlock.facing);
          if (switchBack.getValue()) {
            switchTo(oldSlot);
          }
        } else {
          sendDigging(
              ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
              lastBlock.pos,
              lastBlock.facing);
        }
        doSwing();

        if (!instant.getValue() && breakBlock) {
          allow = true;
          lastBlock =
              new BreakPos(
                  lastBlock.pos,
                  lastBlock.facing,
                  System.currentTimeMillis(),
                  calcBreakTime(lastBlock.pos));
          sendDigging(
              ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
              lastBlock.pos,
              lastBlock.facing);
          sendDigging(
              ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
              lastBlock.pos,
              lastBlock.facing);
          doSwing();
        }

        if (civ) {
          Entity crystal = findCrystal(lastBlock.pos);
          if (crystal == null) {
            cev = true;
          } else {
            breakCrystal(crystal);
          }
        }
      }
    }

    if (breakDoubleMine && bypass == -1) {
      int slot = findTool(doubleMine.pos);
      debug("breaking double-mine " + doubleMine.pos);
      switchDouble(slot, false);
      if (fastSwitch.getValue()) {
        switchDouble(slot, true);
      } else {
        bypass = slot;
      }
    }
  }

  private boolean canPlaceCrystal(BlockPos pos) {
    if (minecraft.level == null) return false;
    BlockState below = minecraft.level.getBlockState(pos);
    if (below.getBlock() != Blocks.OBSIDIAN && below.getBlock() != Blocks.BEDROCK) return false;
    return isAirLike(pos.above()) && isAirLike(pos.above().above());
  }

  private void placeCrystalOn(BlockPos pos) {
    if (minecraft.player == null) return;
    int crystalSlot = PlayerUtil.findInHotbar(stack -> stack.getItem() == Items.END_CRYSTAL);
    if (crystalSlot == -1) return;
    PlayerUtil.swapTo(crystalSlot);
    PlayerUtil.useItemOn(pos, Direction.UP);
    PlayerUtil.swapBack();
  }

  private Entity findCrystal(BlockPos pos) {
    if (minecraft.level == null || minecraft.player == null) return null;
    Vec3 target = Vec3.atCenterOf(pos.above());
    for (var entity : minecraft.level.entitiesForRendering()) {
      if (entity instanceof EndCrystal crystal) {
        if (crystal.position().distanceToSqr(target) <= 4.0) {
          return crystal;
        }
      }
    }
    return null;
  }

  private void breakCrystal(Entity crystal) {
    if (crystal == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (breakCrystal.getValue() == BreakCrystal.NONE) return;
    if (antiWeakness.getValue() && minecraft.player.hasEffect(MobEffects.WEAKNESS)) {
      int weapon = bestWeaponSlot();
      if (weapon != -1) {
        PlayerUtil.swapTo(weapon);
      }
    }
    if (breakCrystal.getValue() == BreakCrystal.VANILLA) {
      minecraft.gameMode.attack(minecraft.player, crystal);
    } else {
      minecraft.player.connection.send(
          new ServerboundInteractPacket(
              crystal.getId(),
              InteractionHand.MAIN_HAND,
              crystal.position(),
              minecraft.player.isShiftKeyDown()));
    }
    doSwing();
    if (antiWeakness.getValue()) {
      PlayerUtil.swapBack();
    }
    debug("broke crystal (" + breakCrystal.getValue() + ")");
  }

  private int bestWeaponSlot() {
    if (minecraft.player == null) return -1;
    int result = -1;
    double best = 1.0;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      double[] damage = {0.0};
      stack.forEachModifier(
          EquipmentSlotGroup.ANY,
          (attribute, modifier, display) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
              damage[0] += modifier.amount();
            }
          });
      if (damage[0] > best) {
        best = damage[0];
        result = i;
      }
    }
    return result;
  }

  private void onRender() {
    if (!display.getValue() || minecraft.level == null || minecraft.player == null) return;
    renderProgress(lastBlock, false);
    if (doubleMineEnabled.getValue()) {
      renderProgress(doubleMine, true);
    }
  }

  private void renderProgress(BreakPos target, boolean isDouble) {
    if (target == null) return;
    if (!PlayerUtil.inRange(target.pos, renderRange.getValue())) return;
    float progress = 1.0f;
    if (target.time > 0) {
      progress =
          Math.min(1.0f, (float) (System.currentTimeMillis() - target.start) / (float) target.time);
    }
    int color = EspRenderManager.getClientColor();
    int fill = ARGB.color(EspRenderManager.getGlobalFillAlpha(), color);
    int outline = ARGB.color(EspRenderManager.getGlobalOutlineAlpha(), color);
    AABB box = new AABB(target.pos);
    if (progress < 1.0f && !isDouble) {
      box = box.inflate(progress / 2.0 - 0.5);
    }
    float lw = lineWidth.getValue();
    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
    GizmoStyle style;
    if (filled && outlined) {
      style = GizmoStyle.strokeAndFill(outline, lw, fill);
    } else if (filled) {
      style = GizmoStyle.fill(fill);
    } else {
      style = GizmoStyle.stroke(outline, lw);
    }
    Gizmos.cuboid(box, style).setAlwaysOnTop();
  }

  private final class BreakPos {
    private final BlockPos pos;
    private final Direction facing;
    private final long start;
    private long time;

    private BreakPos(BlockPos pos, Direction facing, long start, long time) {
      this.pos = pos;
      this.facing = facing;
      this.start = start;
      this.time = time;
    }

    private long getEnd() {
      return start + time;
    }

    private void update() {
      this.time = calcBreakTime(this.pos);
    }

    private void updateDouble() {
      this.time = calcDoubleBreakTime(this.pos);
    }
  }

  private void newDouble() {
    if (lastBlock != null && doubleMine == null && !hadDouble && !isAirLike(lastBlock.pos)) {
      resetDouble();
      doubleMine =
          new BreakPos(
              lastBlock.pos, lastBlock.facing, lastBlock.start, calcDoubleBreakTime(lastBlock.pos));
      debug("double-mine started on " + lastBlock.pos);
    }
  }

  private boolean creative() {
    return switch (creativeMode.getValue()) {
      case AUTO -> minecraft.player.isCreative();
      case ALWAYS -> true;
      case NEVER -> false;
    };
  }

  private boolean inRange(BlockPos pos, double range) {
    if (pos == null || minecraft.player == null) return false;
    if (range == 0.0) return true;
    return minecraft.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
        <= range * range;
  }

  private boolean isSamePos(BlockPos a, BlockPos b) {
    return a != null && b != null && a.equals(b);
  }

  private BlockPos lastBlockPos() {
    return lastBlock != null ? lastBlock.pos : null;
  }

  private boolean isAirLike(BlockPos pos) {
    if (minecraft.level == null) return true;
    return isAirLike(minecraft.level.getBlockState(pos));
  }

  private boolean isAirLike(BlockState state) {
    return state.isAir() || !state.getFluidState().isEmpty();
  }

  private boolean isPistonMoving(BlockPos pos) {
    if (minecraft.level == null) return false;
    return minecraft.level.getBlockState(pos).getBlock() == Blocks.MOVING_PISTON;
  }

  private void sendDigging(
      ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction facing) {
    if (minecraft.player == null) return;
    minecraft.player.connection.send(new ServerboundPlayerActionPacket(action, pos, facing));
  }

  private void doSwing() {
    if (!swing.getValue() || minecraft.player == null) return;
    PlayerUtil.swingHand();
  }

  private boolean isAutoMendRunning() {
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("automend");
    return module instanceof ToggleableModule toggleable && toggleable.isRunning();
  }

  private int findTool(BlockPos pos) {
    if (minecraft.player == null || minecraft.level == null || pos == null) {
      return minecraft.player.getInventory().getSelectedSlot();
    }
    BlockState state = minecraft.level.getBlockState(pos);
    if (state.getDestroySpeed(minecraft.level, pos) < 0.0f) {
      return minecraft.player.getInventory().getSelectedSlot();
    }
    int result = minecraft.player.getInventory().getSelectedSlot();
    double speed = digSpeed(state, minecraft.player.getMainHandItem());
    int limit = bypassSwitch.getValue() ? 36 : 9;
    for (int i = 0; i < limit; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      double stackSpeed = digSpeed(state, stack);
      if (stackSpeed > speed) {
        speed = stackSpeed;
        result = i;
      }
    }
    return result;
  }

  private float digSpeed(BlockState state, ItemStack stack) {
    float str = stack.getDestroySpeed(state);
    int eff = efficiencyLevel(stack);
    float speed = (float) Math.max(str + (str > 1.0f ? eff * eff + 1.0f : 0.0f), 0.0f);
    if (minecraft.player != null) {
      var haste = minecraft.player.getEffect(MobEffects.HASTE);
      if (haste != null) {
        speed *= 1.0f + (haste.getAmplifier() + 1) * 0.2f;
      }
      var fatigue = minecraft.player.getEffect(MobEffects.MINING_FATIGUE);
      if (fatigue != null) {
        float scale;
        switch (fatigue.getAmplifier()) {
          case 0 -> scale = 0.3f;
          case 1 -> scale = 0.09f;
          case 2 -> scale = 0.0027f;
          default -> scale = 8.1e-4f;
        }
        speed *= scale;
      }
      if (minecraft.player.isInWater() && !minecraft.player.onGround()) {
        speed /= 5.0f;
      }
    }
    return speed;
  }

  private int efficiencyLevel(ItemStack stack) {
    ItemEnchantments enchantments = stack.getEnchantments();
    for (var entry : enchantments.entrySet()) {
      if (entry.getKey().is(Enchantments.EFFICIENCY)) return entry.getIntValue();
    }
    return 0;
  }

  private int calcBreakTime(BlockPos pos) {
    return breakTicks(pos) * breakTime.getValue();
  }

  private int calcDoubleBreakTime(BlockPos pos) {
    return breakTicks(pos) * doubleCalc.getValue();
  }

  private int breakTicks(BlockPos pos) {
    if (minecraft.level == null || minecraft.player == null) return -1;
    if (creative() || isAirLike(pos)) return 0;
    BlockState state = minecraft.level.getBlockState(pos);
    float hardness = state.getDestroySpeed(minecraft.level, pos);
    if (hardness < 0.0f) return -1;
    float dig = digSpeed(pos, state);
    if (dig <= 0.0f) return -1;
    return (int) Math.ceil(0.7f / (dig / hardness / 30.0f));
  }

  private float digSpeed(BlockPos pos, BlockState state) {
    int slot = findToolSimple(pos, state);
    return digSpeed(state, minecraft.player.getInventory().getItem(slot));
  }

  private int findToolSimple(BlockPos pos, BlockState state) {
    int result = minecraft.player.getInventory().getSelectedSlot();
    double speed = digSpeed(state, minecraft.player.getMainHandItem());
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      double stackSpeed = digSpeed(state, stack);
      if (stackSpeed > speed) {
        speed = stackSpeed;
        result = i;
      }
    }
    return result;
  }

  private void switchTo(int slot) {
    if (minecraft.player == null) return;
    if (!bypassSwitch.getValue() || inventoryOnly.getValue() && slot <= 8) {
      if (packetSwitch.getValue()) {
        minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
      } else {
        minecraft.player.getInventory().setSelectedSlot(slot);
      }
    } else {
      containerSwap(slot);
    }
  }

  private void containerSwap(int slot) {
    if (minecraft.player == null || minecraft.gameMode == null) return;
    int containerSlot = slot < 9 ? slot + 36 : slot;
    int button = minecraft.player.getInventory().getSelectedSlot();
    minecraft.gameMode.handleContainerInput(
        minecraft.player.containerMenu.containerId,
        containerSlot,
        button,
        ContainerInput.SWAP,
        minecraft.player);
  }

  private void switchDouble(int slot, boolean back) {
    if (minecraft.player == null) return;
    int current = minecraft.player.getInventory().getSelectedSlot();
    if (slot < 0 || slot == current) return;
    if (bypassSwitch.getValue() && (!inventoryOnly.getValue() || slot > 8)) {
      containerSwap(back ? current : slot);
    } else {
      minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(back ? current : slot));
    }
  }
}

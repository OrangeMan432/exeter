package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.util.ExplosionUtil;
import me.friendly.exeter.util.MathUtil;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Piston-crystal port of Leonetics' Homovore PistonCrystalModule. Pushes an end crystal over a
 * holed player's head with a piston (side or top layout, best damage wins) and breaks it once the
 * piston extends. PistonPush instead displaces players directly with pistons. Orphaned piston heads
 * left behind when a blast takes the base are right-clicked away automatically so the cells become
 * placeable again.
 */
public class PistonCrystal extends ToggleableModule {
  private static final double TARGET_RANGE = 10.0;
  private static final int EXTEND_TIMEOUT = 10;

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(10.0, 0.0, 16.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(6.0, 0.0, 10.0, "Place Range");
  private final NumberProperty<Double> breakRange =
      new NumberProperty<Double>(3.0, 0.0, 6.0, "Break Range");
  private final NumberProperty<Double> minDamage =
      new NumberProperty<Double>(6.0, 0.0, 36.0, "Min Damage");
  private final NumberProperty<Double> maxSelfDamage =
      new NumberProperty<Double>(8.0, 0.0, 36.0, "Max Self Damage");
  private final NumberProperty<Integer> delay = new NumberProperty<Integer>(10, 0, 40, "Delay");
  private final Property<Boolean> autoBase = new Property<Boolean>(true, "Auto Base");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final Property<Boolean> showEsp = new Property<Boolean>(true, "Show ESP", "ESP");
  private final Property<Boolean> cleanup = new Property<Boolean>(true, "Cleanup");
  private final NumberProperty<Integer> cleanupDelay =
      new NumberProperty<Integer>(10, 0, 40, "Cleanup Delay");

  private int tickCounter;
  private int lastAttemptTick;
  private Setup active;
  private Setup pending;
  private String pendingKey;
  private int holdTicks;
  private int waitTicks;
  private final List<CleanupEntry> cleanupQueue = new ArrayList<>();
  private ArmorProfile targetProfile;
  private boolean noSelfDmgLogged;
  private final java.util.Map<BlockPos, SuspectHead> suspectHeads = new java.util.LinkedHashMap<>();
  private net.minecraft.world.Difficulty targetDifficulty;
  private final BlockPos.MutableBlockPos rayCursor = new BlockPos.MutableBlockPos();

  public PistonCrystal() {
    super(
        "PistonCrystal",
        new String[] {"pistoncrystal", "piston-crystal"},
        0xFF0000,
        ModuleType.COMBAT);
    setDescription("Pushes a crystal over a player in a hole with a piston, then breaks it.");
    offerProperties(
        targetRange,
        placeRange,
        breakRange,
        minDamage,
        maxSelfDamage,
        delay,
        autoBase,
        rotate,
        swingHand,
        autoSwitch,
        switchBack,
        showEsp,
        cleanup,
        cleanupDelay);
    targetRange.setDescription("Acquire targets within this many blocks.");
    placeRange.setDescription("Maximum reach for placements, in blocks.");
    breakRange.setDescription("Maximum reach for breaking, in blocks.");
    minDamage.setDescription("Skip placements below this target damage.");
    maxSelfDamage.setDescription(
        "Skip placements above this self damage. Lethal hits are always skipped.");
    delay.setDescription("Ticks between placement attempts.");
    autoBase.setDescription("Place an obsidian base when one is missing.");
    rotate.setDescription("Face the target position while acting.");
    swingHand.setDescription("Swing the hand on place and break.");
    autoSwitch.setDescription("Switch to the needed item before acting.");
    switchBack.setDescription("Return to the previous slot afterwards.");
    showEsp.setDescription("Draw markers on the piston and crystal spots.");
    cleanup.setDescription("Break the piston and redstone after the attempt.");
    cleanupDelay.setDescription("Ticks before placed parts are broken.");
    this.listeners.add(
        new Listener<TickEvent>("piston_crystal_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("piston_crystal_packet") {
          @Override
          public void call(PacketEvent event) {
            PlayerUtil.spoofMovement(event);
          }
        });
    this.listeners.add(
        new Listener<TickEvent>("piston_crystal_post") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.POST) return;
            if (PlayerUtil.isSpoofing() && !PlayerUtil.consumeDeliveredFlag()) {
              PlayerUtil.sendSpoofTopUp();
            }
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastAttemptTick = -100;
    active = null;
    pending = null;
    cleanupQueue.clear();
    suspectHeads.clear();
    PlayerUtil.clearSpoofedLook(getLabel());
    waitTicks = 0;
    noSelfDmgLogged = false;
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "enabled: minDamage="
                + minDamage.getValue()
                + " targetRange="
                + targetRange.getValue());
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    active = null;
    pending = null;
    cleanupQueue.clear();
    suspectHeads.clear();
    PlayerUtil.clearSpoofedLook(getLabel());
    DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "disabled");
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    tickCounter++;

    runCleanup();
    sweepSuspectHeads();

    if (active != null) {
      tickActive();
      return;
    }
    // The delay gate only starts new attempts; a held placement runs every tick.
    if (pending == null && tickCounter - lastAttemptTick < delay.getValue()) return;

    int pistonSlot = PlayerUtil.findBlock(Blocks.PISTON);
    if (pistonSlot == -1) pistonSlot = PlayerUtil.findBlock(Blocks.STICKY_PISTON);
    int redstoneSlot = PlayerUtil.findBlock(Blocks.REDSTONE_BLOCK);
    int crystalSlot = PlayerUtil.findInHotbar(stack -> stack.getItem() == Items.END_CRYSTAL);
    if (pistonSlot == -1 || redstoneSlot == -1 || crystalSlot == -1) {
      pending = null;
      PlayerUtil.clearSpoofedLook(getLabel());
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.WARN,
              "missing materials: pistonSlot="
                  + pistonSlot
                  + " redstoneSlot="
                  + redstoneSlot
                  + " crystalSlot="
                  + crystalSlot);
      lastAttemptTick = tickCounter;
      return;
    }

    Setup setup = findSetup();
    if (setup == null) {
      pending = null;
      PlayerUtil.clearSpoofedLook(getLabel());
      lastAttemptTick = tickCounter;
      return;
    }
    if (breakCrystalsAround(setup.head)) {
      pending = null;
      PlayerUtil.clearSpoofedLook(getLabel());
      lastAttemptTick = tickCounter;
      return;
    }

    if (!rotate.getValue()) {
      pending = null;
      logPlacing(setup);
      place(setup, pistonSlot, redstoneSlot, crystalSlot);
      lastAttemptTick = tickCounter;
      return;
    }

    String key = setup.piston + setup.dir.toString() + setup.redstone + setup.crystal;
    if (pending == null || !key.equals(pendingKey)) {
      pendingKey = key;
      holdTicks = 0;
    }
    pending = setup;
    // Hold the piston look on client and server every tick; a rotation packet sent on the
    // same tick as the placement can be dropped server-side, facing the piston the wrong way.
    float pistonYaw = PlayerUtil.yawFor(setup.dir);
    PlayerUtil.setSpoofedLook(getLabel(), pistonYaw, 0f);
    holdTicks++;
    // Place only on a fresh spoof: a stall between hold and placement would otherwise place
    // with whatever look the server last saw. Abort if delivery stalls out.
    if (holdTicks >= 2) {
      if (System.currentTimeMillis() - PlayerUtil.lastSpoofSendMs() < 150) {
        logPlacing(setup);
        place(setup, pistonSlot, redstoneSlot, crystalSlot);
        pending = null;
        lastAttemptTick = tickCounter;
      } else if (holdTicks > 40) {
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.WARN,
                "stale spoof, aborting placement at " + setup.piston);
        pending = null;
        PlayerUtil.clearSpoofedLook(getLabel());
        lastAttemptTick = tickCounter;
      }
    }
  }

  private void logPlacing(Setup setup) {
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "placing: piston "
                + setup.piston
                + " redstone "
                + setup.redstone
                + " crystal "
                + setup.crystal
                + " damage "
                + setup.damage);
  }

  private void place(Setup setup, int pistonSlot, int redstoneSlot, int crystalSlot) {
    int origSlot = minecraft.player.getInventory().getSelectedSlot();
    if (setup.placeBase) {
      int obsidianSlot = PlayerUtil.findBlock(Blocks.OBSIDIAN);
      if (obsidianSlot == -1) return;
      swapTo(obsidianSlot);
      PlayerUtil.clickPlace(setup.base);
      if (swingHand.getValue()) PlayerUtil.swingHand();
      swapBack();
    }

    swapTo(pistonSlot);
    // Silent: the piston look was spoofed server-side while holding, so the camera stays.
    // Ordered top-up right before the use packets: guarantees a spoofed look immediately
    // precedes placement on the wire, regardless of fps or stalls.
    PlayerUtil.sendSpoofTopUp();
    // Single-tick client look so prediction renders the same facing the server places.
    // Restored synchronously before the frame renders, so the camera never visibly moves.
    PlayerUtil.withRotation(
        PlayerUtil.yawFor(setup.dir),
        0f,
        () -> {
          PlayerUtil.clickPlace(setup.piston);
          if (swingHand.getValue()) PlayerUtil.swingHand();
        });
    swapBack();

    if (setup.placeRedstone) {
      swapTo(redstoneSlot);
      PlayerUtil.clickPlace(setup.redstone);
      if (swingHand.getValue()) PlayerUtil.swingHand();
      swapBack();
    }

    swapTo(crystalSlot);
    PlayerUtil.useItemOn(setup.base, Direction.UP);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    swapBack();

    // Heal any silent-swap divergence so later placements use the visibly held stack.
    if (autoSwitch.getValue() && switchBack.getValue()) {
      minecraft.player.getInventory().setSelectedSlot(origSlot);
    }
    PlayerUtil.resyncSlot();

    // Resync the server look back to the real one.
    PlayerUtil.clearSpoofedLook(getLabel());

    if (showEsp.getValue()) {
      if (setup.placeBase) {
        EspRenderManager.getInstance().addBoxEsp(new AABB(setup.base), 3.0f, true, true, -1, -1);
      }
      EspRenderManager.getInstance().addBoxEsp(new AABB(setup.piston), 3.0f, true, true, -1, -1);
      if (setup.placeRedstone) {
        EspRenderManager.getInstance()
            .addBoxEsp(new AABB(setup.redstone), 3.0f, true, true, -1, -1);
      }
      EspRenderManager.getInstance().addBoxEsp(new AABB(setup.crystal), 3.0f, true, true, -1, -1);
    }

    active = setup;
    waitTicks = 0;
    noSelfDmgLogged = false;
    // Live cell again: whatever was suspected here belongs to this attempt now.
    suspectHeads.remove(setup.crystal);
  }

  /**
   * Breaks our placed redstone (letting pistons retract) and then the pistons themselves, so
   * attempts don't leave permanently extended arms behind. Entries only break while the cell still
   * holds our block; still-extended pistons are retried later instead of orphaning heads.
   */
  private void runCleanup() {
    if (cleanupQueue.isEmpty()) return;
    cleanupQueue.removeIf(
        entry -> {
          if (tickCounter < entry.dueTick) return false;
          var state = minecraft.level.getBlockState(entry.pos);
          if (entry.piston) {
            if ((state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON))
                && state.hasProperty(BlockStateProperties.EXTENDED)
                && !state.getValue(BlockStateProperties.EXTENDED)) {
              PlayerUtil.breakBlock(entry.pos, Direction.UP);
              return true;
            }
            if (state.isAir()) return true;
            entry.dueTick = tickCounter + 5;
            return false;
          }
          if (state.is(Blocks.REDSTONE_BLOCK)) {
            PlayerUtil.breakBlock(entry.pos, Direction.UP);
          }
          return true;
        });
  }

  private void swapTo(int slot) {
    if (autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(slot);
      // Keep the client on the same slot so client-side placement prediction consumes
      // the right stack instead of ghost-consuming the visibly held one.
      minecraft.player.getInventory().setSelectedSlot(slot);
    }
  }

  private void swapBack() {
    if (autoSwitch.getValue() && switchBack.getValue()) PlayerUtil.swapBack();
  }

  private void tickActive() {
    waitTicks++;
    BlockState pistonState = minecraft.level.getBlockState(active.piston);
    boolean extended =
        pistonState.hasProperty(BlockStateProperties.EXTENDED)
            && pistonState.getValue(BlockStateProperties.EXTENDED);
    if (!extended) {
      extended =
          minecraft.level.getBlockState(active.crystal).is(Blocks.PISTON_HEAD)
              || minecraft.level.getBlockState(active.crystal).is(Blocks.MOVING_PISTON);
    }
    if (extended) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "piston extended, breaking");
      breakCrystalsAround(active.head);
      scheduleCleanup(active);
      watchHead(active.crystal);
      active = null;
      return;
    }
    if (waitTicks > EXTEND_TIMEOUT) {
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.WARN,
              "piston never extended after " + waitTicks + " ticks, resetting");
      scheduleCleanup(active);
      watchHead(active.crystal);
      active = null;
    }
  }

  private void scheduleCleanup(Setup setup) {
    if (!cleanup.getValue() || setup == null) return;
    int due = tickCounter + cleanupDelay.getValue();
    cleanupQueue.add(new CleanupEntry(setup.redstone, due, false));
    cleanupQueue.add(new CleanupEntry(setup.piston, due + 3, true));
  }

  /** Remembers a cell our piston just pushed into; a head stuck there later is ours to clear. */
  private void watchHead(BlockPos pos) {
    BlockPos key = pos.immutable();
    suspectHeads.putIfAbsent(key, new SuspectHead(tickCounter));
    while (suspectHeads.size() > 24) {
      var oldest = suspectHeads.keySet().iterator();
      oldest.next();
      oldest.remove();
    }
  }

  /**
   * Right-clicks orphaned piston heads (base destroyed by a blast) so the cells become placeable
   * again. Only cells from our own attempts are tracked, never foreign builds. Falls back to mining
   * if clicking doesn't clear the head.
   */
  private void sweepSuspectHeads() {
    if (suspectHeads.isEmpty() || tickCounter % 10 != 0) return;
    var it = suspectHeads.entrySet().iterator();
    int handled = 0;
    while (it.hasNext() && handled < 2) {
      var entry = it.next();
      BlockPos pos = entry.getKey();
      SuspectHead suspect = entry.getValue();
      var state = minecraft.level.getBlockState(pos);
      boolean isHead = state.is(Blocks.PISTON_HEAD);
      boolean isMoving = state.is(Blocks.MOVING_PISTON);
      if (!isHead && !isMoving) {
        if (suspect.clicks > 0) {
          DebugLogger.get()
              .log(getLabel(), DebugLogger.Level.INFO, "cleared orphan piston head at " + pos);
        }
        it.remove();
        continue;
      }
      // A head still attached to its extended base is a live piston, not an orphan.
      if (isHead && !isOrphanHead(pos, state)) {
        it.remove();
        continue;
      }
      // In-flight pushes resolve on their own; only stuck ones get clicked.
      if (isMoving && tickCounter - suspect.firstTick < 20) continue;
      if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;
      // Click while holding the end crystal: it can only land on obsidian/bedrock, so
      // clicking a piston head is a guaranteed no-op everywhere else.
      int crystalSlot = PlayerUtil.findInHotbar(stack -> stack.getItem() == Items.END_CRYSTAL);
      if (crystalSlot == -1) continue;
      if (suspect.clicks == 0) {
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.INFO, "clearing orphan piston head at " + pos);
      }
      int origSlot = minecraft.player.getInventory().getSelectedSlot();
      swapTo(crystalSlot);
      PlayerUtil.useItemOn(pos, Direction.UP);
      if (swingHand.getValue()) PlayerUtil.swingHand();
      swapBack();
      if (autoSwitch.getValue() && switchBack.getValue()) {
        minecraft.player.getInventory().setSelectedSlot(origSlot);
      }
      PlayerUtil.resyncSlot();
      suspect.clicks++;
      if (suspect.clicks > 3) {
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.WARN,
                "head at " + pos + " survived clicking, handing off to mining");
        PlayerUtil.breakBlock(pos, Direction.UP);
        it.remove();
      }
      handled++;
    }
  }

  /** A head whose base is gone (or faces elsewhere) can never retract on its own. */
  private boolean isOrphanHead(BlockPos pos, BlockState state) {
    if (!state.hasProperty(BlockStateProperties.FACING)) return true;
    Direction facing = state.getValue(BlockStateProperties.FACING);
    var base = minecraft.level.getBlockState(pos.relative(facing.getOpposite()));
    if (!base.is(Blocks.PISTON) && !base.is(Blocks.STICKY_PISTON)) return true;
    if (!base.hasProperty(BlockStateProperties.FACING)
        || base.getValue(BlockStateProperties.FACING) != facing) return true;
    return !base.hasProperty(BlockStateProperties.EXTENDED)
        || !base.getValue(BlockStateProperties.EXTENDED);
  }

  /** Breaks crystals around the head position that are within break range. */
  private boolean breakCrystalsAround(BlockPos head) {
    Vec3 eye = minecraft.player.getEyePosition();
    double rangeSq = breakRange.getValue() * breakRange.getValue();
    AABB area = new AABB(head).inflate(1.0);
    boolean found = false;
    for (Entity entity : minecraft.level.getEntities(null, area)) {
      if (!(entity instanceof EndCrystal crystal)) continue;
      found = true;
      if (distSqToBox(eye, crystal.getBoundingBox()) > rangeSq) continue;
      breakCrystal(crystal);
    }
    return found;
  }

  private void breakCrystal(EndCrystal crystal) {
    minecraft.gameMode.attack(minecraft.player, crystal);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    DebugLogger.get()
        .log(getLabel(), DebugLogger.Level.INFO, "broke crystal at " + crystal.blockPosition());
  }

  private Setup findSetup() {
    LivingEntity target = findTarget();
    if (target == null) {
      return null;
    }
    targetProfile = profileOf(target);
    targetDifficulty = minecraft.level.getDifficulty();

    Vec3 eye = minecraft.player.getEyePosition();
    double range = placeRange.getValue();
    AABB box = target.getBoundingBox();
    int minX = Mth.floor(box.minX);
    int maxX = Mth.floor(box.maxX - 1e-7);
    int minY = Mth.floor(box.minY);
    int maxY = Mth.floor(box.maxY - 1e-7);
    int minZ = Mth.floor(box.minZ);
    int maxZ = Mth.floor(box.maxZ - 1e-7);

    Setup best = null;
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
          cursor.set(x, y, z);
          BlockPos head = cursor.immutable().above();
          for (Direction dir : Direction.Plane.HORIZONTAL) {
            Setup side = sideSetup(target, head, dir, eye, range);
            if (side != null && (best == null || side.damage > best.damage)) best = side;
            Setup top = topSetup(target, head, dir, eye, range);
            if (top != null && (best == null || top.damage > best.damage)) best = top;
          }
        }
      }
    }
    if (best != null) {
      // Same armored math as the target check, but against our own profile: the piston
      // extends and the crystal breaks within a tick of placement, so this is imminent.
      float selfDamage =
          calcDamage(
              minecraft.player,
              best.explosionPos,
              best.placeBase ? best.base : null,
              profileOf(minecraft.player));
      boolean lethal = ExplosionUtil.wouldPopMitigated(minecraft.player, selfDamage);
      if (selfDamage > maxSelfDamage.getValue() || lethal) {
        if (!noSelfDmgLogged) {
          noSelfDmgLogged = true;
          DebugLogger.get()
              .log(
                  getLabel(),
                  DebugLogger.Level.WARN,
                  "holding placement: self damage "
                      + selfDamage
                      + (lethal
                          ? " lethal at " + minecraft.player.getHealth() + " hp"
                          : " exceeds cap " + maxSelfDamage.getValue()));
        }
        return null;
      }
      noSelfDmgLogged = false;
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.INFO,
              "best setup for "
                  + target.getName().getString()
                  + ": damage "
                  + best.damage
                  + " dir "
                  + best.dir);
    }
    return best;
  }

  private Setup sideSetup(
      LivingEntity target, BlockPos head, Direction dir, Vec3 eye, double range) {
    BlockPos crystalPos = head.relative(dir);
    BlockPos base = crystalPos.below();
    BlockPos pistonPos = head.relative(dir, 2);
    boolean placeBase = needsBase(base);
    if (placeBase && !canAutoBase(base)) return null;
    if (!minecraft.level.getBlockState(crystalPos).isAir()) return null;
    if (!minecraft.level.getBlockState(head).isAir()) return null;
    Vec3 explosionPos = new Vec3(head.getX() + 0.5, head.getY(), head.getZ() + 0.5);
    return buildSetup(
        target, head, dir, crystalPos, base, pistonPos, explosionPos, eye, range, placeBase);
  }

  private Setup topSetup(
      LivingEntity target, BlockPos head, Direction dir, Vec3 eye, double range) {
    BlockPos base = head.relative(dir);
    BlockPos crystalPos = base.above();
    BlockPos pistonPos = crystalPos.relative(dir);
    boolean placeBase = needsBase(base);
    if (placeBase && !canAutoBase(base)) return null;
    if (!minecraft.level.getBlockState(crystalPos).isAir()) return null;
    BlockPos dest = head.above();
    Vec3 explosionPos =
        minecraft.level.getBlockState(dest).isAir()
            ? new Vec3(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5)
            : new Vec3(
                crystalPos.getX() + 0.5 - dir.getStepX() * 0.5,
                crystalPos.getY(),
                crystalPos.getZ() + 0.5 - dir.getStepZ() * 0.5);
    return buildSetup(
        target, head, dir, crystalPos, base, pistonPos, explosionPos, eye, range, placeBase);
  }

  private boolean needsBase(BlockPos base) {
    var state = minecraft.level.getBlockState(base);
    return !state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK);
  }

  private boolean canAutoBase(BlockPos base) {
    return autoBase.getValue()
        && PlayerUtil.findBlock(Blocks.OBSIDIAN) >= 0
        && PlayerUtil.isAirOrReplaceable(base)
        && PlayerUtil.inRange(base, placeRange.getValue())
        && !minecraft.level.getBlockState(base.below()).isAir();
  }

  private Setup buildSetup(
      LivingEntity target,
      BlockPos head,
      Direction dir,
      BlockPos crystalPos,
      BlockPos base,
      BlockPos pistonPos,
      Vec3 explosionPos,
      Vec3 eye,
      double range,
      boolean placeBase) {
    if (!PlayerUtil.isAirOrReplaceable(pistonPos)) return null;
    // Don't rebuild where a piston already sits (e.g. our last setup before cleanup
    // breaks it); otherwise every cycle replays the same spot.
    var existing = minecraft.level.getBlockState(pistonPos);
    if (existing.is(Blocks.PISTON) || existing.is(Blocks.STICKY_PISTON)) return null;
    double rangeSq = range * range;
    if (eye.distanceToSqr(Vec3.atCenterOf(pistonPos)) > rangeSq) return null;
    if (eye.distanceToSqr(Vec3.atCenterOf(base)) > rangeSq) return null;
    AABB crystalBox =
        new AABB(
            explosionPos.x - 1,
            explosionPos.y,
            explosionPos.z - 1,
            explosionPos.x + 1,
            explosionPos.y + 2,
            explosionPos.z + 1);
    double breakRangeSq = breakRange.getValue() * breakRange.getValue();
    if (distSqToBox(eye, crystalBox) > breakRangeSq) return null;
    float damage = calcDamage(target, explosionPos, placeBase ? base : null, targetProfile);
    if (damage < minDamage.getValue()) return null;
    RedstoneSpot redstone = findRedstoneSpot(pistonPos, dir, explosionPos, eye, rangeSq);
    if (redstone == null) return null;
    return new Setup(
        dir,
        pistonPos,
        redstone.pos,
        crystalPos,
        base,
        head,
        explosionPos,
        redstone.place,
        placeBase,
        damage);
  }

  private RedstoneSpot findRedstoneSpot(
      BlockPos pistonPos, Direction dir, Vec3 explosionPos, Vec3 eye, double rangeSq) {
    BlockPos above = pistonPos.above();
    BlockPos inDir = pistonPos.relative(dir);
    BlockPos cw = pistonPos.relative(dir.getClockWise());
    BlockPos ccw = pistonPos.relative(dir.getCounterClockWise());
    BlockPos below = pistonPos.below();
    if (minecraft.level.getBlockState(above).is(Blocks.REDSTONE_BLOCK)) {
      return new RedstoneSpot(above, false);
    }
    if (minecraft.level.getBlockState(inDir).is(Blocks.REDSTONE_BLOCK)) {
      return new RedstoneSpot(inDir, false);
    }
    if (minecraft.level.getBlockState(cw).is(Blocks.REDSTONE_BLOCK)) {
      return new RedstoneSpot(cw, false);
    }
    if (minecraft.level.getBlockState(ccw).is(Blocks.REDSTONE_BLOCK)) {
      return new RedstoneSpot(ccw, false);
    }
    if (minecraft.level.getBlockState(below).is(Blocks.REDSTONE_BLOCK)) {
      return new RedstoneSpot(below, false);
    }
    BlockPos fallback = null;
    for (BlockPos pos : new BlockPos[] {above, inDir, cw, ccw, below}) {
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > rangeSq) continue;
      if (redstoneSafe(pos, explosionPos)) return new RedstoneSpot(pos, true);
      if (fallback == null) fallback = pos;
    }
    return fallback == null ? null : new RedstoneSpot(fallback, true);
  }

  private boolean redstoneSafe(BlockPos pos, Vec3 explosionPos) {
    Vec3 to = Vec3.atCenterOf(pos);
    if (explosionPos.distanceToSqr(to) > 36.0) return true;
    Vec3 diff = to.subtract(explosionPos);
    int steps = (int) Math.ceil(diff.length() / 0.25);
    long last = Long.MIN_VALUE;
    for (int i = 1; i < steps; i++) {
      double s = (double) i / steps;
      rayCursor.set(
          Mth.floor(explosionPos.x + diff.x * s),
          Mth.floor(explosionPos.y + diff.y * s),
          Mth.floor(explosionPos.z + diff.z * s));
      long key = rayCursor.asLong();
      if (key == last) continue;
      last = key;
      if (rayCursor.equals(pos)) break;
      if (minecraft.level.getBlockState(rayCursor).getBlock().getExplosionResistance() >= 600.0f) {
        return true;
      }
    }
    return false;
  }

  private double calcExposure(Vec3 source, AABB box, BlockPos phantomBase) {
    double dx = box.getXsize();
    double dy = box.getYsize();
    double dz = box.getZsize();
    int steps = 2;
    int total = 0;
    int unblocked = 0;
    for (int xi = 0; xi <= steps; xi++) {
      for (int yi = 0; yi <= steps; yi++) {
        for (int zi = 0; zi <= steps; zi++) {
          Vec3 point =
              new Vec3(
                  box.minX + dx * xi / steps,
                  box.minY + dy * yi / steps,
                  box.minZ + dz * zi / steps);
          if (!explosionBlocked(point, source, phantomBase)) unblocked++;
          total++;
        }
      }
    }
    return total == 0 ? 0 : (double) unblocked / total;
  }

  private boolean explosionBlocked(Vec3 from, Vec3 to, BlockPos phantomBase) {
    Vec3 diff = to.subtract(from);
    int steps = (int) Math.ceil(diff.length() / 0.25);
    long last = Long.MIN_VALUE;
    for (int i = 1; i < steps; i++) {
      double s = (double) i / steps;
      rayCursor.set(
          Mth.floor(from.x + diff.x * s),
          Mth.floor(from.y + diff.y * s),
          Mth.floor(from.z + diff.z * s));
      long key = rayCursor.asLong();
      if (key == last) continue;
      last = key;
      if (phantomBase != null && rayCursor.equals(phantomBase)) return true;
      if (minecraft.level.getBlockState(rayCursor).getBlock().getExplosionResistance() >= 600.0f) {
        return true;
      }
    }
    return false;
  }

  private LivingEntity findTarget() {
    List<Player> players = new ArrayList<>();
    double rangeSq = targetRange.getValue() * targetRange.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }
      if (!(entity instanceof Player player)) continue;
      if (isPhased(player)) continue;
      if (minecraft.player.distanceToSqr(player) > rangeSq) continue;
      if (minecraft.player.distanceToSqr(player) > TARGET_RANGE * TARGET_RANGE) continue;
      players.add(player);
    }
    return players.stream()
        .min(Comparator.comparingDouble(p -> minecraft.player.distanceTo(p)))
        .orElse(null);
  }

  private boolean isPhased(LivingEntity target) {
    AABB box = target.getBoundingBox().deflate(0.001);
    int minX = Mth.floor(box.minX);
    int maxX = Mth.floor(box.maxX);
    int minY = Mth.floor(box.minY);
    int maxY = Mth.floor(box.maxY);
    int minZ = Mth.floor(box.minZ);
    int maxZ = Mth.floor(box.maxZ);
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
          cursor.set(x, y, z);
          BlockState state = minecraft.level.getBlockState(cursor);
          if (state.isAir()) continue;
          VoxelShape shape = state.getCollisionShape(minecraft.level, cursor);
          if (shape.isEmpty()) continue;
          if (shape.bounds().move(x, y, z).intersects(box)) return true;
        }
      }
    }
    return false;
  }

  private float calcDamage(
      LivingEntity target, Vec3 explosionPos, BlockPos phantomBase, ArmorProfile profile) {
    double distSq = target.position().distanceToSqr(explosionPos);
    if (distSq > 144.0) return 0;
    double exposure = calcExposure(explosionPos, target.getBoundingBox(), phantomBase);
    if (exposure <= 0) return 0;
    double impact = (1.0 - Math.sqrt(distSq) / 12.0) * exposure;
    if (impact <= 0) return 0;
    float damage = (float) ((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
    switch (targetDifficulty) {
      case EASY -> damage = Math.min(damage / 2f + 1f, damage);
      case HARD -> damage *= 1.5f;
      default -> {}
    }
    float i = 2.0f + profile.toughness() / 4.0f;
    float j = MathUtil.clamp(profile.armor() - damage / i, profile.armor() * 0.2f, 20.0f);
    damage *= 1.0f - j / 25.0f;
    damage *= profile.resistanceMul();
    damage = CombatRules.getDamageAfterMagicAbsorb(damage, profile.protPoints());
    return Math.max(damage, 0f);
  }

  private ArmorProfile profileOf(LivingEntity target) {
    float armor = (float) target.getAttributeValue(Attributes.ARMOR);
    float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    float resistanceMul = 1.0f;
    MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
    if (resistance != null) resistanceMul = 1.0f - 0.2f * (resistance.getAmplifier() + 1);
    int protPoints = 0;
    if (!target.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) protPoints += 4;
    if (!target.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) protPoints += 4;
    if (!target.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) protPoints += 8;
    if (!target.getItemBySlot(EquipmentSlot.FEET).isEmpty()) protPoints += 4;
    return new ArmorProfile(armor, toughness, resistanceMul, protPoints);
  }

  /**
   * Clicks a solid neighbour when one exists so the block lands in the cell; otherwise clicks the
   * replaceable cell itself.
   */
  private static double distSqToBox(Vec3 point, AABB box) {
    double dx =
        point.x < box.minX ? box.minX - point.x : (point.x > box.maxX ? point.x - box.maxX : 0);
    double dy =
        point.y < box.minY ? box.minY - point.y : (point.y > box.maxY ? point.y - box.maxY : 0);
    double dz =
        point.z < box.minZ ? box.minZ - point.z : (point.z > box.maxZ ? point.z - box.maxZ : 0);
    return dx * dx + dy * dy + dz * dz;
  }

  private static class Setup {
    final Direction dir;
    final BlockPos piston;
    final BlockPos redstone;
    final BlockPos crystal;
    final BlockPos base;
    final BlockPos head;
    final Vec3 explosionPos;
    final boolean placeRedstone;
    final boolean placeBase;
    final float damage;

    Setup(
        Direction dir,
        BlockPos piston,
        BlockPos redstone,
        BlockPos crystal,
        BlockPos base,
        BlockPos head,
        Vec3 explosionPos,
        boolean placeRedstone,
        boolean placeBase,
        float damage) {
      this.dir = dir;
      this.piston = piston;
      this.redstone = redstone;
      this.crystal = crystal;
      this.base = base;
      this.head = head;
      this.explosionPos = explosionPos;
      this.placeRedstone = placeRedstone;
      this.placeBase = placeBase;
      this.damage = damage;
    }
  }

  private static class RedstoneSpot {
    final BlockPos pos;
    final boolean place;

    RedstoneSpot(BlockPos pos, boolean place) {
      this.pos = pos;
      this.place = place;
    }
  }

  private static class CleanupEntry {
    final BlockPos pos;
    int dueTick;
    final boolean piston;

    CleanupEntry(BlockPos pos, int dueTick, boolean piston) {
      this.pos = pos;
      this.dueTick = dueTick;
      this.piston = piston;
    }
  }

  private static class SuspectHead {
    final int firstTick;
    int clicks;

    SuspectHead(int firstTick) {
      this.firstTick = firstTick;
      this.clicks = 0;
    }
  }

  private record ArmorProfile(float armor, float toughness, float resistanceMul, int protPoints) {}
}

package me.friendly.exeter.module.impl.toggle.combat;

import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.render.PlacementRender;
import me.friendly.exeter.util.ExplosionUtil;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Places obsidian under crystal positions that have no support block, the way Meteor's CrystalAura
 * support mode does, but on its own timer and without touching the aura. Only positions that would
 * actually deal damage to a target are considered, scored by predicted explosion damage, so the
 * obsidian goes where the next crystal wants to sit.
 */
public class BasePlace extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(10.0, 0.0, 16.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(4.5, 0.0, 6.0, "Place Range");
  private final NumberProperty<Double> minDamage =
      new NumberProperty<Double>(4.0, 0.0, 36.0, "Min Damage");
  private final NumberProperty<Double> maxSelfDamage =
      new NumberProperty<Double>(8.0, 0.0, 36.0, "Max Self Damage");
  private final NumberProperty<Integer> predict = new NumberProperty<Integer>(0, 0, 20, "Predict");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(2, 0, 40, "Place Delay");
  private final NumberProperty<Integer> scanDelay =
      new NumberProperty<Integer>(2, 0, 20, "Scan Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> airPlace = new Property<Boolean>(true, "Air Place", "airplace");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final Property<Boolean> render = new Property<Boolean>(true, "Render");
  private final Property<Boolean> fade = new Property<Boolean>(true, "Fade");
  private final NumberProperty<Integer> fadeTime =
      new NumberProperty<Integer>(1000, 0, 5000, "Fade Time");
  private final EnumProperty<PlacementRender.FadeMode> fadeMode =
      new EnumProperty<>(PlacementRender.FadeMode.ALPHA, "Fade Mode", "fademode");
  private final Property<Boolean> slide = new Property<Boolean>(false, "Slide");
  private final NumberProperty<Integer> slideTime =
      new NumberProperty<Integer>(250, 1, 1000, "Slide Time");
  private final Property<Boolean> smoothSlide = new Property<Boolean>(false, "Smooth Slide");
  private final Property<Boolean> renderDamage = new Property<Boolean>(true, "Damage Text");
  private final NumberProperty<Integer> renderTime =
      new NumberProperty<Integer>(500, 0, 5000, "Render Time");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(1.5f, 0.5f, 5.0f, "Line Width");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(50f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");

  private final PlacementRender placementRender = new PlacementRender();
  private int tickCounter;
  private int lastPlaceTick = -100;
  private int lastScanTick = -100;
  private Player cachedTarget;
  private BlockPos pendingSupport;
  private float pendingDamage;
  private float pendingSelf;
  private int obsidianCount;
  private float bestDamageThisTick;
  private String bestTargetThisTick;
  private Vec3 prevTargetPos;
  private String prevTargetKey;
  private boolean noObsidianLogged;
  private boolean eatingLogged;
  private boolean noTargetLogged;
  private boolean lowDamageLogged;
  private boolean selfDamageLogged;

  public BasePlace() {
    super("BasePlace", new String[] {"baseplace", "base", "bp"}, 0xFF9B59B6, ModuleType.COMBAT);
    setDescription("Places obsidian under crystal positions that have no support block.");
    fadeMode.visibleWhen(() -> fade.getValue());
    fadeTime.visibleWhen(() -> fade.getValue());
    smoothSlide.visibleWhen(() -> slide.getValue());
    slideTime.visibleWhen(() -> slide.getValue());
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        targetRange,
        placeRange,
        minDamage,
        maxSelfDamage,
        predict,
        placeDelay,
        rotate,
        airPlace,
        swingHand,
        autoSwitch,
        switchBack,
        render,
        fade,
        fadeTime,
        fadeMode,
        slide,
        slideTime,
        smoothSlide,
        renderDamage,
        renderTime,
        lineWidth,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha);
    targetRange.setDescription("Acquire targets within this many blocks.");
    placeRange.setDescription("How far from you obsidian may be placed.");
    minDamage.setDescription("Skip positions below this predicted crystal damage.");
    maxSelfDamage.setDescription(
        "Skip positions where the crystal would hurt you more than this. Lethal hits are skipped.");
    predict.setDescription("Ticks of target movement to extrapolate damage scoring by.");
    placeDelay.setDescription("Ticks between placements.");
    scanDelay.setDescription(
        "Ticks between candidate scans. A scan walks every cell in range, so raising this "
            + "trades reaction time for frames.");
    scanDelay.setDescription(
        "Ticks between candidate scans. Each scan walks every cell in range, so raising this "
            + "trades reaction time for frames.");
    rotate.setDescription("Turn towards the block before placing.");
    airPlace.setDescription("Place blocks without needing a solid neighbour.");
    swingHand.setDescription("Swing the hand on place.");
    autoSwitch.setDescription("Switch to obsidian automatically.");
    switchBack.setDescription("Return to the previous slot after placing.");
    render.setDescription("Draw the placement box.");
    fade.setDescription("Fade the box where you stopped looking at it.");
    fadeTime.setDescription("Milliseconds a past position stays visible.");
    fadeMode.setDescription("Fade past boxes by alpha or by shrinking them away.");
    slide.setDescription("Slide the box over to the new block instead of jumping.");
    slideTime.setDescription("How long a slide takes, in milliseconds.");
    smoothSlide.setDescription("Only restart the slide once the previous one finished.");
    renderDamage.setDescription("Billboard the predicted damage over the box.");
    renderTime.setDescription("Milliseconds a placement box stays visible.");
    lineWidth.setDescription("Box outline thickness in pixels.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Box fill opacity, 0-255.");
    outlineAlpha.setDescription("Box outline opacity, 0-255.");

    this.listeners.add(
        new Listener<TickEvent>("baseplace_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<WorldRenderEvent>("baseplace_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
    bestDamageThisTick = 0f;
    bestTargetThisTick = null;
    prevTargetPos = null;
    prevTargetKey = null;
    noObsidianLogged = false;
    noTargetLogged = false;
    lowDamageLogged = false;
    selfDamageLogged = false;
    placementRender.clear();
    obsidianCount = countObsidian();
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "enabled, place range "
                + placeRange.getValue()
                + " min damage "
                + minDamage.getValue()
                + " max self damage "
                + maxSelfDamage.getValue()
                + " place delay "
                + placeDelay.getValue()
                + " predict "
                + predict.getValue()
                + " obsidian in inventory "
                + obsidianCount);
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    placementRender.clear();
    prevTargetPos = null;
    prevTargetKey = null;
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "disabled, placed " + placements + " obsidian this session");
  }

  private int placements;

  @Override
  public String getTag() {
    if (bestTargetThisTick == null || bestDamageThisTick <= 0f) return null;
    return bestTargetThisTick + " " + String.format("%.1f", bestDamageThisTick);
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    tickCounter++;
    // Swapping to obsidian or placing while a golden apple is being eaten cancels the
    // eat, so hold off entirely until it is done.
    if (eatingGapple()) {
      if (!eatingLogged) {
        eatingLogged = true;
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.INFO, "holding off, eating a golden apple");
      }
      return;
    }
    eatingLogged = false;
    // A full scan walks every cell in range. Doing that every tick is what tanks the
    // frame rate, so scans run on their own interval and the winner is reused between them.
    if (tickCounter - lastScanTick < Math.max(1, scanDelay.getValue())) {
      tryPlacePending(cachedTarget);
      return;
    }
    lastScanTick = tickCounter;
    bestDamageThisTick = 0f;
    bestTargetThisTick = null;
    pendingSupport = null;

    Player target = findTarget();
    if (target == null) {
      if (!noTargetLogged) {
        noTargetLogged = true;
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.WARN,
                "no target within " + targetRange.getValue() + "m, nothing to build support for");
      }
      return;
    }
    noTargetLogged = false;
    cachedTarget = target;

    Vec3 predicted = predictedPos(target);

    BlockPos bestSupport = null;
    float bestSupportDamage = 0f;
    float bestSupportSelf = 0f;
    BlockPos bestExisting = null;
    float bestExistingDamage = 0f;
    int scanned = 0;
    int needSupport = 0;
    int reach = (int) Math.ceil(placeRange.getValue());
    BlockPos origin = minecraft.player.blockPosition();
    // One entity query for the whole volume: per cell queries are what make a full scan slow.
    AABB volume =
        new AABB(
            origin.getX() - reach - 1,
            origin.getY() - 3,
            origin.getZ() - reach - 1,
            origin.getX() + reach + 2,
            origin.getY() + 3,
            origin.getZ() + reach + 2);
    List<Entity> blockers = minecraft.level.getEntities(null, volume);

    for (int dx = -reach; dx <= reach; dx++) {
      // Crystals live at foot level, so a band around it is enough. Scanning the full
      // cube means nine times the cells for positions nothing would ever use.
      for (int dy = -2; dy <= 2; dy++) {
        for (int dz = -reach; dz <= reach; dz++) {
          BlockPos base = origin.offset(dx, dy, dz);
          if (!PlayerUtil.inRange(base, placeRange.getValue())) continue;
          scanned++;
          // Obsidian or bedrock is a base AutoCrystal can already use for free; anything
          // replaceable would need support placed before a crystal can sit there.
          var state = minecraft.level.getBlockState(base);
          boolean hasBlock = state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
          if (!hasBlock && !state.canBeReplaced()) continue;
          if (!hasBlock && !airPlace.getValue() && !hasSupport(base)) continue;
          // A crystal needs the cell above to be empty.
          BlockPos crystal = base.above();
          if (!minecraft.level.getBlockState(crystal).isAir()) continue;
          AABB crystalBox =
              new AABB(
                  base.getX(),
                  base.getY() + 1.0,
                  base.getZ(),
                  base.getX() + 1.0,
                  base.getY() + 2.0,
                  base.getZ() + 1.0);
          if (intersectsAny(blockers, crystalBox)) continue;
          if (!hasBlock) needSupport++;

          Vec3 detonation = crystalCenter(base);
          // Cheap upper bound first: damage with full exposure and no raycast at all.
          // Most cells die here, so the 27 point exposure trace only runs for cells
          // that could actually win.
          float bound = maxDamageAt(detonation, target, predicted);
          if (bound < minDamage.getValue()) continue;
          if (bound <= bestSupportDamage && bound <= bestExistingDamage) continue;

          float damage = predictedDamageAt(detonation, target, predicted);
          if (damage < minDamage.getValue()) continue;

          // Same trick for self damage: outside the blast radius it cannot hurt at all.
          float self = 0f;
          if (minecraft.player.getEyePosition().distanceToSqr(detonation) < 144.0) {
            float rawSelf = damageAt(detonation, minecraft.player);
            self = ExplosionUtil.mitigatedExplosionDamage(minecraft.player, rawSelf);
            if (self > maxSelfDamage.getValue()
                || ExplosionUtil.wouldPopMitigated(minecraft.player, self)) {
              continue;
            }
          }

          if (hasBlock) {
            if (damage > bestExistingDamage) {
              bestExistingDamage = damage;
              bestExisting = base.immutable();
            }
          } else if (damage > bestSupportDamage) {
            bestSupportDamage = damage;
            bestSupportSelf = self;
            bestSupport = base.immutable();
            bestTargetThisTick = target.getName().getString();
          }
        }
      }
    }

    if (bestExisting != null) {
      bestDamageThisTick = bestExistingDamage;
      bestTargetThisTick = target.getName().getString();
      // Only build when the new base is strictly better: an existing base is free to use, so
      // spending obsidian and a place delay on a tie is waste.
      if (bestSupport == null || bestExistingDamage >= bestSupportDamage) {
        DebugLogger.get()
            .logFile(
                getLabel(),
                "existing base at "
                    + bestExisting.toShortString()
                    + " does "
                    + String.format("%.2f", bestExistingDamage)
                    + " damage"
                    + (bestSupport == null
                        ? ", no support cell worth building"
                        : ", support at "
                            + bestSupport.toShortString()
                            + " would only do "
                            + String.format("%.2f", bestSupportDamage))
                    + ", not building");
        return;
      }
    }

    if (bestSupport == null) {
      logNothingToDo(target, needSupport, scanned);
      return;
    }
    lowDamageLogged = false;
    selfDamageLogged = false;
    bestDamageThisTick = bestSupportDamage;
    pendingSupport = bestSupport;
    pendingDamage = bestSupportDamage;
    pendingSelf = bestSupportSelf;

    if (tickCounter - lastPlaceTick < placeDelay.getValue()) {
      DebugLogger.get()
          .logFile(
              getLabel(),
              "holding support at "
                  + bestSupport.toShortString()
                  + " targetDmg="
                  + String.format("%.2f", bestSupportDamage)
                  + " selfDmg="
                  + String.format("%.2f", bestSupportSelf)
                  + " waiting "
                  + (placeDelay.getValue() - (tickCounter - lastPlaceTick))
                  + "t of place delay");
      return;
    }

    int obsidianSlot = PlayerUtil.findInHotbar(stack -> stack.is(Items.OBSIDIAN));
    if (obsidianSlot == -1) {
      if (!noObsidianLogged) {
        noObsidianLogged = true;
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.WARN, "no obsidian in hotbar to place support with");
      }
      return;
    }
    noObsidianLogged = false;

    place(bestSupport, obsidianSlot, target, bestSupportDamage, bestSupportSelf);
  }

  /** True while the player is mid consume of a golden apple. */
  private boolean eatingGapple() {
    if (!minecraft.player.isUsingItem()) return false;
    var used = minecraft.player.getItemInHand(minecraft.player.getUsedItemHand());
    return used.is(Items.GOLDEN_APPLE) || used.is(Items.ENCHANTED_GOLDEN_APPLE);
  }

  /** Places the scan winner once the place delay passes, without walking the cells again. */
  private void tryPlacePending(Player target) {
    if (pendingSupport == null || target == null) return;
    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;
    int obsidianSlot = PlayerUtil.findInHotbar(stack -> stack.is(Items.OBSIDIAN));
    if (obsidianSlot == -1) {
      if (!noObsidianLogged) {
        noObsidianLogged = true;
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.WARN, "no obsidian in hotbar to place support with");
      }
      return;
    }
    noObsidianLogged = false;
    place(pendingSupport, obsidianSlot, target, pendingDamage, pendingSelf);
  }

  /** Air Place off: the cell needs a solid neighbour, the same rule the trap modules use. */
  private boolean hasSupport(BlockPos cell) {
    for (Direction dir : Direction.values()) {
      if (!PlayerUtil.isAirOrReplaceable(cell.relative(dir))) return true;
    }
    return false;
  }

  /**
   * Crystal damage with exposure forced to full, so it is the most that position could ever deal.
   * Cheapest possible filter: no raycasts, so hopeless cells never reach the exposure trace.
   */
  private float maxDamageAt(Vec3 center, Player target, Vec3 predictedFeet) {
    return ExplosionUtil.maxExplosionDamage(minecraft.level, center, 6.0F, predictedFeet);
  }

  private boolean intersectsAny(List<Entity> candidates, AABB box) {
    for (Entity entity : candidates) {
      if (entity.isPickable() && entity.getBoundingBox().intersects(box)) return true;
    }
    return false;
  }

  /** Support blocks are the only thing this module builds, so nothing else to do is a state. */
  private void logNothingToDo(Player target, int needSupport, int scanned) {
    if (needSupport == 0) {
      DebugLogger.get()
          .logFile(
              getLabel(),
              "no open cells needing support within "
                  + placeRange.getValue()
                  + "m of "
                  + target.getName().getString()
                  + " ("
                  + scanned
                  + " cells scanned)");
      return;
    }
    if (!lowDamageLogged) {
      lowDamageLogged = true;
      DebugLogger.get()
          .logFile(
              getLabel(),
              needSupport
                  + " open cells near "
                  + target.getName().getString()
                  + " but none reached min damage "
                  + minDamage.getValue());
    }
  }

  private void place(
      BlockPos base, int obsidianSlot, Player target, float damage, float selfDamage) {
    if (eatingGapple()) {
      DebugLogger.get()
          .log(getLabel(), DebugLogger.Level.INFO, "skipped placement, eating a golden apple");
      return;
    }
    placementRender.setRenderPos(
        base.immutable(),
        renderDamage.getValue() ? String.format("%.1f", damage) : null,
        smoothSlide.getValue(),
        slideTime.getValue());

    int origSlot = minecraft.player.getInventory().getSelectedSlot();
    boolean needSwitch =
        autoSwitch.getValue() && obsidianSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(obsidianSlot);
      minecraft.player.getInventory().setSelectedSlot(obsidianSlot);
    }
    PlayerUtil.sendSpoofTopUp();
    Runnable action =
        () -> {
          // The cell is usually air, so it has to be placed against a real neighbour face:
          // clicking the empty cell itself places nothing.
          PlayerUtil.clickPlace(base);
          if (swingHand.getValue()) PlayerUtil.swingHand();
          if (needSwitch && switchBack.getValue()) PlayerUtil.swapBack();
        };
    if (rotate.getValue()) {
      PlayerUtil.withRotation(
          (float) PlayerUtil.getYaw(base), (float) PlayerUtil.getPitch(base), action);
    } else {
      action.run();
    }
    if (autoSwitch.getValue() && switchBack.getValue()) {
      minecraft.player.getInventory().setSelectedSlot(origSlot);
    }
    PlayerUtil.resyncSlot();

    placements++;
    lastPlaceTick = tickCounter;
    obsidianCount = Math.max(0, obsidianCount - 1);
    double dist = Math.sqrt(minecraft.player.getEyePosition().distanceToSqr(crystalCenter(base)));
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "placed support at "
                + base.toShortString()
                + " target="
                + target.getName().getString()
                + " targetDmg="
                + String.format("%.1f", damage)
                + " selfDmg="
                + String.format("%.1f", selfDamage)
                + " dist="
                + String.format("%.1f", dist)
                + " slot="
                + obsidianSlot
                + " left="
                + obsidianCount);
    DebugLogger.get()
        .logFile(
            getLabel(),
            "placed support at "
                + base.toShortString()
                + " block="
                + minecraft.level.getBlockState(base).getBlock().getName()
                + " target="
                + target.getName().getString()
                + " targetDmg="
                + String.format("%.2f", damage)
                + " selfDmg="
                + String.format("%.2f", selfDamage)
                + " dist="
                + String.format("%.2f", dist)
                + " slot="
                + obsidianSlot
                + " obsidianLeft="
                + obsidianCount);
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (!render.getValue()) return;
    placementRender.render(
        new PlacementRender.Params(
            fade.getValue(),
            fadeTime.getValue(),
            fadeMode.getValue(),
            slide.getValue(),
            slideTime.getValue(),
            smoothSlide.getValue(),
            renderDamage.getValue(),
            renderTime.getValue(),
            lineWidth.getValue(),
            useCustomAlpha.getValue()
                ? Math.round(fillAlpha.getValue())
                : EspRenderManager.getGlobalFillAlpha(),
            useCustomAlpha.getValue()
                ? Math.round(outlineAlpha.getValue())
                : EspRenderManager.getGlobalOutlineAlpha(),
            1.0));
  }

  private Vec3 crystalCenter(BlockPos base) {
    return new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
  }

  private Player findTarget() {
    Player best = null;
    double bestDistSq = Double.MAX_VALUE;
    for (Player player : minecraft.level.players()) {
      if (player == minecraft.player) continue;
      if (!player.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(player.getName().getString())) {
        continue;
      }
      double distSq = minecraft.player.distanceToSqr(player);
      if (distSq > targetRange.getValue() * targetRange.getValue()) continue;
      if (distSq < bestDistSq) {
        bestDistSq = distSq;
        best = player;
      }
    }
    return best;
  }

  private float damageAt(Vec3 center, Player victim) {
    return ExplosionUtil.explosionDamage(minecraft.level, center, 6.0F, victim);
  }

  private float predictedDamageAt(Vec3 center, Player target, Vec3 predictedFeet) {
    Vec3 current = target.position();
    AABB box =
        target
            .getBoundingBox()
            .move(
                predictedFeet.x - current.x,
                predictedFeet.y - current.y,
                predictedFeet.z - current.z);
    return ExplosionUtil.explosionDamage(minecraft.level, center, 6.0F, target, predictedFeet, box);
  }

  /**
   * Extrapolates the target's movement, the same way AutoCrystal scores placements, so the obsidian
   * lands where the target is heading rather than where it already is.
   */
  private Vec3 predictedPos(Player target) {
    Vec3 current = target.position();
    String key = target.getName().getString();
    Vec3 predicted = current;
    int ticks = predict.getValue();
    if (ticks > 0 && key.equals(prevTargetKey) && prevTargetPos != null) {
      Vec3 delta = current.subtract(prevTargetPos);
      if (delta.lengthSqr() <= 64.0 && delta.lengthSqr() > 0) {
        predicted = current.add(delta.x * ticks, delta.y * ticks, delta.z * ticks);
      }
    }
    prevTargetPos = current;
    prevTargetKey = key;
    return ticks <= 0 ? current : predicted;
  }

  private int countObsidian() {
    int count = 0;
    for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
      if (minecraft.player.getInventory().getItem(slot).is(Items.OBSIDIAN)) count++;
    }
    return count;
  }
}

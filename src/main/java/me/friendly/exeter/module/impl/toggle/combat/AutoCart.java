package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.render.PlacementRender;
import me.friendly.exeter.util.ExplosionUtil;
import me.friendly.exeter.util.NotificationManager;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoCart extends ToggleableModule {

  private final NumberProperty<Integer> tntCarts =
      new NumberProperty<Integer>(5, 1, 64, "TNT Carts");
  private final NumberProperty<Integer> cartsPerTick =
      new NumberProperty<Integer>(1, 1, 10, "Carts Per Tick");
  private final NumberProperty<Double> range = new NumberProperty<Double>(5.0, 0.0, 10.0, "Range");
  private final Property<Boolean> pullFromInventory =
      new Property<Boolean>(true, "Pull From Inventory");
  private final Property<Boolean> rotateRail = new Property<Boolean>(true, "Rotate Rail");
  private final Property<Boolean> rotateMinecart = new Property<Boolean>(false, "Rotate Minecart");
  private final Property<Boolean> instaLight = new Property<Boolean>(false, "Insta Light");
  private final NumberProperty<Integer> breakDelay =
      new NumberProperty<Integer>(1, 0, 20, "Break Delay");
  private final NumberProperty<Double> maxSelfDamage =
      new NumberProperty<Double>(10.0, 0.0, 60.0, "Max Self Damage");
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

  private Player target;
  private BlockPos targetPos;
  private int cartsPlaced;
  private int breakTimer;
  private boolean isLighting;
  private int lightStage;
  private boolean doneMessageSent;
  private boolean swapPending;
  private boolean noSelfDmgLogged;
  private final PlacementRender placementRender = new PlacementRender();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("autocart_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public AutoCart() {
    super("AutoCart", new String[] {"autocart", "auto-cart"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Automatically places TNT minecarts on rails for PvP.");
    offerProperties(
        tntCarts,
        cartsPerTick,
        range,
        maxSelfDamage,
        pullFromInventory,
        rotateRail,
        rotateMinecart,
        instaLight,
        breakDelay,
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
    tntCarts.setDescription("TNT minecarts to place on the rail before detonating.");
    cartsPerTick.setDescription("Minecarts placed per tick.");
    range.setDescription("Acquire targets within this many blocks.");
    maxSelfDamage.setDescription(
        "Skip placements above this self damage. Lethal hits are always skipped.");
    pullFromInventory.setDescription("Pull TNT minecarts from the inventory into the hotbar.");
    rotateRail.setDescription("Face the rail position while placing the rail.");
    rotateMinecart.setDescription("Face the rail position while placing minecarts.");
    instaLight.setDescription("Automatically break the rail and ignite the placed carts.");
    breakDelay.setDescription("Ticks between breaks.");
    render.setDescription("Draw the placement marker.");
    fade.setDescription("Keep drawing past positions while they fade out.");
    fadeTime.setDescription("Milliseconds a past position stays visible.");
    fadeMode.setDescription("Alpha fades opacity out; Shrink sinks the box height to zero.");
    slide.setDescription("Glide the box from the previous cell instead of teleporting it.");
    slideTime.setDescription("Milliseconds the glide takes.");
    smoothSlide.setDescription("Only move the box origin once per slide period.");
    renderDamage.setDescription("Billboard the predicted damage over the box.");
    renderTime.setDescription("Milliseconds the box survives after the last update.");
    lineWidth.setDescription("Outline thickness in pixels.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Box fill opacity, 0-255.");
    outlineAlpha.setDescription("Box outline opacity, 0-255.");
    fadeTime.visibleWhen(() -> fade.getValue());
    fadeMode.visibleWhen(() -> fade.getValue());
    slideTime.visibleWhen(() -> slide.getValue());
    smoothSlide.visibleWhen(() -> slide.getValue());
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    this.listeners.add(tickListener);
    this.listeners.add(
        new Listener<me.friendly.exeter.events.WorldRenderEvent>("autocart_render") {
          @Override
          public void call(me.friendly.exeter.events.WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    target = null;
    targetPos = null;
    cartsPlaced = 0;
    breakTimer = 0;
    isLighting = false;
    lightStage = 0;
    doneMessageSent = false;
    swapPending = false;
    noSelfDmgLogged = false;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    target = null;
    targetPos = null;
    placementRender.clear();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  @Override
  public String getTag() {
    return String.format("%d", cartsPlaced);
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null) return;

    if (swapPending) {
      PlayerUtil.swapBack();
      swapPending = false;
    }

    if (isLighting) {
      DebugLogger.get()
          .log(getLabel(), DebugLogger.Level.INFO, "lighting phase stage=" + lightStage);
      tickLighting();
      updateRender();
      return;
    }

    Player newTarget = findTarget();
    if (newTarget == null) {
      if (target != null)
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.WARN, "lost target, clearing " + targetPos);
      target = null;
      targetPos = null;
      placementRender.clear();
      return;
    }

    BlockPos newTargetPos = newTarget.blockPosition();

    if (target == null || target != newTarget || !newTargetPos.equals(targetPos)) {
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.INFO,
              "new target " + newTarget.getName().getString() + " at " + newTargetPos);
      NotificationManager.push("AutoCart target " + newTarget.getName().getString(), "compass");
      target = newTarget;
      targetPos = newTargetPos;
      cartsPlaced = 0;
      doneMessageSent = false;
    }

    if (minecraft.level.getBlockState(targetPos).getBlock() != Blocks.RAIL) {
      cartsPlaced = 0;
      doneMessageSent = false;

      if (!hasTntCarts()) {
        DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no TNT carts available");
        return;
      }

      int railSlot = PlayerUtil.findInHotbar(stack -> stack.is(Items.RAIL));
      if (railSlot != -1) {
        DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "placing rail at " + targetPos);
        NotificationManager.push("Placing rail at " + targetPos.toShortString(), "rail");
        placeBlock(targetPos, railSlot, rotateRail.getValue());
      } else {
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.WARN, "no rail in hotbar for " + targetPos);
      }
      return;
    }

    updateRender();

    if (cartsPlaced < tntCarts.getValue()) {
      int tntCartSlot = findTntCart();
      if (tntCartSlot != -1) {
        if (cartsPlaced == 0) {
          NotificationManager.push("Placing carts at " + targetPos.toShortString(), "tnt_minecart");
        }
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.INFO,
                "placing carts " + cartsPlaced + "/" + tntCarts.getValue() + " at " + targetPos);
        PlayerUtil.swapTo(tntCartSlot);

        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();

        if (rotateMinecart.getValue()) {
          PlayerUtil.setRotation(PlayerUtil.getYaw(targetPos), PlayerUtil.getPitch(targetPos));
        }

        for (int i = 0; i < cartsPerTick.getValue() && cartsPlaced < tntCarts.getValue(); i++) {
          BlockHitResult hit =
              new BlockHitResult(
                  Vec3.atCenterOf(targetPos.above()), Direction.UP, targetPos, false);
          minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
          cartsPlaced++;
        }

        if (rotateMinecart.getValue()) {
          PlayerUtil.restoreRotation(yaw, pitch);
        }

        swapPending = true;
      } else {
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.WARN,
                "out of carts at " + cartsPlaced + ", proceeding to detonate");
        NotificationManager.push("Out of carts, detonating", "tnt");
        // force finish so placed carts can detonate via mining/lighting
        cartsPlaced = tntCarts.getValue();
      }
      return;
    }

    PlayerUtil.swapBack();

    if (instaLight.getValue()) {
      // Detonation is imminent: stationary TNT carts explode at power 4 around the rail.
      // Hold here (keeping the placed carts) until we move clear instead of suiciding.
      float selfDamage =
          ExplosionUtil.mitigatedExplosionDamage(minecraft.player, predictedSelfDamage());
      boolean lethal = ExplosionUtil.wouldPopMitigated(minecraft.player, selfDamage);
      if (selfDamage > maxSelfDamage.getValue() || lethal) {
        if (!noSelfDmgLogged) {
          noSelfDmgLogged = true;
          DebugLogger.get()
              .log(
                  getLabel(),
                  DebugLogger.Level.WARN,
                  "holding lighting: self damage "
                      + selfDamage
                      + (lethal
                          ? " lethal at " + minecraft.player.getHealth() + " hp"
                          : " exceeds cap " + maxSelfDamage.getValue()));
        }
        return;
      }
      noSelfDmgLogged = false;
      DebugLogger.get()
          .log(getLabel(), DebugLogger.Level.INFO, "entering instaLight break at " + targetPos);
      NotificationManager.push("AutoCart lighting", "flint_and_steel");
      isLighting = true;
      breakTimer = 0;
      lightStage = 0;
      return;
    }

    if (!doneMessageSent) {
      DebugLogger.get()
          .log(
              getLabel(), DebugLogger.Level.INFO, "done " + cartsPlaced + " carts at " + targetPos);
      NotificationManager.push("AutoCart done " + cartsPlaced + " carts", "tnt_minecart");
      // Manual path: the user breaks the rail themselves, so this is advisory only.
      float selfDamage =
          ExplosionUtil.mitigatedExplosionDamage(minecraft.player, predictedSelfDamage());
      boolean lethal = ExplosionUtil.wouldPopMitigated(minecraft.player, selfDamage);
      if (selfDamage > maxSelfDamage.getValue() || lethal) {
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.WARN,
                "predicted self damage "
                    + selfDamage
                    + (lethal
                        ? " lethal at " + minecraft.player.getHealth() + " hp"
                        : " exceeds cap " + maxSelfDamage.getValue())
                    + " if lit from here");
      }
    }
    doneMessageSent = true;
  }

  /**
   * Tracks the rail cell for the placement render; the damage number is the power-4 cart blast
   * against the current target.
   */
  private void updateRender() {
    if (target == null || targetPos == null) {
      placementRender.clear();
      return;
    }
    Vec3 center = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
    float damage = ExplosionUtil.explosionDamage(minecraft.level, center, 4.0F, target);
    placementRender.setRenderPos(
        targetPos.below().immutable(),
        String.format("%.1f", damage),
        smoothSlide.getValue(),
        slideTime.getValue());
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

  /**
   * Raw vanilla damage of a single stationary-cart blast (power 4) at the rail against ourselves. A
   * lower bound: chained carts each explode separately.
   */
  private float predictedSelfDamage() {
    Vec3 center = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
    return ExplosionUtil.explosionDamage(minecraft.level, center, 4.0F, minecraft.player);
  }

  private Player findTarget() {
    Player closest = null;
    double closestDist = range.getValue() * range.getValue();

    for (Player player : minecraft.level.players()) {
      if (player == minecraft.player || !player.isAlive()) continue;
      if (player.isCreative()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(player.getName().getString())) {
        continue;
      }

      double dist = minecraft.player.distanceToSqr(player);
      if (dist < closestDist) {
        closestDist = dist;
        closest = player;
      }
    }
    return closest;
  }

  private boolean hasTntCarts() {
    if (PlayerUtil.findInHotbar(stack -> stack.is(Items.TNT_MINECART)) != -1) return true;
    if (pullFromInventory.getValue()) {
      return PlayerUtil.findInInventory(stack -> stack.is(Items.TNT_MINECART)) != -1;
    }
    return false;
  }

  private int findTntCart() {
    int slot = PlayerUtil.findInHotbar(stack -> stack.is(Items.TNT_MINECART));
    if (slot != -1) return slot;

    if (pullFromInventory.getValue()) {
      int invSlot = PlayerUtil.findInInventory(stack -> stack.is(Items.TNT_MINECART));
      if (invSlot != -1) {
        int emptySlot = PlayerUtil.findInHotbar(stack -> stack.isEmpty());
        if (emptySlot != -1) {
          int containerId = minecraft.player.containerMenu.containerId;
          int srcContainerSlot = invSlot >= 9 ? invSlot : invSlot + 36;
          int dstContainerSlot = emptySlot + 36;
          minecraft.gameMode.handleContainerInput(
              containerId,
              srcContainerSlot,
              0,
              net.minecraft.world.inventory.ContainerInput.PICKUP,
              minecraft.player);
          minecraft.gameMode.handleContainerInput(
              containerId,
              dstContainerSlot,
              0,
              net.minecraft.world.inventory.ContainerInput.PICKUP,
              minecraft.player);
          return emptySlot;
        }
      }
    }
    return -1;
  }

  private void placeBlock(BlockPos pos, int slot, boolean rotate) {
    PlayerUtil.swapTo(slot);

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();

    if (rotate) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }

    PlayerUtil.useItemOn(pos, Direction.UP);
    PlayerUtil.swingHand();

    if (rotate) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }

    swapPending = true;
  }

  private void tickLighting() {
    if (lightStage == 0) {
      int pickaxeSlot =
          PlayerUtil.findInHotbar(
              stack -> stack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()));
      if (pickaxeSlot == -1) {
        DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no pickaxe for lighting abort");
        isLighting = false;
        cartsPlaced = 0;
        return;
      }

      PlayerUtil.swapTo(pickaxeSlot);

      if (minecraft.level.getBlockState(targetPos).getBlock() != Blocks.AIR) {
        DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "breaking rail at " + targetPos);
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        PlayerUtil.setRotation(PlayerUtil.getYaw(targetPos), PlayerUtil.getPitch(targetPos));
        PlayerUtil.breakBlock(targetPos, Direction.UP);
        PlayerUtil.restoreRotation(yaw, pitch);
        return;
      }

      swapPending = true;

      breakTimer = breakDelay.getValue();
      DebugLogger.get()
          .log(getLabel(), DebugLogger.Level.INFO, "rail broken, waiting " + breakTimer);
      lightStage = 1;
    } else if (lightStage == 1) {
      if (breakTimer > 0) {
        breakTimer--;
        return;
      }

      int flintSlot = PlayerUtil.findInHotbar(stack -> stack.is(Items.FLINT_AND_STEEL));
      if (flintSlot != -1) {
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.INFO, "lighting at " + targetPos.below());
        PlayerUtil.swapTo(flintSlot);

        BlockHitResult hit =
            new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.UP, targetPos.below(), false);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
        PlayerUtil.swingHand();

        swapPending = true;
      } else {
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.WARN, "no flint and steel for lighting");
      }

      isLighting = false;
      lightStage = 0;
      breakTimer = 0;
      cartsPlaced = 0;
    }
  }
}

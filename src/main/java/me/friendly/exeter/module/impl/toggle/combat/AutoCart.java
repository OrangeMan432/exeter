package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
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

  private Player target;
  private BlockPos targetPos;
  private int cartsPlaced;
  private int breakTimer;
  private boolean isLighting;
  private int lightStage;
  private boolean doneMessageSent;
  private boolean swapPending;

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
        pullFromInventory,
        rotateRail,
        rotateMinecart,
        instaLight,
        breakDelay);
    this.listeners.add(tickListener);
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
      tickLighting();
      return;
    }

    Player newTarget = findTarget();
    if (newTarget == null) {
      target = null;
      targetPos = null;
      return;
    }

    BlockPos newTargetPos = newTarget.blockPosition();

    if (target == null || target != newTarget || !newTargetPos.equals(targetPos)) {
      target = newTarget;
      targetPos = newTargetPos;
      cartsPlaced = 0;
      doneMessageSent = false;
    }

    if (minecraft.level.getBlockState(targetPos).getBlock() != Blocks.RAIL) {
      cartsPlaced = 0;
      doneMessageSent = false;

      if (!hasTntCarts()) return;

      int railSlot = PlayerUtil.findInHotbar(stack -> stack.is(Items.RAIL));
      if (railSlot != -1) {
        placeBlock(targetPos, railSlot, rotateRail.getValue());
      }
      return;
    }

    if (cartsPlaced < tntCarts.getValue()) {
      int tntCartSlot = findTntCart();
      if (tntCartSlot != -1) {
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
      }
      return;
    }

    PlayerUtil.swapBack();

    if (instaLight.getValue()) {
      isLighting = true;
      breakTimer = 0;
      lightStage = 0;
      return;
    }

    doneMessageSent = true;
  }

  private Player findTarget() {
    Player closest = null;
    double closestDist = range.getValue() * range.getValue();

    for (Player player : minecraft.level.players()) {
      if (player == minecraft.player || !player.isAlive()) continue;
      if (player.isCreative()) continue;

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
        isLighting = false;
        cartsPlaced = 0;
        return;
      }

      PlayerUtil.swapTo(pickaxeSlot);

      if (minecraft.level.getBlockState(targetPos).getBlock() != Blocks.AIR) {
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        PlayerUtil.setRotation(PlayerUtil.getYaw(targetPos), PlayerUtil.getPitch(targetPos));
        PlayerUtil.breakBlock(targetPos, Direction.UP);
        PlayerUtil.restoreRotation(yaw, pitch);
        return;
      }

      swapPending = true;

      breakTimer = breakDelay.getValue();
      lightStage = 1;
    } else if (lightStage == 1) {
      if (breakTimer > 0) {
        breakTimer--;
        return;
      }

      int flintSlot = PlayerUtil.findInHotbar(stack -> stack.is(Items.FLINT_AND_STEEL));
      if (flintSlot != -1) {
        PlayerUtil.swapTo(flintSlot);

        BlockHitResult hit =
            new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.UP, targetPos.below(), false);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
        PlayerUtil.swingHand();

        swapPending = true;
      }

      isLighting = false;
      lightStage = 0;
      breakTimer = 0;
      cartsPlaced = 0;
    }
  }
}

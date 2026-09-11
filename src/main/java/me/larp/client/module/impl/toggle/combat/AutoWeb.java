package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Slows targets down by placing cobwebs on their feet (and head). */
public class AutoWeb extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(6.0, 1.0, 10.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final Property<Boolean> doubleWeb = new Property<Boolean>(true, "Double Web");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public AutoWeb() {
    super("AutoWeb", new String[] {"autoweb", "auto-web"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Traps targets in cobwebs.");
    offerProperties(targetRange, placeRange, doubleWeb, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("autoweb_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoWeb.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    Player target = findTarget();
    if (target == null) return;
    int webSlot = PlayerUtil.findInHotbar(AutoWeb::isWeb);
    if (webSlot == -1) return;

    BlockPos feet = target.blockPosition();
    if (PlayerUtil.isAirOrReplaceable(feet) && PlayerUtil.inRange(feet, placeRange.getValue())) {
      placeWeb(feet, webSlot);
      return;
    }
    if (doubleWeb.getValue()) {
      BlockPos head = feet.above();
      if (PlayerUtil.isAirOrReplaceable(head) && PlayerUtil.inRange(head, placeRange.getValue())) {
        placeWeb(head, webSlot);
      }
    }
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      if (entity.getBlockStateOn().getBlock() == Blocks.COBWEB) continue;
      double d = minecraft.player.distanceTo(entity);
      if (d < bestDist) {
        bestDist = d;
        best = (Player) entity;
      }
    }
    return best;
  }

  private void placeWeb(BlockPos pos, int slot) {
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
    clickNeighbor(pos);
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

  private void clickNeighbor(BlockPos pos) {
    for (Direction dir : Direction.values()) {
      BlockPos neighbor = pos.relative(dir);
      if (PlayerUtil.isSolid(neighbor)) {
        PlayerUtil.useItemOn(neighbor, dir.getOpposite());
        return;
      }
    }
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
  }

  private static boolean isWeb(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.COBWEB;
  }
}

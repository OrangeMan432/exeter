package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;

/** 6b6t-pattern SafeWalk: kills horizontal motion before you walk off edges. */
public class SafeWalk extends ToggleableModule {

  private final NumberProperty<Double> lookAhead =
      new NumberProperty<Double>(0.6, 0.2, 1.5, "Look Ahead");
  private final Property<Boolean> blocksOnly = new Property<Boolean>(false, "Blocks Only");

  public SafeWalk() {
    super("SafeWalk", new String[] {"safewalk", "safe-walk"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Stops you from walking off edges.");
    offerProperties(lookAhead, blocksOnly);
    this.listeners.add(
        new Listener<TickEvent>("safewalk_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            SafeWalk.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (!minecraft.player.onGround()) return;
    if (!PlayerUtil.isMoving()) return;
    if (minecraft.player.getDeltaMovement().y > 0.0) return;
    if (blocksOnly.getValue()
        && !(minecraft.player.getMainHandItem().getItem() instanceof BlockItem)) {
      return;
    }

    Vec3 look = minecraft.player.getLookAngle();
    double len = Math.sqrt(look.x * look.x + look.z * look.z);
    if (len < 0.01) return;
    double ax = minecraft.player.getX() + (look.x / len) * lookAhead.getValue();
    double az = minecraft.player.getZ() + (look.z / len) * lookAhead.getValue();
    BlockPos aheadFeet =
        new BlockPos(
            (int) Math.floor(ax), minecraft.player.blockPosition().getY(), (int) Math.floor(az));
    if (!PlayerUtil.isAirOrReplaceable(aheadFeet)) return;
    // A wall at head height stops us anyway: only brake for real drops.
    if (!PlayerUtil.isAirOrReplaceable(aheadFeet.above())) return;
    boolean landing = false;
    for (int i = 1; i <= 4; i++) {
      if (PlayerUtil.isSolid(aheadFeet.below(i))) {
        landing = true;
        break;
      }
    }
    if (landing) return;
    // No floor ahead: hold position like sneak-edge.
    minecraft.player.setDeltaMovement(0, minecraft.player.getDeltaMovement().y, 0);
  }
}

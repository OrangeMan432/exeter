package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** 6b6t-pattern Parkour: auto-jumps ledges you run toward. */
public class Parkour extends ToggleableModule {

  private final NumberProperty<Double> lookAhead =
      new NumberProperty<Double>(0.6, 0.2, 1.2, "Look Ahead");

  private int cooldown;

  public Parkour() {
    super("Parkour", new String[] {"parkour", "auto-jump-edge"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Jumps ledges automatically.");
    offerProperties(lookAhead);
    this.listeners.add(
        new Listener<TickEvent>("parkour_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Parkour.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    cooldown = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (cooldown > 0) {
      cooldown--;
      return;
    }
    if (!minecraft.player.onGround()) return;
    if (!PlayerUtil.isMoving()) return;

    Vec3 look = minecraft.player.getLookAngle();
    double len = Math.sqrt(look.x * look.x + look.z * look.z);
    if (len < 0.01) return;
    double ax = minecraft.player.getX() + (look.x / len) * lookAhead.getValue();
    double az = minecraft.player.getZ() + (look.z / len) * lookAhead.getValue();
    BlockPos aheadFeet =
        new BlockPos(
            (int) Math.floor(ax), minecraft.player.blockPosition().getY(), (int) Math.floor(az));
    // Ledge: air at feet ahead, but landing exists below.
    if (!PlayerUtil.isAirOrReplaceable(aheadFeet)) return;
    boolean landing = false;
    for (int i = 1; i <= 3; i++) {
      if (PlayerUtil.isSolid(aheadFeet.below(i))) {
        landing = true;
        break;
      }
    }
    if (!landing) return;
    minecraft.player.jumpFromGround();
    cooldown = 8;
  }
}

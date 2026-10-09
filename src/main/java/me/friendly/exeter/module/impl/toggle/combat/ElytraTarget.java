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
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.util.MathUtil;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Automatically steers towards the nearest enemy in range. */
public class ElytraTarget extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(12.0, 1.0, 120.0, "Range", "range");
  private final NumberProperty<Float> speed =
      new NumberProperty<Float>(30.0f, 1.0f, 180.0f, "Speed", "speed");
  private final Property<Boolean> showTarget = new Property<Boolean>(true, "Show Target", "esp");

  private Player target;
  private float aimYaw;
  private float aimPitch;
  private boolean aimInit;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("elytra_target_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          ElytraTarget.this.onTick();
        }
      };

  public ElytraTarget() {
    super(
        "ElytraTarget",
        new String[] {"elytratarget", "elytra-target"},
        0xFF5555,
        ModuleType.COMBAT);
    setDescription("Automatically steers towards the nearest enemy in range.");
    offerProperties(range, speed, showTarget);
    range.setDescription("Steer toward the nearest enemy within this many blocks.");
    speed.setDescription("Degrees turned per tick toward the target.");
    showTarget.setDescription("Draw a marker on the current target.");
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    target = null;
    aimInit = false;
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "Enabled (range=" + range.getValue() + ", speed=" + speed.getValue() + ")");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    target = null;
    aimInit = false;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) {
      return;
    }
    if (maceDiveActive()) {
      return;
    }

    Player next = findTarget();
    if (next != target) {
      target = next;
      aimInit = false;
      if (target != null) {
        DebugLogger.get()
            .logFile(
                getLabel(),
                "Target acquired: "
                    + target.getName().getString()
                    + " at "
                    + String.format("%.1f", minecraft.player.distanceTo(target))
                    + "m");
      }
    }
    if (target == null || !target.isAlive()) {
      return;
    }
    if (showTarget.getValue()) {
      EspRenderManager.getInstance().addBoxEsp(target.getBoundingBox(), 3.0f, true, true, -1, -1);
    }

    Vec3 eye = minecraft.player.getEyePosition();
    Vec3 spot = target.getEyePosition();
    double dx = spot.x - eye.x;
    double dy = spot.y - eye.y;
    double dz = spot.z - eye.z;
    float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

    if (!aimInit) {
      aimYaw = minecraft.player.getYRot();
      aimPitch = minecraft.player.getXRot();
      aimInit = true;
    }
    // Turn gradually: instant full snaps trip server rotation validation.
    float step = speed.getValue();
    aimYaw += MathUtil.clamp(Mth.wrapDegrees(wantYaw - aimYaw), -step, step);
    aimPitch += MathUtil.clamp(wantPitch - aimPitch, -step, step);
    // The server disconnects rotation outside vanilla's normalized range; atan2 math can
    // produce yaw down to -270, so wrap every tick (vanilla does this before sending).
    aimYaw = Mth.wrapDegrees(aimYaw);
    aimPitch = MathUtil.clamp(aimPitch, -90.0f, 90.0f);
    PlayerUtil.setRotation(aimYaw, aimPitch);
  }

  /** Stands down while MaceDive runs so the two never fight over steering. */
  private boolean maceDiveActive() {
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("macedive");
    return module instanceof me.friendly.exeter.module.ToggleableModule toggleable
        && toggleable.isRunning();
  }

  private Player findTarget() {
    List<Player> enemies = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }
      if (minecraft.player.distanceTo(entity) <= range.getValue()) {
        enemies.add((Player) entity);
      }
    }
    return enemies.stream()
        .min(Comparator.comparingDouble(minecraft.player::distanceTo))
        .orElse(null);
  }
}

package me.friendly.exeter.module.impl.toggle.movement;

import java.util.HashMap;
import java.util.Map;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class FastFall extends ToggleableModule {

  private enum Mode {
    FAST,
    STRICT
  }

  private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.FAST, "Mode");
  private final Property<Boolean> noLag = new Property<>(true, "NoLag");
  private final NumberProperty<Integer> height = new NumberProperty<>(10, 1, 20, "Height");

  private long lastServerPosTime = 0;
  private boolean useTimer = false;

  public FastFall() {
    super("FastFall", new String[] {"fastfall", "fast fall"}, 0x00AAFF, ModuleType.MOVEMENT);
    setDescription("Makes you fall faster. Fast mode applies on ground, Strict mode uses timer.");
    offerProperties(mode, noLag, height);

    listeners.add(
        new Listener<TickEvent>("fastfall_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });

    listeners.add(
        new Listener<PacketEvent>("fastfall_packet") {
          @Override
          public void call(PacketEvent event) {
            if (minecraft.player == null) return;
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
              lastServerPosTime = System.currentTimeMillis();
            }
          }
        });
  }

  @Override
  public void onDisable() {
    super.onDisable();
    useTimer = false;
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null) return;

    int traceDistance = traceDown();
    if ((height.getValue() > 0 && traceDistance > height.getValue())
        || minecraft.player.isInWall()
        || minecraft.player.isInWater()
        || minecraft.player.isInLava()
        || minecraft.player.onClimbable()
        || (System.currentTimeMillis() - lastServerPosTime) < 1000) {
      return;
    }

    if (minecraft.player.isInPowderSnow) return;

    if (mode.getValue() == Mode.FAST) {
      if (minecraft.player.onGround()) {
        double reduction = noLag.getValue() ? 0.62 : 1.0;
        Vec3 motion = minecraft.player.getDeltaMovement();
        minecraft.player.setDeltaMovement(motion.x, motion.y - reduction, motion.z);
      }

      if (traceDistance != 0
          && traceDistance <= height.getValue()
          && trace()
          && minecraft.player.onGround()) {
        Vec3 motion = minecraft.player.getDeltaMovement();
        minecraft.player.setDeltaMovement(motion.x * 0.05, motion.y, motion.z * 0.05);
      }
    }

    if (mode.getValue() == Mode.STRICT) {
      if (!minecraft.player.onGround()) {
        Vec3 motion = minecraft.player.getDeltaMovement();
        if (motion.y < 0 && useTimer) {
          minecraft.player.setDeltaMovement(motion.x, motion.y * 2.5, motion.z);
          return;
        } else {
          useTimer = false;
        }
      } else {
        Vec3 motion = minecraft.player.getDeltaMovement();
        minecraft.player.setDeltaMovement(motion.x, -0.08, motion.z);
        useTimer = true;
      }
    }
  }

  private int traceDown() {
    int retval = 0;
    int y = (int) Math.round(minecraft.player.getY()) - 1;

    for (int tracey = y; tracey >= 0; tracey--) {
      BlockHitResult trace =
          minecraft.level.clip(
              new net.minecraft.world.level.ClipContext(
                  minecraft.player.position(),
                  new Vec3(minecraft.player.getX(), tracey, minecraft.player.getZ()),
                  net.minecraft.world.level.ClipContext.Block.COLLIDER,
                  net.minecraft.world.level.ClipContext.Fluid.NONE,
                  minecraft.player));

      if (trace.getType() == BlockHitResult.Type.BLOCK) return retval;
      retval++;
    }
    return retval;
  }

  private boolean trace() {
    AABB bbox = minecraft.player.getBoundingBox();
    Vec3 basepos = bbox.getCenter();

    double minX = bbox.minX;
    double minZ = bbox.minZ;
    double maxX = bbox.maxX;
    double maxZ = bbox.maxZ;

    Map<Vec3, Vec3> positions = new HashMap<>();
    positions.put(basepos, new Vec3(basepos.x, basepos.y - 1, basepos.z));
    positions.put(new Vec3(minX, basepos.y, minZ), new Vec3(minX, basepos.y - 1, minZ));
    positions.put(new Vec3(maxX, basepos.y, minZ), new Vec3(maxX, basepos.y - 1, minZ));
    positions.put(new Vec3(minX, basepos.y, maxZ), new Vec3(minX, basepos.y - 1, maxZ));
    positions.put(new Vec3(maxX, basepos.y, maxZ), new Vec3(maxX, basepos.y - 1, maxZ));

    for (Map.Entry<Vec3, Vec3> entry : positions.entrySet()) {
      BlockHitResult result =
          minecraft.level.clip(
              new net.minecraft.world.level.ClipContext(
                  entry.getKey(),
                  entry.getValue(),
                  net.minecraft.world.level.ClipContext.Block.COLLIDER,
                  net.minecraft.world.level.ClipContext.Fluid.NONE,
                  minecraft.player));

      if (result.getType() == BlockHitResult.Type.BLOCK) return false;
    }

    return minecraft
        .level
        .getBlockState(
            new BlockPos(
                (int) minecraft.player.getX(),
                (int) (minecraft.player.getY() - 1),
                (int) minecraft.player.getZ()))
        .isAir();
  }
}

package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;

/**
 * Port of Meteor's AutoWalk (Simple mode): holds a movement key for you. Smart mode needs Baritone
 * and is skipped.
 */
public class AutoWalk extends ToggleableModule {

  public enum Direction {
    FORWARDS,
    BACKWARDS,
    LEFT,
    RIGHT
  }

  private final EnumProperty<Direction> direction =
      new EnumProperty<>(Direction.FORWARDS, "Direction", "direction");
  private final Property<Boolean> disableOnInput = new Property<Boolean>(false, "Disable On Input");
  private final Property<Boolean> disableOnY = new Property<Boolean>(false, "Disable On Y Change");
  private final Property<Boolean> waitForChunks = new Property<Boolean>(true, "No Unloaded Chunks");

  public AutoWalk() {
    super("AutoWalk", new String[] {"autowalk", "walk"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Holds a movement key so you walk without input.");
    offerProperties(direction, disableOnInput, disableOnY, waitForChunks);
    direction.setDescription("Which way to walk.");
    disableOnInput.setDescription("Turn off when you press a movement key yourself.");
    disableOnY.setDescription("Turn off when your height changes.");
    waitForChunks.setDescription("Stop at the edge of unloaded chunks.");
    listeners.add(
        new Listener<TickEvent>("autowalk_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoWalk.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    snapshotKeys();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    unpress();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;
    if (disableOnInput.getValue() && manualPress()) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "manual input, disabling");
      setRunning(false);
      return;
    }
    if (disableOnY.getValue() && minecraft.player.yOld != minecraft.player.getY()) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "y changed, disabling");
      setRunning(false);
      return;
    }
    if (waitForChunks.getValue() && !nextChunkLoaded()) {
      unpress();
      return;
    }
    switch (direction.getValue()) {
      case FORWARDS:
        minecraft.options.keyUp.setDown(true);
        break;
      case BACKWARDS:
        minecraft.options.keyDown.setDown(true);
        break;
      case LEFT:
        minecraft.options.keyLeft.setDown(true);
        break;
      case RIGHT:
        minecraft.options.keyRight.setDown(true);
        break;
    }
  }

  /** Don't walk two blocks ahead into a chunk that isn't loaded yet. */
  private boolean nextChunkLoaded() {
    double x = minecraft.player.getX() + minecraft.player.getDeltaMovement().x * 2.0;
    double z = minecraft.player.getZ() + minecraft.player.getDeltaMovement().z * 2.0;
    return minecraft.level.getChunkSource().hasChunk(((int) x) >> 4, ((int) z) >> 4);
  }

  /**
   * Edge detection on the movement keys: our own forced key is already down, so only a fresh
   * physical press counts as manual input.
   */
  private boolean prevUp;

  private boolean prevDown;
  private boolean prevLeft;
  private boolean prevRight;
  private boolean prevJump;
  private boolean prevSneak;

  private void snapshotKeys() {
    if (minecraft.options == null) return;
    prevUp = minecraft.options.keyUp.isDown();
    prevDown = minecraft.options.keyDown.isDown();
    prevLeft = minecraft.options.keyLeft.isDown();
    prevRight = minecraft.options.keyRight.isDown();
    prevJump = minecraft.options.keyJump.isDown();
    prevSneak = minecraft.options.keyShift.isDown();
  }

  private boolean manualPress() {
    boolean up = minecraft.options.keyUp.isDown();
    boolean down = minecraft.options.keyDown.isDown();
    boolean left = minecraft.options.keyLeft.isDown();
    boolean right = minecraft.options.keyRight.isDown();
    boolean jump = minecraft.options.keyJump.isDown();
    boolean sneak = minecraft.options.keyShift.isDown();
    boolean pressed =
        (up && !prevUp)
            || (down && !prevDown)
            || (left && !prevLeft)
            || (right && !prevRight)
            || (jump && !prevJump)
            || (sneak && !prevSneak);
    prevUp = up;
    prevDown = down;
    prevLeft = left;
    prevRight = right;
    prevJump = jump;
    prevSneak = sneak;
    return pressed;
  }

  private void unpress() {
    if (minecraft.options == null) return;
    minecraft.options.keyUp.setDown(false);
    minecraft.options.keyDown.setDown(false);
    minecraft.options.keyLeft.setDown(false);
    minecraft.options.keyRight.setDown(false);
  }
}

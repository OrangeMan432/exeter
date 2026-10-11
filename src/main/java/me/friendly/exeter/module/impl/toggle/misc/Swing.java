package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;

/**
 * Custom swing animations: how long swings last, whether rapid attacks restart them, and whether
 * the swing dips downward mid-motion.
 */
public class Swing extends ToggleableModule {

  private static Swing instance;

  private final NumberProperty<Float> speed = new NumberProperty<Float>(1.0f, 0.05f, 1.0f, "Speed");
  private final Property<Boolean> swingReset = new Property<Boolean>(true, "Swing Reset");
  private final Property<Boolean> steadyHand = new Property<Boolean>(false, "Steady Hand");
  private final Property<Boolean> rotateX = new Property<Boolean>(true, "Rotate X");
  private final Property<Boolean> rotateY = new Property<Boolean>(true, "Rotate Y");
  private final Property<Boolean> rotateZ = new Property<Boolean>(true, "Rotate Z");
  private final Property<Boolean> translateX = new Property<Boolean>(true, "Translate X");
  private final Property<Boolean> translateY = new Property<Boolean>(true, "Translate Y");
  private final Property<Boolean> translateZ = new Property<Boolean>(true, "Translate Z");

  public Swing() {
    super("Swing", new String[] {"swing"}, 0xFFAA00, ModuleType.MISCELLANEOUS);
    setDescription("Smooth out rapid swings and change how long they last.");
    offerProperties(
        speed,
        swingReset,
        steadyHand,
        rotateX,
        rotateY,
        rotateZ,
        translateX,
        translateY,
        translateZ);
    speed.setDescription("Swing length multiplier, lower is slower.");
    swingReset.setDescription("Let new attacks restart the swing animation.");
    steadyHand.setDescription("Stop the item dipping on every attack.");
    rotateX.setDescription("Chopping rotation during swings.");
    rotateY.setDescription("Sideways rotation during swings.");
    rotateZ.setDescription("Tilt rotation during swings.");
    translateX.setDescription("Sideways slide during swings.");
    translateY.setDescription("Vertical bob during swings.");
    translateZ.setDescription("Forward slide during swings.");
    instance = this;
  }

  /** Swing length divisor for the mixin, clamped off zero. */
  public static float swingSpeed() {
    if (instance == null || !instance.isRunning()) return 1.0f;
    return Math.max(0.05f, instance.speed.getValue());
  }

  /** False means an in-progress swing never restarts. */
  public static boolean allowReset() {
    return instance == null || !instance.isRunning() || instance.swingReset.getValue();
  }

  /** True means attacks keep the crosshair cooldown but skip the hand dip. */
  public static boolean steadyHand() {
    return instance != null && instance.isRunning() && instance.steadyHand.getValue();
  }

  /** Per-axis gates for the swing translation. */
  public static boolean translateX() {
    if (instance == null || !instance.isRunning()) return true;
    return instance.translateX.getValue();
  }

  public static boolean translateY() {
    if (instance == null || !instance.isRunning()) return true;
    return instance.translateY.getValue();
  }

  public static boolean translateZ() {
    if (instance == null || !instance.isRunning()) return true;
    return instance.translateZ.getValue();
  }

  /** Per-axis gates for the swing rotations, all on unless customized. */
  public static boolean rotateX() {
    return instance == null || !instance.isRunning() || instance.rotateX.getValue();
  }

  public static boolean rotateY() {
    return instance == null || !instance.isRunning() || instance.rotateY.getValue();
  }

  public static boolean rotateZ() {
    return instance == null || !instance.isRunning() || instance.rotateZ.getValue();
  }

  @Override
  public String getTag() {
    StringBuilder tag = new StringBuilder();
    if (instance != null && !instance.swingReset.getValue()) tag.append("No Reset, ");
    if (steadyHand()) tag.append("Steady, ");
    if (!rotateX()) tag.append("No RX, ");
    if (!rotateY()) tag.append("No RY, ");
    if (!rotateZ()) tag.append("No RZ, ");
    if (!translateX()) tag.append("No TX, ");
    if (!translateY()) tag.append("No TY, ");
    if (!translateZ()) tag.append("No TZ, ");
    if (tag.length() == 0) return null;
    return tag.substring(0, tag.length() - 2);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }
}

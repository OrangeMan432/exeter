package me.earth.earthhack.impl.modules.movement.strafe;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.movement.elytraflight.ElytraFlight;
import me.earth.earthhack.impl.modules.movement.flight.Flight;
import me.earth.earthhack.impl.modules.movement.packetfly.PacketFly;
import me.earth.earthhack.impl.modules.movement.phase.Phase;
import me.earth.earthhack.impl.modules.player.freecam.Freecam;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.init.MobEffects;

/**
 * Standalone NCP/BHOP strafe with full tuning set.
 * Ported from Phobos 1.9 (Strafe).
 */
public class Strafe extends Module
{
    private static final ModuleCache<Freecam> FREECAM =
            Caches.getModule(Freecam.class);
    private static final ModuleCache<Phase> PHASE =
            Caches.getModule(Phase.class);
    private static final ModuleCache<ElytraFlight> ELYTRA =
            Caches.getModule(ElytraFlight.class);
    private static final ModuleCache<Flight> FLIGHT =
            Caches.getModule(Flight.class);
    private static final ModuleCache<PacketFly> PACKET_FLY =
            Caches.getModule(PacketFly.class);

    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.NCP));
    protected final Setting<Boolean> setGround =
        register(new BooleanSetting("SetGround", true));
    protected final Setting<Boolean> hop =
        register(new BooleanSetting("Hop", true));
    protected final Setting<Boolean> bhop =
        register(new BooleanSetting("Bhop", false));
    protected final Setting<Boolean> noLag =
        register(new BooleanSetting("NoLag", false));
    protected final Setting<Integer> speed =
        register(new NumberSetting<>("Speed", 100, 0, 150));
    protected final Setting<Integer> potionSpeed =
        register(new NumberSetting<>("Speed1", 130, 0, 150));
    protected final Setting<Integer> potionSpeed2 =
        register(new NumberSetting<>("Speed2", 125, 0, 150));
    protected final Setting<Integer> dFactor =
        register(new NumberSetting<>("DFactor", 159, 100, 200));
    protected final Setting<Integer> acceleration =
        register(new NumberSetting<>("Accel", 2149, 1000, 2500));
    protected final Setting<Float> speedLimit =
        register(new NumberSetting<>("SpeedLimit", 35.0f, 20.0f, 60.0f));
    protected final Setting<Float> speedLimit2 =
        register(new NumberSetting<>("SpeedLimit2", 60.0f, 20.0f, 60.0f));
    protected final Setting<Integer> yOffset =
        register(new NumberSetting<>("YOffset", 400, 350, 500));
    protected final Setting<Boolean> potion =
        register(new BooleanSetting("Potion", false));
    protected final Setting<Boolean> wait =
        register(new BooleanSetting("Wait", true));
    protected final Setting<Boolean> hopWait =
        register(new BooleanSetting("HopWait", true));
    protected final Setting<Integer> startStage =
        register(new NumberSetting<>("Stage", 2, 0, 4));
    protected final Setting<Boolean> setPos =
        register(new BooleanSetting("SetPos", true));
    protected final Setting<Boolean> setNull =
        register(new BooleanSetting("SetNull", false));
    protected final Setting<Integer> groundLimit =
        register(new NumberSetting<>("GroundLimit", 138, 0, 1000));
    protected final Setting<Integer> groundFactor =
        register(new NumberSetting<>("GroundFactor", 13, 0, 50));
    protected final Setting<Integer> step =
        register(new NumberSetting<>("SetStep", 1, 0, 2));
    protected final Setting<Boolean> noGroundLag =
        register(new BooleanSetting("NoGroundLag", true));

    int stage = 1;
    double moveSpeed;
    double lastDist;
    int cooldownHops;
    int hops;
    boolean waitForGround;
    final StopWatch timer = new StopWatch();

    public Strafe()
    {
        super("Strafe", Category.Movement);
        this.listeners.add(new ListenerMove(this));
        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerPosLook(this));
        this.setData(new SimpleData(this,
            "Standalone NCP/BHOP strafe, bind it separately from Speed."));
    }

    @Override
    protected void onEnable()
    {
        waitForGround = mc.player != null && !mc.player.onGround;
        hops = 0;
        timer.reset();
        moveSpeed = getBaseMoveSpeed();
    }

    @Override
    protected void onDisable()
    {
        hops = 0;
        moveSpeed = 0.0;
        stage = startStage.getValue();
    }

    boolean shouldReturn()
    {
        return FREECAM.isEnabled()
            || PHASE.isEnabled()
            || ELYTRA.isEnabled()
            || FLIGHT.isEnabled()
            || PACKET_FLY.isEnabled();
    }

    static double getBaseMoveSpeed()
    {
        double base = 0.272;
        if (mc.player != null
            && mc.player.isPotionActive(MobEffects.SPEED)
            && mc.player.getActivePotionEffect(MobEffects.SPEED) != null)
        {
            base *= 1.0 + 0.2 * mc.player
                .getActivePotionEffect(MobEffects.SPEED).getAmplifier();
        }

        return base;
    }

    float getMultiplier()
    {
        float base = speed.getValue();
        if (potion.getValue()
            && mc.player.isPotionActive(MobEffects.SPEED)
            && mc.player.getActivePotionEffect(MobEffects.SPEED) != null)
        {
            int amplifier = mc.player
                .getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1;
            base = amplifier >= 2
                ? potionSpeed2.getValue()
                : potionSpeed.getValue();
        }

        return base / 100.0f;
    }

    double getSpeedKpH()
    {
        double dist = Math.hypot(mc.player.posX - mc.player.prevPosX,
                                 mc.player.posZ - mc.player.prevPosZ);
        return dist * 20.0 * 3.6;
    }

    public enum Mode
    {
        NCP,
        BHOP
    }
}

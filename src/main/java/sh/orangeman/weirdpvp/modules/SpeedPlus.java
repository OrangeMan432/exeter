package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import sh.orangeman.weirdpvp.WeirdPvP;

public class SpeedPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> damageBoost = sgGeneral.add(new BoolSetting.Builder()
        .name("damage-boost")
        .description("Boosts speed when you receive knockback.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useTimer = sgGeneral.add(new BoolSetting.Builder()
        .name("use-timer")
        .description("Uses timer to speed up the game.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> timerSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer-speed")
        .description("Timer multiplier.")
        .defaultValue(1.2)
        .range(1.0, 2.0)
        .visible(() -> useTimer.get())
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Auto jump while moving.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Uses stricter speed calculations.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> lavaBoost = sgGeneral.add(new BoolSetting.Builder()
        .name("lava-boost")
        .description("Boosts speed in lava.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> waterSpeed = sgGeneral.add(new BoolSetting.Builder()
        .name("water-speed")
        .description("Applies speed in water.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> randomBoost = sgGeneral.add(new BoolSetting.Builder()
        .name("random-boost")
        .description("Randomly boosts speed occasionally.")
        .defaultValue(false)
        .build()
    );

    private int level = 1;
    private double moveSpeed;
    private double lastDist;
    private double boostSpeed;
    private long lastRandomBoost;
    private boolean lagDetected;
    private long detectionTime;

    public SpeedPlus() {
        super(WeirdPvP.CATEGORY, "speed+", "Movement speed with damage boost. Ported from Lemon client.");
    }

    @Override
    public void onActivate() {
        boostSpeed = 0;
        lastRandomBoost = System.currentTimeMillis();
        moveSpeed = getBaseSpeed();
        level = 1;
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            Modules.get().get(Timer.class).setOverride(1.0);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;

        if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() == mc.player.getId()) {
                detectionTime = System.currentTimeMillis();
                lagDetected = true;
                Vec3 vel = packet.movement();
                boostSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;

        if (System.currentTimeMillis() - detectionTime > 3182) {
            lagDetected = false;
        }

        // Jump every tick when on ground and moving
        if (jump.get() && PlayerUtils.isMoving() && mc.player.onGround()) {
            mc.player.jumpFromGround();
        }

        Vec3 delta = mc.player.position().subtract(mc.player.xOld, mc.player.yOld, mc.player.zOld);
        lastDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;
        if (mc.player.isFallFlying()) return;

        boolean moving = PlayerUtils.isMoving();
        if (!moving) {
            event.movement = new Vec3(0, event.movement.y, 0);
            level = 1;
            moveSpeed = getBaseSpeed();
            if (useTimer.get()) Modules.get().get(Timer.class).setOverride(1.0);
            return;
        }

        if (!waterSpeed.get() && mc.player.isInWater()) return;
        if (!lavaBoost.get() && mc.player.isInLava()) return;

        // Speed level stages
        if (mc.player.onGround()) {
            level = 2;
        }

        if (level != 1) {
            if (level == 2) {
                level = 3;
                if (!strict.get() && !mc.player.isShiftKeyDown()) {
                    moveSpeed *= 1.64847275;
                } else {
                    moveSpeed *= 1.433;
                }
            } else if (level == 3) {
                level = 4;
                moveSpeed = lastDist - 0.6553 * (lastDist - getBaseSpeed() + 0.04);
            } else {
                if (mc.player.onGround()) {
                    level = 1;
                }
                moveSpeed = lastDist - lastDist / 201.0;
            }
        } else {
            level = 2;
            moveSpeed = 1.418 * getBaseSpeed();
        }

        // Damage boost - add on top of current speed, don't reset
        if (damageBoost.get() && boostSpeed > 0) {
            moveSpeed += boostSpeed;
            boostSpeed = 0;
        }

        if (randomBoost.get() && System.currentTimeMillis() - lastRandomBoost > 3500 && !lagDetected && moving && mc.player.onGround()) {
            moveSpeed += moveSpeed / 6.0;
            lastRandomBoost = System.currentTimeMillis();
        }

        moveSpeed = Math.max(moveSpeed, getBaseSpeed());

        setMoveSpeed(event, moveSpeed);

        if (useTimer.get()) {
            Modules.get().get(Timer.class).setOverride(timerSpeed.get());
        }
    }

    private void setMoveSpeed(PlayerMoveEvent event, double speed) {
        float yaw = mc.player.getYRot();
        double forward = mc.player.zza;
        double strafe = mc.player.xxa;

        forward = forward > 0 ? 1.0 : forward < 0 ? -1.0 : 0;
        strafe = strafe > 0 ? 1.0 : strafe < 0 ? -1.0 : 0;

        double cos = Math.cos(Math.toRadians(yaw + 90));
        double sin = Math.sin(Math.toRadians(yaw + 90));

        double newX = forward * sin * speed + strafe * cos * speed;
        double newZ = forward * cos * speed - strafe * sin * speed;

        event.movement = new Vec3(newX, event.movement.y, newZ);
    }

    private double getBaseSpeed() {
        double speed = 0.2873;

        if (mc.player.hasEffect(MobEffects.SPEED)) {
            speed += 0.2873 * (mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.2;
        }

        if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
            speed -= 0.2873 * (mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1) * 0.15;
        }

        return Math.max(speed, 0.2873);
    }
}

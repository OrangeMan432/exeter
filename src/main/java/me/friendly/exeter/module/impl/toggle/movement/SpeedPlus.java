package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class SpeedPlus extends ToggleableModule {

    private final Property<Boolean> damageBoost = new Property<Boolean>(true, "Damage Boost");
    private final Property<Boolean> jump = new Property<Boolean>(true, "Jump");
    private final Property<Boolean> strict = new Property<Boolean>(false, "Strict");
    private final Property<Boolean> lavaBoost = new Property<Boolean>(true, "Lava Boost");
    private final Property<Boolean> waterSpeed = new Property<Boolean>(true, "Water Speed");
    private final Property<Boolean> randomBoost = new Property<Boolean>(false, "Random Boost");

    private int level = 1;
    private double moveSpeed;
    private double lastDist;
    private double boostSpeed;
    private long lastRandomBoost;
    private boolean lagDetected;
    private long detectionTime;

    public SpeedPlus() {
        super("Speed+", new String[]{"speedplus", "speed+"}, 0x00FF00, ModuleType.MOVEMENT);

        offerProperties(damageBoost, jump, strict, lavaBoost, waterSpeed, randomBoost);

        listeners.add(new Listener<PacketEvent>("speed_plus_packet") {
            @Override
            public void call(PacketEvent event) {
                SpeedPlus.this.onPacketReceive(event);
            }
        });

        listeners.add(new Listener<TickEvent>("speed_plus_tick") {
            @Override
            public void call(TickEvent event) {
                SpeedPlus.this.onTick(event);
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        boostSpeed = 0;
        lastRandomBoost = System.currentTimeMillis();
        moveSpeed = getBaseSpeed();
        level = 1;
    }

    private void onPacketReceive(PacketEvent event) {
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) return;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() == minecraft.player.getId()) {
                detectionTime = System.currentTimeMillis();
                lagDetected = true;
                Vec3 vel = packet.movement();
                boostSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            }
        }
    }

    private void onTick(TickEvent event) {
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) return;

        if (System.currentTimeMillis() - detectionTime > 3182) {
            lagDetected = false;
        }

        if (jump.getValue() && PlayerUtil.isMoving() && minecraft.player.onGround()) {
            minecraft.player.jumpFromGround();
        }

        Vec3 delta = minecraft.player.position().subtract(minecraft.player.xOld, minecraft.player.yOld, minecraft.player.zOld);
        lastDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        boolean moving = PlayerUtil.isMoving();
        if (!moving) return;

        if (!waterSpeed.getValue() && isLiquid()) return;

        if (minecraft.player.onGround()) {
            level = 2;
        }                if (level != 1) {
            if (level == 2) {
                level = 3;
                if (!strict.getValue() && !minecraft.player.isShiftKeyDown()) {
                    // 1.6x jumps flag on 5b5t; 1.35x stays under most max-delta thresholds
                    moveSpeed *= strict.getValue() ? 1.35 : 1.433;
                } else {
                    moveSpeed *= 1.433;
                }
            } else if (level == 3) {
                level = 4;
                moveSpeed = lastDist - 0.6553 * (lastDist - getBaseSpeed() + 0.04);
            } else {
                if (minecraft.player.onGround()) {
                    level = 1;
                }
                moveSpeed = lastDist - lastDist / 201.0;
            }
        } else {
            level = 2;
            moveSpeed = 1.418 * getBaseSpeed();
        }

        if (damageBoost.getValue() && boostSpeed > 0) {
            moveSpeed += boostSpeed;
            boostSpeed = 0;
        }

        if (randomBoost.getValue() && System.currentTimeMillis() - lastRandomBoost > 3500 && !lagDetected && moving && minecraft.player.onGround()) {
            moveSpeed += moveSpeed / 6.0;
            lastRandomBoost = System.currentTimeMillis();
        }

        moveSpeed = Math.max(moveSpeed, getBaseSpeed());

        setMoveSpeed(moveSpeed);
    }

    private void setMoveSpeed(double speed) {
        float yaw = minecraft.player.getYRot();
        double forward = minecraft.player.zza;
        double strafe = minecraft.player.xxa;

        forward = forward > 0 ? 1.0 : forward < 0 ? -1.0 : 0;
        strafe = strafe > 0 ? 1.0 : strafe < 0 ? -1.0 : 0;

        double cos = Math.cos(Math.toRadians(yaw + 90));
        double sin = Math.sin(Math.toRadians(yaw + 90));

        double newX = forward * sin * speed + strafe * cos * speed;
        double newZ = forward * cos * speed - strafe * sin * speed;

        minecraft.player.setDeltaMovement(newX, minecraft.player.getDeltaMovement().y, newZ);
    }

    private double getBaseSpeed() {
        double speed = 0.2873;

        if (minecraft.player.hasEffect(MobEffects.SPEED)) {
            speed += 0.2873 * (minecraft.player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.2;
        }

        if (minecraft.player.hasEffect(MobEffects.SLOWNESS)) {
            speed -= 0.2873 * (minecraft.player.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1) * 0.15;
        }

        return Math.max(speed, 0.2873);
    }

    private boolean isLiquid() {
        return minecraft.player.isInWater() || minecraft.player.isInLava();
    }
}

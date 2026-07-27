package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.api.stopwatch.Stopwatch;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public final class Trails
extends ToggleableModule {
    private final EnumProperty<ParticleType> particleType = new EnumProperty<ParticleType>(ParticleType.CLOUD, "Particle");
    private final NumberProperty<Long> delay = new NumberProperty<Long>(Long.valueOf(250L), 1L, 10000L, "Delay", "d");
    private final NumberProperty<Double> xOffset = new NumberProperty<Double>(Double.valueOf(0.0), -10.0, 10.0, "X Offset", "xoffset", "xo");
    private final NumberProperty<Double> yOffset = new NumberProperty<Double>(Double.valueOf(2.7), -10.0, 10.0, "Y Offset", "yoffset", "yo");
    private final NumberProperty<Double> zOffset = new NumberProperty<Double>(Double.valueOf(0.0), -10.0, 10.0, "Z Offset", "zoffset", "zo");
    private final Stopwatch stopwatch = new Stopwatch();

    public Trails() {
        super("Trails", new String[]{"trails"}, ModuleType.RENDER);
        this.offerProperties(this.delay, this.particleType, this.xOffset, this.yOffset, this.zOffset);
        this.listeners.add(new Listener<TickEvent>("trails_tick_listener"){

            @Override
            public void call(TickEvent event) {
                if (Trails.this.stopwatch.hasCompleted((Long)Trails.this.delay.getValue())) {
                    ((Trails)Trails.this).minecraft.level.addParticle(((ParticleType)((Trails)Trails.this).particleType.getValue()).particleType, false, false, ((Trails)Trails.this).minecraft.player.getX() + (Double)Trails.this.xOffset.getValue(), ((Trails)Trails.this).minecraft.player.getY() + (Double)Trails.this.yOffset.getValue(), ((Trails)Trails.this).minecraft.player.getZ() + (Double)Trails.this.zOffset.getValue(), 0.0, 0.0, 0.0);
                    Trails.this.stopwatch.reset();
                }
            }
        });
    }

    public static enum ParticleType {
        CLOUD(ParticleTypes.CLOUD),
        CRIT(ParticleTypes.CRIT),
        POOF(ParticleTypes.POOF),
        EXPLOSION(ParticleTypes.EXPLOSION),
        EXPLOSION_EMITTER(ParticleTypes.EXPLOSION_EMITTER),
        DRIPPING_LAVA(ParticleTypes.DRIPPING_LAVA),
        DRIPPING_WATER(ParticleTypes.DRIPPING_WATER);

        public SimpleParticleType particleType;

        private ParticleType(SimpleParticleType particleType) {
            this.particleType = particleType;
        }
    }
}


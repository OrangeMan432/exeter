package me.friendly.exeter.module.impl.toggle.world.fakeplayer.util;

import com.mojang.authlib.GameProfile;
import java.util.function.BooleanSupplier;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.util.ExplosionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FakePlayerEntity extends RemotePlayer {
  private BooleanSupplier damageSupplier = () -> true;
  private boolean hasTotem = true;

  public FakePlayerEntity(ClientLevel level, GameProfile gameProfile) {
    super(level, gameProfile);
  }

  @Override
  protected void actuallyHurt(
      net.minecraft.server.level.ServerLevel level, DamageSource damageSource, float damage) {
    if (!damageSupplier.getAsBoolean()) return;

    // Vanilla pipeline: armor/toughness, then resistance/protection, then absorption.
    // Explosion-typed (generic bypasses armor, so it must never be used here).
    damage = ExplosionUtil.mitigatedDamage(this, damageSource, damage);
    float absorbed = Math.min(this.getAbsorptionAmount(), damage);
    this.setAbsorptionAmount(this.getAbsorptionAmount() - absorbed);
    damage -= absorbed;
    if (damage <= 0.0F) return;

    float newHealth = this.getHealth() - damage;
    if (newHealth <= 0.0F && hasTotem) {
      popTotem(damageSource);
      return;
    }

    this.setHealth(Math.max(newHealth, 0.0F));
  }

  private void popTotem(DamageSource source) {
    this.setHealth(1.0F);
    this.setAbsorptionAmount(8.0F);
    this.removeAllEffects();
    this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
    this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
    this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));

    Minecraft mc = Minecraft.getInstance();

    mc.level.addAlwaysVisibleParticle(
        ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
    for (int i = 0; i < 30; i++) {
      mc.level.addAlwaysVisibleParticle(
          ParticleTypes.TOTEM_OF_UNDYING,
          this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
          this.getY() + this.random.nextDouble() * 2.0,
          this.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
          (this.random.nextDouble() - 0.5) * 0.2,
          this.random.nextDouble() * 0.2,
          (this.random.nextDouble() - 0.5) * 0.2);
    }

    mc.level.playLocalSound(
        this.getX(),
        this.getY(),
        this.getZ(),
        SoundEvents.TOTEM_USE,
        SoundSource.PLAYERS,
        1.0F,
        1.0F,
        false);

    ClientboundEntityEventPacket packet = new ClientboundEntityEventPacket(this, (byte) 35);
    Exeter.getInstance().getEventManager().dispatch(new PacketEvent(packet));
  }

  /**
   * Damage the dummy the way the client sees it: i-frames first, then the vanilla mitigation
   * pipeline. Vanilla's own hurt path is server side, so the window is applied here instead of
   * letting every explosion land.
   */
  public void applyDamage(float damage) {
    if (hurtTime > 0) {
      me.friendly.exeter.logging.DebugLogger.get()
          .logFile(
              "FakePlayer",
              "ignored "
                  + String.format("%.1f", damage)
                  + " damage, "
                  + hurtTime
                  + " iframes left");
      return;
    }
    float before = this.getHealth();
    var source = ExplosionUtil.explosionSource(this);
    this.actuallyHurt(null, source, damage);
    if (this.getHealth() != before) {
      // Same values vanilla's client side damage event uses: 10 ticks of invulnerability.
      this.hurtDuration = 10;
      this.hurtTime = 10;
      this.handleDamageEvent(source);
    }
  }

  public void setDamageSupplier(BooleanSupplier supplier) {
    this.damageSupplier = supplier;
  }

  public void setHasTotem(boolean hasTotem) {
    this.hasTotem = hasTotem;
  }

  public boolean hasTotem() {
    return hasTotem;
  }

  public void setMotionDirect(double x, double y, double z) {
    this.setDeltaMovement(x, y, z);
  }
}

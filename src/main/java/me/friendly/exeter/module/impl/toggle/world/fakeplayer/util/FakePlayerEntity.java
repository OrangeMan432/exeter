package me.friendly.exeter.module.impl.toggle.world.fakeplayer.util;

import com.mojang.authlib.GameProfile;
import java.util.function.BooleanSupplier;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

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
    damage = this.getDamageAfterArmorAbsorb(damageSource, damage);
    damage = this.magicAbsorb(damageSource, damage);
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

  /**
   * Client-safe mirror of vanilla {@code getDamageAfterMagicAbsorb} (resistance exact, stats
   * skipped). Protection enchantments resolve their effects against a ServerLevel, which only
   * exists on an integrated server; on remote servers protection is skipped while armor, toughness
   * and resistance still apply.
   */
  private float magicAbsorb(DamageSource damageSource, float damage) {
    if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) return damage;
    if (this.hasEffect(MobEffects.RESISTANCE)
        && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
      int scale = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
      damage = Math.max(damage * (25 - scale) / 25.0F, 0.0F);
    }
    if (damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) return damage;
    var server = Minecraft.getInstance().getSingleplayerServer();
    if (server != null) {
      var serverLevel = server.getLevel(this.level().dimension());
      if (serverLevel != null) {
        damage =
            CombatRules.getDamageAfterMagicAbsorb(
                damage, EnchantmentHelper.getDamageProtection(serverLevel, this, damageSource));
      }
    }
    return damage;
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

  public void applyDamage(float damage) {
    this.actuallyHurt(null, this.level().damageSources().generic(), damage);
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

package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

public class ListenerAttack extends Listener<PacketEvent> {
  private static final Minecraft mc = Minecraft.getInstance();
  private final FakePlayerModule module;

  public ListenerAttack(FakePlayerModule module) {
    super("fakeplayer_attack");
    this.module = module;
  }

  @Override
  public void call(PacketEvent event) {
    if (!module.isDamageEnabled()) return;
    if (mc.level == null || mc.player == null) return;
    if (module.getFakePlayer() == null) return;

    // Modern attacks arrive as ServerboundAttackPacket; older paths use Interact. Handle both.
    if (event.getPacket() instanceof ServerboundAttackPacket attackPacket) {
      Entity target = mc.level.getEntity(attackPacket.entityId());
      if (target == null || target != module.getFakePlayer()) return;
      event.setCanceled(true);
      applyHit();
      return;
    }
    if (!(event.getPacket() instanceof ServerboundInteractPacket packet)) return;

    Entity target = mc.level.getEntity(packet.entityId());
    if (target == null || target != module.getFakePlayer()) return;

    event.setCanceled(true);
    applyHit();
  }

  private void applyHit() {
    Vec3 pos = mc.player.position();
    float cooldown = mc.player.getAttackStrengthScale(0.5f);

    if (cooldown > 0.9f) {
      mc.player
          .level()
          .playLocalSound(
              pos.x,
              pos.y,
              pos.z,
              SoundEvents.PLAYER_ATTACK_STRONG,
              SoundSource.PLAYERS,
              1.0F,
              1.0F,
              false);
    } else {
      mc.player
          .level()
          .playLocalSound(
              pos.x,
              pos.y,
              pos.z,
              SoundEvents.PLAYER_ATTACK_WEAK,
              SoundSource.PLAYERS,
              1.0F,
              1.0F,
              false);
    }

    mc.player.swing(InteractionHand.MAIN_HAND, true);

    float damage = 1.0F + cooldown * 0.5F;
    float sharpBonus = getSharpBonus();
    damage += sharpBonus;

    ItemStack mainHand = mc.player.getMainHandItem();
    if (mainHand.is(Items.MACE) && MaceItem.canSmashAttack(mc.player)) {
      damage += smashBonus(mc.player.fallDistance);
    }

    module.getFakePlayer().applyDamage(damage);
  }

  /**
   * Mirrors vanilla's mace smash fall curve so dive tests register real numbers on the dummy.
   * Density needs the server and is skipped, same as the rest of this local pipeline.
   */
  private float smashBonus(double fallDistance) {
    if (fallDistance <= 3.0) {
      return (float) (4.0 * fallDistance);
    }
    if (fallDistance <= 8.0) {
      return (float) (12.0 + 2.0 * (fallDistance - 3.0));
    }
    return (float) (22.0 + (fallDistance - 8.0));
  }

  private float getSharpBonus() {
    net.minecraft.world.item.ItemStack mainHand = mc.player.getMainHandItem();
    int level = 0;
    for (var entry : mainHand.getEnchantments().entrySet()) {
      if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)) {
        level = entry.getIntValue();
        break;
      }
    }
    if (level > 0) {
      return 0.5F * level + 0.5F;
    }
    return 0.0F;
  }
}

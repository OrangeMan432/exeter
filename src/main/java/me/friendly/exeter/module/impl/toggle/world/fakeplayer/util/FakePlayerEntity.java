package me.friendly.exeter.module.impl.toggle.world.fakeplayer.util;

import me.friendly.exeter.module.Module;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.FakePlayerModule;
import net.minecraft.client.network.OtherPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Client-side-only player dummy. Beta has no status effects, totems or gapples,
 * so those parts of the modern FakePlayer are skipped; recording, playback,
 * armor copy and respawn are kept.
 */
public class FakePlayerEntity extends OtherPlayerEntity {

  public FakePlayerEntity(World world) {
    super(world, "FakePlayer");
  }

  private boolean damageAllowed() {
    if (Exeter.getInstance() == null) return true;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("fakeplayer");
    if (module instanceof FakePlayerModule) {
      return ((FakePlayerModule) module).isDamageEnabled();
    }
    return true;
  }

  @Override
  public boolean damage(Entity attacker, int damage) {
    if (!damageAllowed()) {
      return false;
    }
    return super.damage(attacker, damage);
  }
}

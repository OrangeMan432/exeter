package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.logging.Logger;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Meteor-Rejects-pattern AutoExtinguish: puts out fire around you and water-buckets yourself. */
public class AutoExtinguish extends ToggleableModule {

  private final Property<Boolean> extinguish = new Property<Boolean>(false, "Extinguish");
  private final NumberProperty<Integer> horizontalRadius =
      new NumberProperty<Integer>(4, 0, 6, "Horizontal Radius");
  private final NumberProperty<Integer> verticalRadius =
      new NumberProperty<Integer>(4, 0, 6, "Vertical Radius");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(5, 1, 50, "Blocks Per Tick");
  private final Property<Boolean> waterBucket = new Property<Boolean>(false, "Water Bucket");
  private final Property<Boolean> center = new Property<Boolean>(false, "Center");
  private final Property<Boolean> onGround = new Property<Boolean>(false, "On Ground");

  private boolean hasPlacedWater;
  private boolean warnedNether;

  public AutoExtinguish() {
    super("AutoExtinguish", new String[] {"autoextinguish", "extinguish"}, 0xFF5500,
        ModuleType.MISCELLANEOUS);
    setDescription("Puts out fire around you and buckets yourself.");
    offerProperties(
        extinguish, horizontalRadius, verticalRadius, blocksPerTick, waterBucket, center, onGround);
    this.listeners.add(
        new Listener<TickEvent>("autoextinguish_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoExtinguish.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    hasPlacedWater = false;
    warnedNether = false;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (onGround.getValue() && !minecraft.player.onGround()) return;

    boolean nether = minecraft.level.dimension() == Level.NETHER;
    if (nether && !warnedNether) {
      warnedNether = true;
      Logger.getLogger().printToChat("Water buckets don't work in this dimension.");
    } else if (!nether) {
      warnedNether = false;
    }

    if (waterBucket.getValue() && !nether) {
      if (hasPlacedWater) {
        int slot = PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.BUCKET);
        if (slot != -1) {
          if (center.getValue()) centerPlayer();
          float yaw = minecraft.player.getYRot();
          boolean swapped = PlayerUtil.swapToSlot(slot, true);
          PlayerUtil.withRotation(yaw, 90, () -> useItem());
          PlayerUtil.swapBackIf(swapped);
        }
        hasPlacedWater = false;
      } else if (!minecraft.player.hasEffect(MobEffects.FIRE_RESISTANCE)
          && minecraft.player.isOnFire()) {
        BlockPos feet = minecraft.player.blockPosition();
        if (isFire(minecraft.level.getBlockState(feet))) {
          extinguishFire(feet);
        }
        int slot =
            PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.WATER_BUCKET);
        if (slot != -1) {
          if (center.getValue()) centerPlayer();
          float yaw = minecraft.player.getYRot();
          boolean swapped = PlayerUtil.swapToSlot(slot, true);
          PlayerUtil.withRotation(yaw, 90, () -> useItem());
          PlayerUtil.swapBackIf(swapped);
          hasPlacedWater = true;
        }
      }
    }

    if (extinguish.getValue()) {
      BlockPos origin = minecraft.player.blockPosition();
      int hr = horizontalRadius.getValue();
      int vr = verticalRadius.getValue();
      int budget = blocksPerTick.getValue();
      for (int x = -hr; x <= hr && budget > 0; x++) {
        for (int y = -vr; y <= vr && budget > 0; y++) {
          for (int z = -hr; z <= hr && budget > 0; z++) {
            BlockPos pos = origin.offset(x, y, z);
            if (isFire(minecraft.level.getBlockState(pos))) {
              extinguishFire(pos);
              budget--;
            }
          }
        }
      }
    }
  }

  private static boolean isFire(BlockState state) {
    return state.getBlock() == Blocks.FIRE || state.getBlock() == Blocks.SOUL_FIRE;
  }

  private void extinguishFire(BlockPos pos) {
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
    PlayerUtil.swingHand();
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
  }

  private void useItem() {
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
  }

  private void centerPlayer() {
    BlockPos pos = minecraft.player.blockPosition();
    minecraft.player.absSnapTo(
        pos.getX() + 0.5, minecraft.player.getY(), pos.getZ() + 0.5);
  }
}

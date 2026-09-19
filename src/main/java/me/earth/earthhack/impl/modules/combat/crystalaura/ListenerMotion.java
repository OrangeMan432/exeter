package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ListenerMotion extends ModuleListener<CrystalAura, MotionUpdateEvent>
{
    public ListenerMotion(CrystalAura module)
    {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        switch (event.getStage())
        {
            case PRE:
                break;
            default:
                return;
        }

        module.target = findTarget();
        if (module.target == null)
        {
            module.renderPos = null;
            return;
        }

        if (module.rotate.getValue() == CrystalAura.Rotate.Silent)
        {
            float[] rotations = RotationUtil.getRotations(
                module.target, 1.0);
            Managers.ROTATION.setServerRotations(rotations[0],
                                                 rotations[1]);
        }

        doBreak();
        doPlace();
    }

    private EntityPlayer findTarget()
    {
        EntityPlayer best = null;
        double bestDist = module.targetRange.getValue();
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || player.getHealth() <= 0.0f
                || Managers.FRIENDS.contains(player))
            {
                continue;
            }

            double dist = mc.player.getDistance(player);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = player;
            }
        }

        return best;
    }

    private void doBreak()
    {
        List<EntityEnderCrystal> crystals = new ArrayList<>();
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityEnderCrystal))
            {
                continue;
            }

            EntityEnderCrystal crystal = (EntityEnderCrystal) entity;
            double range = mc.player.canEntityBeSeen(crystal)
                ? module.breakRange.getValue()
                : module.wallRange.getValue();
            if (mc.player.getDistance(crystal) > range)
            {
                continue;
            }

            float targetDamage = DamageUtil.calculate(
                crystal.posX, crystal.posY, crystal.posZ, module.target);
            float selfDamage = DamageUtil.calculate(
                crystal.posX, crystal.posY, crystal.posZ, mc.player);
            if (targetDamage < module.minBreak.getValue()
                || selfDamage > module.maxSelfBreak.getValue())
            {
                continue;
            }

            if (module.antiSuicide.getValue()
                && mc.player.getHealth() + mc.player.getAbsorptionAmount()
                    <= selfDamage)
            {
                continue;
            }

            crystals.add(crystal);
        }

        crystals.sort(Comparator.comparingDouble(
            c -> -DamageUtil.calculate(
                c.posX, c.posY, c.posZ, module.target)));
        int broken = 0;
        for (EntityEnderCrystal crystal : crystals)
        {
            if (broken >= module.maxCrystals.getValue()
                || !module.breakTimer.passed(
                    module.breakDelay.getValue() * 50L))
            {
                break;
            }

            mc.playerController.attackEntity(mc.player, crystal);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            module.breakTimer.reset();
            broken++;
        }
    }

    private void doPlace()
    {
        if (!module.placeTimer.passed(module.placeDelay.getValue() * 50L))
        {
            return;
        }

        int crystalSlot = findCrystal();
        if (crystalSlot == -1)
        {
            module.renderPos = null;
            return;
        }

        List<BlockPos> positions = new ArrayList<>();
        BlockPos playerPos = new BlockPos(mc.player.posX,
                                          mc.player.posY,
                                          mc.player.posZ);
        int range = (int) Math.ceil(module.placeRange.getValue());
        for (int x = -range; x <= range; x++)
        {
            for (int y = -range; y <= range; y++)
            {
                for (int z = -range; z <= range; z++)
                {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (!canPlace(pos))
                    {
                        continue;
                    }

                    double dist = mc.player.getDistance(
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    boolean seen = mc.player.canEntityBeSeen(
                        new net.minecraft.entity.item.EntityEnderCrystal(
                            mc.world, pos.getX() + 0.5, pos.getY() + 1.0,
                            pos.getZ() + 0.5));
                    if (dist > (seen
                            ? module.placeRange.getValue()
                            : module.wallRange.getValue()))
                    {
                        continue;
                    }

                    positions.add(pos.toImmutable());
                }
            }
        }

        if (module.placements.getValue() == CrystalAura.Placements.Damage)
        {
            positions.sort(Comparator.comparingDouble(
                pos -> -DamageUtil.calculate(
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    module.target)));
        }
        else
        {
            positions.sort(Comparator.comparingDouble(
                pos -> mc.player.getDistanceSq(pos)));
        }

        for (BlockPos pos : positions)
        {
            float targetDamage = DamageUtil.calculate(
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                module.target);
            float selfDamage = DamageUtil.calculate(
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                mc.player);
            if (targetDamage < module.minPlace.getValue()
                || selfDamage > module.maxSelfPlace.getValue())
            {
                continue;
            }

            if (module.antiSuicide.getValue()
                && mc.player.getHealth() + mc.player.getAbsorptionAmount()
                    <= selfDamage)
            {
                continue;
            }

            int oldSlot = mc.player.inventory.currentItem;
            swapTo(crystalSlot);
            mc.playerController.processRightClickBlock(
                mc.player,
                mc.world,
                pos,
                EnumFacing.UP,
                new Vec3d(pos).add(0.5, 0.5, 0.5),
                EnumHand.MAIN_HAND);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            if (module.switchMode.getValue() != CrystalAura.Switch.None)
            {
                mc.player.inventory.currentItem = oldSlot;
                mc.playerController.updateController();
            }

            module.renderPos = pos;
            module.placeTimer.reset();
            return;
        }

        module.renderPos = null;
    }

    private boolean canPlace(BlockPos pos)
    {
        return (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN
                || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK)
            && mc.world.getBlockState(pos.up()).getMaterial().isReplaceable()
            && mc.world.getBlockState(pos.up().up()).getMaterial()
                .isReplaceable();
    }

    private int findCrystal()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                == Items.END_CRYSTAL)
            {
                return i;
            }
        }

        return -1;
    }

    private void swapTo(int slot)
    {
        if (module.switchMode.getValue() == CrystalAura.Switch.Packet)
        {
            mc.player.connection.sendPacket(
                new CPacketHeldItemChange(slot));
        }
        else
        {
            mc.player.inventory.currentItem = slot;
            mc.playerController.updateController();
        }
    }
}

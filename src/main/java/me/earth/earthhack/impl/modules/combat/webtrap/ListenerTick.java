package me.earth.earthhack.impl.modules.combat.webtrap;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ListenerTick extends ModuleListener<WebTrap, TickEvent>
{
    public ListenerTick(WebTrap module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        module.target = null;
        int webSlot = findWeb();
        if (webSlot == -1)
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[WebTrap] No webs in hotbar, disabling.");
            module.disable();
            return;
        }

        if (module.autoDisable.getValue()
            && !module.startPos.equals(
                new BlockPos(mc.player.posX,
                             mc.player.posY,
                             mc.player.posZ)))
        {
            module.disable();
            return;
        }

        if (mc.player.inventory.currentItem != module.lastSlot
            && mc.player.inventory.currentItem != webSlot)
        {
            module.lastSlot = mc.player.inventory.currentItem;
        }

        stopSneaking();
        module.target = findTarget();
        if (module.target == null
            || !module.delayTimer.passed(module.delay.getValue()))
        {
            return;
        }

        List<BlockPos> targets = new ArrayList<>();
        BlockPos base = new BlockPos(module.target.posX,
                                     module.target.posY,
                                     module.target.posZ);
        if (module.feet.getValue())
        {
            targets.add(base);
        }

        if (module.face.getValue())
        {
            targets.add(base.up());
        }

        targets.sort(Comparator.comparingDouble(
            pos -> mc.player.getDistanceSq(pos)));
        int oldSlot = mc.player.inventory.currentItem;
        mc.player.inventory.currentItem = webSlot;
        mc.playerController.updateController();

        int placed = 0;
        for (BlockPos pos : targets)
        {
            if (placed >= module.blocksPerTick.getValue())
            {
                break;
            }

            if (!mc.world.getBlockState(pos).getMaterial().isReplaceable()
                || mc.world.getBlockState(pos).getBlock() == Blocks.WEB)
            {
                continue;
            }

            mc.playerController.processRightClickBlock(
                mc.player,
                mc.world,
                pos,
                EnumFacing.UP,
                new Vec3d(pos).add(0.5, 0.5, 0.5),
                EnumHand.MAIN_HAND);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            module.renderBlocks.put(pos, System.currentTimeMillis());
            placed++;
        }

        mc.player.inventory.currentItem = oldSlot;
        mc.playerController.updateController();
        if (placed > 0)
        {
            module.delayTimer.reset();
        }
    }

    private EntityPlayer findTarget()
    {
        EntityPlayer best = null;
        double bestDist = module.range.getValue() * module.range.getValue();
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || player.isInWeb
                || Managers.FRIENDS.contains(player))
            {
                continue;
            }

            double dist = mc.player.getDistanceSq(player);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = player;
            }
        }

        return best;
    }

    private int findWeb()
    {
        Item web = Item.getItemFromBlock(Blocks.WEB);
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem() == web)
            {
                return i;
            }
        }

        return -1;
    }

    private void stopSneaking()
    {
        if (mc.player.isSneaking())
        {
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
            module.isSneaking = true;
        }
        else if (module.isSneaking)
        {
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.START_SNEAKING));
            module.isSneaking = false;
        }
    }
}

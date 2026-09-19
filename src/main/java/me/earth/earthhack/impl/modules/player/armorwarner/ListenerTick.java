package me.earth.earthhack.impl.modules.player.armorwarner;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.Map;

final class ListenerTick extends ModuleListener<ArmorWarner, TickEvent>
{
    public ListenerTick(ArmorWarner module)
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

        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player.isDead
                || player != mc.player
                    && !Managers.FRIENDS.contains(player))
            {
                continue;
            }

            for (ItemStack stack : player.inventory.armorInventory)
            {
                if (stack == null || stack.isEmpty())
                {
                    continue;
                }

                int percent = durabilityPercent(stack);
                if (percent <= module.threshold.getValue()
                    && !module.warnedPieces.containsKey(player))
                {
                    if (player == mc.player && module.notifySelf.getValue())
                    {
                        ChatUtil.sendMessage(TextFormatting.RED
                            + "Your " + pieceName(stack) + " low dura!");
                    }

                    if (player != mc.player
                        && module.notifyFriends.getValue())
                    {
                        mc.player.sendChatMessage("/msg " + player.getName()
                            + " Yo, " + player.getName()
                            + ", ur " + pieceName(stack) + " low dura!");
                    }

                    module.warnedPieces.put(player,
                        player.inventory.armorInventory.indexOf(stack));
                }

                if (!module.warnedPieces.containsKey(player)
                    || module.warnedPieces.get(player)
                        != player.inventory.armorInventory.indexOf(stack)
                    || percent <= module.threshold.getValue())
                {
                    continue;
                }

                module.warnedPieces.remove(player);
            }

            if (!module.warnedPieces.containsKey(player)
                || !player.inventory
                    .armorInventory
                    .get(module.warnedPieces.get(player))
                    .isEmpty())
            {
                continue;
            }

            module.warnedPieces.remove(player);
        }
    }

    private int durabilityPercent(ItemStack stack)
    {
        if (stack.getMaxDamage() <= 0)
        {
            return 100;
        }

        return (stack.getMaxDamage() - stack.getItemDamage()) * 100
            / stack.getMaxDamage();
    }

    private String pieceName(ItemStack stack)
    {
        if (stack.getItem() == Items.DIAMOND_HELMET
            || stack.getItem() == Items.GOLDEN_HELMET
            || stack.getItem() == Items.IRON_HELMET
            || stack.getItem() == Items.CHAINMAIL_HELMET
            || stack.getItem() == Items.LEATHER_HELMET)
        {
            return "helmet is";
        }

        if (stack.getItem() == Items.DIAMOND_CHESTPLATE
            || stack.getItem() == Items.GOLDEN_CHESTPLATE
            || stack.getItem() == Items.IRON_CHESTPLATE
            || stack.getItem() == Items.CHAINMAIL_CHESTPLATE
            || stack.getItem() == Items.LEATHER_CHESTPLATE)
        {
            return "chest is";
        }

        if (stack.getItem() == Items.DIAMOND_LEGGINGS
            || stack.getItem() == Items.GOLDEN_LEGGINGS
            || stack.getItem() == Items.IRON_LEGGINGS
            || stack.getItem() == Items.CHAINMAIL_LEGGINGS
            || stack.getItem() == Items.LEATHER_LEGGINGS)
        {
            return "leggings are";
        }

        return "boots are";
    }
}

package me.earth.earthhack.impl.modules.misc.autodupe;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlaceRecipe;
import net.minecraft.util.text.TextFormatting;

final class ListenerTick extends ModuleListener<AutoItemDupe, TickEvent>
{
    public ListenerTick(AutoItemDupe module)
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

        if (module.phase == AutoItemDupe.Phase.DROP)
        {
            doDrop();
        }
        else
        {
            doPickup();
        }
    }

    private void doDrop()
    {
        int slot = module.mode.getValue() == AutoItemDupe.Mode.HELD
            ? mc.player.inventory.currentItem
            : nextSlot();
        if (slot == -1)
        {
            if (module.mode.getValue() == AutoItemDupe.Mode.ALL)
            {
                ChatUtil.sendMessage("Nothing left to dupe, disabling.");
                module.disable();
            }

            return;
        }

        ItemStack held = mc.player.inventory.getStackInSlot(slot);
        if (held.isEmpty())
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[AutoItemDupe] Hold an item.");
            module.disable();
            return;
        }

        if (findPlanks() == -1)
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[AutoItemDupe] Need planks in inventory.");
            module.disable();
            return;
        }

        module.oldPitch = mc.player.rotationPitch;
        mc.player.rotationPitch = 180.0f;
        module.pitched = true;

        module.idBefore = Item.getIdFromItem(held.getItem());
        module.countBefore = countItem(module.idBefore);
        module.slotBefore = slot;

        mc.playerController.windowClick(
            mc.player.inventoryContainer.windowId,
            slot < 9 ? slot + 36 : slot,
            1,
            ClickType.THROW,
            mc.player);
        mc.displayGuiScreen(new GuiInventory(mc.player));

        module.timer.reset();
        module.phase = AutoItemDupe.Phase.PICKUP;
    }

    private void doPickup()
    {
        if (!module.timer.passed(
                Math.max(1, module.delay.getValue()) * 50L))
        {
            return;
        }

        module.timer.reset();
        if (countItem(module.idBefore) <= module.countBefore)
        {
            return;
        }

        mc.player.connection.sendPacket(new CPacketPlaceRecipe(
            mc.player.openContainer.windowId, module.recipe, false));

        if (module.pitched)
        {
            mc.player.rotationPitch = module.oldPitch;
            module.pitched = false;
        }

        ChatUtil.sendMessage(TextFormatting.GREEN
            + "[AutoItemDupe] Duped, count went up.");
        if (!module.autoRepeat.getValue())
        {
            module.disable();
            return;
        }

        module.phase = AutoItemDupe.Phase.DROP;
    }

    private int nextSlot()
    {
        if (!module.queue.isEmpty())
        {
            return module.queue.poll();
        }

        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && !isIngredient(stack))
            {
                module.queue.add(i);
            }
        }

        return module.queue.isEmpty() ? -1 : module.queue.poll();
    }

    private boolean isIngredient(ItemStack stack)
    {
        if (stack.getItem() instanceof ItemBlock)
        {
            return ((ItemBlock) stack.getItem()).getBlock()
                == Blocks.PLANKS;
        }

        return false;
    }

    private int countItem(int id)
    {
        int count = 0;
        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && Item.getIdFromItem(stack.getItem()) == id)
            {
                count += stack.getCount();
            }
        }

        return count;
    }

    private int findPlanks()
    {
        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock()
                    == Blocks.PLANKS)
            {
                return i;
            }
        }

        return -1;
    }
}

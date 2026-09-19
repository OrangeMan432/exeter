package me.earth.earthhack.impl.modules.render.shulkerpreview;

import me.earth.earthhack.impl.event.events.render.ToolTipEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

import java.awt.*;

final class ListenerToolTip extends ModuleListener<ShulkerPreview, ToolTipEvent>
{
    public ListenerToolTip(ShulkerPreview module)
    {
        super(module, ToolTipEvent.class);
    }

    @Override
    public void invoke(ToolTipEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        ItemStack stack = event.getStack();
        if (stack.isEmpty()
            || !(stack.getItem() instanceof ItemShulkerBox))
        {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null
            || !tag.hasKey("BlockEntityTag", 10)
            || !tag.getCompoundTag("BlockEntityTag").hasKey("Items", 9))
        {
            return;
        }

        int x = event.getX() + 12;
        int y = event.getY() + 12;
        Render2DUtil.drawRect(x, y, x + 176, y + 78,
                              new Color(10, 10, 10, 200).getRGB());
        if (module.showName.getValue())
        {
            mc.fontRenderer.drawStringWithShadow(
                stack.getDisplayName(), x + 8, y + 6, 0xFFFFFF);
        }

        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();

        NonNullList<ItemStack> items =
            NonNullList.withSize(27, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(
            tag.getCompoundTag("BlockEntityTag"), items);
        for (int i = 0; i < items.size(); i++)
        {
            int iX = x + i % 9 * 18 + 8;
            int iY = y + i / 9 * 18 + 18;
            ItemStack item = items.get(i);
            mc.getRenderItem().zLevel = 501.0f;
            mc.getRenderItem().renderItemAndEffectIntoGUI(item, iX, iY);
            mc.getRenderItem().renderItemOverlayIntoGUI(
                mc.fontRenderer, item, iX, iY, null);
            mc.getRenderItem().zLevel = 0.0f;
        }

        GlStateManager.disableLighting();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

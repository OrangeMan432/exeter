package me.earth.earthhack.impl.modules.misc.peek;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ListenerRender2D
        extends ModuleListener<Peek, Render2DEvent>
{
    final Map<EntityPlayer, ItemStack> spied = new ConcurrentHashMap<>();

    public ListenerRender2D(Peek module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        int x = event.getResolution().getScaledWidth() / 2 - 78;
        int y = 24;
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || !(player.getHeldItemMainhand().getItem()
                    instanceof ItemShulkerBox))
            {
                continue;
            }

            renderShulker(player.getHeldItemMainhand(),
                          x,
                          y,
                          player.getName());
        }
    }

    private void renderShulker(ItemStack stack, int x, int y, String name)
    {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null
            || !tag.hasKey("BlockEntityTag", 10)
            || !tag.getCompoundTag("BlockEntityTag").hasKey("Items", 9))
        {
            return;
        }

        Render2DUtil.drawRect(x, y, x + 176, y + 73,
                              new Color(10, 10, 10, 200).getRGB());
        mc.fontRenderer.drawStringWithShadow(
            name == null ? stack.getDisplayName() : name, x + 8, y + 6,
            0xFFFFFF);

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

package me.earth.earthhack.impl.modules.render.inventorypreview;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

final class ListenerRender2D
        extends ModuleListener<InventoryPreview, Render2DEvent>
{
    public ListenerRender2D(InventoryPreview module)
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

        int x = module.x.getValue();
        int y = module.y.getValue();
        Render2DUtil.drawRect(x + 7, y + 17, x + 171, y + 73,
                              module.rectColor.getValue().getRGB());

        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();

        for (int i = 0; i < 27; i++)
        {
            int iX = x + i % 9 * 18 + 8;
            int iY = y + i / 9 * 18 + 18;
            ItemStack stack =
                mc.player.inventory.mainInventory.get(i + 9);
            mc.getRenderItem().zLevel = 501.0f;
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, iX, iY);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer,
                                                        stack,
                                                        iX,
                                                        iY,
                                                        null);
            mc.getRenderItem().zLevel = 0.0f;
        }

        GlStateManager.disableLighting();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

package me.earth.earthhack.impl.modules.render.armourhud;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.item.ItemStack;

final class ListenerRender2D
        extends ModuleListener<ArmourHUD, Render2DEvent>
{
    public ListenerRender2D(ArmourHUD module)
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

        int y = module.y.getValue();
        for (int i = 3; i >= 0; i--)
        {
            ItemStack stack =
                mc.player.inventory.armorInventory.get(i);
            if (stack == null || stack.isEmpty())
            {
                continue;
            }

            String text = stack.getDisplayName();
            if (module.durability.getValue() && stack.getMaxDamage() > 0)
            {
                int percent = (stack.getMaxDamage() - stack.getItemDamage())
                    * 100 / stack.getMaxDamage();
                text += " " + percent + "%";
            }

            mc.fontRenderer.drawStringWithShadow(
                text, module.x.getValue(), y, 0xFFFFFF);
            y += 10;
        }
    }
}

package me.earth.earthhack.impl.modules.misc.autosign;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.util.text.TextComponentString;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class ListenerTick extends ModuleListener<AutoSign, TickEvent>
{
    public ListenerTick(AutoSign module)
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

        if (!(mc.currentScreen instanceof GuiEditSign))
        {
            module.wasInSign = false;
            return;
        }

        if (!module.wasInSign)
        {
            module.wasInSign = true;
            return;
        }

        GuiEditSign sign = (GuiEditSign) mc.currentScreen;
        sign.tileSign.signText[0] =
            new TextComponentString(line(module.line1.getValue()));
        sign.tileSign.signText[1] =
            new TextComponentString(line(module.line2.getValue()));
        sign.tileSign.signText[2] =
            new TextComponentString(line(module.line3.getValue()));
        sign.tileSign.signText[3] =
            new TextComponentString(line(module.line4.getValue()));

        if (module.autoComplete.getValue())
        {
            sign.tileSign.markDirty();
            mc.displayGuiScreen(null);
            module.wasInSign = false;
        }
    }

    private String line(String value)
    {
        if (value.contains("~date~"))
        {
            return value.replace("~date~",
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        }

        return value;
    }
}

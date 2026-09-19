package me.earth.earthhack.impl.modules.misc.coords;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Copies your coordinates to the clipboard.
 * Ported from Mio 0.6.9 (Coords).
 */
public class Coords extends Module
{
    public Coords()
    {
        super("Coords", Category.Misc);
        this.setData(new SimpleData(this,
            "Copies your position to the clipboard."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player == null)
        {
            return;
        }

        String coords = "X: " + (int) mc.player.posX
            + " Y: " + (int) mc.player.posY
            + " Z: " + (int) mc.player.posZ;
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(coords), null);
        disable();
    }
}

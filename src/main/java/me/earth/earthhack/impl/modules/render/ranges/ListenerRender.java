package me.earth.earthhack.impl.modules.render.ranges;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

final class ListenerRender extends ModuleListener<Ranges, Render3DEvent>
{
    public ListenerRender(Ranges module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || !module.circle.getValue())
        {
            return;
        }

        RenderManager renderManager = mc.getRenderManager();
        double x = mc.player.posX - renderManager.renderPosX;
        double y = mc.player.posY - renderManager.renderPosY + 0.1;
        double z = mc.player.posZ - renderManager.renderPosZ;
        float hue = (System.currentTimeMillis() % 7200L) / 7200.0f;
        Color color = new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f));

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GL11.glLineWidth(module.lineWidth.getValue());
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GlStateManager.color(color.getRed() / 255.0f,
                             color.getGreen() / 255.0f,
                             color.getBlue() / 255.0f,
                             1.0f);
        for (int i = 0; i <= 360; i += 2)
        {
            double rad = i * Math.PI / 180.0;
            GL11.glVertex3d(x + Math.sin(rad) * module.radius.getValue(),
                            y,
                            z + Math.cos(rad) * module.radius.getValue());
        }

        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

package me.earth.earthhack.impl.modules.render.hitspheres;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import java.awt.*;

final class ListenerRender extends ModuleListener<HitSpheres, Render3DEvent>
{
    public ListenerRender(HitSpheres module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || Managers.FRIENDS.contains(player)
                || mc.player.getDistance(player)
                    > module.range.getValue())
            {
                continue;
            }

            double dist = mc.player.getDistance(player);
            Color color = dist >= 8.0
                ? module.farColor.getValue()
                : new Color(module.nearColor.getValue().getRed(),
                            (int) Math.min(255.0, dist * 255.0 / 150.0),
                            module.nearColor.getValue().getBlue(),
                            255);
            double x = player.lastTickPosX
                + (player.posX - player.lastTickPosX)
                    * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosX;
            double y = player.lastTickPosY
                + (player.posY - player.lastTickPosY)
                    * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosY;
            double z = player.lastTickPosZ
                + (player.posZ - player.lastTickPosZ)
                    * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosZ;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
            GL11.glLineWidth(module.lineWidth.getValue());
            GlStateManager.color(color.getRed() / 255.0f,
                                 color.getGreen() / 255.0f,
                                 color.getBlue() / 255.0f,
                                 1.0f);
            GL11.glTranslated(x, y + 1.0, z);
            Sphere sphere = new Sphere();
            sphere.setDrawStyle(GLU.GLU_LINE);
            sphere.draw(6.0f, 20, 15);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}

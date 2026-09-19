package me.earth.earthhack.impl.modules.render.offscreen;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

final class ListenerRender2D
        extends ModuleListener<OffscreenESP, Render2DEvent>
{
    public ListenerRender2D(OffscreenESP module)
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

        ScaledResolution resolution = event.getResolution();
        int centerX = resolution.getScaledWidth() / 2;
        int centerY = resolution.getScaledHeight() / 2;
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || player.isInvisible() && !module.invisibles.getValue()
                || Managers.FRIENDS.contains(player)
                || mc.player.getDistance(player)
                    > module.range.getValue())
            {
                continue;
            }

            double dx = player.posX - mc.player.posX;
            double dz = player.posZ - mc.player.posZ;
            double yaw = Math.atan2(dz, dx) * 180.0 / Math.PI
                - 90.0
                - mc.player.rotationYaw;
            double rad = Math.toRadians(yaw);
            float x = (float) (centerX + Math.cos(rad) * module.radius.getValue());
            float y = (float) (centerY + Math.sin(rad) * module.radius.getValue());
            float size = module.size.getValue();

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
            GL11.glTranslatef(x, y, 0.0f);
            GL11.glRotatef((float) (yaw + 90.0), 0.0f, 0.0f, 1.0f);
            GL11.glTranslatef(-x, -y, 0.0f);
            GlStateManager.color(
                module.color.getValue().getRed() / 255.0f,
                module.color.getValue().getGreen() / 255.0f,
                module.color.getValue().getBlue() / 255.0f,
                1.0f);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(x, y - size);
            GL11.glVertex2f(x - size / 2.0f, y + size / 2.0f);
            GL11.glVertex2f(x + size / 2.0f, y + size / 2.0f);
            GL11.glEnd();
            GL11.glTranslatef(x, y, 0.0f);
            GL11.glRotatef((float) (-(yaw + 90.0)), 0.0f, 0.0f, 1.0f);
            GL11.glTranslatef(-x, -y, 0.0f);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}

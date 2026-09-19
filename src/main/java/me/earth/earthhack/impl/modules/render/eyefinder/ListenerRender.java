package me.earth.earthhack.impl.modules.render.eyefinder;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

final class ListenerRender extends ModuleListener<EyeFinder, Render3DEvent>
{
    public ListenerRender(EyeFinder module)
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

        for (Object object : mc.world.loadedEntityList)
        {
            if (!(object instanceof EntityLivingBase)
                || object == mc.player)
            {
                continue;
            }

            EntityLivingBase entity = (EntityLivingBase) object;
            if (entity.isDead
                || entity instanceof EntityPlayer
                    && !module.players.getValue()
                || !(entity instanceof EntityPlayer)
                    && (entity instanceof EntityAnimal
                        ? !module.animals.getValue()
                        : !module.mobs.getValue()))
            {
                continue;
            }

            drawLine(entity);
        }
    }

    private void drawLine(EntityLivingBase entity)
    {
        RayTraceResult result =
            entity.rayTrace(6.0, mc.getRenderPartialTicks());
        if (result == null)
        {
            return;
        }

        Vec3d eyes =
            entity.getPositionEyes(mc.getRenderPartialTicks());
        double x1 = eyes.x - mc.getRenderManager().renderPosX;
        double y1 = eyes.y - mc.getRenderManager().renderPosY;
        double z1 = eyes.z - mc.getRenderManager().renderPosZ;
        double x2 = result.hitVec.x - mc.getRenderManager().renderPosX;
        double y2 = result.hitVec.y - mc.getRenderManager().renderPosY;
        double z2 = result.hitVec.z - mc.getRenderManager().renderPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GL11.glColor4f(0.2f, 0.1f, 0.3f, 0.8f);
        GlStateManager.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

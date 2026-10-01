package me.friendly.exeter.beta.mixin;

import java.util.ArrayList;
import java.util.List;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.render.EntityEsp;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * World-space entity ESP. Runs at the end of the beta camera setup (class_555.method_1840,
 * after method_1851 orients the camera) so the world transform is active. Draws line boxes
 * at absolute coordinates under a camera-relative translation, with depth testing off.
 */
@Mixin(class_555.class)
public class MixinEntityEsp {

  @Inject(
      method = "method_1840(FI)V",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/class_555;method_1851(F)V",
              shift = At.Shift.AFTER))
  private void renderEsp(float tickDelta, int anaglyph, CallbackInfo info) {
    if (!EntityEsp.isActive()) {
      return;
    }
    EntityEsp esp = EntityEsp.get();
    if (esp == null) {
      return;
    }
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null || mc.world == null) {
      return;
    }
    LivingEntity view = mc.field_2807;
    if (view == null) {
      view = mc.player;
    }
    double camX = view.prevX + (view.x - view.prevX) * tickDelta;
    double camY = view.prevY + (view.y - view.prevY) * tickDelta;
    double camZ = view.prevZ + (view.z - view.prevZ) * tickDelta;

    List<Entity> entities = allEntities(mc.world);
    if (entities.isEmpty()) {
      return;
    }
    double rangeSq = esp.getRange() * esp.getRange();

    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
    GL11.glPushMatrix();
    GL11.glTranslated(-camX, -camY, -camZ);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_DEPTH_TEST);
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDisable(GL11.GL_FOG);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glLineWidth(esp.getLineWidth());
    try {
      for (Entity entity : entities) {
        if (entity == null || entity == mc.player || entity.dead) {
          continue;
        }
        double dx = entity.x - mc.player.x;
        double dy = entity.y - mc.player.y;
        double dz = entity.z - mc.player.z;
        if (dx * dx + dy * dy + dz * dz > rangeSq) {
          continue;
        }
        int color = colorFor(entity, esp);
        if (color == 0) {
          continue;
        }
        drawBox(entity.boundingBox, color);
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private List<Entity> allEntities(World world) {
    List<Entity> result = new ArrayList<Entity>();
    java.util.List[] lists =
        new java.util.List[] {world.field_198, world.field_199, world.field_200, world.field_201};
    for (int i = 0; i < lists.length; i++) {
      if (lists[i] == null) continue;
      Object[] copy = lists[i].toArray();
      for (int j = 0; j < copy.length; j++) {
        if (copy[j] instanceof Entity) {
          result.add((Entity) copy[j]);
        }
      }
    }
    return result;
  }

  private int colorFor(Entity entity, EntityEsp esp) {
    if (entity instanceof PlayerEntity) {
      if (!esp.showPlayers()) {
        return 0;
      }
      PlayerEntity player = (PlayerEntity) entity;
      if (Exeter.getInstance() != null
          && !Exeter.getInstance().getFriendManager().isTargetable(player.name)) {
        return 0xFF00FF00;
      }
      return 0xFF00FFFF;
    }
    if (!(entity instanceof LivingEntity)) {
      return 0;
    }
    if (isHostile(entity)) {
      return esp.showHostile() ? 0xFFFF5555 : 0;
    }
    if (isPassive(entity)) {
      return esp.showPassive() ? 0xFF55FF55 : 0;
    }
    return 0;
  }

  private boolean isHostile(Entity entity) {
    return entity instanceof net.minecraft.class_146
        || entity instanceof net.minecraft.class_451
        || entity instanceof net.minecraft.class_364;
  }

  private boolean isPassive(Entity entity) {
    return entity instanceof net.minecraft.class_258
        || entity instanceof net.minecraft.entity.mob.WaterCreatureEntity;
  }

  private void drawBox(Box box, int color) {
    if (box == null) {
      return;
    }
    float a = ((color >> 24) & 0xFF) / 255.0F;
    float r = ((color >> 16) & 0xFF) / 255.0F;
    float g = ((color >> 8) & 0xFF) / 255.0F;
    float b = (color & 0xFF) / 255.0F;
    double x0 = box.minX;
    double y0 = box.minY;
    double z0 = box.minZ;
    double x1 = box.maxX;
    double y1 = box.maxY;
    double z1 = box.maxZ;
    Tessellator tess = Tessellator.INSTANCE;
    tess.start(1);
    tess.color(r, g, b, a);
    // bottom face
    vertex(tess, x0, y0, z0);
    vertex(tess, x1, y0, z0);
    vertex(tess, x1, y0, z0);
    vertex(tess, x1, y0, z1);
    vertex(tess, x1, y0, z1);
    vertex(tess, x0, y0, z1);
    vertex(tess, x0, y0, z1);
    vertex(tess, x0, y0, z0);
    // top face
    vertex(tess, x0, y1, z0);
    vertex(tess, x1, y1, z0);
    vertex(tess, x1, y1, z0);
    vertex(tess, x1, y1, z1);
    vertex(tess, x1, y1, z1);
    vertex(tess, x0, y1, z1);
    vertex(tess, x0, y1, z1);
    vertex(tess, x0, y1, z0);
    // verticals
    vertex(tess, x0, y0, z0);
    vertex(tess, x0, y1, z0);
    vertex(tess, x1, y0, z0);
    vertex(tess, x1, y1, z0);
    vertex(tess, x1, y0, z1);
    vertex(tess, x1, y1, z1);
    vertex(tess, x0, y0, z1);
    vertex(tess, x0, y1, z1);
    tess.draw();
  }

  private void vertex(Tessellator tess, double x, double y, double z) {
    tess.vertex(x, y, z);
  }
}

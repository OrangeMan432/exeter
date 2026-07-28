package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Matrix3x2fStack;

public abstract class Panel
implements Labeled {
    private final String label;
    private int angle;
    private int x;
    private int y;
    private int x2;
    private int y2;
    private int width;
    private int height;
    private boolean open;
    public boolean drag;
    private final ArrayList<Item> items = new ArrayList();

    private static final Identifier ARROW_ID = Identifier.parse("minecraft:textures/exeter/arrow.png");
    public static final Identifier GEAR_ID = Identifier.parse("minecraft:textures/exeter/gear.png");
    private static boolean texturesRegistered = false;

    private static void registerTextures(Minecraft mc) {
        if (texturesRegistered) return;
        texturesRegistered = true;
        try {
            InputStream arrowStream = mc.getResourceManager().open(ARROW_ID);
            NativeImage arrowImg = NativeImage.read(arrowStream);
            DynamicTexture arrowTex = new DynamicTexture(() -> "exeter:arrow", arrowImg);
            mc.getTextureManager().register(ARROW_ID, arrowTex);

            InputStream gearStream = mc.getResourceManager().open(GEAR_ID);
            NativeImage gearImg = NativeImage.read(gearStream);
            DynamicTexture gearTex = new DynamicTexture(() -> "exeter:gear", gearImg);
            mc.getTextureManager().register(GEAR_ID, gearTex);


        } catch (Exception e) {
            System.out.println("Failed to load arrow/gear textures: " + e.getMessage());
        }
    }

    public Panel(String label, int x, int y, boolean open) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.angle = 180;
        this.width = 88;
        this.height = 18;
        this.open = open;
        this.setupItems();
    }

    public abstract void setupItems();

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drag(mouseX, mouseY);
        registerTextures(Minecraft.getInstance());
        float totalItemHeight = this.open ? this.getTotalItemHeight() - 2.0f : 0.0f;
        RenderMethods.drawGradientRect(this.x, (float)this.y - 1.5f, this.x + this.width, this.y + this.height - 6, Colors.getClientColorCustomAlpha(77), Colors.getClientColorCustomAlpha(77));
        RenderMethods.drawRect(this.x, (float)this.y + 12f, this.x + this.width, this.y + this.height + (this.open ? totalItemHeight : -1), 0x77000000);
        FontUtil.drawString(this.getLabel(), (float)this.x + 3.0f, (float)this.y + 1.5f, -1);

        if (!open) {
            if (this.angle > 0) {
                this.angle -= 3;
            }
        } else if (this.angle < 180) {
            this.angle += 3;
        }

        int arrowX = getX() + getWidth() - 14;
        int arrowY = getY();
        Matrix3x2fStack pose = RenderMethods.guiGraphics.pose();
        pose.pushMatrix();
        pose.rotateAbout(this.angle * (float)Math.PI / 180.0f, arrowX + 5, arrowY + 5);
        RenderMethods.guiGraphics.blit(ARROW_ID, arrowX, arrowY, arrowX + 10, arrowY + 10, 0.0f, 1.0f, 0.0f, 1.0f);
        pose.popMatrix();

        if (this.open) {
            int y = this.getY() + this.getHeight() - 3;
            for (Item item : getItems()) {
                item.setLocation((float)this.x + 2.0f, (float)y);
                item.setWidth(this.getWidth() - 4);
                item.drawScreen(mouseX, mouseY, partialTicks);
                y += item.getHeight() + 1;
            }
        }
    }

    private void drag(int mouseX, int mouseY) {
        if (!this.drag) {
            return;
        }
        this.x = this.x2 + mouseX;
        this.y = this.y2 + mouseY;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.x2 = this.x - mouseX;
            this.y2 = this.y - mouseY;
            ClickGui.getClickGui().getPanels().forEach(panel -> {
                if (panel.drag) {
                    panel.drag = false;
                }
            });
            this.drag = true;
            return;
        }
        if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
//            Minecraft.getInstance().getSoundHandler().playSound(PositionedSoundRecord.createPositionedSoundRecord(new ResourceLocation("random.click"), 1.0f));
            return;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
    }

    public void addButton(Button button) {
        this.items.add(button);
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            this.drag = false;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseReleased(mouseX, mouseY, releaseButton));
    }

    @Override
    public final String getLabel() {
        return this.label;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean getOpen() {
        return this.open;
    }

    public final ArrayList<Item> getItems() {
        return this.items;
    }

    private boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight() - (this.open ? 2 : 0);
    }

    //added this method in, just to fix shit. It is from uz1 class in future
    public static float calculateRotation(float var0) {
        if ((var0 %= 360.0F) >= 180.0F) {
            var0 -= 360.0F;
        }

        if (var0 < -180.0F) {
            var0 += 360.0F;
        }

        return var0;
    }

    private int getTotalItemHeight() {
        int height = 0;
        for (Item item : getItems()) {
            height += item.getHeight() + 1;
        }
        return height;
    }

    public void setX(int dragX) {
        this.x = dragX;
    }

    public void setY(int dragY) {
        this.y = dragY;
    }
}


package me.larp.client.events;

import com.mojang.blaze3d.platform.Window;
import me.larp.api.event.Event;
import net.minecraft.world.entity.item.ItemEntity;

public class RenderGameOverlayEvent extends Event {
  private Window window;
  private ItemEntity itemEntity;
  private final Type type;
  private boolean renderPumpkin = false;
  private boolean renderItems = false;
  private boolean renderHurtcam = false;
  private boolean renderFire = false;

  public RenderGameOverlayEvent(Type type) {
    this.type = type;
  }

  public RenderGameOverlayEvent(ItemEntity itemEntity) {
    this.type = Type.ITEM;
    this.itemEntity = itemEntity;
  }

  public RenderGameOverlayEvent(Window window) {
    this.type = Type.IN_GAME;
    this.window = window;
  }

  public Type getType() {
    return this.type;
  }

  public ItemEntity getEntityItem() {
    return this.itemEntity;
  }

  public Window getWindow() {
    return this.window;
  }

  public boolean isRenderFire() {
    return this.renderFire;
  }

  public void setRenderFire(boolean renderFire) {
    this.renderFire = renderFire;
  }

  public boolean isRenderPumpkin() {
    return this.renderPumpkin;
  }

  public void setRenderPumpkin(boolean renderPumpkin) {
    this.renderPumpkin = renderPumpkin;
  }

  public boolean isRenderItems() {
    return this.renderItems;
  }

  public void setRenderItems(boolean renderItems) {
    this.renderItems = renderItems;
  }

  public boolean isRenderHurtcam() {
    return this.renderHurtcam;
  }

  public void setRenderHurtcam(boolean renderHurtcam) {
    this.renderHurtcam = renderHurtcam;
  }

  public static enum Type {
    IN_GAME,
    PUMPKIN,
    ITEM,
    HURTCAM,
    FIRE;
  }

  public static enum ElementType {
    ALL,
    HELMET,
    PORTAL,
    CROSSHAIRS,
    BOSSHEALTH, // All boss bars
    BOSSINFO, // Individual boss bar
    ARMOR,
    HEALTH,
    FOOD,
    AIR,
    HOTBAR,
    EXPERIENCE,
    TEXT,
    HEALTHMOUNT,
    JUMPBAR,
    CHAT,
    PLAYER_LIST,
    DEBUG,
    POTION_ICONS,
    SUBTITLES,
    FPS_GRAPH,
    VIGNETTE
  }
}

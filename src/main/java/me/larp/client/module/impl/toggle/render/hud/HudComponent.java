package me.larp.client.module.impl.toggle.render.hud;

import me.larp.api.interfaces.Labeled;

public class HudComponent implements Labeled {
  private final String label;
  private int x;
  private int y;
  private int width;
  private int height;
  private boolean visible = true;

  public HudComponent(String label, int x, int y, int width, int height) {
    this.label = label;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public void render(int scaledWidth, int scaledHeight) {}

  public int getSnapX(int scaledWidth, int scaledHeight) {
    int snapMargin = 5;
    int bestX = this.x;
    int bestDist = Integer.MAX_VALUE;

    int[] candidates = {
      snapMargin, scaledWidth / 2 - this.width / 2, scaledWidth - this.width - snapMargin
    };

    for (int cx : candidates) {
      int dist = Math.abs(this.x - cx);
      if (dist < bestDist) {
        bestDist = dist;
        bestX = cx;
      }
    }

    return bestX;
  }

  public int getSnapY(int scaledWidth, int scaledHeight) {
    int snapMargin = 5;
    int bestY = this.y;
    int bestDist = Integer.MAX_VALUE;

    int[] candidates = {snapMargin, scaledHeight - this.height - snapMargin};

    for (int cy : candidates) {
      int dist = Math.abs(this.y - cy);
      if (dist < bestDist) {
        bestDist = dist;
        bestY = cy;
      }
    }

    return bestY;
  }

  @Override
  public final String getLabel() {
    return this.label;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setX(int x) {
    this.x = x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }
}

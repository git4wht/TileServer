package com.alicewolves.application.tileserver.store;


import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;

@Persistent(version = 8)
public class TileDbKey {

  @KeyField(1)
  public int zoom;

  @KeyField(2)
  public int x;

  @KeyField(3)
  public int y;

  protected TileDbKey() {
  }

  public TileDbKey(int x, int y, int zoom) {
    super();
    this.x = x;
    this.y = y;
    this.zoom = zoom;
  }

  @Override
  public String toString() {
    return "[x=" + x + ", y=" + y + ", zoom=" + zoom + "]";
  }

}

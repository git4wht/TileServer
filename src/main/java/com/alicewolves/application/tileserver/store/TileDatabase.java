package com.alicewolves.application.tileserver.store;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Mutations;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class TileDatabase {

  final String mapSourceName;
  final Environment env;
  final EntityStore store;
  final PrimaryIndex<TileDbKey, TileDbEntry> tileIndex;
  boolean dbClosed = false;

  long lastAccess;

  public TileDatabase(MapSource mapSource) throws IOException, DatabaseException {
    this(mapSource.getName(), mapSource.getStoreDir(), mapSource.getEnvConfig(), mapSource.getMutations());
  }

  public TileDatabase(String mapSourceName, File databaseDirectory, EnvironmentConfig envConfig, Mutations mutations) throws IOException, DatabaseException {
    log.debug("Opening tile store db: \"" + databaseDirectory + "\"");
    File storeDir = databaseDirectory;

    try {
      this.mapSourceName = mapSourceName;
      lastAccess = System.currentTimeMillis();

      if (!storeDir.exists()) {
        Files.createDirectories(storeDir.toPath());
      }

      env = new Environment(storeDir, envConfig);

      StoreConfig storeConfig = new StoreConfig();
      storeConfig.setAllowCreate(false);
      storeConfig.setReadOnly(true);
      storeConfig.setTransactional(false);
      storeConfig.setMutations(mutations);
      store = new EntityStore(env, "TilesEntityStore", storeConfig);

      tileIndex = store.getPrimaryIndex(TileDbKey.class, TileDbEntry.class);
    } finally {

    }
  }

  public boolean isClosed() {
    return dbClosed;
  }

  public long entryCount() throws DatabaseException {
    return tileIndex.count();
  }

  public void put(TileDbEntry tile) throws DatabaseException {
    try {
      tileIndex.put(tile);
    } finally {
    }
  }

  public boolean contains(TileDbKey key) throws DatabaseException {
    return tileIndex.contains(key);
  }

  public TileDbEntry get(TileDbKey key) throws DatabaseException {
    return tileIndex.get(key);
  }

  public PrimaryIndex<TileDbKey, TileDbEntry> getTileIndex() {
    return tileIndex;
  }

  public BufferedImage getCacheCoverage(int zoom, Point tileNumMin, Point tileNumMax) throws DatabaseException,
          InterruptedException {
    log.debug("Loading cache coverage for region " + tileNumMin + " " + tileNumMax + " of zoom level " + zoom);
    int width = tileNumMax.x - tileNumMin.x + 1;
    int height = tileNumMax.y - tileNumMin.y + 1;
    byte ff = (byte) 0xFF;
    byte[] colors = new byte[]{120, 120, 120, 120, // alpha-gray
            10, ff, 0, 120 // alpha-green
    };
    IndexColorModel colorModel = new IndexColorModel(2, 2, colors, 0, true);
    BufferedImage image = null;
    try {
      image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    } catch (Throwable e) {
      log.error("Failed to create coverage image: " + e.toString());
      image = null;
      System.gc();
      return null;
    }
    WritableRaster raster = image.getRaster();

    // We are loading the coverage of the selected area column by column which is much faster than loading the
    // whole region at once
    for (int x = tileNumMin.x; x <= tileNumMax.x; x++) {
      TileDbKey fromKey = new TileDbKey(x, tileNumMin.y, zoom);
      TileDbKey toKey = new TileDbKey(x, tileNumMax.y, zoom);
      EntityCursor<TileDbKey> cursor = tileIndex.keys(fromKey, true, toKey, true);
      try {
        TileDbKey key = cursor.next();
        while (key != null) {
          int pixelx = key.x - tileNumMin.x;
          int pixely = key.y - tileNumMin.y;
          raster.setSample(pixelx, pixely, 0, 1);
          key = cursor.next();

        }
      } finally {
        cursor.close();
      }
    }
    return image;
  }

  public void purge() {
    try {
      store.sync();
      env.cleanLog();
    } catch (DatabaseException e) {
      log.error("database compression failed: ", e);
    }
  }

  public void close() {
    close(true);
  }

  public void close(boolean removeFromMap) {
    if (dbClosed) {
      return;
    }

    try {
      try {
        log.debug("Closing tile store db \"" + mapSourceName + "\"");
        if (store != null) {
          store.close();
        }
      } catch (Exception e) {
        log.error("", e);
      }
      try {
        env.close();
      } catch (Exception e) {
        log.error("", e);
      } finally {
        dbClosed = true;
      }
    } finally {

    }
  }

  @Override
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

}


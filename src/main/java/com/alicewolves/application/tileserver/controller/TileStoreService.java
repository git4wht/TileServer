package com.alicewolves.application.tileserver.controller;

import com.alicewolves.application.tileserver.store.MapSource;
import com.alicewolves.application.tileserver.store.TileDatabase;
import com.alicewolves.application.tileserver.store.TileDbEntry;
import com.alicewolves.application.tileserver.store.TileDbKey;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.evolve.Renamer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TileStoreService {
  private static final String LOG_FILE_MAX = String.valueOf(1L << 30); // = 2^30 (1G)
  private final Map<String, TileDatabase> tileStoreMap = new HashMap<>();
  private EnvironmentConfig envConfig;
  private Mutations mutations;

  @Value("${tiles.store:./db}")
  private String tileStoreDir;

  @Value("${tiles.prefix:db-}")
  private String prefix;

  private File tileStoreFile;


  public TileStoreService() throws IOException {
    envConfig = new EnvironmentConfig();
    envConfig.setTransactional(false);
    envConfig.setLocking(true);
    envConfig.setAllowCreate(false);
    envConfig.setReadOnly(true);
    envConfig.setSharedCache(true);
    envConfig.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, LOG_FILE_MAX);

    mutations = new Mutations();

    String oldPackage1 = "tac.tilestore.berkeleydb";
    String oldPackage2 = "tac.program.tilestore.berkeleydb";
    String oldPackage3 = "mobac.program.tilestore.berkeleydb";
    String entry = ".TileDbEntry";
    String key = ".TileDbEntry$TileDbKey";
    mutations.addRenamer(new Renamer(oldPackage1 + entry, 0, TileDbEntry.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage1 + key, 0, TileDbKey.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage1 + entry, 1, TileDbEntry.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage1 + key, 1, TileDbKey.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage2 + entry, 2, TileDbEntry.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage2 + key, 2, TileDbKey.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage3 + entry, 3, TileDbEntry.class.getName()));
    mutations.addRenamer(new Renamer(oldPackage3 + key, 3, TileDbKey.class.getName()));

  }

  private synchronized File getTileStoreDir() throws IOException, DatabaseException {
    if (tileStoreFile != null) {
      return tileStoreFile;
    }
    //文件夹
    tileStoreFile = new File(tileStoreDir);
    if (!tileStoreFile.exists()) {
      Files.createDirectories(tileStoreFile.toPath());
    }
    prefix = StringUtils.isEmpty(prefix) ? "" : prefix;
    //遍历文件夹，加入已有的存储
    for (File file : tileStoreFile.listFiles()) {
      if (file.isDirectory() && file.getName().startsWith(prefix)) {
        //加入存储
        String map = file.getName().substring(prefix.length());
        MapSource mapSource = new MapSource(map, file, envConfig, mutations);
        TileDatabase tileDatabase = new TileDatabase(mapSource);
        tileStoreMap.put(mapSource.getName(), tileDatabase);
        log.info("found map :" + map);
      }
    }
    return this.tileStoreFile;
  }

  private byte[] getTileData(String map, int z, int x, int y) throws IOException, DatabaseException {
    getTileStoreDir();
    TileDatabase db = tileStoreMap.get(map);
    if (db == null) {
      return null;
    }
    TileDbKey key = new TileDbKey(x, y, z);
    TileDbEntry entry = db.get(key);
    if (entry != null) {
      return entry.getData();
    }
    return null;
  }

  @Cacheable(value = "tiles", key = "#map+'_'+#z+'_'+#x+'_'+#y")
  public BufferedImage getTile(String map, int z, int x, int y) throws IOException, DatabaseException {
    y = ((1 << z) - y - 1);
    byte[] data = getTileData(map, z, x, y);
    if (data != null) {
      return ImageIO.read(new ByteArrayInputStream(data));
    }
    return null;
  }

  @CacheEvict(value = "tiles", key = "#map+'_'+#z+'_'+#x+'_'+#y")
  public void clearCache(String map, int z, int x, int y) {
    y = ((1 << z) - y - 1);
    log.info(String.format("clear cache on '%s' by key:%d-%d-%d", map, z, x, y));
  }

  @CacheEvict(value = "tiles", allEntries = true)
  public void clearAllCache(String map) {
    log.info(String.format("clear all cache on '%s'", map));
  }
}

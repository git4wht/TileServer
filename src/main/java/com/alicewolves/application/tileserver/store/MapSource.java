package com.alicewolves.application.tileserver.store;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.evolve.Mutations;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapSource {
  private String name;
  private File storeDir;
  private EnvironmentConfig envConfig;
  private Mutations mutations;
}

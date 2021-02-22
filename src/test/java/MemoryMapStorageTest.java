/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MemoryMapStorageTest {

  /**
   * Test the creation of X memory mapped files to test the boundaries mannually
   *
   * @throws IOException
   */
  @Disabled("Disabled. Should only be run manually. Define temp folder and make sure to delete the files afterwards")
  @Test
  public void testMemoryMapStorage() throws IOException {
    List<MemoryMapStorage> storages = new ArrayList<>();
    File f = new File("D:/tmpmzmine/");
    try {
      if (!f.exists()) {
        f.mkdirs();
      }
      List<DoubleBuffer> list = new ArrayList<>();
      double[] data = new double[]{1d, 2d, 3d, 4d};
      for (int i = 0; i < 2000; i++) {
        MemoryMapStorage s = new MemoryMapStorage(f);
        storages.add(s);
        list.add(s.storeData(data));
      }

      deleteFolder(f);
    } catch (IOException ex) {
      // try to delete files
      storages.forEach(s -> {
        try {
          s.discard();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      deleteFolder(f);
      throw ex;
    }
  }

  /**
   * Test the discard method of memory map storage. Expected to fail on Windows due to an issue in
   * the JDK/WINDOWS
   *
   * @throws IOException
   */
  @Disabled("Disabled. Should only be run manually. Define temp folder and make sure to delete the files afterwards")
  @Test
  public void testDiscardMemoryMapStorage() throws IOException {
    MemoryMapStorage storage = null;
    File f = new File("D:/tmpmzmine/");
    try {
      if (!f.exists()) {
        f.mkdirs();
      }
      double[] data = new double[]{1d, 2d, 3d, 4d};
      storage = new MemoryMapStorage(f);
      storage.storeData(data);

      // test discard
      assertTrue(storage.discard(),
          "File was not deleted (expected on Windows due to an issue)");
    } catch (IOException ex) {
      // try to delete files
      deleteFolder(f);
      throw ex;
    }
  }

  private void deleteFolder(File f) {
    try {
      f.delete();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}

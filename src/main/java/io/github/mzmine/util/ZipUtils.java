/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP related utilities
 */
public class ZipUtils {

  public static void unzipStream(ZipInputStream zipStream, File destinationFolder)
      throws IOException {

    if (!destinationFolder.exists()) {
      destinationFolder.mkdirs();
    }

    ZipEntry entry;
    int readLen;
    byte readBuffer[] = new byte[10000000];
    while ((entry = zipStream.getNextEntry()) != null) {
      File extractedFile = new File(destinationFolder, entry.getName());
      if (entry.isDirectory()) {
        extractedFile.mkdirs();
        continue;
      }
      try (FileOutputStream outputStream = new FileOutputStream(extractedFile)) {
        while ((readLen = zipStream.read(readBuffer)) != -1) {
          outputStream.write(readBuffer, 0, readLen);
        }
        outputStream.close();
        extractedFile.setExecutable(true);
      }
    }

  }

}

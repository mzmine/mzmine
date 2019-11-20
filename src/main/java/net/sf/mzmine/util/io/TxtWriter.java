/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.io;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import net.sf.mzmine.util.files.FileAndPathUtil;

public class TxtWriter {
  private static final Logger log = Logger.getLogger(TxtWriter.class.getName());


  public static boolean write(String s, File file, boolean append) {
    try {
      if (!file.getParentFile().exists())
        file.getParentFile().mkdirs();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot create folder " + file.getParent() + " ", e);
    }

    try {
      CharSink chs = append ? Files.asCharSink(file, StandardCharsets.UTF_8, FileWriteMode.APPEND)
          : Files.asCharSink(file, StandardCharsets.UTF_8);
      chs.write(s);
      return true;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Write file " + file.getAbsolutePath(), e);
      return false;
    }
  }

  public static boolean write(String s, File file) {
    return write(s, file, false);
  }

  public static boolean write(String s, File file, String format) {
    return write(s, FileAndPathUtil.getRealFilePath(file, format));
  }

  public static boolean write(String s, File file, String format, boolean append) {
    return write(s, FileAndPathUtil.getRealFilePath(file, format), append);
  }
}

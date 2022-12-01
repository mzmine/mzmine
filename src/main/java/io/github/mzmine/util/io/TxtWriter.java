/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.io;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import io.github.mzmine.util.files.FileAndPathUtil;

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

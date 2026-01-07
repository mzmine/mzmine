/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.logging;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class LoggerUtils {

  private static final Logger logger = Logger.getLogger(LoggerUtils.class.getName());

  public static @Nullable File getLogFile() {
    try {
      // get root logger
      return Stream.of(logger).<Handler>mapMulti((logger, downstream) -> {
            Logger parent = logger;
            while (((parent = parent.getParent()) != null)) {
              for (final Handler handler : parent.getHandlers()) {
                downstream.accept(handler);
              }
            }
          }).filter(FileHandler.class::isInstance).map(FileHandler.class::cast) // find file handlers
          .map(LoggerUtils::reflectiveAccessToLogFile) // find first file
          .filter(Objects::nonNull).findFirst().orElse(null);
    } catch (Exception ex) {
      // silent for now
    }
    return null;
  }

  private static @Nullable File reflectiveAccessToLogFile(final FileHandler fh) {
    try {
      Field filesField = fh.getClass().getDeclaredField("files");
      filesField.setAccessible(true);
      File[] logFiles = (File[]) filesField.get(fh);
      if (logFiles.length > 0 && logFiles[0] != null) {
        return logFiles[0];
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // silent for now
      logger.log(Level.WARNING, "Cannot get logger via reflection: " + e.getMessage(), e);
    }
    return null;
  }
}

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

package io.github.mzmine.main;

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * MZmine logging support
 */
public final class MZmineLoggingConfiguration {

  private static final Logger logger = Logger.getLogger(MZmineLoggingConfiguration.class.getName());

  /**
   * Configures the logging properties according to the logging.properties file found in the jar
   * resources
   */
  public MZmineLoggingConfiguration() {
    ClassLoader cl = MZmineLoggingConfiguration.class.getClassLoader();
    final String fileName = "logging.properties";
    final File externalConfig = FileAndPathUtil.resolveInMzmineDir(fileName);
    if (!externalConfig.exists()) {
      // copy file to the user/.mzmine directory to allow edits of log level by user 
      try (InputStream is = cl.getResourceAsStream(fileName)) {
        FileAndPathUtil.createDirectory(externalConfig.getParentFile());
        if (is != null) {
          Files.copy(is, externalConfig.toPath());
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, "Could not copy logging configuration file: " + e.getMessage(),
            e);
      }
    }

    Properties merged = new Properties();
    // user facing config
    if (externalConfig.exists()) {
      try (InputStream base = Files.newInputStream(externalConfig.toPath())) {
        merged.load(base);
      } catch (Exception e) {
      }
    }
    if (merged.isEmpty()) {
      // loading failed or file was not there
      try (InputStream base = cl.getResourceAsStream(fileName)) {
        merged.load(base);
      } catch (Exception e) {
      }
    }
    // overlay internal log config
    try (InputStream env = cl.getResourceAsStream("internal_logging.properties")) {
      Properties overlay = new Properties();
      overlay.load(env);
      merged.putAll(overlay); // overwrite user properties
    } catch (Exception e) {
    }

    // apply merged config by turning properties into InputStream
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      merged.store(out, null);
      ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
      LogManager.getLogManager().readConfiguration(in);
      return;
    } catch (Exception e) {
      logger.log(Level.WARNING, "error during logger setup: " + e.getMessage(), e);
      e.printStackTrace();
    }
  }

}

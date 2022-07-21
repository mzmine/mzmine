/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import java.io.File;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * List of external tools used by MZmine
 */
public enum ExternalTool {
  /**
   * Thermo raw parser is used to import raw files
   */
  THERMO_RAW_PARSER;

  private static final Logger logger = Logger.getLogger(ExternalTool.class.getName());

  private String getFolderName() {
    return switch (this) {
      case THERMO_RAW_PARSER -> "thermo_raw_file_parser";
    };
  }

  /**
   * External tools are located in the repository as external_tools
   *
   * @return the file path to the external tool - usually the directory and not the tool itself as
   * this might be OS dependent. null if the tool does not exist
   */
  @Nullable
  public File getExternalToolPath() {
    File directory = FileAndPathUtil.getWorkingDirectory().toFile();
    logger.finest(() -> "Searching for external tool folder at runtime dir %s".formatted(
        directory.getAbsoluteFile().toString()));
    // this is the MZmine directory
    // during development the files are in the parent dir
    String toolPath = this.getFolderName();

    // try if it exists otherwise jump into parent dir
    File combined = new File(directory, "external_tools/" + toolPath);
    if (combined.exists()) {
      return combined;
    } else {
      combined = new File(directory.getParentFile(), "external_tools/" + toolPath);
      return combined.exists() ? combined : null;
    }
  }
}

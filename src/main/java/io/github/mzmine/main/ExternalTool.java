/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.main;

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import org.jetbrains.annotations.Nullable;

/**
 * List of external tools used by MZmine
 */
public enum ExternalTool {
  /**
   * Thermo raw parser is used to import raw files
   */
  THERMO_RAW_PARSER;

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
    File directory = FileAndPathUtil.getPathOfJar().getParentFile();
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

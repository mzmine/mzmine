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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.standardslist;

import com.google.common.io.Files;

import java.io.IOException;

/**
 * Interface for extracting a list of standard molecules
 * given a file with specific format,
 * extract data on molecules and return StandardsList object
 */
public interface StandardsListExtractor {

  /**
   * Static factory method for returning appropriate implementation
   * based on filename extension
   *
   * @param filename spreadsheet filename
   * @return instantiated extractor object
   * @throws IOException thrown by concrete extractor while opening the file
   */
  static StandardsListExtractor createFromFilename(String filename) throws IOException {
    String extension = Files.getFileExtension(filename);

    if (extension.equals("xls") || extension.equals("xlsx")) {
      return new StandardsListSpreadsheetExtractor(filename);
    }

    throw new IllegalArgumentException("Unsupported extension " + extension +
            " in spreadsheet file " + filename);
  }

  StandardsList extractStandardsList();
}

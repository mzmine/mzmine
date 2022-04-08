/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.util;

import io.github.mzmine.parameters.parametertypes.ImportType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class CSVParsingUtils {

  private static final Logger logger = Logger.getLogger(CSVParsingUtils.class.getName());

  /**
   * Searches an array of strings for a specified list of import types. Returns all selected import
   * types or null if they were found.
   *
   * @param importTypes A list of {@link ImportType}s. Only if a type {@link
   *                    ImportType#isSelected()}, it will be included in the output list.
   * @param firstLine   The column headers
   * @return A new list of the selected import types with their line indices set. The returned list
   * might not have the same size as the input list, if not all lines were found.
   */
  @NotNull
  public static List<ImportType> findLineIds(List<ImportType> importTypes, String[] firstLine) {
    List<ImportType> lines = new ArrayList<>();
    for (ImportType importType : importTypes) {
      if (importType.isSelected()) {
        ImportType type = new ImportType(importType.isSelected(), importType.getCsvColumnName(),
            importType.getDataType());
        lines.add(type);
      }
    }

    for (ImportType importType : lines) {
      for (int i = 0; i < firstLine.length; i++) {
        String columnName = firstLine[i];
        if (columnName.trim().equalsIgnoreCase(importType.getCsvColumnName().trim())) {
          if (importType.getColumnIndex() != -1) {
            logger.warning(
                () -> "Library file contains two columns called \"" + columnName + "\".");
          }
          importType.setColumnIndex(i);
        }
      }
    }
    final List<ImportType> nullMappings = lines.stream().filter(val -> val.getColumnIndex() == -1)
        .toList();
    if (!nullMappings.isEmpty()) {
      logger.warning(() -> "Did not find specified column " + Arrays.toString(
          nullMappings.stream().map(ImportType::getCsvColumnName).toArray()) + " in file.");
//      return null;
    }

    return nullMappings.isEmpty() ? lines
        : lines.stream().filter(val -> val.getColumnIndex() != -1).toList();
  }
}

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

package io.github.mzmine.modules.visualization.projectmetadata.table;

import java.io.File;

public interface TableExportUtility {

  /**
   * Export the metadata table using .tsv format.
   * todo: add extra argument defining the format of the exported data (e.g. GNPS or .tsv)
   * File format would be:
   * ====================================================================
   * NAME TYPE DESC FILE VALUE
   * ====================================================================
   * NAME  - parameter name
   * TYPE  - type of the parameter
   * DESC  - description of the parameter
   * FILE  - name of the file to which the parameter belong to
   * VALUE - value of the parameter
   *
   * @param file the file in which exported metadata will be stored
   * @return true if the export was successful, false otherwise
   */
  boolean exportTo(File file);

  /**
   * Import the metadata to the metadata table.
   * todo: add extra argument defining the format of the imported data (e.g. GNPS or .tsv)
   *
   * @param file       source of the metadata
   * @param appendMode whether the new metadata should be appended or they should replace the old
   *                   metadata
   * @return true if the metadata were successfully imported, false otherwise
   */
  boolean importFrom(File file, boolean appendMode);
}

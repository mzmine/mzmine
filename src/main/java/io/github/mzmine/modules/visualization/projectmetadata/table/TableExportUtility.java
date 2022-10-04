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

/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import org.jetbrains.annotations.NotNull;

public class MetadataUtils {

  public static MetadataColumn<?> getColumnByName(String header) {
    return getMetadata().getColumnByName(header);
  }

  /**
   * Maybe push into MetadataTable?
   *
   * @return
   */
  public static DateMetadataColumn getRunDateColumn() {
    return (DateMetadataColumn) getMetadata().getColumnByName(MetadataColumn.DATE_HEADER);
  }

  public static @NotNull MetadataTable getMetadata() {
    return ProjectService.getProject().getProjectMetadata();
  }

  /**
   * Return parameter value of the corresponding RawData file.
   *
   * @param column      project parameter column
   * @param rawDataFile RawData file
   * @param <T>         type of the project parameter
   * @return parameter value
   */
  public static <T> T getValue(MetadataColumn<T> column, RawDataFile rawDataFile) {
    return getMetadata().getValue(column, rawDataFile);
  }
}

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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Comparator;

public class RawDataByMetadataSorter {

  public static Comparator<RawDataFile> byColumn(String column) {
    MetadataColumn col = ProjectService.getMetadata().getColumnByName(column);
    return byColumn(col);
  }

  public static <T extends Comparable<T>> Comparator<RawDataFile> byColumn(
      MetadataColumn<T> column) {
    if (column == null) {
      return null;
    }
    return Comparator.comparing(
        raw -> ProjectService.getProject().getProjectMetadata().getValue(column, raw),
        Comparator.nullsLast(Comparator.naturalOrder()));
  }


  /**
   * Sort by run date and then by name - or just by name if run date is empty
   */
  public static Comparator<RawDataFile> byDateAndName() {
    DateMetadataColumn dateCol = ProjectService.getMetadata().getRunDateColumn();
    if (dateCol == null) {
      return Comparator.comparing(RawDataFile::getName);
    }

    return Comparator.comparing(
        (RawDataFile raw) -> ProjectService.getMetadata().getValue(dateCol, raw),
        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(RawDataFile::getName);
  }


}

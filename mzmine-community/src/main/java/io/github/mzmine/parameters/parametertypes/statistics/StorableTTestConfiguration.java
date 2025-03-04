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

package io.github.mzmine.parameters.parametertypes.statistics;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.ttest.StudentTTest;
import io.github.mzmine.modules.dataanalysis.significance.ttest.TTestSamplingConfig;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public record StorableTTestConfiguration(@NotNull TTestSamplingConfig samplingConfig, String column,
                                         String groupA, String groupB) {

  private static final Logger logger = Logger.getLogger(StorableTTestConfiguration.class.getName());

  /**
   * @return A {@link StudentTTest} or null. The configuration is only returned if the column exists
   * and the respective values exist in that column.
   */
  public <T> StudentTTest<T> toValidConfig() {
    final MetadataTable metadata = MZmineCore.getProjectMetadata();

    final MetadataColumn<T> col = (MetadataColumn<T>) metadata.getColumnByName(column);
    if (col == null) {
      return null;
    }
    final List<T> distinctColumnValues = metadata.getDistinctColumnValues(col);

    final T a = col.convertOrElse(groupA, null);
    final T b = col.convertOrElse(groupB, null);

    if (a == null || b == null) {
      logger.warning(() -> "Could not convert metadata value " + a + " or " + b
          + " to values of required type " + col.getType().name());
      return null;
    }

    if (!distinctColumnValues.contains(a)) {
      logger.warning(() -> "Metadata column " + col.getTitle() + " does not contain value " + a
          + ". (available: " + distinctColumnValues.stream().map(Object::toString)
          .collect(Collectors.joining(",")));
      return null;
    }

    if (!distinctColumnValues.contains(b)) {
      logger.warning(() -> "Metadata column " + col.getTitle() + " does not contain value " + b
          + ". (available: " + distinctColumnValues.stream().map(Object::toString)
          .collect(Collectors.joining(",")));
      return null;
    }

    if (a.equals(b)) {
      logger.warning(
          () -> "Same grouping parameter selected for both groups of the t-Test. (" + a + ")");
    }

    return new StudentTTest<>(samplingConfig, col, a, b);
  }
}

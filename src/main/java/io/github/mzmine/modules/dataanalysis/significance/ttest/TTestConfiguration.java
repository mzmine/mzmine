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

package io.github.mzmine.modules.dataanalysis.significance.ttest;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;

public record TTestConfiguration<T>(TTestSamplingConfig samplingConfig, MetadataColumn<T> column,
                                    T groupA, T groupB) {

  // enable when statements before super is enabled.
  /*public TTestConfiguration(TTestSamplingConfig samplingConfig, MetadataColumn<T> column) {
    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    final List<T> distinctColumnValues = metadata.getDistinctColumnValues(column);
    if (distinctColumnValues.size() != 2) {
      throw new InvalidTestModelException(
          STR."t-Test only applies to two groups, \{column.getTitle()} contains \{distinctColumnValues.size()} (\{distinctColumnValues.stream()
              .map(Objects::toString).collect(Collectors.joining(","))}");
    }
    this(samplingConfig, column, distinctColumnValues.get(0), distinctColumnValues.get(1));
  }*/
}

/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.significance.anova;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.statistics.DataTableUtils;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.inference.TestUtils;

public class AnovaTest implements RowSignificanceTest {

  private final List<List<RawDataFile>> groupedFiles;
  private final MetadataColumn<?> groupingColumn;
  // the data table
  private final FeaturesDataTable dataTable;

  public AnovaTest(FeaturesDataTable dataTable, MetadataColumn<?> groupingColumn)
      throws MetadataColumnDoesNotExistException {
    this.dataTable = dataTable;
    this.groupingColumn = groupingColumn;

    final MetadataTable metadata = ProjectService.getMetadata();
    final Map<?, List<RawDataFile>> fileGrouping = metadata.groupFilesByColumn(groupingColumn);
    // can check conditions here that all groups have at least two values because we impute missing values
    for (var group : fileGrouping.entrySet()) {
      if (group.getValue().size() < 2) {
        throw new IllegalArgumentException(
            "Group %s has less than two samples which is a requirement for ANOVA.".formatted(
                group.getKey()));
      }
    }
    groupedFiles = fileGrouping.values().stream().toList();
  }

  @Override
  public AnovaResult test(FeatureListRow row) {
    // conditions are already checked in the constructor
    final List<double[]> intensityGroups = DataTableUtils.extractGroupsRowData(dataTable, row,
        groupedFiles);

    final double pValue = TestUtils.oneWayAnovaPValue(intensityGroups);
    final double fValue = TestUtils.oneWayAnovaFValue(intensityGroups);
    return new AnovaResult(row, groupingColumn.getTitle(), pValue, fValue);
  }
}

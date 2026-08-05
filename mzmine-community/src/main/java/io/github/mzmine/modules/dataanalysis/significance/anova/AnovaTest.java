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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // the metadata groups all files of the project, but the data table only contains the samples of
    // the selected feature list. only keep the samples that are actually in the data table. use the
    // table order because the metadata grouping has no stable order.
    final List<RawDataFile> tableSamples = dataTable.getRawDataFiles();
    final List<List<RawDataFile>> groups = new ArrayList<>();

    // can check conditions here that all groups have at least two values because we impute missing values
    for (var group : fileGrouping.entrySet()) {
      final Set<RawDataFile> groupSamples = Set.copyOf(group.getValue());
      final List<RawDataFile> inTable = tableSamples.stream().filter(groupSamples::contains)
          .toList();
      if (inTable.isEmpty()) {
        // group is not represented in this feature list at all
        continue;
      }
      if (inTable.size() < 2) {
        throw new IllegalArgumentException(
            "Group %s has less than two samples in the selected feature list (n=%d of %d samples in the metadata) which is a requirement for ANOVA.".formatted(
                group.getKey(), inTable.size(), group.getValue().size()));
      }
      groups.add(inTable);
    }

    if (groups.size() < 2) {
      throw new IllegalArgumentException(
          "Column %s defines only %d group(s) with at least two samples in the selected feature list. ANOVA requires at least two groups.".formatted(
              groupingColumn.getTitle(), groups.size()));
    }
    groupedFiles = List.copyOf(groups);
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

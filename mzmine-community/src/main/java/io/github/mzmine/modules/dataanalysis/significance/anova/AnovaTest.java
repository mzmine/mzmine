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

package io.github.mzmine.modules.dataanalysis.significance.anova;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.inference.TestUtils;

public class AnovaTest implements RowSignificanceTest {

  private static final Logger logger = Logger.getLogger(AnovaTest.class.getName());
  private final AtomicDouble finishedPercentage = new AtomicDouble(0d);
  private final List<List<RawDataFile>> groupedFiles;
  private final MetadataColumn<?> groupingColumn;
  private List<AnovaResult> result = List.of();

  public AnovaTest(MetadataColumn<?> groupingColumn) throws MetadataColumnDoesNotExistException {
    this.groupingColumn = groupingColumn;

    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    final Map<?, List<RawDataFile>> fileGrouping = metadata.groupFilesByColumn(groupingColumn);
    groupedFiles = fileGrouping.values().stream().toList();
  }

  /**
   * @param groupAbundances one array of abundances for each group or an 2D
   *                        array[group][abundances]
   */
  private boolean checkConditions(List<double[]> groupAbundances) {
    // anova usually used for more than two groups. each group must have at least 2 values
    return groupAbundances.size() > 2 && groupAbundances.stream()
        .allMatch(array -> array.length >= 2);
  }

  @Override
  public AnovaResult test(FeatureListRow row, AbundanceMeasure abundanceMeasure) {
    final List<double[]> intensityGroups = groupedFiles.stream()
        .map(group -> StatisticUtils.extractAbundance(row, group, abundanceMeasure)).toList();

    if (checkConditions(intensityGroups)) {
      final double pValue = TestUtils.oneWayAnovaPValue(intensityGroups);
      final double fValue = TestUtils.oneWayAnovaFValue(intensityGroups);
      return new AnovaResult(row, groupingColumn.getTitle(), pValue, fValue);
    }

    return null;
  }
}

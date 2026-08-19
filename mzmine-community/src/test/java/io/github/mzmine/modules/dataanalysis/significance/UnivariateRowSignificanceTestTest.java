/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.significance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureListTestUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The metadata groups all files of the project, but a data table only holds the samples of the
 * selected feature list. These tests cover the mismatch, which used to surface as a
 * {@link NullPointerException} instead of a usable plot.
 */
class UnivariateRowSignificanceTestTest {

  private static final String COLUMN = "group";
  private static final int NUM_ROWS = 5;

  private StringMetadataColumn groupColumn;
  private List<RawDataFileImpl> allFiles;

  @BeforeEach
  void setUp() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());

    // 6 files in the project metadata: 3 in group A, 3 in group B
    allFiles = FeatureListTestUtils.createRawFiles(6, "sample", LocalDateTime.of(2026, 1, 1, 0, 0),
        Duration.ofMinutes(1));

    final MetadataTable metadata = ProjectService.getMetadata();
    groupColumn = new StringMetadataColumn(COLUMN);
    metadata.addColumn(groupColumn);
    for (int i = 0; i < allFiles.size(); i++) {
      metadata.setValue(groupColumn, allFiles.get(i), i < 3 ? "A" : "B");
    }
  }

  /**
   * @param fileIndices the project files that the feature list contains
   */
  private FeaturesDataTable createDataTable(List<Integer> fileIndices) {
    final List<RawDataFile> files = fileIndices.stream().map(i -> (RawDataFile) allFiles.get(i))
        .toList();
    final ModularFeatureList flist = FeatureListTestUtils.createFeatureList("flist", files,
        NUM_ROWS, 100f);
    return StatisticUtils.extractAbundancesPrepareData(flist,
        new AbundanceDataTablePreparationConfig(AbundanceMeasure.Height, ImputationFunctions.None));
  }

  @Test
  void testGroupsAreLimitedToTheSamplesOfTheDataTable() {
    // feature list only contains 2 of 3 samples of each group
    final FeaturesDataTable table = createDataTable(List.of(0, 1, 3, 4));

    final UnivariateRowSignificanceTest<String> test = new UnivariateRowSignificanceTest<>(table,
        SignificanceTests.WELCHS_T_TEST, groupColumn, "A", "B");

    // in data table order, the metadata grouping itself has no stable order
    assertEquals(List.of(allFiles.get(0), allFiles.get(1)),
        test.getGroupAData().getRawDataFiles());
    assertEquals(List.of(allFiles.get(3), allFiles.get(4)),
        test.getGroupBData().getRawDataFiles());
    assertEquals(2, test.getGroupAData().getNumberOfSamples());
    assertEquals(2, test.getGroupBData().getNumberOfSamples());
    assertEquals(NUM_ROWS, test.getGroupAData().getNumberOfFeatures());

    // and a test result is produced for each row of the table
    for (final var row : table.getFeatureListRows()) {
      assertNotNull(test.test(row));
    }
  }

  @Test
  void testGetMatchingFilesIsLimitedToTheGivenFiles() {
    final List<RawDataFile> subset = List.of(allFiles.get(0), allFiles.get(1), allFiles.get(3));

    assertEquals(List.of(allFiles.get(0), allFiles.get(1)),
        ProjectService.getMetadata().getMatchingFiles(subset, groupColumn, "A"));
    assertEquals(List.of(allFiles.get(3)),
        ProjectService.getMetadata().getMatchingFiles(subset, groupColumn, "B"));
    // and all known files are still matched when they are all passed in
    assertEquals(3, ProjectService.getMetadata()
        .getMatchingFiles(List.<RawDataFile>copyOf(allFiles), groupColumn, "A").size());
  }

  @Test
  void testTooFewSamplesOfAGroupInTheFeatureListReportsBothCounts() {
    // group B only has a single sample in the feature list
    final FeaturesDataTable table = createDataTable(List.of(0, 1, 2, 3));

    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> new UnivariateRowSignificanceTest<>(table, SignificanceTests.WELCHS_T_TEST,
            groupColumn, "A", "B"));
    // message needs to name the feature list as the reason, not just the metadata
    assertTrue(ex.getMessage().contains("feature list"), ex.getMessage());
  }

  @Test
  void testSubsetBySamplesReportsTheMissingFileInsteadOfNpe() {
    final FeaturesDataTable table = createDataTable(List.of(0, 1, 3, 4));
    final RawDataFile notInTable = allFiles.get(2);

    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> table.subsetBySamples(List.of(allFiles.get(0), notInTable)));
    assertTrue(ex.getMessage().contains(notInTable.getName()), ex.getMessage());
  }

  @Test
  void testUnknownRowThrowsInsteadOfResolvingToTheFirstRow() {
    final FeaturesDataTable table = createDataTable(List.of(0, 1, 3, 4));
    // a row of a different feature list is not part of the table
    final FeaturesDataTable otherTable = createDataTable(List.of(0, 1, 3, 4));
    final var foreignRow = otherTable.getFeatureListRows().getFirst();

    assertThrows(IllegalArgumentException.class, () -> table.getFeatureIndex(foreignRow));
    assertThrows(IllegalArgumentException.class, () -> table.getSampleIndex(allFiles.get(2)));
  }

  @Test
  void testConstantAbundancesInBothGroupsHaveNoPValue() {
    // this is what global limit of detection imputation produces for sparse rows: all values equal,
    // so the t-test has no variance to work with and returns NaN. Those rows used to disappear from
    // the volcano plot without any notice.
    final FeaturesDataTable table = createDataTable(List.of(0, 1, 3, 4));
    final UnivariateRowSignificanceTest<String> test = new UnivariateRowSignificanceTest<>(table,
        SignificanceTests.WELCHS_T_TEST, groupColumn, "A", "B");

    final RowSignificanceTestResult result = test.test(table.getFeatureListRows().getFirst());
    assertNotNull(result);
    assertTrue(Double.isNaN(result.pValue()), "expected NaN but was " + result.pValue());
  }
}

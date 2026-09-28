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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata1GroupSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureListTestUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RsdFilterTest {

  @Test
  void testUndetectedQcRowIsRejectedAfterMissingValueImputation() {
    final List<RawDataFileImpl> files = FeatureListTestUtils.createRawFiles(3, "sample",
        LocalDateTime.of(2026, 1, 1, 0, 0), Duration.ofMinutes(1));
    final List<RawDataFile> rawFiles = List.copyOf(files);
    final ModularFeatureList featureList = new ModularFeatureList("feature list", null,
        rawFiles.toArray(RawDataFile[]::new));
    final ModularFeatureListRow row = FeatureListTestUtils.addRow(featureList, 1, rawFiles,
        Arrays.asList(null, null, 100f));

    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
    final MetadataTable metadata = ProjectService.getMetadata();
    final MetadataColumn<String> sampleTypeColumn = metadata.getSampleTypeColumn();
    metadata.setValue(sampleTypeColumn, rawFiles.get(0), SampleType.QC.toString());
    metadata.setValue(sampleTypeColumn, rawFiles.get(1), SampleType.QC.toString());
    metadata.setValue(sampleTypeColumn, rawFiles.get(2), SampleType.SAMPLE.toString());

    final Metadata1GroupSelection qcSelection = new Metadata1GroupSelection(
        MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC.toString());
    final RsdFilterParameters parameters = new RsdFilterParameters();
    parameters.setAll(AbundanceMeasure.Height, ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION, 0.2,
        0.2, false, qcSelection);
    final RsdFilter filter = parameters.createFilter(featureList.getRows(), rawFiles);

    Assertions.assertEquals(rawFiles.subList(0, 2), filter.getGroupDataFiles());
    Assertions.assertEquals(2, filter.dataTable().getFeatureRow(0).countOriginalMissingValues());
    Assertions.assertFalse(filter.matches(row, 0));
  }
}

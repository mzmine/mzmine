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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataListGroupsSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

/**
 * Metadata based selections and sample type filters are evaluated against the metadata of the given
 * project and not against the current project.
 */
class ProjectMetadataSelectionTest {

  private static final String GROUP_COLUMN = "group";

  private MZmineProject otherProject;
  private RawDataFile fileA;
  private RawDataFile fileB;

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @BeforeEach
  void setUp() {
    // the current project has neither the files nor the metadata
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());

    otherProject = new MZmineProjectImpl();
    fileA = new RawDataFileImpl("a.mzML", null, null);
    fileB = new RawDataFileImpl("b.mzML", null, null);
    otherProject.addFile(fileA);
    otherProject.addFile(fileB);

    final MetadataTable metadata = otherProject.getProjectMetadata();
    final StringMetadataColumn group = new StringMetadataColumn(GROUP_COLUMN);
    metadata.setValue(group, fileA, "x");
    metadata.setValue(group, fileB, "y");
    final MetadataColumn<String> sampleType = metadata.getSampleTypeColumn();
    metadata.setValue(sampleType, fileA, SampleType.QC.toString());
    metadata.setValue(sampleType, fileB, SampleType.SAMPLE.toString());
  }

  @AfterEach
  void clearProject() {
    MZmineTestUtil.clearProjectAndLibraries();
  }

  @Test
  void byMetadataSelectionUsesGivenProject() {
    final RawDataFilesSelection selection = new RawDataFilesSelection(
        RawDataFilesSelectionType.BY_METADATA);
    selection.setIncludeMetadataSelection(
        new MetadataListGroupsSelection(GROUP_COLUMN, List.of("x")));

    Assertions.assertArrayEquals(new RawDataFile[]{fileA},
        selection.getMatchingRawDataFiles(otherProject));
  }

  @Test
  void sampleTypeFilterUsesGivenMetadata() {
    final List<RawDataFile> qcs = SampleTypeFilter.qc()
        .filterFiles(otherProject.getProjectMetadata(), List.of(fileA, fileB));

    Assertions.assertEquals(List.of(fileA), qcs);
  }

  @Test
  void filenameColumnContainsFilesOfTable() {
    final MetadataTable metadata = otherProject.getProjectMetadata();
    final Map<RawDataFile, Object> fileNames = metadata.getColumnData(
        new StringMetadataColumn(MetadataColumn.FILENAME_HEADER));

    Assertions.assertNotNull(fileNames);
    Assertions.assertEquals(Map.of(fileA, "a.mzML", fileB, "b.mzML"), fileNames);
  }
}

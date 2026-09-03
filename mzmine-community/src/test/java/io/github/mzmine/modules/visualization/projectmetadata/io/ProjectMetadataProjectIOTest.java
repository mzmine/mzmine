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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testutils.MZmineTestUtil;

class ProjectMetadataProjectIOTest {

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @TempDir
  Path tempDir;

  @AfterEach
  void resetProject() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }

  @Test
  void savesAndRestoresCurrentMetadataSnapshot() throws IOException {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.setStandalone(true);
    ProjectService.getProjectManager().setCurrentProject(project);
    final RawDataFileImpl rawA = new RawDataFileImpl("a.mzML", null, null);
    final RawDataFileImpl rawB = new RawDataFileImpl("b.mzML", null, null);
    project.addFile(rawA);
    project.addFile(rawB);

    final MetadataTable metadata = project.getProjectMetadata();
    metadata.clearData();
    final StringMetadataColumn group = new StringMetadataColumn("Group", "User-entered group");
    final DoubleMetadataColumn concentration = new DoubleMetadataColumn("Concentration", "mg/L");
    metadata.setValue(group, rawA, "control");
    metadata.setValue(group, rawB, "treated");
    metadata.setValue(concentration, rawB, 12.5d);

    final Path archive = tempDir.resolve("metadata-roundtrip.mzmine");
    try (ZipOutputStream output = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
      assertTrue(ProjectMetadataProjectIO.saveToZip(output));
    }

    metadata.clearData();
    try (ZipFile zipFile = new ZipFile(archive.toFile())) {
      assertTrue(ProjectMetadataProjectIO.loadFromZip(zipFile));
    }

    final StringMetadataColumn loadedGroup = (StringMetadataColumn) metadata.getColumnByName(
        "Group");
    final DoubleMetadataColumn loadedConcentration = (DoubleMetadataColumn) metadata.getColumnByName(
        "Concentration");
    assertEquals("User-entered group", loadedGroup.getDescription());
    assertEquals("control", metadata.getValue(loadedGroup, rawA));
    assertEquals("treated", metadata.getValue(loadedGroup, rawB));
    assertNull(metadata.getValue(loadedConcentration, rawA));
    assertEquals(12.5d, metadata.getValue(loadedConcentration, rawB));
  }

  @Test
  void omitsEmptyMetadataSnapshot() throws IOException {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final Path archive = tempDir.resolve("empty-metadata.mzmine");

    try (ZipOutputStream output = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
      assertFalse(ProjectMetadataProjectIO.saveToZip(output));
    }

    try (ZipFile zipFile = new ZipFile(archive.toFile())) {
      assertNull(zipFile.getEntry(ProjectMetadataProjectIO.PROJECT_METADATA_FILENAME));
      assertFalse(ProjectMetadataProjectIO.loadFromZip(zipFile));
    }
  }
}

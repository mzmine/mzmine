/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class ProjectMetadataReaderTest {

  static RawDataFile rawA = new RawDataFileImpl("a.mzML", null, null);
  static RawDataFile rawB = new RawDataFileImpl("b.mzML", null, null);
  static RawDataFile rawC = new RawDataFileImpl("c.mzML", null, null);

  @BeforeAll
  public static void init() {
    MZmineTestUtil.startMzmineCore();
    MZmineProject project = ProjectService.getProject();
    project.addFile(rawA);
    project.addFile(rawB);
    project.addFile(rawC);
  }

  @Test
  void readFileRemoveAttributePrefix() {
    ProjectMetadataReader reader = new ProjectMetadataReader(false, true);

    var files = List.of("metadata/metadata_wide.tsv", "metadata/metadata_wide_defined.tsv");
    for (String f : files) {
      File file = new File(getClass().getClassLoader().getResource(f).getFile());

      MetadataTable meta = reader.readFile(file);
      Assertions.assertNotNull(meta);
      Assertions.assertTrue(reader.getErrors().isEmpty());

      var group2Col = (MetadataColumn<String>) meta.getColumnByName("Group2");
      Assertions.assertNotNull(group2Col);

      final Map<String, List<RawDataFile>> groupedCol2Files = meta.groupFilesByColumn(group2Col);
      Assertions.assertEquals(2, groupedCol2Files.size());
      Assertions.assertEquals(1, groupedCol2Files.get("A").size());
      Assertions.assertEquals(2, groupedCol2Files.get("B").size());

      var numCol = (MetadataColumn<Double>) meta.getColumnByName("NumberCol");
      final Map<Double, List<RawDataFile>> groupedNumberColFiles = meta.groupFilesByColumn(numCol);
      Assertions.assertEquals(2, groupedNumberColFiles.size());
      Assertions.assertEquals(1, groupedNumberColFiles.get(12d).size());
      Assertions.assertEquals(1, groupedNumberColFiles.get(0.5).size());

      Assertions.assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(numCol)).containsAll(List.of(0.5d, 12d)));
      Assertions.assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(group2Col)).containsAll(List.of("A", "B")));
    }
  }

  @Test
  void readFileKeepAttributePrefix() {
    ProjectMetadataReader reader = new ProjectMetadataReader(false, false);

    var files = List.of("metadata/metadata_wide.tsv", "metadata/metadata_wide_defined.tsv");
    for (String f : files) {
      File file = new File(getClass().getClassLoader().getResource(f).getFile());

      MetadataTable meta = reader.readFile(file);
      Assertions.assertNotNull(meta);
      Assertions.assertTrue(reader.getErrors().isEmpty());

      var group2Col = (MetadataColumn<String>) meta.getColumnByName("ATTRIBUTE_Group2");
      Assertions.assertNotNull(group2Col);

      final Map<String, List<RawDataFile>> groupedCol2Files = meta.groupFilesByColumn(group2Col);
      Assertions.assertEquals(2, groupedCol2Files.size());
      Assertions.assertEquals(1, groupedCol2Files.get("A").size());
      Assertions.assertEquals(2, groupedCol2Files.get("B").size());

      var numCol = (MetadataColumn<Double>) meta.getColumnByName("ATTRIBUTE_NumberCol");
      final Map<Double, List<RawDataFile>> groupedNumberColFiles = meta.groupFilesByColumn(numCol);
      Assertions.assertEquals(2, groupedNumberColFiles.size());
      Assertions.assertEquals(1, groupedNumberColFiles.get(12d).size());
      Assertions.assertEquals(1, groupedNumberColFiles.get(0.5).size());

      Assertions.assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(numCol)).containsAll(List.of(0.5d, 12d)));
      Assertions.assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(group2Col)).containsAll(List.of("A", "B")));
    }
  }


  @Test
  void testImportWideNumberFormatError() {
    File file = new File(getClass().getClassLoader()
        .getResource("metadata/metadata_wide_defined_numberparse_exception.tsv").getFile());
    ProjectMetadataReader reader = new ProjectMetadataReader(true, true);
    var meta = reader.readFile(file);
    Assertions.assertNotNull(meta);
    Assertions.assertEquals(1, reader.getErrors().size()); // warning on wrong column

    reader = new ProjectMetadataReader(false, true);
    meta = reader.readFile(file);
    Assertions.assertNull(meta); // should be null
    Assertions.assertEquals(1, reader.getErrors().size()); // warning on wrong column
  }

  @Test
  void testImportWideOnlyTypesDefined() {
    File file = new File(
        getClass().getClassLoader().getResource("metadata/metadata_wide_only_types_defined.tsv")
            .getFile());
    var reader = new ProjectMetadataReader(false, true);
    var meta = reader.readFile(file);
    Assertions.assertNotNull(meta);
    Assertions.assertEquals(0, reader.getErrors().size()); // warning on wrong column
  }
}
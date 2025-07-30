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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.util.date.DateTimeUtils;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

/**
 * Loads metadata by using placeholders instead of actual raw data files.
 */
class ProjectMetadataReaderWithoutDataTest {

  RawDataFilePlaceholder rawA = new RawDataFilePlaceholder("a.mzML", null);
  RawDataFilePlaceholder rawB = new RawDataFilePlaceholder("b.mzML", null);
  RawDataFilePlaceholder rawC = new RawDataFilePlaceholder("c.mzML", null);

  @BeforeEach
  void init() {
    MZmineTestUtil.cleanProject();
  }

  @Test
  void readWithTypeMismatch() {
    // should read all as string as number and date also contains a string
    ProjectMetadataReader reader = new ProjectMetadataReader(false, true, true);
    File file = new File(
        getClass().getClassLoader().getResource("metadata/metadata_wide_type_mismatch.tsv")
            .getFile());

    final MetadataTable table = reader.readFile(file);
    for (MetadataColumn<?> column : table.getColumns()) {
      if (column.getTitle().equals("date2")) {
        assertInstanceOf(DateMetadataColumn.class, column);
      } else {
        assertInstanceOf(StringMetadataColumn.class, column);
      }
    }
    // Filename	ATTRIBUTE_Group	ATTRIBUTE_NumberCol	ATTRIBUTE_Group2	run_date	sample_id	date2
    //a.mzML	A	12	A	2021-08-31T15:33:15	A	20240125
    //b.mzML	A	0.5	B	NODATE	20241005	2025-10-30
    //c.mzML	c	NONUMBER	B	2021-08-31T19:23:46	1	30-12-2020
    assertEquals("A", table.getValue(table.getColumnByName("sample_id"), rawA));
    assertEquals("20241005", table.getValue(table.getColumnByName("sample_id"), rawB));
    assertEquals("1", table.getValue(table.getColumnByName("sample_id"), rawC));

    assertEquals("2021-08-31T15:33:15", table.getValue(table.getColumnByName("run_date"), rawA));
    assertEquals("NODATE", table.getValue(table.getColumnByName("run_date"), rawB));
    assertEquals("2021-08-31T19:23:46", table.getValue(table.getColumnByName("run_date"), rawC));

    assertEquals("12", table.getValue(table.getColumnByName("NumberCol"), rawA));
    assertEquals("0.5", table.getValue(table.getColumnByName("NumberCol"), rawB));
    assertEquals("NONUMBER", table.getValue(table.getColumnByName("NumberCol"), rawC));

    assertEquals(DateTimeUtils.parse("2024-01-25"),
        table.getValue(table.getColumnByName("date2"), rawA));
    assertEquals(DateTimeUtils.parse("2025-10-30"),
        table.getValue(table.getColumnByName("date2"), rawB));
    assertNull(table.getValue(table.getColumnByName("date2"), rawC));
  }

  @Test
  void readFileRemoveAttributePrefix() {
    ProjectMetadataReader reader = new ProjectMetadataReader(false, true, true);

    var files = List.of("metadata/metadata_wide.tsv", "metadata/metadata_wide_defined.tsv");
    for (String f : files) {
      File file = new File(getClass().getClassLoader().getResource(f).getFile());

      MetadataTable meta = reader.readFile(file);
      assertNotNull(meta);
      assertTrue(reader.getErrors().isEmpty());

      var group2Col = (MetadataColumn<String>) meta.getColumnByName("Group2");
      assertNotNull(group2Col);

      final Map<String, List<RawDataFile>> groupedCol2Files = meta.groupFilesByColumn(group2Col);
      assertEquals(2, groupedCol2Files.size());
      assertEquals(1, groupedCol2Files.get("A").size());
      assertEquals(2, groupedCol2Files.get("B").size());

      var numCol = (MetadataColumn<Double>) meta.getColumnByName("NumberCol");
      final Map<Double, List<RawDataFile>> groupedNumberColFiles = meta.groupFilesByColumn(numCol);
      assertEquals(2, groupedNumberColFiles.size());
      assertEquals(1, groupedNumberColFiles.get(12d).size());
      assertEquals(1, groupedNumberColFiles.get(0.5).size());

      assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(numCol)).containsAll(List.of(0.5d, 12d)));
      assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(group2Col)).containsAll(List.of("A", "B")));
    }
  }

  @Test
  void readFileKeepAttributePrefix() {
    ProjectMetadataReader reader = new ProjectMetadataReader(false, false, true);

    var files = List.of("metadata/metadata_wide.tsv", "metadata/metadata_wide_defined.tsv");
    for (String f : files) {
      File file = new File(getClass().getClassLoader().getResource(f).getFile());

      MetadataTable meta = reader.readFile(file);
      assertNotNull(meta);
      assertTrue(reader.getErrors().isEmpty());

      var group2Col = (MetadataColumn<String>) meta.getColumnByName("ATTRIBUTE_Group2");
      assertNotNull(group2Col);

      final Map<String, List<RawDataFile>> groupedCol2Files = meta.groupFilesByColumn(group2Col);
      assertEquals(2, groupedCol2Files.size());
      assertEquals(1, groupedCol2Files.get("A").size());
      assertEquals(2, groupedCol2Files.get("B").size());

      var numCol = (MetadataColumn<Double>) meta.getColumnByName("ATTRIBUTE_NumberCol");
      final Map<Double, List<RawDataFile>> groupedNumberColFiles = meta.groupFilesByColumn(numCol);
      assertEquals(2, groupedNumberColFiles.size());
      assertEquals(1, groupedNumberColFiles.get(12d).size());
      assertEquals(1, groupedNumberColFiles.get(0.5).size());

      assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(numCol)).containsAll(List.of(0.5d, 12d)));
      assertTrue(
          new HashSet<>(meta.getDistinctColumnValues(group2Col)).containsAll(List.of("A", "B")));
    }
  }


  @Test
  void testImportWideNumberFormatError() {
    File file = new File(getClass().getClassLoader()
        .getResource("metadata/metadata_wide_defined_numberparse_exception.tsv").getFile());
    ProjectMetadataReader reader = new ProjectMetadataReader(true, true, true);
    var meta = reader.readFile(file);
    assertNotNull(meta);
    assertEquals(1, reader.getErrors().size()); // warning on wrong column

    reader = new ProjectMetadataReader(false, true, true);
    meta = reader.readFile(file);
    assertNull(meta); // should be null
    assertEquals(1, reader.getErrors().size()); // warning on wrong column
  }

  @Test
  void testImportWideOnlyTypesDefined() {
    File file = new File(
        getClass().getClassLoader().getResource("metadata/metadata_wide_only_types_defined.tsv")
            .getFile());
    var reader = new ProjectMetadataReader(false, true, true);
    var meta = reader.readFile(file);
    assertNotNull(meta);
    assertEquals(0, reader.getErrors().size()); // warning on wrong column
  }
}
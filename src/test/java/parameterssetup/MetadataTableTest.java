/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package parameterssetup;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataTableTest {

  MetadataTable emptyMetadataTable;
  MetadataTable filledMetadataTable;

  String columnName1 = "param1";
  String columnName2 = "param2";
  String columnName3 = "param3";
  String columnNameNew = "paramNew";
  String columnValue1 = "stringValue";
  Double columnValue2 = 0.14d;
  LocalDateTime columnValue3 = LocalDateTime.now();
  String columnValueNew = "stringValueNew";
  MetadataColumn<String> column1 = new StringMetadataColumn(columnName1);
  MetadataColumn<Double> column2 = new DoubleMetadataColumn(columnName2);
  MetadataColumn<LocalDateTime> column3 = new DateMetadataColumn(columnName3);
  MetadataColumn<String> columnNew = new StringMetadataColumn(columnNameNew, columnValueNew);

  @Mock
  RawDataFile rawDataFile1;
  @Mock
  RawDataFile rawDataFile2;
  @Mock
  RawDataFile rawDataFile3;

  @BeforeEach
  void setUp() {
    Map<MetadataColumn<?>, Map<RawDataFile, Object>> data = new HashMap<>();
    ConcurrentMap<RawDataFile, Object> row1 = new ConcurrentHashMap<>();
    ConcurrentMap<RawDataFile, Object> row2 = new ConcurrentHashMap<>();
    ConcurrentMap<RawDataFile, Object> row3 = new ConcurrentHashMap<>();
    row1.put(rawDataFile1, columnValue1);
    row2.put(rawDataFile2, columnValue2);
    row3.put(rawDataFile3, columnValue3);
    data.put(column1, row1);
    data.put(column2, row2);
    data.put(column3, row3);

    filledMetadataTable = new MetadataTable(data);
    emptyMetadataTable = new MetadataTable();
  }

  @Test
  void getData() {
    Assertions.assertTrue(emptyMetadataTable.getData().isEmpty());
    Assertions.assertEquals(filledMetadataTable.getData().size(), 3);
  }

  @Test
  void clearData() {
    Assertions.assertFalse(filledMetadataTable.getData().isEmpty());
    filledMetadataTable.clearData();
    Assertions.assertTrue(filledMetadataTable.getData().isEmpty());
  }

  @Test
  void addColumn() {
    // add a column into an empty table
    Assertions.assertTrue(emptyMetadataTable.getData().isEmpty());
    emptyMetadataTable.addColumn(columnNew);
    Assertions.assertEquals(emptyMetadataTable.getData().size(), 1);
    // add a column into a filled table
    Assertions.assertFalse(filledMetadataTable.getData().isEmpty());
    filledMetadataTable.addColumn(columnNew);
    Assertions.assertEquals(filledMetadataTable.getData().size(), 4);
  }

  @Test
  void removeColumn() {
    // removing an unreal column will have no affect
    emptyMetadataTable.removeColumn(columnNew);
    Assertions.assertTrue(emptyMetadataTable.getData().isEmpty());
    // removing a real column
    Assertions.assertEquals(filledMetadataTable.getData().size(), 3);
    filledMetadataTable.removeColumn(column1);
    Assertions.assertEquals(filledMetadataTable.getData().size(), 2);
  }

  @Test
  void hasColumn() {
    Assertions.assertFalse(emptyMetadataTable.hasColumn(columnNew));
    Assertions.assertFalse(filledMetadataTable.hasColumn(columnNew));
    filledMetadataTable.addColumn(columnNew);
    Assertions.assertTrue(filledMetadataTable.hasColumn(columnNew));
    Assertions.assertTrue(filledMetadataTable.hasColumn(column1));
  }

  @Test
  void getColumns() {
    Assertions.assertEquals(emptyMetadataTable.getColumns().size(), 0);
    Set<MetadataColumn<?>> filledMetadataTableColumns = Set.of(column1, column2, column3);
    Assertions.assertEquals(filledMetadataTable.getColumns(), filledMetadataTableColumns);
  }

  @Test
  void getColumnByName() {
    Assertions.assertNull(emptyMetadataTable.getColumnByName(columnNameNew));
    Assertions.assertEquals(filledMetadataTable.getColumnByName(columnName1), column1);
    Assertions.assertEquals(filledMetadataTable.getColumnByName(columnName2), column2);
    Assertions.assertEquals(filledMetadataTable.getColumnByName(columnName3), column3);
    Assertions.assertNull(filledMetadataTable.getColumnByName(columnNameNew));
  }

  @Test
  void getValue() {
    Assertions.assertNull(emptyMetadataTable.getValue(column1, rawDataFile1));
    Assertions.assertEquals(filledMetadataTable.getValue(column1, rawDataFile1), columnValue1);
    Assertions.assertEquals(filledMetadataTable.getValue(column2, rawDataFile2), columnValue2);
    Assertions.assertEquals(filledMetadataTable.getValue(column3, rawDataFile3), columnValue3);
  }

  @Test
  void setValue() {
    // setting value to an unreal parameter column will lead to creation of this column
    Assertions.assertNull(emptyMetadataTable.getValue(column1, rawDataFile1));
    emptyMetadataTable.setValue(column1, rawDataFile1, columnValue1);
    Assertions.assertEquals(emptyMetadataTable.getValue(column1, rawDataFile1), columnValue1);
    // change value of the existing parameter column (it has String type)
    Assertions.assertEquals(filledMetadataTable.getValue(column1, rawDataFile1), columnValue1);
    filledMetadataTable.setValue(column1, rawDataFile1, columnValueNew);
    Assertions.assertEquals(filledMetadataTable.getValue(column1, rawDataFile1), columnValueNew);
  }
}
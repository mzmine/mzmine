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

package parameterssetup;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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
  void columnsOfConstructedTable() {
    Assertions.assertTrue(emptyMetadataTable.getColumns().isEmpty());
    Assertions.assertEquals(filledMetadataTable.getColumns().size(), 3);
  }

  @Test
  void clearData() {
    Assertions.assertFalse(filledMetadataTable.getColumns().isEmpty());
    filledMetadataTable.clearData();
    Assertions.assertTrue(filledMetadataTable.getColumns().isEmpty());
  }

  @Test
  void addColumn() {
    // add a column into an empty table
    Assertions.assertTrue(emptyMetadataTable.getColumns().isEmpty());
    emptyMetadataTable.addColumn(columnNew);
    Assertions.assertEquals(emptyMetadataTable.getColumns().size(), 1);
    // add a column into a filled table
    Assertions.assertFalse(filledMetadataTable.getColumns().isEmpty());
    filledMetadataTable.addColumn(columnNew);
    Assertions.assertEquals(filledMetadataTable.getColumns().size(), 4);
  }

  @Test
  void removeColumn() {
    // removing an unreal column will have no affect
    emptyMetadataTable.removeColumn(columnNew);
    Assertions.assertTrue(emptyMetadataTable.getColumns().isEmpty());
    // removing a real column
    Assertions.assertEquals(filledMetadataTable.getColumns().size(), 3);
    filledMetadataTable.removeColumn(column1);
    Assertions.assertEquals(filledMetadataTable.getColumns().size(), 2);
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

  @Test
  void dataIsOnlyExposedAsUnmodifiableView() {
    final Map<RawDataFile, Object> columnData = filledMetadataTable.getColumnData(column1);
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> columnData.put(rawDataFile2, columnValueNew));
    Assertions.assertThrows(UnsupportedOperationException.class, columnData::clear);
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> filledMetadataTable.getColumns().remove(column1));
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> filledMetadataTable.getColumns().clear());

    // the table is unchanged
    Assertions.assertEquals(3, filledMetadataTable.getColumns().size());
    Assertions.assertEquals(columnValue1, filledMetadataTable.getValue(column1, rawDataFile1));
  }

  @Test
  void viewsReflectLaterChanges() {
    final Set<MetadataColumn<?>> columns = emptyMetadataTable.getColumns();
    Assertions.assertTrue(columns.isEmpty());

    emptyMetadataTable.setValue(column1, rawDataFile1, columnValue1);
    // the column view is live, the caller does not have to fetch it again
    Assertions.assertEquals(Set.of(column1), columns);

    final Map<RawDataFile, Object> columnData = emptyMetadataTable.getColumnData(column1);
    emptyMetadataTable.setValue(column1, rawDataFile2, columnValueNew);
    Assertions.assertEquals(columnValueNew, columnData.get(rawDataFile2));
  }

  @Test
  void constructorCopiesTheInputData() {
    final Map<RawDataFile, Object> row = new ConcurrentHashMap<>();
    row.put(rawDataFile1, columnValue1);
    final Map<MetadataColumn<?>, Map<RawDataFile, Object>> input = new HashMap<>();
    input.put(column1, row);

    final MetadataTable table = new MetadataTable(input);
    // changing the input afterwards must not change the table
    row.put(rawDataFile2, columnValueNew);
    input.remove(column1);

    Assertions.assertEquals(1, table.getColumns().size());
    Assertions.assertEquals(columnValue1, table.getValue(column1, rawDataFile1));
    Assertions.assertNull(table.getValue(column1, rawDataFile2));
  }

  @Test
  void setValuesAppliesAllAndCountsAsOneChange() {
    final long version = emptyMetadataTable.getVersion();
    final Map<RawDataFile, String> values = new HashMap<>();
    values.put(rawDataFile1, columnValue1);
    values.put(rawDataFile2, columnValueNew);
    emptyMetadataTable.setValues(column1, values);

    Assertions.assertEquals(version + 1, emptyMetadataTable.getVersion());
    Assertions.assertEquals(columnValue1, emptyMetadataTable.getValue(column1, rawDataFile1));
    Assertions.assertEquals(columnValueNew, emptyMetadataTable.getValue(column1, rawDataFile2));

    // null values remove the entry, the column is kept
    final Map<RawDataFile, String> removals = new HashMap<>();
    removals.put(rawDataFile1, null);
    emptyMetadataTable.setValues(column1, removals);
    Assertions.assertNull(emptyMetadataTable.getValue(column1, rawDataFile1));
    Assertions.assertTrue(emptyMetadataTable.hasColumn(column1));
  }

  @Test
  void batchUpdateIncrementsVersionOnce() {
    final long version = filledMetadataTable.getVersion();
    filledMetadataTable.batchUpdate(() -> {
      filledMetadataTable.setValue(column1, rawDataFile1, columnValueNew);
      filledMetadataTable.addColumn(columnNew);
      filledMetadataTable.removeColumn(column2);
    });

    Assertions.assertEquals(version + 1, filledMetadataTable.getVersion());
    Assertions.assertEquals(columnValueNew, filledMetadataTable.getValue(column1, rawDataFile1));
    Assertions.assertTrue(filledMetadataTable.hasColumn(columnNew));
    Assertions.assertFalse(filledMetadataTable.hasColumn(column2));
  }

  @Test
  void batchUpdateWithoutChangesKeepsVersion() {
    final long version = filledMetadataTable.getVersion();
    filledMetadataTable.batchUpdate(() -> filledMetadataTable.getColumns().size());
    Assertions.assertEquals(version, filledMetadataTable.getVersion());
  }

  @Test
  void removeFilesAndColumnsCountAsOneChange() {
    long version = filledMetadataTable.getVersion();
    filledMetadataTable.removeFiles(List.of(rawDataFile1, rawDataFile2));
    Assertions.assertEquals(version + 1, filledMetadataTable.getVersion());
    Assertions.assertNull(filledMetadataTable.getValue(column1, rawDataFile1));
    Assertions.assertNull(filledMetadataTable.getValue(column2, rawDataFile2));
    // columns are kept, only the values are removed
    Assertions.assertEquals(3, filledMetadataTable.getColumns().size());

    version = filledMetadataTable.getVersion();
    filledMetadataTable.removeColumns(List.of(column1, column2));
    Assertions.assertEquals(version + 1, filledMetadataTable.getVersion());
    Assertions.assertEquals(Set.of(column3), filledMetadataTable.getColumns());
  }

  @Test
  void setDataReplacesEverything() {
    final Map<MetadataColumn<?>, Map<RawDataFile, Object>> newData = new HashMap<>();
    newData.put(columnNew, Map.of(rawDataFile1, columnValueNew));

    final long version = filledMetadataTable.getVersion();
    filledMetadataTable.setData(newData);

    Assertions.assertEquals(version + 1, filledMetadataTable.getVersion());
    Assertions.assertEquals(Set.of(columnNew), filledMetadataTable.getColumns());
    Assertions.assertEquals(columnValueNew, filledMetadataTable.getValue(columnNew, rawDataFile1));
  }
}
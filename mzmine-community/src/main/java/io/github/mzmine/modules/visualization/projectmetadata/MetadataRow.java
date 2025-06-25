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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

/**
 * Represents a single row in the TableView, linked to a RawDataFile.
 */
public class MetadataRow {

  private static final Logger logger = Logger.getLogger(MetadataRow.class.getName());

  private final RawDataFile rawDataFile;
  private final MetadataTable sourceTable; // Keep reference for updates

  public MetadataRow(RawDataFile rawDataFile, MetadataTable sourceTable) {
    Objects.requireNonNull(rawDataFile, "RawDataFile cannot be null");
    Objects.requireNonNull(sourceTable, "Source MetadataTable cannot be null");
    this.rawDataFile = rawDataFile;
    this.sourceTable = sourceTable;
  }

  public RawDataFile getRawDataFile() {
    return rawDataFile;
  }

  /**
   * Gets an ObservableValue for a specific column in this row. This is used by the TableColumn's
   * cellValueFactory.
   *
   * @param column The MetadataColumn definition.
   * @return An ObservableValue containing the data for the cell.
   */
  public ObservableValue<?> getCellValueProperty(MetadataColumn<?> column) {
    // We use SimpleStringProperty to wrap the value.
    // It reads directly from the underlying sourceTable.
    // NOTE: This doesn't automatically update if the underlying table changes
    // externally. For full two-way binding *observability*, the underlying
    // MetadataTable would need to support listeners, which is much more complex.
    // This implementation focuses on displaying and *editing* via the TableView.
    Object value = sourceTable.getValue(column, this.rawDataFile);

    return switch (column) {
      case StringMetadataColumn _, DateMetadataColumn _ ->
          new SimpleStringProperty(Objects.requireNonNullElse(value, "").toString());
      case DoubleMetadataColumn _ -> new SimpleObjectProperty<Double>((Double) value) {
      };
    };

  }

  /**
   * Updates the value in the underlying MetadataTable for a specific column. This is typically
   * called from the TableColumn's onEditCommit handler.
   *
   * @param column   The column being updated.
   * @param newValue The new value from the TableView cell editor.
   */
  public <T> void updateValue(MetadataColumn<T> column, String newValue) {
    // Perform type check/conversion if necessary before putting
    T castedValue = column.convertOrElse(newValue, column.defaultValue());

    // Update the underlying map
    sourceTable.setValue(column, this.rawDataFile, castedValue);
    logger.finest("Updated Model: [" + rawDataFile.getName() + ", " + column.getTitle() + "] = "
                  + castedValue); // For debugging
  }

  // equals/hashCode based on the unique identifier RawDataFile
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataRow that = (MetadataRow) o;
    return Objects.equals(rawDataFile, that.rawDataFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawDataFile);
  }

  @Override
  public String toString() {
    return "MetadataRow{" + "rawDataFile=" + rawDataFile + '}';
  }
}

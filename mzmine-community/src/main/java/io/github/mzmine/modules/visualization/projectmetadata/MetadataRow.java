package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import java.util.Objects;

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
  public ObservableValue<String> getCellValueProperty(MetadataColumn<?> column) {
    // We use SimpleStringProperty to wrap the value.
    // It reads directly from the underlying sourceTable.
    // NOTE: This doesn't automatically update if the underlying table changes
    // externally. For full two-way binding *observability*, the underlying
    // MetadataTable would need to support listeners, which is much more complex.
    // This implementation focuses on displaying and *editing* via the TableView.
    Object value = sourceTable.getValue(column, this.rawDataFile);
    return new SimpleStringProperty(Objects.requireNonNullElse(value, "").toString());
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

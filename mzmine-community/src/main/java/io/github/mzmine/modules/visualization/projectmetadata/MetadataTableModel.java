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
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.util.StringUtils;
import static io.github.mzmine.util.StringUtils.inQuotes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

/**
 * Model/Wrapper for displaying a MetadataTable in a JavaFX TableView. Handles row creation, column
 * definition, data access, and updates.
 */
public class MetadataTableModel {

  private final MetadataTable metadataTable;
  private final TableView<MetadataRow> tableView;

  public MetadataTableModel(MetadataTable metadataTable, TableView<MetadataRow> tableView) {
    this.tableView = tableView;
    Objects.requireNonNull(metadataTable, "MetadataTable cannot be null");
    this.metadataTable = metadataTable;

    createAndSetExistingColumns();
  }

  private void addRows(MetadataTable metadataTable, TableView<MetadataRow> tableView) {
    // 2. Determine the unique rows (gather all unique RawDataFile keys)
    Set<RawDataFile> uniqueFiles = metadataTable.getData().values().stream()
        .flatMap(innerMap -> innerMap.keySet().stream()).collect(Collectors.toSet());

    // 3. Create MetadataRow objects for each unique RawDataFile
    List<MetadataRow> rows = uniqueFiles.stream()
        .map(file -> new MetadataRow(file, this.metadataTable)) // Pass table ref
        .sorted(Comparator.comparing(row -> row.getRawDataFile().getName()))
        .collect(Collectors.toCollection(ArrayList::new));
    tableView.setItems(FXCollections.observableList(rows));
  }

  /**
   * Creates and configures the TableColumn instances for a given TableView based on the
   * MetadataTable structure.
   * <p>
   * NOTE: This clears existing columns in the TableView.
   */
  public void createAndSetExistingColumns() {
    tableView.getColumns().clear();
    tableView.setEditable(true); // Make table editable

    final TableColumn<MetadataRow, String> dataFileColumn = createDataFileColumn();
    tableView.getColumns().add(dataFileColumn);

    final List<MetadataColumn<?>> tableColumns = metadataTable.getData().keySet().stream().sorted()
        .toList();

    for (MetadataColumn<?> metaColumn : tableColumns) {
      createAndAddColumnToTableView(metaColumn);
    }

    addRows(metadataTable, tableView);
  }

  /**
   * Adds a column that already exists to the table view. To create a new column in the
   * {@link MetadataTable} as well use {@link MetadataTableModel#createAndAddNewColumn}
   */
  private void createAndAddColumnToTableView(MetadataColumn<?> metaColumn) {
    final TableColumn<MetadataRow, ?> fxColumn = createColumn(metaColumn);
    tableView.getColumns().add(fxColumn);
  }

  /**
   * Adds a new column to the underlying {@link MetadataTable} and the GUI model.
   */
  public void createAndAddNewColumn(MetadataColumn<?> column) {
    metadataTable.addColumn(column);
    createAndAddColumnToTableView(column);
  }

  /**
   * Creates the java fx column from an existing metadata column
   */
  private @NotNull TableColumn<MetadataRow, ?> createColumn(MetadataColumn<?> metaColumn) {
    // Create a JavaFX TableColumn for each MetadataColumn
    TableColumn<MetadataRow, ?> fxColumn = new TableColumn<>(metaColumn.getTitle());

    // --- Cell Value Factory ---
    // Tells the column how to get the data for a cell from a MetadataRow object
    fxColumn.setCellValueFactory(cellDataFeatures -> {
      MetadataRow row = cellDataFeatures.getValue();
      // Use the helper method in MetadataRow to get an ObservableValue
      return (ObservableValue) row.getCellValueProperty(metaColumn);
    });

    // --- Cell Factory (for editing) ---
    // Use appropriate cell factories and converters based on MetadataColumn type
    fxColumn.setCellFactory(column -> (TableCell) createEditingCell(metaColumn));

    // --- On Edit Commit ---
    // Tells the column what to do when a cell edit is committed
    fxColumn.setOnEditCommit(event -> {
      MetadataRow row = event.getRowValue(); // Get the row that was edited
      Object newValue = event.getNewValue(); // Get the new value from the editor

      // Ask the MetadataRow object to update the underlying MetadataTable
      // Pass the specific MetadataColumn definition to ensure correct update
      // (Need to capture metaColumn in the lambda scope)
      row.updateValue(metaColumn, newValue != null ? newValue.toString() : null);

      // Optional: Refresh the specific cell or row visually if needed,
      // though the change should ideally be reflected via the
      // observable value returned by getCellValueProperty if it were
      // truly bound to an observable underlying model.
      // In this setup, a manual refresh might sometimes be needed
      // if the underlying change isn't picked up automatically.
      // event.getTableView().refresh(); // Can be heavy
      event.getTableView().getItems().set(event.getTablePosition().getRow(), row); // Force update?
    });

    // Set preferred width or other properties if desired
    fxColumn.setPrefWidth(140);
    return fxColumn;
  }

  /**
   * The data file column is the index of the {@link MetadataTable} and thus not a real column. need
   * to add manually.
   */
  private @NotNull TableColumn<MetadataRow, String> createDataFileColumn() {
    final TableColumn<MetadataRow, String> fxColumn = new TableColumn<>(
        MetadataColumn.FILENAME_HEADER);

    fxColumn.setCellValueFactory(cellDataFeatures -> {
      MetadataRow row = cellDataFeatures.getValue();
      // Use the helper method in MetadataRow to get an ObservableValue
      return new SimpleStringProperty(row.getRawDataFile().getName());
    });

    // Set preferred width or other properties if desired
    fxColumn.setPrefWidth(180);
    return fxColumn;
  }

  /**
   * Helper method to create an appropriate TextFieldTableCell with a StringConverter based on the
   * target data type defined in the MetadataColumn.
   */
  private TextFieldTableCell<MetadataRow, ?> createEditingCell(MetadataColumn<?> metaColumn) {
    StringConverter<?> converter = getConverterForType(metaColumn);
    // Cast needed because forTableColumn expects TableColumn<S, T>, CellFactory<S, T, U>
    // Here S=MetadataRow, T=Object. The converter handles String <-> String and validation.
    return new TextFieldTableCell<>(converter);
  }

  /**
   * Use the string converter as a validator
   */
  private StringConverter<?> getConverterForType(MetadataColumn<?> column) {
    return new StringConverter<>() {
      @Override
      public String toString(Object o) {
        if (o == null) {
          return "";
        }
        return o.toString();
      }

      @Override
      public Object fromString(String s) {
        if (StringUtils.isBlank(s)) {
          return "";
        }

        final var converted = column.convertOrElse(s, null);
        if (converted == null) {
          DialogLoggerUtil.showErrorDialog("Cannot convert input",
              "The input %s is invalid for the column %s of type %s. Please adhere to the format %s.".formatted(
                  inQuotes(s), inQuotes(column.getTitle()), inQuotes(column.getType().toString()),
                  inQuotes(column.exampleValue().toString())));
          return null;
        }
        return s;
      }
    };
  }

  /**
   * Removes a column from the view and the underlying table.
   */
  public void removeColumn(MetadataColumn<?> metaColumn) {
    tableView.getColumns().removeIf(col -> col.getText().equalsIgnoreCase(metaColumn.getTitle()));
    metadataTable.removeColumn(metaColumn);
  }
}

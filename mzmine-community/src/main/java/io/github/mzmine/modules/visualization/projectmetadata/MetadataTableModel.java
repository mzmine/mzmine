package io.github.mzmine.modules.visualization.projectmetadata;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;


import java.util.*;
import java.util.stream.Collectors;
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
    final TableColumn<MetadataRow, String> fxColumn = createColumn(metaColumn);
    tableView.getColumns().add(fxColumn);
  }

  /**
   * Adds a new column to the underlying {@link MetadataTable} and the GUI model.
   */
  public void createAndAddNewColumn(MetadataColumn<?> column) {
    metadataTable.addColumn(column);
    createAndAddColumnToTableView(column);
  }

  private @NotNull TableColumn<MetadataRow, String> createColumn(MetadataColumn<?> metaColumn) {
    // Create a JavaFX TableColumn for each MetadataColumn
    TableColumn<MetadataRow, String> fxColumn = new TableColumn<>(metaColumn.getTitle());

    // --- Cell Value Factory ---
    // Tells the column how to get the data for a cell from a MetadataRow object
    fxColumn.setCellValueFactory(cellDataFeatures -> {
      MetadataRow row = cellDataFeatures.getValue();
      // Use the helper method in MetadataRow to get an ObservableValue
      return row.getCellValueProperty(metaColumn);
    });

    // --- Cell Factory (for editing) ---
    // Use appropriate cell factories and converters based on MetadataColumn type
    fxColumn.setCellFactory(column -> createEditingCell(metaColumn));

    // --- On Edit Commit ---
    // Tells the column what to do when a cell edit is committed
    fxColumn.setOnEditCommit(event -> {
      MetadataRow row = event.getRowValue(); // Get the row that was edited
      String newValue = event.getNewValue(); // Get the new value from the editor

      // Ask the MetadataRow object to update the underlying MetadataTable
      // Pass the specific MetadataColumn definition to ensure correct update
      // (Need to capture metaColumn in the lambda scope)
      row.updateValue(metaColumn, newValue);

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
  private <T> TextFieldTableCell<MetadataRow, String> createEditingCell(
      MetadataColumn<T> metaColumn) {
    StringConverter<String> converter = getConverterForType(metaColumn);
    // Cast needed because forTableColumn expects TableColumn<S, T>, CellFactory<S, T, U>
    // Here S=MetadataRow, T=Object. The converter handles String <-> String and validation.
    return new TextFieldTableCell<>(converter);
  }

  /**
   * Use the string converter as a validator
   */
  private StringConverter<String> getConverterForType(MetadataColumn<?> column) {
    return new StringConverter<>() {
      @Override
      public String toString(String o) {
        if (o == null) {
          return "";
        }
        return o;
      }

      @Override
      public String fromString(String s) {
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

  public void removeColumn(MetadataColumn<?> metaColumn, TableView<MetadataRow> tableView) {
    tableView.getColumns().removeIf(col -> col.getText().equalsIgnoreCase(metaColumn.getTitle()));
    metadataTable.removeColumn(metaColumn);
  }
}

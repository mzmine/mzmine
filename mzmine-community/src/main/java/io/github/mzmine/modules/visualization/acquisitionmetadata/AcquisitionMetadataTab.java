/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.visualization.acquisitionmetadata;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.DefaultStringConverter;

/** Acquisition metadata with the same file-row layout and cell editing as Sample metadata. */
public class AcquisitionMetadataTab extends SimpleTab {

  private final TableView<RawDataFile> table = new TableView<>();

  public AcquisitionMetadataTab() {
    super("Acquisition metadata", false, false);
    table.setEditable(true);
    table.getSelectionModel().setCellSelectionEnabled(true);
    table.setPlaceholder(new Label("No raw data files are loaded."));
    refresh();

    final Button reload = new Button("Reload");
    reload.setOnAction(_ -> refresh());
    ButtonBar.setButtonData(reload, ButtonBar.ButtonData.LEFT);
    final ButtonBar buttons = new ButtonBar();
    buttons.getButtons().add(reload);
    buttons.setPadding(new Insets(5, 5, 10, 5));
    final BorderPane pane = new BorderPane(table);
    pane.setBottom(buttons);
    setContent(pane);
    selectedProperty().addListener((_, _, selected) -> {
      if (selected) {
        refresh();
      }
    });
  }

  private void refresh() {
    final List<RawDataFile> files = ProjectService.getProject().getCurrentRawDataFiles().stream()
        .sorted(Comparator.comparing(RawDataFile::getName)).toList();
    final TreeSet<String> names = new TreeSet<>();
    files.forEach(file -> names.addAll(values(file).keySet()));
    // Keep resized/reordered columns when only the file selection or cell values change.
    final var displayed = table.getColumns().stream().skip(1).map(TableColumn::getUserData)
        .collect(Collectors.toSet());
    if (table.getColumns().isEmpty() || !names.equals(displayed)) {
      table.getColumns().clear();
      final TableColumn<RawDataFile, String> filename = new TableColumn<>(MetadataColumn.FILENAME_HEADER);
      filename.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
      filename.setEditable(false);
      filename.setReorderable(false);
      filename.setPrefWidth(180);
      table.getColumns().add(filename);
      names.forEach(name -> table.getColumns().add(column(name)));
    }
    table.setItems(FXCollections.observableArrayList(files));
    table.sort();
  }

  private TableColumn<RawDataFile, String> column(final String name) {
    final TableColumn<RawDataFile, String> column = new TableColumn<>(title(name));
    column.setUserData(name);
    column.setCellValueFactory(cell -> new SimpleStringProperty(values(cell.getValue())
        .getOrDefault(name, "")));
    column.setCellFactory(_ -> new TextFieldTableCell<>(new DefaultStringConverter()) {
      @Override
      public void updateItem(final String value, final boolean empty) {
        super.updateItem(value, empty);
        final RawDataFile file = getTableRow() == null ? null : getTableRow().getItem();
        setTooltip(empty || file == null ? null : new Tooltip(sourceDetails(file, name)));
      }
    });
    column.setOnEditCommit(event -> {
      final RawDataFile file = event.getRowValue();
      file.setAcquisitionMetadata(file.getAcquisitionMetadata()
          .withValue(name, Objects.requireNonNullElse(event.getNewValue(), "")));
      table.refresh();
    });
    column.setPrefWidth(140);
    return column;
  }

  private static String title(final String name) {
    if (name.startsWith("Imported: ")) {
      return name.substring("Imported: ".length());
    }
    if (name.startsWith("Acquisition: ") || name.startsWith("Measured: ")) {
      final String label = name.substring(name.indexOf(": ") + 2);
      return label.isEmpty() || label.startsWith("m/z") ? label
          : Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
    return name;
  }

  private static String sourceDetails(final RawDataFile file, final String name) {
    final AcquisitionMetadata metadata = file.getAcquisitionMetadata();
    final String source;
    if (name.startsWith("Acquisition: ")) {
      final String terms = metadata.terms().stream()
          .filter(term -> name.equals("Acquisition: " + term.field().name()
              .toLowerCase(Locale.ROOT).replace('_', ' ')))
          .map(term -> term.label() + " [" + term.accession() + "]").distinct().sorted()
          .collect(Collectors.joining("; "));
      source = terms.isEmpty() ? "Not declared in file header" : "File header: " + terms;
    } else {
      source = name.startsWith("Measured: ") ? "Derived from loaded scans" : "File header";
    }
    return metadata.editedValues().containsKey(name) ? "Edited in mzmine\n" + source : source;
  }

  static Map<String, String> values(final RawDataFile file) {
    final Map<String, String> values = new LinkedHashMap<>();
    final AcquisitionMetadata metadata = file.getAcquisitionMetadata();
    for (final AcquisitionMetadata.Field field : AcquisitionMetadata.Field.values()) {
      values.put("Acquisition: " + field.name().toLowerCase(Locale.ROOT).replace('_', ' '),
          metadata.terms().stream().filter(term -> term.field() == field)
              .map(AcquisitionMetadata.Term::label).distinct().sorted()
              .collect(Collectors.joining("; ")));
    }
    metadata.localFields().forEach((name, value) -> values.put("Imported: " + name, value));
    values.put("Measured: MS levels", Arrays.stream(file.getMSLevels()).mapToObj(Integer::toString)
        .collect(Collectors.joining(", ")));
    values.put("Measured: polarity", file.getDataPolarity().toString());
    values.put("Measured: spectrum type", Objects.toString(file.getSpectraType(), ""));
    values.put("Measured: scan count", Integer.toString(file.getNumOfScans()));
    values.put("Measured: RT range (min)", file.getNumOfScans() > 0
        ? file.getDataRTRange().toString() : "");
    values.put("Measured: m/z range", file.getNumOfScans() > 0
        ? file.getDataMZRange().toString() : "");
    values.putAll(metadata.editedValues());
    return values;
  }

  @Override
  public void onRawDataFileSelectionChanged(final Collection<? extends RawDataFile> files) {
    if (isSelected()) {
      refresh();
    }
  }
}

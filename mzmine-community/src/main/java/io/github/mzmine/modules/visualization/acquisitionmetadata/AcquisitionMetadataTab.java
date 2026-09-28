/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.visualization.acquisitionmetadata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Read-only view of acquisition declarations and scan-derived measurements. */
public class AcquisitionMetadataTab extends SimpleTab {

  private final TableView<Row> table = new TableView<>();

  public AcquisitionMetadataTab() {
    super("Acquisition metadata", false, false);
    table.setItems(FXCollections.observableArrayList(rows()));
    table.setEditable(false);
    table.getColumns().addAll(column("Data file", Row::file), column("Field", Row::field),
        column("Value", Row::value), column("Source", Row::source), column("Unit", Row::unit));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(new Label("No raw data files are loaded."));

    final Label description = FxLabels.newLabel(
        "Read-only file-header declarations and values derived from loaded scans. "
            + "Editable sample annotations belong in Sample metadata.");
    description.setWrapText(true);
    final Button refresh = new Button("Refresh");
    refresh.setOnAction(_ -> refresh());
    final VBox header = FxLayout.newVBox(description, refresh);
    header.setSpacing(6);
    header.setPadding(new Insets(10));
    final BorderPane pane = new BorderPane(table);
    pane.setTop(header);
    setContent(pane);
    selectedProperty().addListener((_, _, selected) -> {
      if (selected) {
        refresh();
      }
    });
  }

  private void refresh() {
    table.getItems().setAll(rows());
  }

  private static TableColumn<Row, String> column(final String title,
      final java.util.function.Function<Row, String> value) {
    final TableColumn<Row, String> column = new TableColumn<>(title);
    column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(value.apply(
        cell.getValue())));
    return column;
  }

  static List<Row> rows() {
    final List<Row> rows = new ArrayList<>();
    for (final RawDataFile file : ProjectService.getProject().getCurrentRawDataFiles()) {
      final AcquisitionMetadata metadata = file.getAcquisitionMetadata();
      for (final AcquisitionMetadata.Field field : AcquisitionMetadata.Field.values()) {
        final List<AcquisitionMetadata.Term> values = metadata.terms().stream()
            .filter(term -> term.field() == field).toList();
        if (values.isEmpty()) {
          rows.add(new Row(file.getName(), display(field), "Unknown", "Not declared in file header", ""));
        } else {
          rows.add(new Row(file.getName(), display(field), values.stream()
              .map(term -> term.label() + " (" + term.accession() + ")")
              .collect(Collectors.joining(", ")), "File header", ""));
        }
      }
      metadata.localFields().forEach((name, value) -> rows.add(new Row(file.getName(), name,
          value, "File header", "")));
      rows.add(new Row(file.getName(), "Scan count", Integer.toString(file.getNumOfScans()),
          "Derived from loaded scans", "scans"));
      rows.add(new Row(file.getName(), "MS levels", java.util.Arrays.toString(file.getMSLevels()),
          "Derived from loaded scans", ""));
      rows.add(new Row(file.getName(), "Polarity", file.getDataPolarity().isEmpty() ? "Unknown"
          : file.getDataPolarity().toString(),
          "Derived from loaded scans", ""));
      rows.add(new Row(file.getName(), "Spectrum type", file.getSpectraType() == null ? "Unknown"
          : file.getSpectraType().toString(),
          "Derived from loaded scans", ""));
      rows.add(rangeRow(file, "Retention-time range", file.getDataRTRange(), "min"));
      rows.add(rangeRow(file, "m/z range", file.getDataMZRange(), "m/z"));
    }
    return rows;
  }

  private static Row rangeRow(final RawDataFile file, final String field,
      final Range<? extends Number> range, final String unit) {
    if (file.getNumOfScans() == 0) {
      return new Row(file.getName(), field, "Unknown", "Not available (no loaded scans)", unit);
    }
    return new Row(file.getName(), field, range.lowerEndpoint() + " – " + range.upperEndpoint(),
        "Derived from loaded scans", unit);
  }

  private static String display(final AcquisitionMetadata.Field field) {
    return switch (field) {
      case INSTRUMENT_MODEL -> "Instrument model";
      case ANALYZER -> "Analyzer";
      case IONIZATION -> "Ionization";
      case DETECTOR -> "Detector";
      case ACQUISITION_METHOD -> "Acquisition method";
    };
  }

  record Row(String file, String field, String value, String source, String unit) {
  }

  @Override
  public void onRawDataFileSelectionChanged(final java.util.Collection<? extends RawDataFile> files) {
    if (isSelected()) {
      refresh();
    }
  }
}

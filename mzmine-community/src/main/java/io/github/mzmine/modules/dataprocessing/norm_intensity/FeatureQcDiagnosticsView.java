/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.Comparator;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

/** Read-only native view of the typed diagnostics saved with feature-QC normalization. */
public final class FeatureQcDiagnosticsView {

  static final int ROWS_PER_PAGE = 100;
  private static final int MAX_VISIBLE_FIT_POINTS = 500;

  private FeatureQcDiagnosticsView() {
  }

  public static void show(@NotNull final List<FeatureQcProvenance> provenance) {
    final Stage stage = new Stage();
    stage.setTitle("QC diagnostics");
    final VBox content = new VBox(8d);
    content.setPadding(new Insets(10d));
    for (final FeatureQcProvenance entry : provenance) {
      content.getChildren().add(createDiagnosticsPane(entry));
    }
    stage.setScene(new Scene(new BorderPane(content), 980d, 720d));
    stage.show();
  }

  private static @NotNull VBox createDiagnosticsPane(@NotNull final FeatureQcProvenance provenance) {
    final FeatureQcDiagnostics diagnostics = provenance.diagnostics();
    final Label overview = new Label(("%s | modeled abundance %s (factor applied to both area and height)\n"
        + "Solver: %s | coordinate: %s | scale: %s\nSettings: %s\nAvailability: %s").formatted(
        diagnostics.settings().description(), provenance.modeledAbundance(),
        diagnostics.solver(), diagnostics.coordinatePolicy(), diagnostics.scalePolicy(),
        diagnostics.settings(), diagnostics.availability()));
    overview.setWrapText(true);

    final TextArea detail = new TextArea("Select a feature row to inspect batch coverage and fit diagnostics.");
    detail.setEditable(false);
    detail.setWrapText(false);
    detail.setPrefRowCount(10);

    final List<RowEntry> rows = rows(diagnostics);
    final Pagination pages = new Pagination(Math.max(1, (int) Math.ceil(rows.size() / (double) ROWS_PER_PAGE)));
    pages.setPageFactory(index -> createPage(rows, index, detail));
    return new VBox(8d, overview, pages, detail);
  }

  private static @NotNull TableView<RowEntry> createPage(@NotNull final List<RowEntry> rows,
      final int page, @NotNull final TextArea detail) {
    final TableView<RowEntry> table = new TableView<>();
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    final TableColumn<RowEntry, Integer> rowId = new TableColumn<>("Row ID");
    rowId.setCellValueFactory(new PropertyValueFactory<>("rowId"));
    final TableColumn<RowEntry, String> availability = new TableColumn<>("Availability");
    availability.setCellValueFactory(new PropertyValueFactory<>("availability"));
    final TableColumn<RowEntry, String> usable = new TableColumn<>("Usable / total QC");
    usable.setCellValueFactory(new PropertyValueFactory<>("qcCoverage"));
    final TableColumn<RowEntry, Integer> batchCount = new TableColumn<>("Batches");
    batchCount.setCellValueFactory(new PropertyValueFactory<>("batchCount"));
    table.getColumns().addAll(rowId, availability, usable, batchCount);
    table.setItems(FXCollections.observableArrayList(page(rows, page, ROWS_PER_PAGE)));
    table.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
      if (selected != null) {
        detail.setText(formatDetail(selected.diagnostic()));
      }
    });
    return table;
  }

  static @NotNull List<RowEntry> rows(@NotNull final FeatureQcDiagnostics diagnostics) {
    return diagnostics.rows().values().stream().sorted(Comparator.comparingInt(
        FeatureQcRowDiagnostic::rowId)).map(RowEntry::new).toList();
  }

  static @NotNull <T> List<T> page(@NotNull final List<T> values, final int page,
      final int pageSize) {
    final int from = page * pageSize;
    if (pageSize <= 0 || from < 0 || from >= values.size()) {
      return List.of();
    }
    return values.subList(from, Math.min(values.size(), from + pageSize));
  }

  private static @NotNull String formatDetail(@NotNull final FeatureQcRowDiagnostic row) {
    final StringBuilder text = new StringBuilder("Row ").append(row.rowId())
        .append("; usable QC ").append(row.usableQcs()).append('/').append(row.totalQcs());
    if (row.unavailableReason() != null) {
      text.append("\nGlobal outcome: ").append(row.unavailableReason());
    }
    for (final FeatureQcBatchDiagnostic batch : row.batches().values().stream().sorted(
        Comparator.comparing(FeatureQcBatchDiagnostic::batch)).toList()) {
      text.append("\n\nBatch ").append(batch.batch()).append(": usable QC ")
          .append(batch.usableQcs()).append('/').append(batch.totalQcs())
          .append(", interval ").append(batch.usableQcCoordinateMinimum()).append(" to ")
          .append(batch.usableQcCoordinateMaximum()).append(", boundaries ")
          .append(batch.boundarySampleCount()).append(", factors ").append(batch.factorCount())
          .append("\nRSD before/after: ").append(batch.beforeQcRsd()).append(" / ")
          .append(batch.afterQcRsd()).append(" (n=").append(batch.rsdObservationCount())
          .append(")\nLOO MSE: ").append(batch.leaveOneOutMse()).append(" (").append(batch.validationScale())
          .append("; predictions ")
          .append(batch.leaveOneOutPredictionCount()).append(", failures ")
          .append(batch.leaveOneOutFailureCount()).append(")\nFactor range: ")
          .append(batch.minimumFactor()).append(" to ").append(batch.maximumFactor());
      text.append("\nSelected degree/span/neighbors: ").append(batch.selectedDegree())
          .append(" / ").append(batch.selectedSpan()).append(" / ")
          .append(batch.selectedNeighborhoodSize()).append("; target: ")
          .append(batch.correctionTarget());
      if (batch.unavailableReason() != null) text.append("\nOutcome: ").append(batch.unavailableReason());
      if (batch.validationUnavailableReason() != null) text.append("\nValidation: ")
          .append(batch.validationUnavailableReason());
      if (!batch.fitPoints().isEmpty()) {
        text.append("\nFit points (coordinate, observed QC, fitted QC in the stated validation scale):\n")
            .append(batch.fitPoints().stream().limit(MAX_VISIBLE_FIT_POINTS)
                .map(point -> point.coordinate() + ", " + point.observedQc() + ", "
                    + point.fittedQc()).collect(java.util.stream.Collectors.joining("\n")));
        if (batch.fitPoints().size() > MAX_VISIBLE_FIT_POINTS) {
          text.append("\n… ").append(batch.fitPoints().size() - MAX_VISIBLE_FIT_POINTS)
              .append(" additional fit points are retained in the saved diagnostics.");
        }
      }
    }
    return text.toString();
  }

  public record RowEntry(@NotNull FeatureQcRowDiagnostic diagnostic) {
    public int getRowId() { return diagnostic.rowId(); }
    public String getAvailability() {
      if (!diagnostic.globallyAvailable()) return String.valueOf(diagnostic.unavailableReason());
      final long availableBatches = diagnostic.batches().values().stream()
          .filter(batch -> batch.unavailableReason() == null).count();
      return availableBatches == 0 ? "No supported batch"
          : availableBatches == diagnostic.batches().size() ? "Fully corrected" : "Partially corrected";
    }
    public String getQcCoverage() { return diagnostic.usableQcs() + " / " + diagnostic.totalQcs(); }
    public int getBatchCount() { return diagnostic.batches().size(); }
  }
}

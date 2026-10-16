/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingParameters;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Side panel showing per-isotopologue details (observed Δm/z, theoretical Δm/z, deviation, ppm,
 * and likely annotation) for the currently selected cluster.
 */
public class IsotopologueDetailPanel extends VBox {

  private static final DecimalFormat MZ_FORMAT = new DecimalFormat("0.00000");
  private static final DecimalFormat DIFF_FORMAT = new DecimalFormat("0.000000");
  private static final DecimalFormat PPM_FORMAT = new DecimalFormat("0.0");

  private final TableView<IsotopologueDetailRow> table;
  private final Label titleLabel;

  // Theoretical mass difference per isotopologue step for the target tracer (at charge 1)
  private double isotopeMassDifference = 1.003355; // default to 13C

  public IsotopologueDetailPanel() {
    super(5);
    setPadding(new Insets(5));

    titleLabel = new Label("Isotopologue Details");
    titleLabel.setStyle("-fx-font-weight: bold;");

    table = new TableView<>();
    table.setPlaceholder(new Label("Select a cluster to see isotopologue details"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    VBox.setVgrow(table, Priority.ALWAYS);

    table.getColumns().addAll(
        makeCol("M+n", 45, r -> "M+" + r.rank()),
        makeCol("m/z", 80, r -> MZ_FORMAT.format(r.mz())),
        makeCol("Obs. Δm/z", 80, r -> r.rank() == 0 ? "—" : DIFF_FORMAT.format(r.obsDiff())),
        makeCol("Theor. Δm/z", 80, r -> r.rank() == 0 ? "—" : DIFF_FORMAT.format(r.theorDiff())),
        makeCol("Dev. (Da)", 75, r -> r.rank() == 0 ? "—" : DIFF_FORMAT.format(r.diffToTheor())),
        makeCol("Dev. (ppm)", 75, r -> r.rank() == 0 ? "—" : PPM_FORMAT.format(r.ppmToTheor())),
        makeCol("Annotation", 65, IsotopologueDetailRow::annotation)
    );

    getChildren().addAll(titleLabel, table);
  }

  /** Update the panel with isotopologues from the given cluster rows. */
  public void update(List<FeatureListRow> rows, double tracerMassDifference) {
    if (rows == null || rows.isEmpty()) {
      table.setItems(FXCollections.emptyObservableList());
      titleLabel.setText("Isotopologue Details");
      return;
    }

    this.isotopeMassDifference = tracerMassDifference;

    // Find base peak (M+0)
    FeatureListRow baseRow = IsotopeLabelingModel.findBasePeak(rows);
    double baseMz = baseRow != null ? baseRow.getAverageMZ() : rows.get(0).getAverageMZ();

    // Sort rows by rank
    List<FeatureListRow> sorted = new ArrayList<>(rows);
    sorted.sort((a, b) -> {
      Integer ra = IsotopeLabelingModel.getIsotopologueRank(a);
      Integer rb = IsotopeLabelingModel.getIsotopologueRank(b);
      return Integer.compare(ra != null ? ra : 0, rb != null ? rb : 0);
    });

    List<IsotopologueDetailRow> detailRows = new ArrayList<>();
    for (FeatureListRow row : sorted) {
      Integer rank = IsotopeLabelingModel.getIsotopologueRank(row);
      int r = rank != null ? rank : 0;
      double mz = row.getAverageMZ();
      double obsDiff = mz - baseMz;
      double theorDiff = r * isotopeMassDifference;
      double diffToTheor = obsDiff - theorDiff;
      double ppmToTheor = r == 0 ? 0 : diffToTheor / (baseMz + theorDiff) * 1e6;

      String annotation = "";
      if (row instanceof ModularFeatureListRow modRow) {
        String stored = modRow.get(UntargetedLabelingParameters.isotopologueAnnotationType);
        if (stored != null) {
          annotation = stored;
        }
      }

      detailRows.add(new IsotopologueDetailRow(r, mz, obsDiff, theorDiff, diffToTheor,
          ppmToTheor, annotation));
    }

    table.setItems(FXCollections.observableArrayList(detailRows));
    titleLabel.setText("Isotopologue Details (" + detailRows.size() + " rows)");
  }

  public void clear() {
    table.setItems(FXCollections.emptyObservableList());
    titleLabel.setText("Isotopologue Details");
  }

  @SuppressWarnings("unchecked")
  private static TableColumn<IsotopologueDetailRow, String> makeCol(String header, double maxWidth,
      java.util.function.Function<IsotopologueDetailRow, String> extractor) {
    TableColumn<IsotopologueDetailRow, String> col = new TableColumn<>(header);
    col.setCellValueFactory(c -> new SimpleStringProperty(extractor.apply(c.getValue())));
    col.setMaxWidth(maxWidth);
    return col;
  }

  /** Immutable data class for one row in the detail table. */
  public record IsotopologueDetailRow(int rank, double mz, double obsDiff, double theorDiff,
                                      double diffToTheor, double ppmToTheor, String annotation) {
  }
}

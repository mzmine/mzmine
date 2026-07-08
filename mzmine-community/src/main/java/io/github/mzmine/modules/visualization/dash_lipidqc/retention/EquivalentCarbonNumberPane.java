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

package io.github.mzmine.modules.visualization.dash_lipidqc.retention;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardComputationPane;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardModel;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * Dashboard panel that displays ECN and DBE retention trend plots for the selected lipid
 * annotation and reuses one shared {@link SimpleXYChart} across all retention modes.
 */
public class EquivalentCarbonNumberPane extends DashboardComputationPane {

  private final @NotNull LipidAnnotationQCDashboardModel model;
  private final @NotNull SimpleXYChart<PlotXYDataProvider> sharedChart = new SimpleXYChart<>(
      "Retention time", "Value");
  private final @NotNull RetentionChartController chartController;
  private final StringProperty paneTitle = new SimpleStringProperty("Retention time analysis");
  private final ComboBox<RetentionTrendMode> trendModeCombo = new ComboBox<>(
      FXCollections.observableArrayList(RetentionTrendMode.values()));
  private final CheckBox showAllLipidsOfSelectedClassCheckBox = new CheckBox(
      "Show all lipids of selected class");
  private @NotNull List<FeatureListRow> rowsWithLipidIds = List.of();

  public EquivalentCarbonNumberPane(final @NotNull LipidAnnotationQCDashboardModel model) {
    super("Select a row with lipid annotations.");
    this.model = model;
    chartController = new RetentionChartController(this, model, sharedChart);
    sharedChart.setShowCrosshair(false);

    model.featureListProperty().subscribe(flist -> {
      if (model.isRetentionTimeAnalysisEnabled()) {
        rowsWithLipidIds = flist.getRows().stream()
            .filter(EquivalentCarbonNumberPane::rowHasMatchedLipidSignals)
            .map(r -> (FeatureListRow) r).toList();
        requestUpdate();
      }
    });
    model.rowProperty().subscribe(_ -> {
      if (model.isRetentionTimeAnalysisEnabled()) {
        requestUpdate();
      }
    });
    model.retentionTimeAnalysisEnabledProperty().subscribe(enabled -> {
      if (enabled) {
        rowsWithLipidIds = model.getFeatureList().getRows().stream()
            .filter(EquivalentCarbonNumberPane::rowHasMatchedLipidSignals)
            .map(r -> (FeatureListRow) r).toList();
        requestUpdate();
      }
    });

    trendModeCombo.getSelectionModel().select(RetentionTrendMode.COMBINED_CARBON_DBE_TRENDS);
    trendModeCombo.valueProperty().addListener((_, _, _) -> requestUpdate());

    showAllLipidsOfSelectedClassCheckBox.setSelected(false);
    showAllLipidsOfSelectedClassCheckBox.selectedProperty()
        .addListener((_, _, _) -> requestUpdate());
    showAllLipidsOfSelectedClassCheckBox.managedProperty()
        .bind(showAllLipidsOfSelectedClassCheckBox.visibleProperty());
    showAllLipidsOfSelectedClassCheckBox.visibleProperty().bind(
        trendModeCombo.valueProperty().isNotEqualTo(RetentionTrendMode.COMBINED_CARBON_DBE_TRENDS));

    final HBox trendRow = new HBox(6, FxLabels.newLabel("Trend:"), trendModeCombo,
        showAllLipidsOfSelectedClassCheckBox);
    trendRow.setAlignment(Pos.CENTER_LEFT);
    final VBox controlBox = new VBox(6, trendRow);
    controlBox.setAlignment(Pos.TOP_LEFT);
    final TitledPane controls = new TitledPane("Retention time analysis options", controlBox);
    controls.setCollapsible(true);
    final Accordion accordion = new Accordion(controls);
    accordion.setExpandedPane(null);
    setBottom(accordion);
  }

  public @NotNull StringProperty paneTitleProperty() {
    return paneTitle;
  }

  private static boolean rowHasMatchedLipidSignals(final @NotNull FeatureListRow row) {
    final List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    return matches != null && !matches.isEmpty();
  }

  public void requestUpdate() {
    final List<FeatureListRow> currentRowsWithLipidIds = getCurrentRowsWithLipidIds();
    final RetentionTrendMode mode = trendModeCombo.getValue();
    final boolean showAllLipidsOfSelectedClass = showAllLipidsOfSelectedClassCheckBox.isVisible()
        && showAllLipidsOfSelectedClassCheckBox.isSelected();
    scheduleUpdate(new RetentionComputationTask(this, model.getRow(), currentRowsWithLipidIds, mode,
        showAllLipidsOfSelectedClass));
  }

  void applyComputationResult(final @NotNull RetentionComputationResult result) {
    chartController.apply(result);
  }

  void updatePaneTitle(final @NotNull String title) {
    paneTitle.set(title);
  }

  void showRetentionPlaceholder(final @NotNull String text) {
    showPlaceholder(text);
  }

  void showRetentionChart(final @NotNull Node node) {
    setCenter(node);
  }

  static double clampToUnit(final double value) {
    return Math.max(0d, Math.min(1d, value));
  }

  private @NotNull List<FeatureListRow> getCurrentRowsWithLipidIds() {
    if (rowsWithLipidIds.isEmpty()) {
      return List.of();
    }
    final List<FeatureListRow> current = rowsWithLipidIds.stream()
        .filter(EquivalentCarbonNumberPane::rowHasMatchedLipidSignals).toList();
    rowsWithLipidIds = current;
    return current;
  }
}

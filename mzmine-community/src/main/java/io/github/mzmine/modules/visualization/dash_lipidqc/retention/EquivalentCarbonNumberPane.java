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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardComputationPane;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardModel;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeDetector;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalsePositiveUtils;
import io.github.mzmine.modules.visualization.equivalentcarbonnumberplot.EquivalentCarbonNumberChart;
import io.github.mzmine.modules.visualization.equivalentcarbonnumberplot.EquivalentCarbonNumberDataset;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Dashboard panel that displays ECN (equivalent carbon number) and DBE retention trend plots for
 * the selected lipid annotation, helping to validate whether the annotation follows the expected
 * reversed-phase or HILIC elution pattern.
 */
public class EquivalentCarbonNumberPane extends DashboardComputationPane {

  private static final int FP_OVERLAY_DATASET_INDEX = 5;
  private static final int FN_OVERLAY_DATASET_INDEX = 6;
  private static final int COMBINED_CARBON_FP_DATASET_INDEX = 12;
  private static final int COMBINED_DBE_FP_DATASET_INDEX = 13;
  private static final int COMBINED_CARBON_FN_DATASET_INDEX = 14;
  private static final int COMBINED_DBE_FN_DATASET_INDEX = 15;
  private static final @NotNull Color SELECTED_POINT_COLOR = ConfigService.getDefaultColorPalette()
      .getPositiveColorAWT();
  private final @NotNull LipidAnnotationQCDashboardModel model;
  private final StringProperty paneTitle = new SimpleStringProperty("Retention time analysis");
  private final ComboBox<RetentionTrendMode> trendModeCombo = new ComboBox<>(
      FXCollections.observableArrayList(RetentionTrendMode.values()));
  private final CheckBox showAllLipidsOfSelectedClassCheckBox = new CheckBox(
      "Show all lipids of selected class");
  private List<FeatureListRow> rowsWithLipidIds = List.of();

  public EquivalentCarbonNumberPane(final @NotNull LipidAnnotationQCDashboardModel model) {
    super("Select a row with lipid annotations.");
    this.model = model;
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
    updatePaneTitle("Retention time analysis");
    if (result.placeholderText() != null) {
      showPlaceholder(result.placeholderText());
      return;
    }

    final RetentionComputationPayload payload = result.payload();
    if (payload == null) {
      showPlaceholder("No retention trend data available.");
      return;
    }
    updatePaneTitle("Retention time analysis: " + payload.mode());
    switch (payload) {
      case EcnRetentionPayload ecnPayload -> showEcnTrend(ecnPayload.selectedMatch(),
          ecnPayload.rowsWithLipidIds(), ecnPayload.selectedClass(), ecnPayload.dbe(),
          ecnPayload.matchCount());
      case DbeRetentionPayload dbePayload -> showDbeTrend(dbePayload.selectedMatch(),
          dbePayload.selectedClass(), dbePayload.carbons(), dbePayload.selectedDbe(),
          dbePayload.dataset());
      case CombinedRetentionPayload combinedPayload -> showCombinedTrend(
          combinedPayload.selectedMatch(), combinedPayload.selectedClass(),
          combinedPayload.selectedCarbons(), combinedPayload.selectedDbe(),
          combinedPayload.carbonsDataset(), combinedPayload.dbeDataset());
      case TotalLipidClassRetentionPayload totalClassPayload ->
          showTotalLipidClassTrend(totalClassPayload.selectedMatch(),
              totalClassPayload.rowsWithLipidIds(), totalClassPayload.selectedClass(),
              totalClassPayload.mode());
    }
  }

  private void showEcnTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull List<FeatureListRow> currentRowsWithLipidIds,
      final @NotNull ILipidClass selectedClass, final int dbe, final int matchCount) {
    showPlaceholder("Loading ECN model...");
    final EquivalentCarbonNumberDataset dataset = new EquivalentCarbonNumberDataset(
        currentRowsWithLipidIds, currentRowsWithLipidIds.toArray(new FeatureListRow[0]),
        selectedClass, dbe);
    final int selectedRowId = model.getRow() == null ? -1 : model.getRow().getID();
    final Runnable renderChart = () -> {
      if (trendModeCombo.getValue() != RetentionTrendMode.ECN_CARBON_TREND) {
        return;
      }
      if (selectedRowId >= 0 && (model.getRow() == null || model.getRow().getID() != selectedRowId)) {
        return;
      }
      final EquivalentCarbonNumberChart chart = new EquivalentCarbonNumberChart("",
          "Retention time", "Number of carbons", dataset);
      configureNoCrosshair(chart.getChart().getXYPlot());
      if (chart.getChart().getXYPlot().getRenderer(0) instanceof XYLineAndShapeRenderer renderer) {
        renderer.setSeriesPaint(0, ecnTrendDatasetColor());
        renderer.setDefaultItemLabelPaint(retentionLabelPaint());
      }
      if (chart.getChart().getXYPlot().getRenderer(1) instanceof XYLineAndShapeRenderer renderer) {
        renderer.setSeriesPaint(0, ecnTrendDatasetColor());
      }
      configureEcnAxisRanges(chart.getChart().getXYPlot(), dataset);
      chart.addChartMouseListener(new ChartMouseListenerFX() {
        @Override
        public void chartMouseClicked(final ChartMouseEventFX event) {
          if (event.getTrigger() == null || !event.getTrigger().isStillSincePress()
              || event.getTrigger().getButton() != MouseButton.PRIMARY) {
            return;
          }
          if (!(event.getEntity() instanceof XYItemEntity entity)
              || entity.getDataset() != dataset) {
            return;
          }
          final int item = entity.getItem();
          if (item < 0 || item >= dataset.getItemCount(0)) {
            return;
          }
          final MatchedLipid clickedLipid = dataset.getMatchedLipid(item);
          final FeatureListRow clickedRow = findRowForLipid(clickedLipid);
          if (clickedRow != null) {
            model.setRow(clickedRow);
            FeatureTableFXUtil.selectAndScrollTo(clickedRow, model.getFeatureTableFx());
          }
        }

        @Override
        public void chartMouseMoved(final ChartMouseEventFX event) {
        }
      });
      if (model.getRow() != null) {
        highlightSelectedLipid(chart, model.getRow(), selectedMatch);
      }
      final @Nullable SelectionQualityFlag selectionQualityFlag = model.getRow() == null ? null
          : determineSelectionQualityFlag(model.getRow());
      final int falsePositiveCount = addEcnFalsePositiveOverlay(chart, dataset);
      final int selectedCarbons = LipidQcAnnotationSelectionUtils.extractCarbons(selectedMatch.getLipidAnnotation());
      if (model.getRow() != null
          && selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
        if (selectedCarbons >= 0) {
          addFalseNegativeSelectionOverlay(chart.getChart().getXYPlot(), FN_OVERLAY_DATASET_INDEX,
              0, model.getRow().getAverageRT(), selectedCarbons);
        }
      }
      if (model.getRow() != null && selectedCarbons >= 0) {
        ensurePointVisible(chart.getChart().getXYPlot(), model.getRow().getAverageRT(),
            selectedCarbons, 0);
      }
      chart.setMinSize(250, 120);
      updatePaneTitle(
          "Retention time analysis: " + selectedClass.getAbbr() + " ECN DBE=" + dbe + " (R2 "
              + ConfigService.getConfiguration().getScoreFormat().format(chart.getR2()) + ", n="
              + matchCount + ")" + formatQualityIndicator(falsePositiveCount,
              selectionQualityFlag));
      setCenter(chart);
    };
    dataset.addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.FINISHED) {
        Platform.runLater(renderChart);
      } else if (newStatus == TaskStatus.ERROR || newStatus == TaskStatus.CANCELED) {
        Platform.runLater(
            () -> showPlaceholder("Not enough lipids for ECN model or computation failed."));
      }
    });
    if (dataset.getStatus() == TaskStatus.FINISHED) {
      renderChart.run();
    } else if (dataset.getStatus() == TaskStatus.ERROR
        || dataset.getStatus() == TaskStatus.CANCELED) {
      showPlaceholder("Not enough lipids for ECN model or computation failed.");
    }
  }

  private void showDbeTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass, final int carbons, final int selectedDbe,
      final @NotNull RetentionTrendDataset trendDataset) {
    final TrendChartResult chartResult = createTrendChart(trendDataset, "Number of DBEs",
        dbeTrendDatasetColor(), model.preferredLipidLevelProperty());
    final EChartViewer chart = chartResult.chart();
    configureNoCrosshair(chart.getChart().getXYPlot());
    chart.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(final ChartMouseEventFX event) {
        if (event.getTrigger() == null || !event.getTrigger().isStillSincePress()
            || event.getTrigger().getButton() != MouseButton.PRIMARY) {
          return;
        }
        if (!(event.getEntity() instanceof XYItemEntity entity)
            || !(entity.getDataset() instanceof RetentionTrendDataset clickedDataset)) {
          return;
        }
        final int item = entity.getItem();
        if (item < 0 || item >= clickedDataset.getItemCount(0)) {
          return;
        }
        final MatchedLipid clickedLipid = clickedDataset.getMatchedLipid(item);
        if (clickedLipid == null) {
          return;
        }
        final FeatureListRow clickedRow = findRowForLipid(clickedLipid);
        if (clickedRow != null) {
          model.setRow(clickedRow);
          FeatureTableFXUtil.selectAndScrollTo(clickedRow, model.getFeatureTableFx());
        }
      }

      @Override
      public void chartMouseMoved(final ChartMouseEventFX event) {
      }
    });

    if (model.getRow() != null) {
      highlightSelectedTrendPoint(chart, model.getRow(), selectedDbe);
    }
    final @Nullable SelectionQualityFlag selectionQualityFlag = model.getRow() == null ? null
        : determineSelectionQualityFlag(model.getRow());
    final int falsePositiveCount = addTrendFalsePositiveOverlay(chart.getChart().getXYPlot(),
        trendDataset, FP_OVERLAY_DATASET_INDEX, 0);
    if (model.getRow() != null && selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
      addFalseNegativeSelectionOverlay(chart.getChart().getXYPlot(), FN_OVERLAY_DATASET_INDEX, 0,
          model.getRow().getAverageRT(), selectedDbe);
    }
    if (model.getRow() != null) {
      ensurePointVisible(chart.getChart().getXYPlot(), model.getRow().getAverageRT(), selectedDbe, 0);
    }
    updatePaneTitle(
        "Retention time analysis: " + selectedClass.getAbbr() + " DBE C=" + carbons + " (R2 "
            + ConfigService.getConfiguration().getScoreFormat().format(chartResult.r2()) + ", n="
            + trendDataset.getItemCount(0) + ")" + formatQualityIndicator(falsePositiveCount,
            selectionQualityFlag));
    setCenter(chart);
  }

  private void showCombinedTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull ILipidClass selectedClass, final int selectedCarbons,
      final int selectedDbe, final @Nullable RetentionTrendDataset carbonTrendDataset,
      final @Nullable RetentionTrendDataset dbeTrendDataset) {
    final CombinedTrendChartResult chartResult = createCombinedTrendChart(carbonTrendDataset,
        dbeTrendDataset, model.preferredLipidLevelProperty());
    final EChartViewer chart = chartResult.chart();
    configureNoCrosshair(chart.getChart().getXYPlot());
    chart.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(final ChartMouseEventFX event) {
        if (event.getTrigger() == null || !event.getTrigger().isStillSincePress()
            || event.getTrigger().getButton() != MouseButton.PRIMARY) {
          return;
        }
        if (!(event.getEntity() instanceof XYItemEntity entity)
            || !(entity.getDataset() instanceof RetentionTrendDataset clickedDataset)) {
          return;
        }
        final int item = entity.getItem();
        if (item < 0 || item >= clickedDataset.getItemCount(0)) {
          return;
        }
        final MatchedLipid clickedLipid = clickedDataset.getMatchedLipid(item);
        if (clickedLipid == null) {
          return;
        }
        final FeatureListRow clickedRow = findRowForLipid(clickedLipid);
        if (clickedRow != null) {
          model.setRow(clickedRow);
          FeatureTableFXUtil.selectAndScrollTo(clickedRow, model.getFeatureTableFx());
        }
      }

      @Override
      public void chartMouseMoved(final ChartMouseEventFX event) {
      }
    });

    if (model.getRow() != null) {
      highlightSelectedCombinedTrendPoints(chart, model.getRow(), selectedCarbons, selectedDbe,
          chartResult);
    }
    final @Nullable SelectionQualityFlag selectionQualityFlag = model.getRow() == null ? null
        : determineSelectionQualityFlag(model.getRow());
    int falsePositiveCount = 0;
    if (carbonTrendDataset != null && chartResult.carbonsAxisIndex() >= 0) {
      falsePositiveCount += addTrendFalsePositiveOverlay(chart.getChart().getXYPlot(),
          carbonTrendDataset, COMBINED_CARBON_FP_DATASET_INDEX, chartResult.carbonsAxisIndex());
    }
    if (dbeTrendDataset != null && chartResult.dbeAxisIndex() >= 0) {
      falsePositiveCount += addTrendFalsePositiveOverlay(chart.getChart().getXYPlot(),
          dbeTrendDataset, COMBINED_DBE_FP_DATASET_INDEX, chartResult.dbeAxisIndex());
    }
    if (model.getRow() != null
        && selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE
        && model.getRow().getAverageRT() != null) {
      if (chartResult.carbonsAxisIndex() >= 0) {
        addFalseNegativeSelectionOverlay(chart.getChart().getXYPlot(),
            COMBINED_CARBON_FN_DATASET_INDEX, chartResult.carbonsAxisIndex(),
            model.getRow().getAverageRT(), selectedCarbons);
      }
      if (chartResult.dbeAxisIndex() >= 0) {
        addFalseNegativeSelectionOverlay(chart.getChart().getXYPlot(),
            COMBINED_DBE_FN_DATASET_INDEX, chartResult.dbeAxisIndex(), model.getRow().getAverageRT(),
            selectedDbe);
      }
    }
    if (model.getRow() != null) {
      if (chartResult.carbonsAxisIndex() >= 0) {
        ensurePointVisible(chart.getChart().getXYPlot(), model.getRow().getAverageRT(),
            selectedCarbons, chartResult.carbonsAxisIndex());
      }
      if (chartResult.dbeAxisIndex() >= 0) {
        ensurePointVisible(chart.getChart().getXYPlot(), model.getRow().getAverageRT(), selectedDbe,
            chartResult.dbeAxisIndex());
      }
    }
    updatePaneTitle(
        "Retention time analysis: " + selectedClass.getAbbr() + " " + selectedCarbons + ":"
            + selectedDbe + formatQualityIndicator(falsePositiveCount, selectionQualityFlag));
    setCenter(chart);
  }

  private void showTotalLipidClassTrend(final @NotNull MatchedLipid selectedMatch,
      final @NotNull List<FeatureListRow> currentRowsWithLipidIds,
      final @NotNull ILipidClass selectedClass, final @NotNull RetentionTrendMode mode) {
    final boolean showEcnTrend = mode == RetentionTrendMode.ECN_CARBON_TREND;
    final GroupedRetentionTrendDataset trendDataset = new GroupedRetentionTrendDataset(
        currentRowsWithLipidIds, selectedClass,
        showEcnTrend ? match -> LipidQcAnnotationSelectionUtils.extractDbe(
            match.getLipidAnnotation())
            : match -> LipidQcAnnotationSelectionUtils.extractCarbons(match.getLipidAnnotation()),
        showEcnTrend ? match -> LipidQcAnnotationSelectionUtils.extractCarbons(
            match.getLipidAnnotation())
            : match -> LipidQcAnnotationSelectionUtils.extractDbe(match.getLipidAnnotation()),
        showEcnTrend ? dbe -> "DBE " + dbe : carbons -> "C " + carbons);
    if (trendDataset.getTotalItemCount() == 0) {
      showPlaceholder("No lipid annotations available for the selected lipid class.");
      return;
    }
    final TotalLipidClassTrendChartResult chartResult = createTotalLipidClassTrendChart(
        trendDataset, model.preferredLipidLevelProperty(),
        showEcnTrend ? "Number of carbons" : "Number of DBEs");
    final EChartViewer chart = chartResult.chart();
    configureNoCrosshair(chart.getChart().getXYPlot());
    chart.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(final ChartMouseEventFX event) {
        if (event.getTrigger() == null || !event.getTrigger().isStillSincePress()
            || event.getTrigger().getButton() != MouseButton.PRIMARY) {
          return;
        }
        if (!(event.getEntity() instanceof XYItemEntity entity)
            || !(entity.getDataset() instanceof GroupedRetentionTrendDataset clickedDataset)) {
          return;
        }
        final int series = entity.getSeriesIndex();
        final int item = entity.getItem();
        if (series < 0 || series >= clickedDataset.getSeriesCount() || item < 0
            || item >= clickedDataset.getItemCount(series)) {
          return;
        }
        final @Nullable FeatureListRow clickedRow = clickedDataset.getRow(series, item);
        if (clickedRow != null) {
          model.setRow(clickedRow);
          FeatureTableFXUtil.selectAndScrollTo(clickedRow, model.getFeatureTableFx());
        }
      }

      @Override
      public void chartMouseMoved(final ChartMouseEventFX event) {
      }
    });

    final XYPlot plot = chart.getChart().getXYPlot();
    final @Nullable SelectionQualityFlag selectionQualityFlag =
        model.getRow() == null ? null : determineSelectionQualityFlag(model.getRow());
    final int falsePositiveDatasetIndex = nextDatasetIndex(plot);
    final int falsePositiveCount = addGroupedTrendFalsePositiveOverlay(plot, trendDataset,
        falsePositiveDatasetIndex, 0);
    if (model.getRow() != null) {
      final int selectedYValue = showEcnTrend ? LipidQcAnnotationSelectionUtils.extractCarbons(
          selectedMatch.getLipidAnnotation())
          : LipidQcAnnotationSelectionUtils.extractDbe(selectedMatch.getLipidAnnotation());
      final @Nullable Float selectedRt = model.getRow().getAverageRT();
      if (selectedRt != null && Double.isFinite(selectedYValue)) {
        final int selectedDatasetIndex = nextDatasetIndex(plot);
        addSelectedOverlayPoint(plot, selectedDatasetIndex, 0, selectedRt.doubleValue(),
            selectedYValue, SELECTED_POINT_COLOR, new Ellipse2D.Double(-5d, -5d, 10d, 10d));
      }
      if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
        final int falseNegativeDatasetIndex = nextDatasetIndex(plot);
        addFalseNegativeSelectionOverlay(plot, falseNegativeDatasetIndex, 0, selectedRt,
            selectedYValue);
      }
      ensurePointVisible(plot, selectedRt, selectedYValue, 0);
    }
    updatePaneTitle("Retention time analysis: " + selectedClass.getAbbr() + " " + (showEcnTrend
        ? "ECN carbon trend" : "DBE trend") + " all class lipids (n="
        + chartResult.annotationCount() + ", " + (showEcnTrend ? "DBE lines=" : "carbon lines=")
        + chartResult.trendCount() + ")" + formatQualityIndicator(falsePositiveCount,
        selectionQualityFlag));
    setCenter(chart);
  }


  private static TrendChartResult createTrendChart(final @NotNull RetentionTrendDataset dataset,
      final @NotNull String yAxisLabel, final @NotNull Paint trendPaint,
      final @NotNull ObjectProperty<LipidAnnotationLevel> levelProperty) {
    final JFreeChart chart = ChartFactory.createScatterPlot("", "Retention time", yAxisLabel,
        dataset, PlotOrientation.VERTICAL, false, true, true);
    final EChartViewer viewer = new EChartViewer(chart);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(viewer);

    final XYPlot plot = chart.getXYPlot();
    configureNoCrosshair(plot);
    final XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true);
    pointRenderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
    pointRenderer.setSeriesPaint(0, trendPaint);
    pointRenderer.setDefaultItemLabelGenerator(
        (xyDataset, series, item) -> dataset.getLabel(item, levelProperty.get()));
    pointRenderer.setDefaultItemLabelPaint(retentionLabelPaint());
    pointRenderer.setDefaultItemLabelsVisible(true);
    pointRenderer.setDefaultToolTipGenerator(
        (xyDataset, series, item) -> dataset.getTooltip(item, levelProperty.get()));
    plot.setRenderer(0, pointRenderer);

    final double[] regression = calculateLinearRegression(dataset);
    if (dataset.getItemCount(0) > 1 && Double.isFinite(regression[0]) && Double.isFinite(
        regression[1])) {
      plot.setDataset(1, createRegressionDataset(regression[0], regression[1], dataset));
      final XYLineAndShapeRenderer regressionRenderer = new XYLineAndShapeRenderer(true, false);
      regressionRenderer.setSeriesPaint(0, trendPaint);
      plot.setRenderer(1, regressionRenderer);
    }
    configureTrendAxisRanges(plot, dataset);
    return new TrendChartResult(viewer, regression[2]);
  }

  private static @NotNull TotalLipidClassTrendChartResult createTotalLipidClassTrendChart(
      final @NotNull GroupedRetentionTrendDataset dataset,
      final @NotNull ObjectProperty<LipidAnnotationLevel> levelProperty,
      final @NotNull String yAxisLabel) {
    final JFreeChart chart = ChartFactory.createScatterPlot("", "Retention time", yAxisLabel,
        dataset, PlotOrientation.VERTICAL, true, true, true);
    final EChartViewer viewer = new EChartViewer(chart);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(viewer);

    final XYPlot plot = chart.getXYPlot();
    configureNoCrosshair(plot);
    final int seriesCount = dataset.getSeriesCount();
    final List<Color> trendColors = createDistinctTrendColors(seriesCount);
    final XYLineAndShapeRenderer pointRenderer = createGroupedPointRenderer(dataset, trendColors,
        levelProperty);
    plot.setRenderer(0, pointRenderer);

    int datasetIndex = 1;
    int trendCount = 0;
    final RegressionYBounds regressionBounds = new RegressionYBounds();
    for (int series = 0; series < seriesCount; series++) {
      final double[] regression = calculateLinearRegression(dataset, series);
      if (appendGroupedRegressionDatasetIfValid(plot, datasetIndex, regression, dataset, series, 0,
          trendColors.get(series), groupedRegressionStroke())) {
        trendCount++;
        extendRegressionBounds(regressionBounds, regression, dataset, series);
        datasetIndex++;
      }
    }

    if (trendCount == 0 && chart.getLegend() != null) {
      chart.removeLegend();
    }

    configureTotalLipidClassAxisRanges(plot, dataset, regressionBounds);
    return new TotalLipidClassTrendChartResult(viewer, dataset.getTotalItemCount(), trendCount);
  }

  private static double[] calculateLinearRegression(
      final @NotNull RetentionTrendDataset dataset) {
    final int itemCount = dataset.getItemCount(0);
    if (itemCount < 2) {
      return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }
    double sumX = 0d;
    double sumY = 0d;
    double sumXX = 0d;
    double sumXY = 0d;
    for (int i = 0; i < itemCount; i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      sumX += x;
      sumY += y;
      sumXX += x * x;
      sumXY += x * y;
    }
    final double denominator = itemCount * sumXX - sumX * sumX;
    if (Math.abs(denominator) < 1e-10d) {
      return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }
    final double slope = (itemCount * sumXY - sumX * sumY) / denominator;
    final double intercept = (sumY - slope * sumX) / itemCount;

    final double meanY = sumY / itemCount;
    double ssr = 0d;
    double sse = 0d;
    for (int i = 0; i < itemCount; i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      final double predicted = slope * x + intercept;
      ssr += (predicted - meanY) * (predicted - meanY);
      sse += (y - predicted) * (y - predicted);
    }
    final double r2 = (ssr + sse) > 0d ? ssr / (ssr + sse) : Double.NaN;
    return new double[]{slope, intercept, r2};
  }

  private static double[] calculateLinearRegression(
      final @NotNull GroupedRetentionTrendDataset dataset, final int series) {
    final int itemCount = dataset.getItemCount(series);
    if (itemCount < 2) {
      return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }
    double sumX = 0d;
    double sumY = 0d;
    double sumXX = 0d;
    double sumXY = 0d;
    for (int item = 0; item < itemCount; item++) {
      final double x = dataset.getXValue(series, item);
      final double y = dataset.getYValue(series, item);
      sumX += x;
      sumY += y;
      sumXX += x * x;
      sumXY += x * y;
    }
    final double denominator = itemCount * sumXX - sumX * sumX;
    if (Math.abs(denominator) < 1e-10d) {
      return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }
    final double slope = (itemCount * sumXY - sumX * sumY) / denominator;
    final double intercept = (sumY - slope * sumX) / itemCount;

    final double meanY = sumY / itemCount;
    double ssr = 0d;
    double sse = 0d;
    for (int item = 0; item < itemCount; item++) {
      final double x = dataset.getXValue(series, item);
      final double y = dataset.getYValue(series, item);
      final double predicted = slope * x + intercept;
      ssr += (predicted - meanY) * (predicted - meanY);
      sse += (y - predicted) * (y - predicted);
    }
    final double r2 = (ssr + sse) > 0d ? ssr / (ssr + sse) : Double.NaN;
    return new double[]{slope, intercept, r2};
  }

  private static @NotNull XYSeriesCollection createRegressionDataset(final double slope,
      final double intercept, final @NotNull RetentionTrendDataset dataset) {
    final double minX = Arrays.stream(dataset.getXValues()).min().orElse(0d);
    final double maxX = Arrays.stream(dataset.getXValues()).max().orElse(minX);
    final XYSeries series = new XYSeries("Regression");
    series.add(minX, slope * minX + intercept);
    series.add(maxX, slope * maxX + intercept);
    final XYSeriesCollection regressionDataset = new XYSeriesCollection();
    regressionDataset.addSeries(series);
    return regressionDataset;
  }

  private static @NotNull XYSeriesCollection createRegressionDataset(final double slope,
      final double intercept, final @NotNull GroupedRetentionTrendDataset dataset,
      final int series) {
    final double minX = minDatasetX(dataset, series);
    final double maxX = maxDatasetX(dataset, series);
    final XYSeries regressionSeries = new XYSeries(dataset.getSeriesKey(series));
    regressionSeries.add(minX, slope * minX + intercept);
    regressionSeries.add(maxX, slope * maxX + intercept);
    final XYSeriesCollection regressionDataset = new XYSeriesCollection();
    regressionDataset.addSeries(regressionSeries);
    return regressionDataset;
  }

  private static @NotNull CombinedTrendChartResult createCombinedTrendChart(
      final @Nullable RetentionTrendDataset carbonDataset,
      final @Nullable RetentionTrendDataset dbeDataset,
      final @NotNull ObjectProperty<LipidAnnotationLevel> levelProperty) {
    final XYPlot plot = new XYPlot();
    configureNoCrosshair(plot);
    final NumberAxis domainAxis = new NumberAxis("Retention time");
    plot.setDomainAxis(domainAxis);

    int carbonAxisIndex = -1;
    int dbeAxisIndex = -1;
    double carbonR2 = Double.NaN;
    double dbeR2 = Double.NaN;
    final boolean hasCarbon = carbonDataset != null;
    final boolean hasDbe = dbeDataset != null;
    final int carbonDatasetIndex = hasCarbon ? 0 : -1;
    final int dbeDatasetIndex = hasDbe ? (hasCarbon ? 1 : 0) : -1;
    final int carbonRegressionIndex = hasCarbon ? (hasDbe ? 2 : 1) : -1;
    final int dbeRegressionIndex = hasDbe ? (hasCarbon ? 3 : 1) : -1;
    if (carbonDataset != null) {
      carbonAxisIndex = 0;
      final NumberAxis carbonAxis = new NumberAxis("Number of carbons");
      plot.setRangeAxis(carbonAxisIndex, carbonAxis);
      plot.setDataset(carbonDatasetIndex, carbonDataset);
      plot.mapDatasetToRangeAxis(carbonDatasetIndex, carbonAxisIndex);
      plot.setRenderer(carbonDatasetIndex, createTrendPointRenderer(carbonDataset,
          ecnTrendDatasetColor(), new Ellipse2D.Double(-3.2d, -3.2d, 6.4d, 6.4d), levelProperty));
      final double[] regression = calculateLinearRegression(carbonDataset);
      carbonR2 = regression[2];
      appendRegressionDatasetIfValid(plot, carbonRegressionIndex, regression, carbonDataset,
          carbonAxisIndex, ecnTrendDatasetColor());
    }
    if (dbeDataset != null) {
      dbeAxisIndex = carbonDataset != null ? 1 : 0;
      final NumberAxis dbeAxis = new NumberAxis("Number of DBEs");
      plot.setRangeAxis(dbeAxisIndex, dbeAxis);
      plot.setDataset(dbeDatasetIndex, dbeDataset);
      plot.mapDatasetToRangeAxis(dbeDatasetIndex, dbeAxisIndex);
      plot.setRenderer(dbeDatasetIndex, createTrendPointRenderer(dbeDataset,
          dbeTrendDatasetColor(), new Rectangle2D.Double(-3d, -3d, 6d, 6d), levelProperty));
      final double[] regression = calculateLinearRegression(dbeDataset);
      dbeR2 = regression[2];
      appendRegressionDatasetIfValid(plot, dbeRegressionIndex, regression, dbeDataset,
          dbeAxisIndex, dbeTrendDatasetColor());
    }

    final JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    chart.setBackgroundPaint(new Color(0, 0, 0, 0));
    final EChartViewer viewer = new EChartViewer(chart);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(viewer);
    enforceCombinedDatasetColors(plot, carbonDatasetIndex, dbeDatasetIndex, carbonRegressionIndex,
        dbeRegressionIndex);
    configureNoCrosshair(plot);
    configureCombinedAxisRanges(plot, carbonDataset, dbeDataset, carbonAxisIndex, dbeAxisIndex);
    return new CombinedTrendChartResult(viewer, carbonR2, dbeR2, carbonAxisIndex, dbeAxisIndex);
  }

  private static void enforceCombinedDatasetColors(final @NotNull XYPlot plot,
      final int carbonDatasetIndex, final int dbeDatasetIndex, final int carbonRegressionIndex,
      final int dbeRegressionIndex) {
    if (carbonDatasetIndex >= 0
        && plot.getRenderer(carbonDatasetIndex) instanceof XYLineAndShapeRenderer renderer) {
      renderer.setSeriesPaint(0, ecnTrendDatasetColor());
      renderer.setDefaultItemLabelPaint(retentionLabelPaint());
    }
    if (dbeDatasetIndex >= 0
        && plot.getRenderer(dbeDatasetIndex) instanceof XYLineAndShapeRenderer renderer) {
      renderer.setSeriesPaint(0, dbeTrendDatasetColor());
      renderer.setDefaultItemLabelPaint(retentionLabelPaint());
    }
    if (carbonRegressionIndex >= 0
        && plot.getRenderer(carbonRegressionIndex) instanceof XYLineAndShapeRenderer renderer) {
      renderer.setSeriesPaint(0, ecnTrendDatasetColor());
    }
    if (dbeRegressionIndex >= 0
        && plot.getRenderer(dbeRegressionIndex) instanceof XYLineAndShapeRenderer renderer) {
      renderer.setSeriesPaint(0, dbeTrendDatasetColor());
    }
  }

  private static void configureCombinedAxisRanges(final @NotNull XYPlot plot,
      final @Nullable RetentionTrendDataset carbonDataset,
      final @Nullable RetentionTrendDataset dbeDataset, final int carbonAxisIndex,
      final int dbeAxisIndex) {
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      double minX = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      if (carbonDataset != null) {
        for (int i = 0; i < carbonDataset.getItemCount(0); i++) {
          final double value = carbonDataset.getXValue(0, i);
          if (Double.isFinite(value)) {
            minX = Math.min(minX, value);
            maxX = Math.max(maxX, value);
          }
        }
      }
      if (dbeDataset != null) {
        for (int i = 0; i < dbeDataset.getItemCount(0); i++) {
          final double value = dbeDataset.getXValue(0, i);
          if (Double.isFinite(value)) {
            minX = Math.min(minX, value);
            maxX = Math.max(maxX, value);
          }
        }
      }
      setAxisRangeToData(domainAxis, minX, maxX, true);
    }

    if (carbonAxisIndex >= 0 && carbonDataset != null && plot.getRangeAxis(
        carbonAxisIndex) instanceof NumberAxis carbonAxis) {
      final @NotNull TrendYBounds carbonBounds = calculateTrendYBounds(carbonDataset);
      setAxisRangeToData(carbonAxis, carbonBounds.minY(), carbonBounds.maxY(), false);
    }
    if (dbeAxisIndex >= 0 && dbeDataset != null && plot.getRangeAxis(
        dbeAxisIndex) instanceof NumberAxis dbeAxis) {
      final @NotNull TrendYBounds dbeBounds = calculateTrendYBounds(dbeDataset);
      setAxisRangeToData(dbeAxis, dbeBounds.minY(), dbeBounds.maxY(), false);
    }
  }

  private static void configureTotalLipidClassAxisRanges(final @NotNull XYPlot plot,
      final @NotNull GroupedRetentionTrendDataset dataset,
      final @NotNull RegressionYBounds regressionBounds) {
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      setAxisRangeToData(domainAxis, minDatasetX(dataset), maxDatasetX(dataset), true);
    }
    if (plot.getRangeAxis() instanceof NumberAxis rangeAxis) {
      double minY = minDatasetY(dataset);
      double maxY = maxDatasetY(dataset);
      if (Double.isFinite(regressionBounds.minY)) {
        minY =
            Double.isFinite(minY) ? Math.min(minY, regressionBounds.minY) : regressionBounds.minY;
      }
      if (Double.isFinite(regressionBounds.maxY)) {
        maxY =
            Double.isFinite(maxY) ? Math.max(maxY, regressionBounds.maxY) : regressionBounds.maxY;
      }
      setAxisRangeToData(rangeAxis, minY, maxY, false);
    }
  }

  private static double minDatasetY(final @NotNull RetentionTrendDataset dataset) {
    double min = Double.POSITIVE_INFINITY;
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final double value = dataset.getYValue(0, i);
      if (Double.isFinite(value)) {
        min = Math.min(min, value);
      }
    }
    return min;
  }

  private static double minDatasetY(final @NotNull GroupedRetentionTrendDataset dataset) {
    double min = Double.POSITIVE_INFINITY;
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int item = 0; item < dataset.getItemCount(series); item++) {
        final double value = dataset.getYValue(series, item);
        if (Double.isFinite(value)) {
          min = Math.min(min, value);
        }
      }
    }
    return min;
  }

  private static double maxDatasetY(final @NotNull RetentionTrendDataset dataset) {
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final double value = dataset.getYValue(0, i);
      if (Double.isFinite(value)) {
        max = Math.max(max, value);
      }
    }
    return max;
  }

  private static double maxDatasetY(final @NotNull GroupedRetentionTrendDataset dataset) {
    double max = Double.NEGATIVE_INFINITY;
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int item = 0; item < dataset.getItemCount(series); item++) {
        final double value = dataset.getYValue(series, item);
        if (Double.isFinite(value)) {
          max = Math.max(max, value);
        }
      }
    }
    return max;
  }

  private static double minDatasetX(final @NotNull GroupedRetentionTrendDataset dataset) {
    double min = Double.POSITIVE_INFINITY;
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      min = Math.min(min, minDatasetX(dataset, series));
    }
    return min;
  }

  private static double maxDatasetX(final @NotNull GroupedRetentionTrendDataset dataset) {
    double max = Double.NEGATIVE_INFINITY;
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      max = Math.max(max, maxDatasetX(dataset, series));
    }
    return max;
  }

  private static double minDatasetX(final @NotNull GroupedRetentionTrendDataset dataset,
      final int series) {
    double min = Double.POSITIVE_INFINITY;
    for (int item = 0; item < dataset.getItemCount(series); item++) {
      final double value = dataset.getXValue(series, item);
      if (Double.isFinite(value)) {
        min = Math.min(min, value);
      }
    }
    return min;
  }

  private static double maxDatasetX(final @NotNull GroupedRetentionTrendDataset dataset,
      final int series) {
    double max = Double.NEGATIVE_INFINITY;
    for (int item = 0; item < dataset.getItemCount(series); item++) {
      final double value = dataset.getXValue(series, item);
      if (Double.isFinite(value)) {
        max = Math.max(max, value);
      }
    }
    return max;
  }

  private static void setAxisRangeToData(final @NotNull NumberAxis axis, final double min,
      final double max, final boolean lockLowerToFirstPoint) {
    if (!Double.isFinite(min) || !Double.isFinite(max)) {
      return;
    }
    axis.setAutoRange(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);

    if (max <= min) {
      final double delta = Math.max(Math.abs(min) * 0.05d, 0.2d);
      final double lower = lockLowerToFirstPoint ? min - delta * 0.5d : min - delta;
      axis.setRange(lower, min + delta * 1.6d);
      return;
    }

    final double span = max - min;
    final double lowerPadding = lockLowerToFirstPoint ? span * 0.16d : span * 0.18d;
    final double upperPadding = span * 0.30d;
    axis.setRange(min - lowerPadding, max + upperPadding);
  }

  private static void configureEcnAxisRanges(final @NotNull XYPlot plot,
      final @NotNull EquivalentCarbonNumberDataset dataset) {
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final double x = dataset.getXValue(0, i);
      final double y = dataset.getYValue(0, i);
      if (Double.isFinite(x)) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
      }
      if (Double.isFinite(y)) {
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      }
    }
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      setAxisRangeToData(domainAxis, minX, maxX, true);
    }
    if (plot.getRangeAxis() instanceof NumberAxis rangeAxis) {
      setAxisRangeToData(rangeAxis, minY, maxY, false);
    }
  }

  private static void configureTrendAxisRanges(final @NotNull XYPlot plot,
      final @NotNull RetentionTrendDataset dataset) {
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      final double minX = Arrays.stream(dataset.getXValues()).min()
          .orElse(Double.NaN);
      final double maxX = Arrays.stream(dataset.getXValues()).max()
          .orElse(Double.NaN);
      setAxisRangeToData(domainAxis, minX, maxX, true);
    }
    if (plot.getRangeAxis() instanceof NumberAxis rangeAxis) {
      final @NotNull TrendYBounds bounds = calculateTrendYBounds(dataset);
      setAxisRangeToData(rangeAxis, bounds.minY(), bounds.maxY(), false);
    }
  }

  private static void ensurePointVisible(final @NotNull XYPlot plot, final @Nullable Float xValue,
      final double yValue, final int rangeAxisIndex) {
    if (xValue == null || !Double.isFinite(yValue)) {
      return;
    }
    if (plot.getDomainAxis() instanceof NumberAxis domainAxis) {
      extendAxisToIncludeValue(domainAxis, xValue.doubleValue(), true);
    }
    if (plot.getRangeAxis(rangeAxisIndex) instanceof NumberAxis rangeAxis) {
      extendAxisToIncludeValue(rangeAxis, yValue, false);
    }
  }

  private static void extendAxisToIncludeValue(final @NotNull NumberAxis axis, final double value,
      final boolean lockLowerToFirstPoint) {
    if (!Double.isFinite(value)) {
      return;
    }
    final double lower = axis.getLowerBound();
    final double upper = axis.getUpperBound();
    if (!Double.isFinite(lower) || !Double.isFinite(upper)) {
      setAxisRangeToData(axis, value, value, lockLowerToFirstPoint);
      return;
    }
    if (value >= lower && value <= upper) {
      return;
    }
    setAxisRangeToData(axis, Math.min(lower, value), Math.max(upper, value),
        lockLowerToFirstPoint);
  }

  private static @NotNull TrendYBounds calculateTrendYBounds(
      final @NotNull RetentionTrendDataset dataset) {
    double minY = minDatasetY(dataset);
    double maxY = maxDatasetY(dataset);
    if (dataset.getItemCount(0) < 2) {
      return new TrendYBounds(minY, maxY);
    }
    final double[] regression = calculateLinearRegression(dataset);
    if (!Double.isFinite(regression[0]) || !Double.isFinite(regression[1])) {
      return new TrendYBounds(minY, maxY);
    }
    final double minX = Arrays.stream(dataset.getXValues()).min().orElse(Double.NaN);
    final double maxX = Arrays.stream(dataset.getXValues()).max().orElse(Double.NaN);
    if (!Double.isFinite(minX) || !Double.isFinite(maxX)) {
      return new TrendYBounds(minY, maxY);
    }
    final double lowerFitY = regression[0] * minX + regression[1];
    final double upperFitY = regression[0] * maxX + regression[1];
    if (Double.isFinite(lowerFitY)) {
      minY = Math.min(minY, lowerFitY);
      maxY = Math.max(maxY, lowerFitY);
    }
    if (Double.isFinite(upperFitY)) {
      minY = Math.min(minY, upperFitY);
      maxY = Math.max(maxY, upperFitY);
    }
    return new TrendYBounds(minY, maxY);
  }

  private static void appendRegressionDatasetIfValid(final @NotNull XYPlot plot,
      final int datasetIndex, final @NotNull double[] regression,
      final @NotNull RetentionTrendDataset dataset, final int rangeAxisIndex,
      final @NotNull Paint linePaint) {
    if (datasetIndex < 0 || dataset.getItemCount(0) < 2 || !Double.isFinite(regression[0])
        || !Double.isFinite(regression[1])) {
      return;
    }
    plot.setDataset(datasetIndex, createRegressionDataset(regression[0], regression[1], dataset));
    plot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
    final XYLineAndShapeRenderer regressionRenderer = new XYLineAndShapeRenderer(true, false);
    regressionRenderer.setSeriesPaint(0, linePaint);
    regressionRenderer.setSeriesStroke(0, new BasicStroke(1.6f));
    regressionRenderer.setSeriesVisibleInLegend(0, false);
    plot.setRenderer(datasetIndex, regressionRenderer);
  }

  private static boolean appendGroupedRegressionDatasetIfValid(final @NotNull XYPlot plot,
      final int datasetIndex, final @NotNull double[] regression,
      final @NotNull GroupedRetentionTrendDataset dataset, final int series,
      final int rangeAxisIndex, final @NotNull Paint linePaint, final @NotNull BasicStroke stroke) {
    if (datasetIndex < 0 || dataset.getItemCount(series) < 2 || !Double.isFinite(regression[0])
        || !Double.isFinite(regression[1])) {
      return false;
    }
    plot.setDataset(datasetIndex,
        createRegressionDataset(regression[0], regression[1], dataset, series));
    plot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
    final XYLineAndShapeRenderer regressionRenderer = new XYLineAndShapeRenderer(true, false);
    regressionRenderer.setSeriesPaint(0, linePaint);
    regressionRenderer.setSeriesStroke(0, stroke);
    plot.setRenderer(datasetIndex, regressionRenderer);
    return true;
  }

  private static @NotNull XYLineAndShapeRenderer createTrendPointRenderer(
      final @NotNull RetentionTrendDataset dataset, final @NotNull Paint seriesPaint,
      final @NotNull java.awt.Shape seriesShape,
      final @NotNull ObjectProperty<LipidAnnotationLevel> levelProperty) {
    final XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true);
    pointRenderer.setSeriesShape(0, seriesShape);
    pointRenderer.setSeriesPaint(0, seriesPaint);
    pointRenderer.setDefaultItemLabelGenerator(
        (xyDataset, series, item) -> dataset.getLabel(item, levelProperty.get()));
    pointRenderer.setDefaultItemLabelPaint(retentionLabelPaint());
    pointRenderer.setDefaultItemLabelsVisible(true);
    pointRenderer.setDefaultToolTipGenerator(
        (xyDataset, series, item) -> dataset.getTooltip(item, levelProperty.get()));
    pointRenderer.setSeriesVisibleInLegend(0, false);
    return pointRenderer;
  }

  private static @NotNull XYLineAndShapeRenderer createGroupedPointRenderer(
      final @NotNull GroupedRetentionTrendDataset dataset, final @NotNull List<Color> seriesColors,
      final @NotNull ObjectProperty<LipidAnnotationLevel> levelProperty) {
    final XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true);
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      pointRenderer.setSeriesShape(series, new Ellipse2D.Double(-3d, -3d, 6d, 6d));
      pointRenderer.setSeriesPaint(series, seriesColors.get(series));
      pointRenderer.setSeriesVisibleInLegend(series, false);
    }
    pointRenderer.setDefaultItemLabelGenerator(
        (xyDataset, series, item) -> dataset.getLabel(series, item, levelProperty.get()));
    pointRenderer.setDefaultItemLabelPaint(retentionLabelPaint());
    pointRenderer.setDefaultItemLabelsVisible(true);
    pointRenderer.setDefaultToolTipGenerator(
        (xyDataset, series, item) -> dataset.getTooltip(series, item, levelProperty.get()));
    return pointRenderer;
  }

  private int addEcnFalsePositiveOverlay(final @NotNull EquivalentCarbonNumberChart chart,
      final @NotNull EquivalentCarbonNumberDataset dataset) {
    final XYSeries falsePositiveSeries = new XYSeries("Potential false positives");
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final @Nullable MatchedLipid matchedLipid = dataset.getMatchedLipid(i);
      if (matchedLipid == null) {
        continue;
      }
      final @Nullable FeatureListRow row = findRowForLipid(matchedLipid);
      if (row == null || !isPotentialFalsePositive(row)) {
        continue;
      }
      falsePositiveSeries.add(dataset.getXValue(0, i), dataset.getYValue(0, i));
    }
    if (falsePositiveSeries.isEmpty()) {
      chart.getChart().getXYPlot().setDataset(FP_OVERLAY_DATASET_INDEX, null);
      chart.getChart().getXYPlot().setRenderer(FP_OVERLAY_DATASET_INDEX, null);
      return 0;
    }
    final XYSeriesCollection overlayDataset = new XYSeriesCollection();
    overlayDataset.addSeries(falsePositiveSeries);
    final XYLineAndShapeRenderer overlayRenderer = createOutlinedOverlayRenderer(
        falsePositiveColor(), new Ellipse2D.Double(-6d, -6d, 12d, 12d), null);
    chart.getChart().getXYPlot().setDataset(FP_OVERLAY_DATASET_INDEX, overlayDataset);
    chart.getChart().getXYPlot().setRenderer(FP_OVERLAY_DATASET_INDEX, overlayRenderer);
    return falsePositiveSeries.getItemCount();
  }

  private int addTrendFalsePositiveOverlay(final @NotNull XYPlot plot,
      final @NotNull RetentionTrendDataset dataset, final int datasetIndex,
      final int rangeAxisIndex) {
    final XYSeries falsePositiveSeries = new XYSeries("Potential false positives");
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      final @Nullable FeatureListRow row = dataset.getRow(i);
      if (row == null || !isPotentialFalsePositive(row)) {
        continue;
      }
      falsePositiveSeries.add(dataset.getXValue(0, i), dataset.getYValue(0, i));
    }
    if (falsePositiveSeries.isEmpty()) {
      plot.setDataset(datasetIndex, null);
      plot.setRenderer(datasetIndex, null);
      return 0;
    }
    final XYSeriesCollection overlayDataset = new XYSeriesCollection();
    overlayDataset.addSeries(falsePositiveSeries);
    plot.setDataset(datasetIndex, overlayDataset);
    plot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
    plot.setRenderer(datasetIndex, createOutlinedOverlayRenderer(falsePositiveColor(),
        new Ellipse2D.Double(-6d, -6d, 12d, 12d), null));
    return falsePositiveSeries.getItemCount();
  }

  private int addGroupedTrendFalsePositiveOverlay(final @NotNull XYPlot plot,
      final @NotNull GroupedRetentionTrendDataset dataset, final int datasetIndex,
      final int rangeAxisIndex) {
    final XYSeries falsePositiveSeries = new XYSeries("Potential false positives");
    final Set<String> addedCoordinates = new HashSet<>();
    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      for (int item = 0; item < dataset.getItemCount(series); item++) {
        final @Nullable FeatureListRow row = dataset.getRow(series, item);
        if (row == null || !isPotentialFalsePositive(row)) {
          continue;
        }
        final double xValue = dataset.getXValue(series, item);
        final double yValue = dataset.getYValue(series, item);
        final String coordinateKey = xValue + ":" + yValue;
        if (!addedCoordinates.add(coordinateKey)) {
          continue;
        }
        falsePositiveSeries.add(xValue, yValue);
      }
    }
    if (falsePositiveSeries.isEmpty()) {
      plot.setDataset(datasetIndex, null);
      plot.setRenderer(datasetIndex, null);
      return 0;
    }
    final XYSeriesCollection overlayDataset = new XYSeriesCollection();
    overlayDataset.addSeries(falsePositiveSeries);
    plot.setDataset(datasetIndex, overlayDataset);
    plot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
    plot.setRenderer(datasetIndex, createOutlinedOverlayRenderer(falsePositiveColor(),
        new Ellipse2D.Double(-6d, -6d, 12d, 12d), null));
    return falsePositiveSeries.getItemCount();
  }

  private static void addFalseNegativeSelectionOverlay(final @NotNull XYPlot plot,
      final int datasetIndex, final int rangeAxisIndex, final @Nullable Float xValue,
      final double yValue) {
    if (xValue == null || !Double.isFinite(yValue)) {
      plot.setDataset(datasetIndex, null);
      plot.setRenderer(datasetIndex, null);
      return;
    }
    final XYSeries falseNegativeSeries = new XYSeries("Potential false negative");
    falseNegativeSeries.add((double) xValue, yValue);
    final XYSeriesCollection overlayDataset = new XYSeriesCollection();
    overlayDataset.addSeries(falseNegativeSeries);
    plot.setDataset(datasetIndex, overlayDataset);
    plot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
    plot.setRenderer(datasetIndex, createOutlinedOverlayRenderer(falseNegativeColor(),
        falseNegativeMarkerShape(), "FN"));
  }

  private static @NotNull XYLineAndShapeRenderer createOutlinedOverlayRenderer(
      final @NotNull Color color, final @NotNull java.awt.Shape shape,
      final @Nullable String label) {
    final XYLineAndShapeRenderer overlayRenderer = new XYLineAndShapeRenderer(false, true);
    overlayRenderer.setSeriesPaint(0, color);
    overlayRenderer.setSeriesStroke(0, new BasicStroke(2.2f));
    overlayRenderer.setSeriesShape(0, shape);
    overlayRenderer.setDefaultShapesFilled(false);
    overlayRenderer.setUseOutlinePaint(true);
    overlayRenderer.setSeriesOutlinePaint(0, color);
    overlayRenderer.setSeriesOutlineStroke(0, new BasicStroke(2.2f));
    if (label != null) {
      overlayRenderer.setDefaultItemLabelGenerator((xyDataset, series, item) -> label);
      overlayRenderer.setDefaultItemLabelsVisible(true);
      overlayRenderer.setDefaultItemLabelPaint(color);
      overlayRenderer.setDefaultPositiveItemLabelPosition(
          new org.jfree.chart.labels.ItemLabelPosition(org.jfree.chart.labels.ItemLabelAnchor.OUTSIDE12,
              TextAnchor.BOTTOM_CENTER));
    }
    overlayRenderer.setSeriesVisibleInLegend(0, false);
    return overlayRenderer;
  }

  private static @NotNull java.awt.Shape falseNegativeMarkerShape() {
    final Path2D.Double diamond = new Path2D.Double();
    diamond.moveTo(0d, -6.5d);
    diamond.lineTo(6.5d, 0d);
    diamond.lineTo(0d, 6.5d);
    diamond.lineTo(-6.5d, 0d);
    diamond.closePath();
    return diamond;
  }

  private @NotNull SelectionQualityFlag determineSelectionQualityFlag(
      final @Nullable FeatureListRow row) {
    if(row == null) {
      return SelectionQualityFlag.NONE;
    }
    if (!(row.getFeatureList() instanceof ModularFeatureList modularFeatureList)) {
      return SelectionQualityFlag.NONE;
    }
    if (row.getLipidMatches().isEmpty()) {
      final @Nullable KendrickFalseNegativeCandidate candidate =
          new KendrickFalseNegativeDetector(modularFeatureList).detectCandidate(row);
      return candidate != null ? SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE
          : SelectionQualityFlag.NONE;
    }
    final @Nullable String falsePositiveReason =
        KendrickFalsePositiveUtils.potentialFalsePositiveReason(modularFeatureList, row, true);
    return falsePositiveReason != null ? SelectionQualityFlag.POTENTIAL_FALSE_POSITIVE
        : SelectionQualityFlag.NONE;
  }

  private static boolean isPotentialFalsePositive(final @NotNull FeatureListRow row) {
    if (!(row.getFeatureList() instanceof ModularFeatureList modularFeatureList)) {
      return false;
    }
    return KendrickFalsePositiveUtils.potentialFalsePositiveReason(modularFeatureList, row,
        true) != null;
  }

  private static @NotNull String formatQualityIndicator(final int falsePositiveCount,
      final @Nullable SelectionQualityFlag selectionQualityFlag) {
    final StringBuilder suffixBuilder = new StringBuilder();
    if (falsePositiveCount > 0) {
      suffixBuilder.append(" | FP outlined: ").append(falsePositiveCount);
    }
    if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_NEGATIVE) {
      suffixBuilder.append(" | FN outlined");
    } else if (selectionQualityFlag == SelectionQualityFlag.POTENTIAL_FALSE_POSITIVE
        && falsePositiveCount == 0) {
      suffixBuilder.append(" | FP outlined");
    }
    return suffixBuilder.toString();
  }

  private void updatePaneTitle(final @NotNull String title) {
    paneTitle.set(title);
  }

  static double clampToUnit(final double value) {
    return Math.max(0d, Math.min(1d, value));
  }

  private @Nullable FeatureListRow findRowForLipid(@NotNull MatchedLipid clickedLipid) {
    for (final FeatureListRow candidate : getCurrentRowsWithLipidIds()) {
      if (candidate.getLipidMatches().stream().anyMatch(clickedLipid::equals)) {
        return candidate;
      }
    }
    return null;
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

  private static void highlightSelectedLipid(EquivalentCarbonNumberChart chart,
      final @Nullable FeatureListRow row, final @NotNull MatchedLipid selectedMatch) {
    final int carbons = LipidQcAnnotationSelectionUtils.extractCarbons(selectedMatch.getLipidAnnotation());
    if (row == null || carbons < 0 || row.getAverageRT() == null) {
      return;
    }
    addSelectedOverlayPoint(chart.getChart().getXYPlot(), 2, 0, row.getAverageRT(), carbons,
        SELECTED_POINT_COLOR, new Ellipse2D.Double(-5d, -5d, 10d, 10d));
  }

  private static void highlightSelectedTrendPoint(final @NotNull EChartViewer chart,
      final @Nullable FeatureListRow row, final double yValue) {
    if (row == null || row.getAverageRT() == null || !Double.isFinite(yValue)) {
      return;
    }
    addSelectedOverlayPoint(chart.getChart().getXYPlot(), 2, 0, row.getAverageRT(), yValue,
        SELECTED_POINT_COLOR, new Ellipse2D.Double(-5d, -5d, 10d, 10d));
  }

  private static void highlightSelectedCombinedTrendPoints(final @NotNull EChartViewer chart,
      final @NotNull FeatureListRow row, final double carbonValue, final double dbeValue,
      final @NotNull CombinedTrendChartResult chartResult) {
    if (row.getAverageRT() == null) {
      return;
    }
    final XYPlot plot = chart.getChart().getXYPlot();
    plot.clearDomainMarkers();
    plot.setDataset(10, null);
    plot.setRenderer(10, null);
    plot.setDataset(11, null);
    plot.setRenderer(11, null);

    final double rt = row.getAverageRT();
    final Paint selectedColor = SELECTED_POINT_COLOR;
    final boolean hasCarbonPoint =
        chartResult.carbonsAxisIndex() >= 0 && Double.isFinite(carbonValue);
    final boolean hasDbePoint = chartResult.dbeAxisIndex() >= 0 && Double.isFinite(dbeValue);
    if (!hasCarbonPoint || !hasDbePoint) {
      final ValueMarker selectionMarker = new ValueMarker(rt);
      selectionMarker.setPaint(selectedColor);
      selectionMarker.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_BEVEL, 0f, new float[]{6f, 4f}, 0f));
      selectionMarker.setAlpha(0.75f);
      plot.addDomainMarker(selectionMarker);
    } else {
      synchronizeSelectedPointAcrossAxes(plot, chartResult.carbonsAxisIndex(),
          chartResult.dbeAxisIndex(), carbonValue, dbeValue);
    }

    if (hasCarbonPoint) {
      addSelectedOverlayPoint(plot, 10, chartResult.carbonsAxisIndex(), rt, carbonValue,
          selectedColor, new Ellipse2D.Double(-5d, -5d, 10d, 10d));
    }
    if (hasDbePoint) {
      addSelectedOverlayPoint(plot, 11, chartResult.dbeAxisIndex(), rt, dbeValue, selectedColor,
          new Ellipse2D.Double(-5d, -5d, 10d, 10d));
    }
  }

  private static void addSelectedOverlayPoint(final @NotNull XYPlot plot, final int datasetIndex,
      final int rangeAxisIndex, final double xValue, final double yValue,
      final @NotNull Paint strokePaint, final @NotNull java.awt.Shape marker) {
    final XYSeries overlaySeries = new XYSeries("Selected lipid");
    overlaySeries.add(xValue, yValue);
    final XYSeriesCollection overlayDataset = new XYSeriesCollection();
    overlayDataset.addSeries(overlaySeries);

    final XYLineAndShapeRenderer overlayRenderer = new XYLineAndShapeRenderer(false, true);
    overlayRenderer.setSeriesPaint(0, strokePaint);
    overlayRenderer.setSeriesStroke(0, new BasicStroke(2f));
    overlayRenderer.setSeriesShape(0, marker);
    overlayRenderer.setDefaultShapesFilled(true);
    overlayRenderer.setUseOutlinePaint(true);
    overlayRenderer.setSeriesOutlinePaint(0,
        ConfigService.getConfiguration().isDarkMode() ? Color.WHITE : Color.BLACK);
    overlayRenderer.setSeriesOutlineStroke(0, new BasicStroke(1.1f));
    overlayRenderer.setSeriesVisibleInLegend(0, false);

    plot.setDataset(datasetIndex, overlayDataset);
    plot.mapDatasetToRangeAxis(datasetIndex, rangeAxisIndex);
    plot.setRenderer(datasetIndex, overlayRenderer);
  }

  private static void synchronizeSelectedPointAcrossAxes(final @NotNull XYPlot plot,
      final int primaryAxisIndex, final int secondaryAxisIndex, final double primaryValue,
      final double secondaryValue) {
    if (!(plot.getRangeAxis(primaryAxisIndex) instanceof NumberAxis primaryAxis)
        || !(plot.getRangeAxis(secondaryAxisIndex) instanceof NumberAxis secondaryAxis)) {
      return;
    }
    final double primaryLower = primaryAxis.getLowerBound();
    final double primaryUpper = primaryAxis.getUpperBound();
    final double secondaryLower = secondaryAxis.getLowerBound();
    final double secondaryUpper = secondaryAxis.getUpperBound();
    final SelectedPointAxisSynchronizer synchronizer = new SelectedPointAxisSynchronizer(
        primaryAxis, secondaryAxis, primaryValue, secondaryValue, primaryLower, primaryUpper,
        secondaryLower, secondaryUpper);
    synchronizer.install();
    synchronizer.syncSecondaryToPrimary();
  }

  private static @NotNull List<Color> createDistinctTrendColors(final int colorCount) {
    final List<Color> colors = new java.util.ArrayList<>(colorCount);
    final Set<Integer> usedColorCodes = new HashSet<>();
    final var palette = ConfigService.getConfiguration().getDefaultColorPalette();
    final int paletteSize = Math.max(0, palette.size());
    int colorIndex = 0;
    while (colors.size() < colorCount) {
      int overflowIndex = Math.max(0, colorIndex - paletteSize);
      Color color = colorIndex < paletteSize ? palette.getAWT(colorIndex)
          : generateDistinctOverflowColor(overflowIndex);
      while (usedColorCodes.contains(color.getRGB())) {
        color = generateDistinctOverflowColor(++overflowIndex);
      }
      usedColorCodes.add(color.getRGB());
      colors.add(color);
      colorIndex++;
    }
    return colors;
  }

  private static @NotNull Color generateDistinctOverflowColor(final int index) {
    final double hue = (index * 0.6180339887498949d) % 1d;
    return Color.getHSBColor((float) hue, 0.72f, 0.88f);
  }

  private static @NotNull BasicStroke groupedRegressionStroke() {
    return new BasicStroke(1.8f);
  }

  private static void extendRegressionBounds(final @NotNull RegressionYBounds regressionBounds,
      final @NotNull double[] regression, final @NotNull GroupedRetentionTrendDataset dataset,
      final int series) {
    if (!Double.isFinite(regression[0]) || !Double.isFinite(regression[1])) {
      return;
    }
    final double minX = minDatasetX(dataset, series);
    final double maxX = maxDatasetX(dataset, series);
    if (Double.isFinite(minX)) {
      regressionBounds.include(regression[0] * minX + regression[1]);
    }
    if (Double.isFinite(maxX)) {
      regressionBounds.include(regression[0] * maxX + regression[1]);
    }
  }

  private static int nextDatasetIndex(final @NotNull XYPlot plot) {
    int index = 0;
    while (plot.getDataset(index) != null || plot.getRenderer(index) != null) {
      index++;
    }
    return index;
  }

  private static @NotNull Paint retentionLabelPaint() {
    return ConfigService.getConfiguration().isDarkMode() ? new Color(230, 230, 230)
        : new Color(35, 35, 35);
  }

  private static @NotNull Color ecnTrendDatasetColor() {
    return ConfigService.getDefaultColorPalette().getPositiveColorAWT();
  }

  private static @NotNull Color dbeTrendDatasetColor() {
    return ConfigService.getDefaultColorPalette().getNeutralColorAWT();
  }

  private static @NotNull Color falsePositiveColor() {
    return ConfigService.getDefaultColorPalette().getNegativeColorAWT();
  }

  private static @NotNull Color falseNegativeColor() {
    return ConfigService.getDefaultColorPalette().getPositiveColorAWT();
  }

  private static void configureNoCrosshair(final @NotNull XYPlot plot) {
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);
  }

  private enum SelectionQualityFlag {
    NONE, POTENTIAL_FALSE_POSITIVE, POTENTIAL_FALSE_NEGATIVE
  }

  private record TrendYBounds(double minY, double maxY) {

  }

  private static final class RegressionYBounds {

    private double minY = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    private void include(final double yValue) {
      if (!Double.isFinite(yValue)) {
        return;
      }
      minY = Math.min(minY, yValue);
      maxY = Math.max(maxY, yValue);
    }
  }
}

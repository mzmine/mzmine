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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.ValueAxis;

/// Custom [QualityCheckResult] for the isotope pattern check. Unlike the other checks this card is
/// live: it follows the selected adduct row (the quality pane's selected member row) and shows that
/// row's isotope evidence without a full recompute. The chart holds the detected isotope pattern in
/// the positive color and the predicted isotopes of the row's formula as short horizontal line
/// markers.
public final class IsotopePatternQualityResult extends QualityCheckResult {

  /// Width of the predicted-isotope marker in pixels.
  private static final double MARKER_WIDTH = 13.0;
  /// Thickness of the predicted-isotope marker. Filled (not stroked) so it renders identically in
  /// the plot and in the legend.
  private static final double MARKER_THICKNESS = 2.0;
  /// Short horizontal line marking the expected intensity of a predicted isotope signal without
  /// covering the measured stick underneath.
  private static final Shape PREDICTED_MARKER = new Rectangle2D.Double(-MARKER_WIDTH / 2,
      -MARKER_THICKNESS / 2, MARKER_WIDTH, MARKER_THICKNESS);
  /// Height of the chart inside the card. Tall enough to read an isotope envelope, small enough to
  /// leave the neighbouring cards reachable in the scroll column.
  private static final double CHART_HEIGHT = 200;
  /// Fraction of the data width added left and right of the m/z auto range. Well above the
  /// JFreeChart default (0.05) so a narrow isotope envelope is not drawn edge to edge.
  private static final double DOMAIN_AXIS_MARGIN = 0.2;

  /// A predicted isotope pattern together with the formula it was predicted for. The formula is
  /// null when only a stored pattern without a formula was available; the dataset label then drops
  /// the parenthesis.
  record PredictedPattern(@NotNull IsotopePattern pattern, @Nullable String formula) {

  }

  /// Isotope evidence of one member row: every detected charge-state hypothesis (best first, empty
  /// when nothing was detected) and the predicted pattern of the row's formula (null when the row
  /// has neither an annotation nor a predicted formula).
  record RowIsotopes(@NotNull List<@NotNull IsotopePattern> chargeStates,
                     @Nullable PredictedPattern predicted) {

    public static final RowIsotopes EMPTY = new RowIsotopes(List.of(), null);
  }

  private final @NotNull Map<@NotNull FeatureListRow, @NotNull RowIsotopes> byRow;
  private final @NotNull FeatureListRow defaultRow;
  private final @Nullable ObservableValue<@Nullable FeatureListRow> selectedMemberRow;

  // Live state, read and written on the FX thread only. Plain fields plus explicit updaters (not
  // observable properties) so a row switch — which changes both fields — redraws the card exactly
  // once, from a consistent state.
  private @NotNull RowIsotopes current = RowIsotopes.EMPTY;
  private @Nullable IsotopePattern selectedChargeState;
  private @Nullable Runnable summaryUpdater;
  private @Nullable Runnable chartUpdater;

  public IsotopePatternQualityResult(@NotNull QualityCheckStatus status,
      @NotNull Map<@NotNull FeatureListRow, @NotNull RowIsotopes> byRow,
      @NotNull FeatureListRow defaultRow,
      @Nullable ObservableValue<@Nullable FeatureListRow> selectedMemberRow,
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    super(QualityCheckType.ISOTOPE_PATTERN, status, involvedRows);
    this.byRow = Map.copyOf(byRow);
    this.defaultRow = defaultRow;
    this.selectedMemberRow = selectedMemberRow;
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = FragmentParentsRendering.configureWrap(
        FxLabels.newBoldLabel(type.getLabel()));

    // Charge-state navigation, only shown when the row carries more than one hypothesis. The
    // buttons consume their mouse-clicked event so a click cycles the charge instead of also
    // toggling the card (QualityCheckItem toggles the sub pane on any header click).
    final ButtonBase prev = FxIconUtil.newIconButton(FxIcons.ARROW_LEFT, "Previous charge state",
        () -> cycleChargeState(-1));
    final ButtonBase next = FxIconUtil.newIconButton(FxIcons.ARROW_RIGHT, "Next charge state",
        () -> cycleChargeState(1));
    prev.setOnMouseClicked(Event::consume);
    next.setOnMouseClicked(Event::consume);

    final Label summaryLabel = FragmentParentsRendering.configureWrap(FxLabels.newLabel(""));
    final HBox summaryLine = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, prev, next,
        summaryLabel);
    summaryLine.setMinWidth(0);

    summaryUpdater = () -> {
      final boolean multipleCharges = current.chargeStates().size() > 1;
      setVisible(prev, multipleCharges);
      setVisible(next, multipleCharges);
      summaryLabel.setText(summaryText());
    };
    summaryUpdater.run();

    final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, summaryLine);
    box.setMinWidth(0);
    bindSelectedRow(box);
    return box;
  }

  @Override
  public @Nullable Region buildSubPane() {
    final SimpleColorPalette colors = ConfigService.getDefaultColorPalette();
    final SimpleSpectraChartController controller = new SimpleSpectraChartController();
    controller.domainAxisLabelProperty().set("m/z");
    controller.rangeAxisLabelProperty().set("Intensity");
    // several datasets with different meanings — the legend is what makes them distinguishable.
    controller.setLegendItemsVisible(true);

    // Build the view BEFORE adding datasets: SimpleSpectraChartViewBuilder.build() installs the
    // MapChangeListener that forwards datasets to the plot, and adding them runs inline on the FX
    // thread. Datasets added first would sit in the model and never reach the chart, leaving an
    // empty plot with the default 0-1 axes.
    final Region view = controller.buildView();
    view.setMinHeight(CHART_HEIGHT);
    view.setPrefHeight(CHART_HEIGHT);
    view.setMinWidth(0);

    chartUpdater = () -> updateChart(controller, colors);
    // Populate for whatever row is selected right now — buildMainPane already applied it.
    chartUpdater.run();
    return view;
  }

  /// The chart needs the full card width; the isotope envelope is unreadable in the indented column
  /// left over next to the status icon.
  @Override
  public boolean wantsFullWidthSubPane() {
    return true;
  }

  /// Follow the selected adduct row: show that row's patterns instead of the compound's preferred
  /// row. Uses a {@link WeakChangeListener} (the pattern of
  /// {@link FragmentParentsRendering#bindSelectionBold}) so the long-lived selection property does
  /// not keep this result alive after its card left the scene; the strong reference lives on the
  /// main pane's properties map and is collected together with it.
  private void bindSelectedRow(@NotNull final Region anchor) {
    if (selectedMemberRow == null) {
      applyRow(defaultRow);
      return;
    }
    final ChangeListener<FeatureListRow> listener = (_, _, is) -> applyRow(is);
    // Key under which the strong reference to the selected-row listener is parked on the main pane.
    // keep listener alive as long as UI element
    anchor.getProperties().put("isotopePatternSelectedRowListener", listener);
    selectedMemberRow.addListener(new WeakChangeListener<>(listener));
    applyRow(selectedMemberRow.getValue());
  }

  /// Switch the card to {@code row}, resetting the charge-state selection to the best hypothesis.
  /// Rows without precomputed data (only possible while a compound switch is still settling) show
  /// the empty chart rather than another row's pattern.
  private void applyRow(@Nullable final FeatureListRow row) {
    current = byRow.getOrDefault(row == null ? defaultRow : row, RowIsotopes.EMPTY);
    selectedChargeState =
        current.chargeStates().isEmpty() ? null : current.chargeStates().getFirst();
    refresh();
  }

  /// Cycle the shown charge-state hypothesis, wrapping in both directions.
  private void cycleChargeState(final int delta) {
    final List<IsotopePattern> states = current.chargeStates();
    if (states.size() < 2) {
      return;
    }
    final int index = states.indexOf(selectedChargeState);
    final int next = ((index + delta) % states.size() + states.size()) % states.size();
    selectedChargeState = states.get(next);
    refresh();
  }

  /// Push the current state into whichever parts of the card have been built already.
  private void refresh() {
    if (summaryUpdater != null) {
      summaryUpdater.run();
    }
    if (chartUpdater != null) {
      chartUpdater.run();
    }
  }

  /// Summary text next to the charge buttons: the charge of the shown hypothesis, marked
  /// {@code (preferred)} while that hypothesis is the best-ranked one of the pattern.
  private @NotNull String summaryText() {
    final IsotopePattern pattern = selectedChargeState;
    if (pattern == null) {
      return current.predicted() == null ? "No isotope pattern detected"
          : "No isotope pattern detected, showing the predicted isotopes";
    }
    final String charge =
        pattern.getCharge() > 0 ? "Charge = " + pattern.getCharge() : "Charge unknown";
    // chargeStates is ranked best first, so the first entry is the preferred hypothesis.
    return pattern == current.chargeStates().getFirst() ? charge + " (preferred)" : charge;
  }

  /// Rebuild the datasets for the currently shown row + charge state:
  /// <ul>
  ///   <li>detected pattern present: one dataset in the positive color;</li>
  ///   <li>no detected pattern but a predicted one: an empty placeholder dataset so the legend
  ///       states that nothing was detected;</li>
  ///   <li>predicted pattern present: the `-` markers on top;</li>
  ///   <li>neither: no datasets at all, i.e. an empty chart.</li>
  /// </ul>
  private void updateChart(@NotNull final SimpleSpectraChartController controller,
      @NotNull final SimpleColorPalette colors) {
    controller.clearDatasets();
    final IsotopePattern detected = selectedChargeState;
    final PredictedPattern predicted = current.predicted();

    if (detected != null) {
      controller.addSpectrum(
          new MassSpectrumProvider(detected, detectedLabel(detected), colors.getPositiveColorAWT()),
          MassSpectrumType.CENTROIDED);
    } else if (predicted != null) {
      controller.addSpectrum(
          new MassSpectrumProvider(new double[0], new double[0], "No detected isotopes",
              colors.getPositiveColorAWT()), MassSpectrumType.CENTROIDED);
    }
    if (predicted != null) {
      controller.addDataset(new MassSpectrumProvider(predicted.pattern(), predictedLabel(predicted),
              colors.getNegativeColorAWT()),
          // ignoreZPaintScale: take the color from the dataset, there is no z dimension here.
          new ColoredXYShapeRenderer(false, PREDICTED_MARKER, true));
    }
    applyDomainMargin(controller);
  }

  /// Widen the m/z auto range. Applied after every dataset change because the axis is shared with
  /// the datasets that were just replaced.
  private static void applyDomainMargin(@NotNull final SimpleSpectraChartController controller) {
    final ValueAxis axis = controller.getChart().getXYPlot().getDomainAxis();
    axis.setLowerMargin(DOMAIN_AXIS_MARGIN);
    axis.setUpperMargin(DOMAIN_AXIS_MARGIN);
  }

  private static void setVisible(@NotNull final Region node, final boolean visible) {
    node.setVisible(visible);
    node.setManaged(visible);
  }

  private static @NotNull String detectedLabel(@NotNull final IsotopePattern pattern) {
    final int charge = pattern.getCharge();
    return "Detected isotopes" + (charge > 0 ? " (z=" + charge + ")" : "");
  }

  private static @NotNull String predictedLabel(@NotNull final PredictedPattern predicted) {
    final String formula = predicted.formula();
    return "Predicted isotopes" + (formula == null || formula.isBlank() ? ""
        : " (" + formula + ")");
  }
}

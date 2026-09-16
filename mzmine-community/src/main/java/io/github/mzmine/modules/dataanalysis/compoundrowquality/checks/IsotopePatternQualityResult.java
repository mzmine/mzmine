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
/// LIVE: it follows the selected adduct row and shows that row's isotope evidence without a
/// recompute. The detected pattern is drawn in the positive color, the predicted isotopes as short
/// horizontal markers.
public final class IsotopePatternQualityResult extends QualityCheckResult {

  private static final double MARKER_WIDTH = 13.0;
  /// Filled rather than stroked, so it renders identically in the plot and in the legend.
  private static final double MARKER_THICKNESS = 2.0;
  /// Marks a predicted signal's expected intensity without covering the measured stick under it.
  private static final Shape PREDICTED_MARKER = new Rectangle2D.Double(-MARKER_WIDTH / 2,
      -MARKER_THICKNESS / 2, MARKER_WIDTH, MARKER_THICKNESS);
  /// Tall enough to read an envelope, small enough to leave neighbouring cards reachable.
  private static final double CHART_HEIGHT = 200;
  /// Well above the JFreeChart default (0.05), so a narrow envelope is not drawn edge to edge.
  private static final double DOMAIN_AXIS_MARGIN = 0.2;

  /// A predicted pattern plus the formula it came from. The formula is null when only a stored
  /// pattern was available, and the dataset label then drops the parenthesis.
  record PredictedPattern(@NotNull IsotopePattern pattern, @Nullable String formula) {

  }

  /// Isotope evidence of one member row: the detected charge-state hypotheses (best first) and the
  /// predicted pattern of the row's formula, null when the row has no formula at all.
  record RowIsotopes(@NotNull List<@NotNull IsotopePattern> chargeStates,
                     @Nullable PredictedPattern predicted) {

    public static final RowIsotopes EMPTY = new RowIsotopes(List.of(), null);
  }

  private final @NotNull Map<@NotNull FeatureListRow, @NotNull RowIsotopes> byRow;
  private final @NotNull FeatureListRow defaultRow;
  private final @Nullable ObservableValue<@Nullable FeatureListRow> selectedMemberRow;

  // FX thread only. Plain fields with explicit updaters rather than observable properties, so a row
  // switch - which changes both - redraws the card exactly once, from a consistent state.
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

    // the buttons consume their mouse-clicked event, or the click would also toggle the card -
    // QualityCheckItem toggles the sub pane on any header click
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

    // build the view BEFORE adding datasets: build() installs the MapChangeListener that forwards
    // them to the plot, so datasets added first would sit in the model and never reach the chart
    final Region view = controller.buildView();
    view.setMinHeight(CHART_HEIGHT);
    view.setPrefHeight(CHART_HEIGHT);
    view.setMinWidth(0);

    chartUpdater = () -> updateChart(controller, colors);
    // populate for whatever row is selected right now
    chartUpdater.run();
    return view;
  }

  /// The envelope is unreadable in the indented column left next to the status icon.
  @Override
  public boolean wantsFullWidthSubPane() {
    return true;
  }

  /// Follow the selected adduct row instead of the compound's preferred one. A
  /// {@link WeakChangeListener} (as in {@link FragmentParentsRendering#bindSelectionBold}) keeps
  /// the long-lived selection property from holding this result alive after its card leaves the
  /// scene; the strong reference lives on the main pane's properties map and dies with it.
  private void bindSelectedRow(@NotNull final Region anchor) {
    if (selectedMemberRow == null) {
      applyRow(defaultRow);
      return;
    }
    final ChangeListener<FeatureListRow> listener = (_, _, is) -> applyRow(is);
    // parks the strong reference on the main pane, keeping the listener alive as long as the UI
    anchor.getProperties().put("isotopePatternSelectedRowListener", listener);
    selectedMemberRow.addListener(new WeakChangeListener<>(listener));
    applyRow(selectedMemberRow.getValue());
  }

  /// Switch the card to {@code row}, resetting the charge selection to the best hypothesis. A row
  /// without precomputed data - possible while a compound switch settles - shows the empty chart
  /// rather than another row's pattern.
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

  private void refresh() {
    if (summaryUpdater != null) {
      summaryUpdater.run();
    }
    if (chartUpdater != null) {
      chartUpdater.run();
    }
  }

  /// Charge of the shown hypothesis, marked {@code (preferred)} while it is the best-ranked one.
  private @NotNull String summaryText() {
    final IsotopePattern pattern = selectedChargeState;
    if (pattern == null) {
      return current.predicted() == null ? "No isotope pattern detected"
          : "No isotope pattern detected, showing the predicted isotopes";
    }
    final String charge =
        pattern.getCharge() > 0 ? "Charge = " + pattern.getCharge() : "Charge unknown";
    // ranked best first, so index 0 is the preferred hypothesis
    return pattern == current.chargeStates().getFirst() ? charge + " (preferred)" : charge;
  }

  /// Rebuild the datasets for the shown row + charge state. A predicted pattern with nothing
  /// detected still gets an empty placeholder dataset, so the legend can say so.
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
          // ignoreZPaintScale: no z dimension here, so take the color from the dataset
          new ColoredXYShapeRenderer(false, PREDICTED_MARKER, true));
    }
    applyDomainMargin(controller);
  }

  /// Re-applied after every dataset change, as the axis is shared with the replaced datasets.
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

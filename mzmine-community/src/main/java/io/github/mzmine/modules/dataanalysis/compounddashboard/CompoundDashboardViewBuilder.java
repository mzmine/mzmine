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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.CompoundRowQualityController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.featurerow4dplot.FeatureRow4DPlotController;
import io.github.mzmine.modules.visualization.featurerow4dplot.FeatureRow4DPlotIcon;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Builds the dashboard layout. Receives the controller for prev/next callbacks and the
 * sub-controllers' built views; the controller is responsible for wiring data flow.
 */
public class CompoundDashboardViewBuilder extends FxViewBuilder<CompoundDashboardModel> {

  private final CompoundDashboardController controller;
  private final ChromatogramPlotController eicPlot;
  private final ChromatogramPlotController mobilogramPlot;
  private final SimpleSpectraChartController ms1Chart;
  private final SimpleSpectraChartController ms2Chart;
  private final SimpleSpectraChartController isotopeSpectrumChart;
  private final CompoundRowQualityController qualityCtrl;
  private final FxFeatureTableController tableCtrl;
  private final FeatureRow4DPlotController featurePlot4D;
  private SplitPane mainVerticalChartsSplit;

  public CompoundDashboardViewBuilder(@NotNull CompoundDashboardModel model,
      @NotNull CompoundDashboardController controller, @NotNull ChromatogramPlotController eicPlot,
      @NotNull ChromatogramPlotController mobilogramPlot,
      @NotNull SimpleSpectraChartController ms1Chart,
      @NotNull SimpleSpectraChartController ms2Chart,
      @NotNull SimpleSpectraChartController isotopeSpectrumChart,
      @NotNull CompoundRowQualityController qualityCtrl,
      @NotNull FxFeatureTableController tableCtrl,
      @NotNull FeatureRow4DPlotController featurePlot4D) {
    super(model);
    this.controller = controller;
    this.eicPlot = eicPlot;
    this.mobilogramPlot = mobilogramPlot;
    this.ms1Chart = ms1Chart;
    this.ms2Chart = ms2Chart;
    this.isotopeSpectrumChart = isotopeSpectrumChart;
    this.qualityCtrl = qualityCtrl;
    this.tableCtrl = tableCtrl;
    this.featurePlot4D = featurePlot4D;
  }

  @Override
  public Region build() {
    final Region eicWithToolbar = buildEicWithToolbar();
    final Region spectraColumn = buildSpectraColumn();

    final ModularFeatureList flist = model.getFeatureList();
    boolean hasMobilogram = flist != null && flist.hasRowType(FeatureShapeMobilogramType.class);

    final Region plot4DView = featurePlot4D.buildView();
    plot4DView.setMinWidth(150);
    plot4DView.setPadding(new Insets(0, FxLayout.DEFAULT_SPACE, 0, 0));

    mainVerticalChartsSplit = new SplitPane(plot4DView, eicWithToolbar, spectraColumn);
    mainVerticalChartsSplit.setOrientation(Orientation.HORIZONTAL);
    // Plot4D | EIC | Spectra. EIC stays the largest column; spectra column reuses the same width
    // it had before the 4D pane existed.
    mainVerticalChartsSplit.setDividerPositions(0.25, hasMobilogram ? 0.55 : 0.50);

    // Toggle hides/restores the 4D pane without rebuilding it. Mirrors the chartsSplit pattern
    // used for the optional mobilogram view.
    model.featurePlot4DVisibleProperty().subscribe(show -> {
      final boolean alreadyShown = mainVerticalChartsSplit.getItems().contains(plot4DView);
      if (Boolean.TRUE.equals(show) && !alreadyShown) {
        mainVerticalChartsSplit.getItems().addFirst(plot4DView);
        mainVerticalChartsSplit.setDividerPositions(0.25, hasMobilogram ? 0.55 : 0.50);
      } else if (!Boolean.TRUE.equals(show) && alreadyShown) {
        mainVerticalChartsSplit.getItems().remove(plot4DView);
      }
    });

    // Legend FlowPane sits directly under the charts SplitPane and lists every member row of the
    // currently selected compound. Labels mirror the plot colors and are clickable to focus a row.
    final FlowPane legendPane = buildLegendPane();
    VBox.setVgrow(mainVerticalChartsSplit, Priority.ALWAYS);
    final VBox chartsWithLegend = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true,
        mainVerticalChartsSplit, legendPane);

    final BorderPane topArea = new BorderPane(chartsWithLegend);
    final Region quality = qualityCtrl.buildView();
    topArea.setRight(quality);

    final SplitPane outer = new SplitPane(topArea, tableCtrl.buildView());
    outer.setOrientation(Orientation.VERTICAL);
    outer.setDividerPositions(0.55);
    return outer;
  }

  private @NotNull Region buildEicWithToolbar() {
    // Toggle for the optional 4D feature plot pane. The stylised bubble-scatter icon advertises
    // what the pane contains; opacity drops while the pane is hidden so the button reads as
    // "currently collapsed — click to show" without changing the icon's identity. The button is
    // forced to a square size (icon size + symmetric padding) so it lines up cleanly with the
    // neighbouring icon buttons regardless of platform default insets.
    final double iconSize = 24.0;
    final double buttonSize = 28.0;
    final Button toggle4D = new Button(null, new FeatureRow4DPlotIcon(iconSize));
    toggle4D.getStyleClass().add("icon-button");
    toggle4D.setMinSize(buttonSize, buttonSize);
    toggle4D.setPrefSize(buttonSize, buttonSize);
    toggle4D.setMaxSize(buttonSize, buttonSize);
    toggle4D.setPadding(Insets.EMPTY);
    toggle4D.setOnAction(
        _ -> model.featurePlot4DVisibleProperty().set(!model.isFeaturePlot4DVisible()));
    model.featurePlot4DVisibleProperty().subscribe(show -> {
      toggle4D.getGraphic().setOpacity(Boolean.FALSE.equals(show) ? 1.0 : 0.45);
      toggle4D.setTooltip(
          new Tooltip(Boolean.TRUE.equals(show) ? "Hide 4D feature plot" : "Show 4D feature plot"));
    });

    final ButtonBase prev = FxIconUtil.newIconButton(FxIcons.ARROW_LEFT, "Previous raw data file",
        controller::previousRawDataFile);
    final ButtonBase next = FxIconUtil.newIconButton(FxIcons.ARROW_RIGHT, "Next raw data file",
        controller::nextRawDataFile);
    final ComboBox<RawDataFile> rawCombo = FxComboBox.createComboBox("Raw data file",
        model.getAvailableRawDataFiles(), model.currentRawDataFileProperty());
    rawCombo.setCellFactory(_ -> rawFileCell());
    rawCombo.setButtonCell(rawFileCell());
    HBox.setHgrow(rawCombo, Priority.SOMETIMES);

    final ValueAxis domainAxis = eicPlot.getXYPlot().getDomainAxis();
    domainAxis.setLowerMargin(0.15);
    domainAxis.setUpperMargin(0.15);
    eicPlot.setTitle("Chromatographic shapes");
    // Legend is rendered by the FlowPane under the charts instead of on every plot.
    eicPlot.setLegendItemsVisible(false);

    final HBox toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, toggle4D, prev, rawCombo,
        next);

    final Region eicView = eicPlot.buildView();
    VBox.setVgrow(eicView, Priority.ALWAYS);

    // Mobilogram view sits underneath the EIC in a vertical SplitPane when the feature list
    // carries the FeatureShapeMobilogramType (i.e. IMS data was detected upstream). For non-IMS
    // feature lists we add only the EIC view so the split divider doesn't appear at all.
    final ValueAxis mobDomainAxis = mobilogramPlot.getXYPlot().getDomainAxis();
    mobDomainAxis.setLowerMargin(0.15);
    mobDomainAxis.setUpperMargin(0.15);
    final Region mobilogramView = mobilogramPlot.buildView();
    mobilogramPlot.setTitle("Mobilograms");
    mobilogramPlot.setLegendItemsVisible(false);

    final SplitPane chartsSplit = new SplitPane(eicView);
    chartsSplit.setOrientation(Orientation.VERTICAL);

    final BooleanBinding hasMobilograms = Bindings.createBooleanBinding(() -> {
      final ModularFeatureList flist = model.getFeatureList();
      return flist != null && flist.hasRowType(FeatureShapeMobilogramType.class);
    }, model.featureListProperty());
    hasMobilograms.subscribe(show -> {
      final boolean alreadyShown = chartsSplit.getItems().contains(mobilogramView);
      if (Boolean.TRUE.equals(show) && !alreadyShown) {
        chartsSplit.getItems().add(mobilogramView);
        chartsSplit.setDividerPositions(0.55);
      } else if (!Boolean.TRUE.equals(show) && alreadyShown) {
        chartsSplit.getItems().remove(mobilogramView);
      }
    });

    VBox.setVgrow(chartsSplit, Priority.ALWAYS);
    return FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, toolbar, chartsSplit);
  }

  private @NotNull Region buildSpectraColumn() {
    // MS1 (no extra toolbar; gray background + per-row sticks)
    final Region ms1View = ms1Chart.buildView();
    // MS2 keeps its legend; MS1 mirrors the dashboard legend FlowPane below the charts.
    ms1Chart.setLegendItemsVisible(false);
    // MS2 with adduct selector above. The ComboBox drives `selectedMs2Row` (can be null when no
    // member row has an MS2 scan); `selectedAdductRow` is derived from it and always non-null when
    // a compound is selected, so the EIC/MS1 highlight has a stable target.
    final ComboBox<FeatureListRow> adductCombo = FxComboBox.createComboBox("Adduct (MS2 source)",
        model.getAdductRows(), model.selectedMs2RowProperty());
    adductCombo.setCellFactory(_ -> adductCell());
    adductCombo.setButtonCell(adductCell());
    HBox.setHgrow(adductCombo, Priority.SOMETIMES);
    final Label ms2OfLabel = FxLabels.newBoldLabel("MS2 of");

    // Scan selector: first item is the merged MS2 (REPRESENTATIVE across samples), followed by the
    // row's individual fragment scans. Prev/next buttons cycle through the list in the controller.
    final ButtonBase prevScan = FxIconUtil.newIconButton(FxIcons.ARROW_LEFT, "Previous MS2 scan",
        controller::previousMs2Scan);
    final ButtonBase nextScan = FxIconUtil.newIconButton(FxIcons.ARROW_RIGHT, "Next MS2 scan",
        controller::nextMs2Scan);
    final ComboBox<Scan> scanCombo = FxComboBox.createComboBox("MS2 scan",
        model.getAvailableMs2Scans(), model.selectedMs2ScanProperty());
    scanCombo.setCellFactory(_ -> ms2ScanCell(model.getAvailableMs2Scans()));
    scanCombo.setButtonCell(ms2ScanCell(model.getAvailableMs2Scans()));
    HBox.setHgrow(scanCombo, Priority.SOMETIMES);

    final HBox ms2Toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, ms2OfLabel, adductCombo, prevScan,
        scanCombo, nextScan);

    final Region ms2View = ms2Chart.buildView();
    // Overlay a centered bold message when no MS2 dataset is available so the user knows the
    // plot is empty by design, not by glitch.
    final Label noMs2Label = FxLabels.newBoldTitle("No MS2 for selected ion");
    noMs2Label.setMouseTransparent(true);
    noMs2Label.visibleProperty().bind(Bindings.isEmpty(model.getMs2Datasets()));
    noMs2Label.managedProperty().bind(noMs2Label.visibleProperty());
    final StackPane ms2Stack = new StackPane(ms2View, noMs2Label);
    StackPane.setAlignment(noMs2Label, Pos.CENTER);
    VBox.setVgrow(ms2Stack, Priority.ALWAYS);

    final BorderPane mainMS2 = new BorderPane(ms2Stack);
    mainMS2.setTop(ms2Toolbar);

    // Isotope pattern mirror sits between MS1 and MS2: detected isotope pattern on top, the
    // representative MS1 on the bottom, with a charge-state selector toolbar above.
    final Region isotopeMirror = buildIsotopeMirror();

    final SplitPane sp = new SplitPane(ms1View, isotopeMirror, mainMS2);
    sp.setOrientation(Orientation.VERTICAL);
    sp.setDividerPositions(0.33, 0.66);
    return sp;
  }

  /**
   * Builds the isotope pattern mirror pane: a toolbar with prev/next charge-state icon buttons and
   * a charge ComboBox (mirroring the MS2 toolbar layout), above a content area that shows one of
   * three things depending on the selected adduct row:
   * <ul>
   *   <li>the mirror plot (detected isotope pattern on top, representative MS1 on bottom) when the
   *       row has a detected isotope pattern — the domain is zoomed to the pattern m/z range ±5;</li>
   *   <li>a plain full MS1 spectrum of the row's representative scan (in a
   *       {@link SimpleSpectraChartController}, like the MS1 / MS2 charts) when the row has no
   *       isotope pattern;</li>
   *   <li>a centered bold message when the row has neither a pattern nor a representative scan.</li>
   * </ul>
   */
  private @NotNull Region buildIsotopeMirror() {
    final Label chargeLabel = FxLabels.newBoldLabel("Potential isotopes");
    final ButtonBase prevCharge = FxIconUtil.newIconButton(FxIcons.ARROW_LEFT,
        "Previous charge state", controller::previousChargeState);
    final ButtonBase nextCharge = FxIconUtil.newIconButton(FxIcons.ARROW_RIGHT, "Next charge state",
        controller::nextChargeState);
    final ComboBox<IsotopePattern> chargeCombo = FxComboBox.createComboBox("Charge state",
        model.getIsotopeChargeStates(), model.selectedIsotopePatternProperty());
    chargeCombo.setCellFactory(_ -> chargeStateCell());
    chargeCombo.setButtonCell(chargeStateCell());
    HBox.setHgrow(chargeCombo, Priority.SOMETIMES);

    final HBox toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, chargeLabel, prevCharge, chargeCombo,
        nextCharge);

    // Stack of three mutually-exclusive layers toggled by visibility: the mirror chart holder, the
    // fallback full-MS1 spectrum chart, and the "no data" message.
    final BorderPane mirrorHolder = new BorderPane();
    final Region spectrumView = isotopeSpectrumChart.buildView();
    final Label noDataLabel = FxLabels.newBoldTitle("No MS1 for selected ion");
    noDataLabel.setMouseTransparent(true);
    final StackPane stack = new StackPane(mirrorHolder, spectrumView, noDataLabel);
    StackPane.setAlignment(noDataLabel, Pos.CENTER);
    VBox.setVgrow(stack, Priority.ALWAYS);

    final Runnable rebuild = () -> rebuildIsotopeMirror(mirrorHolder, spectrumView, noDataLabel);
    model.selectedIsotopePatternProperty().subscribe(_ -> rebuild.run());
    model.isotopeRepresentativeScanProperty().subscribe(_ -> rebuild.run());
    rebuild.run();

    final BorderPane main = new BorderPane(stack);
    main.setTop(toolbar);
    return main;
  }

  /**
   * Rebuilds the isotope mirror content. With a detected pattern: draws the mirror
   * {@link EChartViewer} (pattern top, representative MS1 bottom) and zooms the shared m/z domain
   * to the pattern's m/z range ±5. Without a pattern but with a representative scan: shows that
   * scan as a plain full MS1 spectrum. With neither: shows the centered "no data" message.
   */
  private void rebuildIsotopeMirror(@NotNull final BorderPane mirrorHolder,
      @NotNull final Region spectrumView, @NotNull final Label noDataLabel) {
    final IsotopePattern pattern = model.getSelectedIsotopePattern();
    final Scan representative = model.getIsotopeRepresentativeScan();

    if (pattern != null && representative != null) {
      final int charge = pattern.getCharge();
      final String topLabel =
          "Detected isotope pattern" + (charge > 0 ? " (z=" + charge + ")" : "");
      // MirrorChartFactory accepts any MassSpectrum; IsotopePattern and Scan are both MassSpectrum.
      final EChartViewer viewer = MirrorChartFactory.createMirrorChartViewer(pattern,
          representative, topLabel, "Representative MS1", false, true);
      mirrorHolder.setCenter(viewer);
      zoomToPatternRange(viewer, pattern);
      setLayerVisible(mirrorHolder, true);
      setLayerVisible(spectrumView, false);
      noDataLabel.setVisible(false);
      return;
    }

    if (representative != null) {
      // No isotope pattern: show a plain full MS1 spectrum of the row (same chart type as MS1/MS2).
      final java.awt.Color awt = FxColorUtil.fxColorToAWT(
          ConfigService.getDefaultColorPalette().getNeutralColor());
      isotopeSpectrumChart.clearDatasets();
      isotopeSpectrumChart.addSpectrum(new MassSpectrumProvider(representative,
          "MS1 " + representative.getDataFile().getName() + ":" + representative.getScanNumber(),
          awt), representative.getSpectrumType());
      mirrorHolder.setCenter(null);
      setLayerVisible(mirrorHolder, false);
      setLayerVisible(spectrumView, true);
      noDataLabel.setVisible(false);
      return;
    }

    // Neither a pattern nor a representative scan.
    mirrorHolder.setCenter(null);
    isotopeSpectrumChart.clearDatasets();
    setLayerVisible(mirrorHolder, false);
    setLayerVisible(spectrumView, false);
    noDataLabel.setVisible(true);
  }

  /**
   * Zoom the shared m/z domain axis of the mirror plot to the pattern's m/z range ±5 so the newly
   * selected pattern fills the plot instead of the full representative MS1 range, then rescale the
   * intensity (range) axis of the top and bottom subplots to the tallest signal within that m/z
   * window — otherwise the full-scan MS1 on the bottom keeps its baseline near a peak that has been
   * zoomed out of view.
   */
  private static void zoomToPatternRange(@NotNull final EChartViewer viewer,
      @NotNull final IsotopePattern pattern) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      final double mz = pattern.getMzValue(i);
      min = Math.min(min, mz);
      max = Math.max(max, mz);
    }
    if (min > max) {
      return;
    }
    final double lo = min - 5;
    final double hi = max + 5;
    final XYPlot plot = viewer.getChart().getXYPlot();
    plot.getDomainAxis().setRange(lo, hi);
    // The mirror is a CombinedDomainXYPlot with one subplot per spectrum (top + bottom, the bottom
    // range axis inverted); auto-range each subplot independently to the data in the m/z window.
    if (plot instanceof CombinedDomainXYPlot combined) {
      for (final Object sub : combined.getSubplots()) {
        if (sub instanceof XYPlot subplot) {
          autoRangeToDomainWindow(subplot, lo, hi);
        }
      }
    } else {
      autoRangeToDomainWindow(plot, lo, hi);
    }
  }

  /**
   * Set {@code plot}'s range axis to {@code [0, maxIntensity * 1.05]} where {@code maxIntensity} is
   * the tallest signal of any dataset whose m/z falls within {@code [lo, hi]}. Intensities are
   * stored positive in both subplots (the bottom axis is inverted for display), so a single
   * non-negative scan works for both.
   */
  private static void autoRangeToDomainWindow(@NotNull final XYPlot plot, final double lo,
      final double hi) {
    double maxIntensity = 0d;
    for (int d = 0; d < plot.getDatasetCount(); d++) {
      final XYDataset ds = plot.getDataset(d);
      if (ds == null) {
        continue;
      }
      for (int s = 0; s < ds.getSeriesCount(); s++) {
        for (int i = 0; i < ds.getItemCount(s); i++) {
          final double x = ds.getXValue(s, i);
          if (x < lo || x > hi) {
            continue;
          }
          maxIntensity = Math.max(maxIntensity, ds.getYValue(s, i));
        }
      }
    }
    if (maxIntensity > 0d) {
      plot.getRangeAxis().setRange(0d, maxIntensity * 1.05);
    }
  }

  private static void setLayerVisible(@NotNull final Node node, final boolean visible) {
    node.setVisible(visible);
    node.setManaged(visible);
  }

  private static @NotNull ListCell<IsotopePattern> chargeStateCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(final IsotopePattern item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(formatChargeStateLabel(item));
        }
        setGraphic(null);
      }
    };
  }

  private static @NotNull String formatChargeStateLabel(@NotNull final IsotopePattern pattern) {
    final int charge = pattern.getCharge();
    final String chargeStr = charge > 0 ? ("z = " + charge) : "z = ?";
    final double score = pattern.getScore();
    if (Double.isNaN(score)) {
      return chargeStr;
    }
    final NumberFormats fmt = ConfigService.getGuiFormats();
    return chargeStr + " · score " + fmt.score(score);
  }

  /**
   * Builds the FlowPane that lists every member row of the currently selected compound, using the
   * same per-row colors as the EIC / mobilogram / MS1 plots. Ion-identified rows come first
   * (formatted as {@code [M+H]+ (300.0000)}), followed by an italic {@code "Unknown m/z:"} marker
   * and the unidentified rows (m/z only). The label of the currently selected adduct row is shown
   * bold. Each label is clickable: it sets the selected MS2 row when the row carries an MS2 scan
   * (so the adduct ComboBox stays in sync), otherwise it sets the selected adduct row directly.
   */
  private @NotNull FlowPane buildLegendPane() {
    final FlowPane pane = FxLayout.newFlowPane();
    // Horizontal breathing room so the labels don't touch the left/right edges of the dashboard.
    pane.setPadding(new Insets(0, FxLayout.DEFAULT_SPACE, 0, FxLayout.DEFAULT_SPACE));
    final Runnable rebuild = () -> rebuildLegendPane(pane);
    model.getLegendEntries()
        .addListener((ListChangeListener<CompoundDashboardLegendEntry>) _ -> rebuild.run());
    // Rebuild on selection change to swap the bold marker; cheap because there are few entries.
    model.selectedAdductRowProperty().subscribe(_ -> rebuild.run());
    return pane;
  }

  private void rebuildLegendPane(@NotNull final FlowPane pane) {
    pane.getChildren().clear();
    final FeatureListRow selected = model.getSelectedAdductRow();
    final List<CompoundDashboardLegendEntry> ions = new ArrayList<>();
    final List<CompoundDashboardLegendEntry> unknowns = new ArrayList<>();
    for (final CompoundDashboardLegendEntry entry : model.getLegendEntries()) {
      if (entry.row().getBestIonIdentity() != null) {
        ions.add(entry);
      } else {
        unknowns.add(entry);
      }
    }
    for (final CompoundDashboardLegendEntry entry : ions) {
      pane.getChildren().add(buildIonLegendLabel(entry, selected));
    }
    if (!unknowns.isEmpty()) {
      pane.getChildren().add(FxLabels.newItalicLabel("Unknown m/z:"));
      for (final CompoundDashboardLegendEntry entry : unknowns) {
        pane.getChildren().add(buildUnknownLegendLabel(entry, selected));
      }
    }
  }

  private @NotNull Label buildIonLegendLabel(@NotNull final CompoundDashboardLegendEntry entry,
      @Nullable final FeatureListRow selected) {
    final FeatureListRow row = entry.row();
    // Non-null here: this builder is only invoked for ion-identified rows.
    final IonIdentity ion = row.getBestIonIdentity();
    final Double mz = row.getAverageMZ();
    final String mzStr = mz == null ? "?" : ConfigService.getGuiFormats().mz(mz);
    final String text = ion.getIonType().toString() + " (" + mzStr + ")";
    return buildLegendLabel(row, entry.color(), text, selected);
  }

  private @NotNull Label buildUnknownLegendLabel(@NotNull final CompoundDashboardLegendEntry entry,
      @Nullable final FeatureListRow selected) {
    final FeatureListRow row = entry.row();
    final Double mz = row.getAverageMZ();
    // Fall back to the row ID when no m/z is available — keeps the label informative.
    final String text = mz == null ? ("row " + row.getID()) : ConfigService.getGuiFormats().mz(mz);
    return buildLegendLabel(row, entry.color(), text, selected);
  }

  private @NotNull Label buildLegendLabel(@NotNull final FeatureListRow row,
      @NotNull final Color color, @NotNull final String text,
      @Nullable final FeatureListRow selected) {
    final Label label = FxLabels.colored(new Label(text), color);
    if (row == selected) {
      label.getStyleClass().add("bold-label");
    }
    label.setCursor(Cursor.HAND);
    label.setOnMouseClicked(_ -> onLegendLabelClicked(row));
    return label;
  }

  private void onLegendLabelClicked(@NotNull final FeatureListRow row) {
    // selectedAdductRow is the master selection. The controller derives selectedMs2Row (and the
    // adduct ComboBox value) from it, so a single write is enough regardless of whether the row
    // carries MS2.
    if (model.getSelectedAdductRow() != row) {
      model.setSelectedAdductRow(row);
    }
  }

  private static @NotNull ListCell<RawDataFile> rawFileCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(final RawDataFile item, final boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item.getName());
        setGraphic(null);
      }
    };
  }

  private static @NotNull ListCell<Scan> ms2ScanCell(@NotNull final List<Scan> items) {
    return new ListCell<>() {
      @Override
      protected void updateItem(final Scan item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(formatMs2ScanLabel(item, items));
        }
        setGraphic(null);
      }
    };
  }

  private static @NotNull String formatMs2ScanLabel(@NotNull final Scan scan,
      @NotNull final List<Scan> items) {
    // PASEF_SINGLE is the per-precursor merged mobility frame: a single fragmentation event with
    // a real energy + method, just averaged across mobility scans. Treat it like a regular MS2 in
    // the dropdown so the user sees its energy/method rather than a generic "Merged MS2".
    if (scan instanceof MergedMassSpectrum merged
        && merged.getMergingType() != MergingType.PASEF_SINGLE) {
      return "Merged MS2";
    }
    // 1-based position within the source-scan section of the list (i.e. the position in the
    // dropdown, where index 0 is the merged scan and the first regular scan reads as "1").
    final int idx = items.indexOf(scan);
    final String itemNumber = idx < 0 ? "?" : String.valueOf(idx);
    final MsMsInfo info = scan.getMsMsInfo();
    final ActivationMethod method = info != null ? info.getActivationMethod() : null;
    final Float energy = ScanUtils.extractCollisionEnergy(scan);
    final String methodStr =
        method == null ? ActivationMethod.UNKNOWN.getAbbreviation() : method.getAbbreviation();
    final String energyStr;
    if (energy == null) {
      energyStr = "N.A.";
    } else if (method != null && !method.getUnit().isBlank()) {
      energyStr = formatEnergy(energy) + " " + method.getUnit();
    } else {
      energyStr = formatEnergy(energy);
    }
    return itemNumber + ", " + energyStr + ", " + methodStr;
  }

  /// Energy format: integer when {@code |energy| >= 10}, one decimal otherwise. Mirrors the chip
  /// formatting used by the MS2-availability quality check so the combo box and chips read the same
  /// way for the same scan.
  private static @NotNull String formatEnergy(@NotNull final Float energy) {
    final float v = energy;
    if (Math.abs(v) >= 10f) {
      return Integer.toString(Math.round(v));
    }
    return "%.1f".formatted(v);
  }

  private static @NotNull ListCell<FeatureListRow> adductCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(final FeatureListRow item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(formatAdductLabel(item));
        }
        setGraphic(null);
      }
    };
  }

  private static @NotNull String formatAdductLabel(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    final Double mz = row.getAverageMZ();
    final String mzStr = mz == null ? "?" : String.format("%.4f", mz);
    return ion == null ? ("row " + row.getID() + " · m/z " + mzStr)
        : (ion.getIonType().toString() + " · m/z " + mzStr);
  }

  // visualVar to keep parameters around for FxLayout's overload resolution
  @SuppressWarnings("unused")
  private static @NotNull Node spacer() {
    return FxLayout.newHVFillSpacer();
  }
}

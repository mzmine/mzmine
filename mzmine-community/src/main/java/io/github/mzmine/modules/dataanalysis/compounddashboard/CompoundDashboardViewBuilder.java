package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.CompoundRowQualityController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
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
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
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
  private final CompoundRowQualityController qualityCtrl;
  private final FxFeatureTableController tableCtrl;
  private SplitPane mainVerticalChartsSplit;

  public CompoundDashboardViewBuilder(@NotNull CompoundDashboardModel model,
      @NotNull CompoundDashboardController controller, @NotNull ChromatogramPlotController eicPlot,
      @NotNull ChromatogramPlotController mobilogramPlot,
      @NotNull SimpleSpectraChartController ms1Chart,
      @NotNull SimpleSpectraChartController ms2Chart,
      @NotNull CompoundRowQualityController qualityCtrl,
      @NotNull FxFeatureTableController tableCtrl) {
    super(model);
    this.controller = controller;
    this.eicPlot = eicPlot;
    this.mobilogramPlot = mobilogramPlot;
    this.ms1Chart = ms1Chart;
    this.ms2Chart = ms2Chart;
    this.qualityCtrl = qualityCtrl;
    this.tableCtrl = tableCtrl;
  }

  @Override
  public Region build() {
    final Region eicWithToolbar = buildEicWithToolbar();
    final Region spectraColumn = buildSpectraColumn();

    final ModularFeatureList flist = model.getFeatureList();
    boolean hasMobilogram = flist != null && flist.hasRowType(FeatureShapeMobilogramType.class);

    mainVerticalChartsSplit = new SplitPane(eicWithToolbar, spectraColumn);
    mainVerticalChartsSplit.setOrientation(Orientation.HORIZONTAL);

    // more width if mobilogram
    mainVerticalChartsSplit.setDividerPositions(hasMobilogram? 0.34 : 0.25);

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

    final HBox toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, prev, rawCombo, next);

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
    final HBox ms2Toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, ms2OfLabel, adductCombo);

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

    final SplitPane sp = new SplitPane(ms1View, mainMS2);
    sp.setOrientation(Orientation.VERTICAL);
    sp.setDividerPositions(0.45);
    return sp;
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

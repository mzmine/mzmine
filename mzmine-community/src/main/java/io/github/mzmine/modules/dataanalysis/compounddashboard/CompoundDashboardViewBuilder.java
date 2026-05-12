package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.CompoundRowQualityController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.ValueAxis;

/**
 * Builds the dashboard layout. Receives the controller for prev/next callbacks and the
 * sub-controllers' built views; the controller is responsible for wiring data flow.
 */
public class CompoundDashboardViewBuilder extends FxViewBuilder<CompoundDashboardModel> {

  private final CompoundDashboardController controller;
  private final ChromatogramPlotController eicPlot;
  private final SimpleSpectraChartController ms1Chart;
  private final SimpleSpectraChartController ms2Chart;
  private final CompoundRowQualityController qualityCtrl;
  private final FxFeatureTableController tableCtrl;

  public CompoundDashboardViewBuilder(@NotNull CompoundDashboardModel model,
      @NotNull CompoundDashboardController controller, @NotNull ChromatogramPlotController eicPlot,
      @NotNull SimpleSpectraChartController ms1Chart,
      @NotNull SimpleSpectraChartController ms2Chart,
      @NotNull CompoundRowQualityController qualityCtrl,
      @NotNull FxFeatureTableController tableCtrl) {
    super(model);
    this.controller = controller;
    this.eicPlot = eicPlot;
    this.ms1Chart = ms1Chart;
    this.ms2Chart = ms2Chart;
    this.qualityCtrl = qualityCtrl;
    this.tableCtrl = tableCtrl;
  }

  @Override
  public Region build() {
    final Region eicWithToolbar = buildEicWithToolbar();
    final Region spectraColumn = buildSpectraColumn();

    final SplitPane chartsSplit = new SplitPane(eicWithToolbar, spectraColumn);
    chartsSplit.setOrientation(Orientation.HORIZONTAL);
    chartsSplit.setDividerPositions(0.25);

    final BorderPane topArea = new BorderPane(chartsSplit);
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

    final HBox toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, prev, rawCombo, next);

    final Region eicView = eicPlot.buildView();
    VBox.setVgrow(eicView, Priority.ALWAYS);
    return FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, toolbar, eicView);
  }

  private @NotNull Region buildSpectraColumn() {
    // MS1 (no extra toolbar; gray background + per-row sticks)
    final Region ms1View = ms1Chart.buildView();

    // MS2 with adduct selector above
    final ComboBox<FeatureListRow> adductCombo = FxComboBox.createComboBox("Adduct (MS2 source)",
        model.getAdductRows(), model.selectedAdductRowProperty());
    adductCombo.setCellFactory(_ -> adductCell());
    adductCombo.setButtonCell(adductCell());
    HBox.setHgrow(adductCombo, Priority.SOMETIMES);
    final HBox ms2Toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, adductCombo);

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
    final VBox ms2Box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, ms2Toolbar, ms2Stack);

    final SplitPane sp = new SplitPane(ms1View, ms2Box);
    sp.setOrientation(Orientation.VERTICAL);
    sp.setDividerPositions(0.5);
    return sp;
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

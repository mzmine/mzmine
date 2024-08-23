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

package io.github.mzmine.modules.visualization.other_correlationdashboard;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.OtherFeatureUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.MzRangeChromatogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.otherdata.align_msother.MsOtherCorrelationResult;
import io.github.mzmine.modules.dataprocessing.otherdata.align_msother.MsOtherCorrelationType;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.OtherFeatureSelectionPane;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.xy.XYDataset;

public class CorrelationDashboardViewBuilder extends FxViewBuilder<CorrelationDashboardModel> {

  private final FeatureTableTab featureTableTab = new FeatureTableTab(null);
  private final FeatureTableFX featureTable = featureTableTab.getFeatureTable();
  private Region uvPlot;
  private Region msPlot;
  private Region correlatedPlot;
  private SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
  private Color otherFeatureColor = palette.getNegativeColorAWT();

  private final ComboBox<OtherFeature> alreadyCorrelatedBox = new ComboBox<>();
  private OtherFeatureSelectionPane otherFeatureSelectionPane;
  private NumberFormats formats = ConfigService.getGuiFormats();

  public CorrelationDashboardViewBuilder(CorrelationDashboardModel model) {
    super(model);
  }

  private static @NotNull List<DatasetAndRenderer> getMsFeatureAndChromatogramDatasets(
      Feature feature, RawDataFile file) {
    final List<DatasetAndRenderer> datasets = new ArrayList<>();
    final IonTimeSeriesToXYProvider featureDataSet = new IonTimeSeriesToXYProvider(feature);
    datasets.add(new DatasetAndRenderer(featureDataSet, new ColoredAreaShapeRenderer()));

    final Range<Double> toleranceRange = new MZTolerance(0.005, 10).getToleranceRange(
        feature.getRawDataPointsMZRange());
    final MzRangeChromatogramProvider chromatogram = new MzRangeChromatogramProvider(toleranceRange,
        feature.getFeatureList().getSeletedScans(file),
        "Chromatogram m/z " + ConfigService.getGuiFormats().mz(toleranceRange), file.getColorAWT());

    datasets.add(new DatasetAndRenderer(chromatogram, new ColoredXYLineRenderer()));
    return datasets;
  }

  @Override
  public Region build() {

    uvPlot = model.getUvPlotController().buildView();
    msPlot = model.getMsPlotController().buildView();
    correlatedPlot = model.getCorrelationPlotController().buildView();

    model.getMsPlotController().setDomainAxisLabel(formats.unit("RT", "min"));
    model.getMsPlotController().setDomainAxisFormat(formats.rtFormat());
    model.getMsPlotController().setRangeAxisFormat(formats.intensityFormat());
    model.getMsPlotController().setRangeAxisLabel(formats.unit("Intensity", "a.u."));

    model.getUvPlotController().setDomainAxisLabel(formats.unit("RT", "min"));
    model.getUvPlotController().setDomainAxisFormat(formats.rtFormat());

    model.getCorrelationPlotController().setRangeAxisLabel("Normalized intensity");
    model.getCorrelationPlotController().setDomainAxisLabel(formats.unit("RT", "min"));
    model.getCorrelationPlotController().setDomainAxisFormat(formats.rtFormat());

    final BorderPane plotsAndControls = new BorderPane();

    // plots and controls on tob and feature table at the bottom
    final SplitPane topBottomSplit = new SplitPane();
    topBottomSplit.setOrientation(Orientation.VERTICAL);
    topBottomSplit.getItems().addAll(plotsAndControls, featureTableTab.getMainPane());

    GridPane plots = new GridPane();
    plots.add(uvPlot, 0, 0);
    plots.add(msPlot, 0, 1);
    GridPane.setHgrow(uvPlot, Priority.SOMETIMES);
    GridPane.setHgrow(msPlot, Priority.SOMETIMES);
    GridPane.setVgrow(uvPlot, Priority.SOMETIMES);
    GridPane.setVgrow(msPlot, Priority.SOMETIMES);

    plotsAndControls.setCenter(plots);

    final DoubleComponent shiftComponent = createShiftComponent();
    final HBox shiftBox = FxLayout.newHBox(FxLabels.newLabel("RT shift:"), shiftComponent,
        FxLabels.newLabel("min"));

    otherFeatureSelectionPane = new OtherFeatureSelectionPane(OtherRawOrProcessed.RAW);
    final HBox alreadyCorrelatedPane = FxLayout.newHBox(FxLabels.newLabel("Correlated features:"),
        alreadyCorrelatedBox);

    final Button btnFilterMsFeatures = FxButtons.createButton("Filter MS features", FxIcons.FILTER,
        "Filter MS features by the retention time range of the selected UV signal (adjusted by RT offset).",
        this::applyUvToMsRtFilter);
    final Button btnSetCorrelated = FxButtons.createButton("Define as correlated",
        this::correlateSelectedFeatures);
    final Button btnClearCorrelation = FxButtons.createButton("Clear selected correlation",
        "Remove the selected UV feature from the selected MS feature",
        this::clearSelectedCorrelation);
    final HBox buttonBox = FxLayout.newHBox(btnFilterMsFeatures, btnSetCorrelated, btnClearCorrelation);

    final VBox controlsAndCorrelation = FxLayout.newVBox(Pos.CENTER_LEFT, Insets.EMPTY, true,
        otherFeatureSelectionPane, shiftBox, buttonBox, alreadyCorrelatedPane, correlatedPlot);
    VBox.setVgrow(correlatedPlot, Priority.SOMETIMES);
    plotsAndControls.setRight(controlsAndCorrelation);

    featureTable.getSelectionModel().selectedItemProperty()
        .addListener((_, _, _) -> updateSelectedRowFromTable());

    featureTable.featureListProperty().bind(model.featureListProperty());
    model.featureListProperty().subscribe(flist -> {
      if (flist == null) {
        return;
      }
      otherFeatureSelectionPane.getRawFiles().setAll(flist.getRawDataFiles());
      otherFeatureSelectionPane.fileProperty().set(flist.getRawDataFile(0));
    });
    otherFeatureSelectionPane.fileProperty().bindBidirectional(model.selectedRawDataFileProperty());
    otherFeatureSelectionPane.featureProperty()
        .addListener((_, _, f) -> model.setSelectedOtherRawTrace(f));

    // updates for the ms feature chart
    model.selectedRowProperty().addListener((_, _, _) -> updateMsPlot());
    model.selectedRawDataFileProperty().addListener((_, _, _) -> updateMsPlot());

    // updates for uv feature chart
    model.selectedOtherFeatureProperty().addListener((_, _, _) -> updateUvChart());
    model.selectedOtherFeatureProperty().addListener((_, _, _) -> updateSelectionInUvChart());
    model.selectedOtherRawTraceProperty().addListener((_, _, _) -> updateUvChart());

    // updates for correlation plot
    model.selectedOtherFeatureProperty().addListener((_, _, _) -> updateCorrelationChart());
    model.selectedRowProperty().addListener((_, _, _) -> updateCorrelationChart());
    model.selectedRawDataFileProperty().addListener((_, _, _) -> updateCorrelationChart());
    model.uvToMsRtOffsetProperty().addListener((_, _, _) -> updateCorrelationChart());

    // update the alreadyCorrelatedBox
    model.selectedRowProperty().addListener((_, _, _) -> updateAlreadyCorrelatedBox());
    alreadyCorrelatedBox.valueProperty().addListener((_, _, corr) -> {
      if (corr != null) {
        otherFeatureSelectionPane.setFeature(corr.get(RawTraceType.class));
        model.setSelectedOtherFeature(corr);
      }
    });

    return new BorderPane(topBottomSplit);
  }

  private void updateSelectionInUvChart() {
    final OtherFeature selected = model.getSelectedOtherFeature();
    var pos = model.getUvPlotController().cursorPositionProperty().get();
    if (selected != null && pos != null) {
      final Optional<XYDataset> optionalDs = model.getUvPlotController().getDatasetRenderers()
          .keySet().stream().filter(ds -> ds instanceof ColoredXYDataset cds
              // find correct dataset
              && cds.getValueProvider() instanceof OtherFeatureDataProvider ofdp
              && ofdp.getFeature() == selected
              // not already selected
              && pos.getDataset() != cds).findFirst();
      if (optionalDs.isPresent()) {
        var ds = optionalDs.get();
        final Float rt = selected.getRT();
        final Float height = selected.get(HeightType.class);
        final int index = BinarySearch.binarySearch(rt.doubleValue(), DefaultTo.CLOSEST_VALUE,
            ds.getItemCount(0), i -> ds.getXValue(0, i));
        model.getUvPlotController().cursorPositionProperty()
            .set(new PlotCursorPosition(rt, height, index, ds));
      }
    }
    if (selected == null) {
      model.getUvPlotController().cursorPositionProperty().set(null);
    }
  }

  private void updateSelectedRowFromTable() {
    if (featureTable.getSelectedRow() == null) {
      model.setSelectedRow(null);
      return;
    }
    model.setSelectedRow(featureTable.getSelectedRow());
  }

  private void updateAlreadyCorrelatedBox() {
    alreadyCorrelatedBox.getSelectionModel().clearSelection();
    final FeatureListRow row = model.getSelectedRow();
    final RawDataFile raw = model.getSelectedRawDataFile();

    if (row == null || raw == null) {
      alreadyCorrelatedBox.setItems(FXCollections.emptyObservableList());
      return;
    }
    final ModularFeature feature = (ModularFeature) row.getFeature(raw);
    if (feature == null) {
      alreadyCorrelatedBox.setItems(FXCollections.emptyObservableList());
      return;
    }

    final List<MsOtherCorrelationResult> results = feature.get(MsOtherCorrelationResultType.class);
    if (results == null || results.isEmpty()) {
      alreadyCorrelatedBox.setItems(FXCollections.emptyObservableList());
      return;
    }

    final List<OtherFeature> correlated = results.stream()
        .map(MsOtherCorrelationResult::otherFeature).toList();
    alreadyCorrelatedBox.setItems(FXCollections.observableArrayList(correlated));
    alreadyCorrelatedBox.getSelectionModel().selectFirst();
  }

  private @NotNull DoubleComponent createShiftComponent() {
    DoubleComponent shiftCompoment = new DoubleComponent(40, Double.MIN_VALUE, Double.MAX_VALUE,
        ConfigService.getGuiFormats().rtFormat(), 0d);
    Bindings.bindBidirectional(shiftCompoment.getTextField().textProperty(),
        model.uvToMsRtOffsetProperty(), new StringConverter<>() {
          @Override
          public String toString(Number object) {
            return object != null ? object.toString() : "";
          }

          @Override
          public Number fromString(String string) {
            if (string == null) {
              return 0d;
            }
            string = string.replaceAll("[^0-9\\.]+", "").trim();
            return !string.isEmpty() ? Double.parseDouble(string) : 0;
          }
        });
    return shiftCompoment;
  }

  /**
   * clears the uv plot and puts the raw trace and all features in that raw trace into the chart.
   */
  private void updateUvChart() {
    model.getUvPlotController().clearDatasets();
    final OtherFeature trace = model.getSelectedOtherRawTrace();
    if (trace == null) {
      return;
    }

    final OtherTimeSeriesData data = trace.getOtherDataFile().getOtherTimeSeries();
    model.getUvPlotController().setRangeAxisLabel(
        formats.unit(data.getTimeSeriesRangeLabel(), data.getTimeSeriesRangeUnit()));

    final List<OtherFeature> processed = data.getProcessedFeaturesForTrace(trace);

    palette = ConfigService.getDefaultColorPalette();
    List<DatasetAndRenderer> datasets = new ArrayList<>();
    datasets.add(new DatasetAndRenderer(new OtherFeatureDataProvider(trace, otherFeatureColor),
        new ColoredXYLineRenderer()));
    processed.forEach(p -> datasets.add(
        new DatasetAndRenderer(new OtherFeatureDataProvider(p, otherFeatureColor),
            new ColoredAreaShapeRenderer())));

    model.getUvPlotController().addDatasets(datasets);
  }

  /**
   * clears the plot and puts the selected feature and other trace into the chart.
   */
  private void updateCorrelationChart() {
    final ChromatogramPlotController controller = model.getCorrelationPlotController();
    controller.clearDatasets();

    final FeatureListRow row = model.getSelectedRow();
    final RawDataFile file = model.getSelectedRawDataFile();
    final OtherFeature other = model.getSelectedOtherFeature();

    final OtherFeature shifted = Double.compare(model.uvToMsRtOffsetProperty().get(), 0d) != 0
        ? OtherFeatureUtils.shiftRtAxis(null, other, model.uvToMsRtOffsetProperty().floatValue())
        : other;

    final List<DatasetAndRenderer> datasets = new ArrayList<>();
    if (other != null) {
      datasets.add(new DatasetAndRenderer(
          new OtherFeatureDataProvider(shifted, otherFeatureColor, 1 / other.get(HeightType.class)),
          new ColoredXYLineRenderer()));
    }
    if (row != null && file != null && row.getFeature(file) != null) {
      final Feature feature = row.getFeature(file);
      datasets.add(
          new DatasetAndRenderer(new IonTimeSeriesToXYProvider(feature, 1 / feature.getHeight()),
              new ColoredAreaShapeRenderer()));
    }
    controller.addDatasets(datasets);
  }

  /**
   * clears the ms plot and updates it to the currently selected row and feature.
   */
  private void updateMsPlot() {
    model.getMsPlotController().clearDatasets();
    final RawDataFile file = model.getSelectedRawDataFile();
    final FeatureListRow row = model.getSelectedRow();
    if (row == null || file == null) {
      return;
    }
    final Feature feature = row.getFeature(file);
    if (feature == null) {
      return;
    }
    // todo update titles
    final List<DatasetAndRenderer> datasets = getMsFeatureAndChromatogramDatasets(feature, file);
    model.msPlotControllerProperty().get().addDatasets(datasets);
  }

  private void applyUvToMsRtFilter() {
    final OtherFeature selected = model.getSelectedOtherFeature();
    if (selected == null) {
      return;
    }

    final double offset = model.getUvToMsRtOffset();
    final Range<Float> rtRange = selected.getRtRange();
    final Range<Double> rangeWithOffset = Range.closed(rtRange.lowerEndpoint() + offset,
        rtRange.upperEndpoint() + offset);

    featureTableTab.getController().setRtFilterString(formats.rt(rangeWithOffset));
  }

  private void clearSelectedCorrelation() {
    final FeatureListRow selectedRow = model.selectedRowProperty().get();
    final OtherFeature selectedOtherFeature = model.getSelectedOtherFeature();
    final RawDataFile raw = model.getSelectedRawDataFile();
    if (selectedRow == null || selectedOtherFeature == null || raw == null) {
      return;
    }
    final ModularFeature feature = (ModularFeature) selectedRow.getFeature(raw);
    if (feature == null) {
      return;
    }
    final List<MsOtherCorrelationResult> results = feature.get(MsOtherCorrelationResultType.class);
    final MsOtherCorrelationResult result = results.stream()
        .filter(r -> r.otherFeature() == selectedOtherFeature).findFirst().orElse(null);
    if (result == null) {
      return;
    }
    final List<MsOtherCorrelationResult> filtered = new ArrayList<>(results);
    filtered.remove(result);
    feature.set(MsOtherCorrelationResultType.class, filtered);
    featureTable.refresh();
  }

  private void correlateSelectedFeatures() {
    final FeatureListRow selectedRow = model.selectedRowProperty().get();
    final OtherFeature selectedOtherFeature = model.getSelectedOtherFeature();
    final RawDataFile raw = model.getSelectedRawDataFile();
    if (selectedRow == null || selectedOtherFeature == null || raw == null) {
      return;
    }
    final ModularFeature feature = (ModularFeature) selectedRow.getFeature(raw);
    if (feature == null) {
      return;
    }
    final List<MsOtherCorrelationResult> results = feature.get(MsOtherCorrelationResultType.class);
    if (results != null && results.stream().anyMatch(r -> r.otherFeature() == selectedOtherFeature)) {
      DialogLoggerUtil.showMessageDialog("Already correlated",
          "Selected UV feature is already correlated to MS feature.");
      return;
    }
    final List<MsOtherCorrelationResult> newResults = new ArrayList<>();
    newResults.add(
        new MsOtherCorrelationResult(selectedOtherFeature, MsOtherCorrelationType.MANUAL));
    if(results != null) {
      newResults.addAll(results);
    }
    feature.set(MsOtherCorrelationResultType.class, newResults);
    featureTable.refresh();
  }
}

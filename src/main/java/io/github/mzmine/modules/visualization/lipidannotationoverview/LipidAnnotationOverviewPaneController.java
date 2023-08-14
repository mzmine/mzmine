package io.github.mzmine.modules.visualization.lipidannotationoverview;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.FeatureRawMobilogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotChart;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickPlotDataTypes;
import io.github.mzmine.modules.visualization.lipidannotationoverview.lipidbarchartplot.LipidAnnotationSunburstPlot;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidSpectrumTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;

public class LipidAnnotationOverviewPaneController {

  public BorderPane featureTablePane;
  public Tab eicTab;
  public Tab matchtedLipidSpectrumTab;
  public BorderPane msOne;
  public BorderPane matchedMSMS;
  public Tab kendrickPlotTab;
  public BorderPane lipidIDsPane;
  public BorderPane bestLipidIDsPane;

  private FeatureTableFX featureTable;
  private KendrickMassPlotChart kendrickMassPlotChart;
  private SpectraPlot spectrumPlot;

  private FeatureTableFX internalFeatureTable;

  private List<FeatureListRow> rowsWithLipidID;
  private ObservableList<ModularFeatureListRow> focussedRows;

  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();


  @FXML
  public void initialize(List<ModularFeatureListRow> rows, List<ModularFeature> selectedFeatures,
      FeatureTableFX table) {
    this.focussedRows = FXCollections.observableArrayList();
    this.featureTable = table;
    initSelectedRows();
    focussedRows.setAll(rows);
    // filter rows for lipid annotations
    rowsWithLipidID = table.getFeatureList().getRows().stream()
        .filter(row -> rowHasMatchedLipidSignals((ModularFeatureListRow) row))
        .collect(Collectors.toList());

    // create internal table
    createInternalTable(table.getFeatureList());
    linkFeatureTableSelections(internalFeatureTable, featureTable);

    //kendrick plot
    buildKendrickMassPlot();

    //MS1 plot
    buildMsSpectrum(rows.get(0).getBestFeature());

    //EIC
    buildEic(rows.get(0).getBestFeature());

    //Matched Lipid signals plot
    buildMatchedLipidSpectrum(focussedRows);

    //Lipid ID summary as bar chart
    buildTotalLipidIDSunburstPlot();

  }

  private void buildEic(ModularFeature bestFeature) {
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(1,
        bestFeature.getRawDataFile().getDataRTRange(1));

    // mz range
    Range<Double> mzRange = null;
    mzRange = bestFeature.getRawDataPointsMZRange();
    // optimize output by extending the range
    double upper = mzRange.upperEndpoint();
    double lower = mzRange.lowerEndpoint();
    double fiveppm = (upper * 5E-6);
    mzRange = Range.closed(lower - fiveppm, upper + fiveppm);

    // labels
    labelsMap.put(bestFeature, bestFeature.toString());

    // get EIC window
    TICVisualizerTab window = new TICVisualizerTab(new RawDataFile[]{bestFeature.getRawDataFile()},
        // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        null,
        // new Feature[] {peak}, // selected features
        labelsMap); // labels

    // get EIC Plot
    TICPlot ticPlot = window.getTICPlot();
    // ticPlot.setPreferredSize(new Dimension(600, 200));
    ticPlot.getChart().getLegend().setVisible(false);

    SplitPane ticAndMobilogram = new SplitPane();
    ticAndMobilogram.setOrientation(Orientation.VERTICAL);
    ticAndMobilogram.getItems().add(ticPlot);

    if (bestFeature.getFeatureData() instanceof IonMobilogramTimeSeries series
        && bestFeature.getMostIntenseFragmentScan() instanceof MergedMsMsSpectrum mergedMsMs) {
      SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
      SimpleXYChart<PlotXYDataProvider> mobilogramChart = createMobilogramChart(bestFeature,
          mzRange, palette, series, mergedMsMs);
      ticAndMobilogram.getItems().add(mobilogramChart);
    }

    eicTab.setContent(ticAndMobilogram);
  }

  private SimpleXYChart<PlotXYDataProvider> createMobilogramChart(ModularFeature peak,
      Range<Double> mzRange, SimpleColorPalette palette, IonMobilogramTimeSeries series,
      MergedMsMsSpectrum mergedMsMs) {
    SimpleXYChart<PlotXYDataProvider> mobilogramChart = new SimpleXYChart<>();
    mobilogramChart.addDataset(new FeatureRawMobilogramProvider(peak, mzRange));
    mobilogramChart.addDataset(new SummedMobilogramXYProvider(series.getSummedMobilogram(),
        new SimpleObjectProperty<>(
            ColorUtils.getContrastPaletteColor(peak.getRawDataFile().getColor(), palette)),
        FeatureUtils.featureToString(peak)));
    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(peak.getMobilityUnit().getAxisLabel());
    mobilogramChart.setRangeAxisLabel(unitFormat.format("Summed intensity", "a.u."));

    var optMin = mergedMsMs.getSourceSpectra().stream()
        .mapToDouble(s -> ((MobilityScan) s).getMobility()).min();
    var optMax = mergedMsMs.getSourceSpectra().stream()
        .mapToDouble(s -> ((MobilityScan) s).getMobility()).max();
    if (optMin.isPresent() && optMax.isPresent()) {
      IntervalMarker msmsInterval = new IntervalMarker(optMin.getAsDouble(), optMax.getAsDouble(),
          palette.getPositiveColorAWT(), new BasicStroke(1f), palette.getPositiveColorAWT(),
          new BasicStroke(1f), 0.2f);
      mobilogramChart.getXYPlot().addDomainMarker(msmsInterval);
    }
    return mobilogramChart;
  }

  private void createInternalTable(final @NotNull ModularFeatureList featureList) {
    FeatureTableTab tempTab = new FeatureTableTab(featureList);
    internalFeatureTable = tempTab.getFeatureTable();
    internalFeatureTable.getNewColumnMap();
    featureTablePane.setCenter(tempTab.getMainPane());
  }

  private void linkFeatureTableSelections(final @NotNull FeatureTableFX internal,
      final @Nullable FeatureTableFX external) {

    if (internalFeatureTable != null) {
      internalFeatureTable.getSelectedTableRows()
          .addListener((ListChangeListener<? super TreeItem<ModularFeatureListRow>>) c -> {
            var list = c.getList().stream().map(TreeItem::getValue).toList();
            focussedRows.setAll(list);
            updatePlots();
          });
    }
    if (external != null) {
      external.getSelectedTableRows()
          .addListener((ListChangeListener<? super TreeItem<ModularFeatureListRow>>) c -> {
            var list = c.getList().stream().map(TreeItem::getValue).toList();
            focussedRows.setAll(list);
            updatePlots();
          });
    }
  }

  private void buildTotalLipidIDSunburstPlot() {

    List<MatchedLipid> matchedLipids = new ArrayList<>();
    for (FeatureListRow featureListRow : rowsWithLipidID) {
      if (featureListRow instanceof ModularFeatureListRow) {
        matchedLipids.addAll(featureListRow.get(LipidMatchListType.class));
      }
    }

    LipidAnnotationSunburstPlot lipidAnnotationSunburstPlotAllLipids = new LipidAnnotationSunburstPlot(
        matchedLipids, true, true, true, false);
    Text titleLipids = new Text("All Lipid Annotations");
    lipidIDsPane.setCenter(lipidAnnotationSunburstPlotAllLipids.getSunburstChart());
    lipidIDsPane.setTop(titleLipids);

    List<MatchedLipid> bestLipidMatches = new ArrayList<>();
    for (FeatureListRow featureListRow : rowsWithLipidID) {
      if (featureListRow instanceof ModularFeatureListRow) {
        bestLipidMatches.add(featureListRow.get(LipidMatchListType.class).get(0));
      }
    }

    LipidAnnotationSunburstPlot lipidAnnotationSunburstPlotBestLipids = new LipidAnnotationSunburstPlot(
        bestLipidMatches, true, true, true, false);
    Text titleBestLipids = new Text("Unique Lipid Annotations");
    bestLipidIDsPane.setCenter(lipidAnnotationSunburstPlotBestLipids.getSunburstChart());
    bestLipidIDsPane.setTop(titleBestLipids);
  }

  private void buildKendrickMassPlot() {
    kendrickMassPlotChart = buildKendrickMassPlotChart();
    addChartListener();
    kendrickPlotTab.setContent(kendrickMassPlotChart);
  }

  private void addChartListener() {
    kendrickMassPlotChart.addChartMouseListener(new ChartMouseListenerFX() {

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
      }

      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        XYPlot plot = kendrickMassPlotChart.getChart().getXYPlot();
        double xValue = plot.getDomainCrosshairValue();
        double yValue = plot.getRangeCrosshairValue();
        KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
        double[] xValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < xValues.length; i++) {
          if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY)) && (
              event.getTrigger().getClickCount() == 1)) {
            if (dataset.getX(0, i).doubleValue() == xValue
                && dataset.getY(0, i).doubleValue() == yValue) {
              focussedRows.clear();
              focussedRows.setAll((ModularFeatureListRow) rowsWithLipidID.get(i));
              updatePlots();
            }
          }
        }
      }
    });
  }

  private boolean rowHasMatchedLipidSignals(ModularFeatureListRow row) {
    List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    return matches != null && !matches.isEmpty();
  }


  private void buildMatchedLipidSpectrum(List<ModularFeatureListRow> rows) {
    List<MatchedLipid> matchedLipids = rows.get(0).get(LipidMatchListType.class);
    if (!matchedLipids.isEmpty()) {
      MatchedLipidSpectrumTab matchedLipidSpectrumTab = new MatchedLipidSpectrumTab(
          matchedLipids.get(0).getLipidAnnotation().getAnnotation() + " Matched Signals",
          new LipidSpectrumChart(rows.get(0), null, false));
      matchedMSMS.setCenter(matchedLipidSpectrumTab.getContent());
    } else {
      matchedMSMS.setCenter(null);
    }
  }

  private void buildMsSpectrum(ModularFeature feature) {
    SpectraVisualizerTab spectrumTab = buildSpectrumTab(feature.getRawDataFile(), feature);
    spectrumPlot = spectrumTab.getSpectrumPlot();
    msOne.setCenter(spectrumPlot);
  }

  private void initSelectedRows() {
    if (featureTable != null) {
      featureTable.getSelectedTableRows()
          .addListener((ListChangeListener<? super TreeItem<ModularFeatureListRow>>) c -> {
            var list = c.getList().stream().map(TreeItem::getValue).toList();
            focussedRows.setAll(list);
            updatePlots();
          });
    }
  }

  private void updatePlots() {
    buildMsSpectrum(focussedRows.get(0).getBestFeature());
    buildMatchedLipidSpectrum(focussedRows);
    buildEic(focussedRows.get(0).getBestFeature());
    updateKendrickCorssHair(focussedRows.get(0));
  }

  private void updateKendrickCorssHair(ModularFeatureListRow row) {
    XYPlot plot = kendrickMassPlotChart.getChart().getXYPlot();
    double xValue = row.getAverageMZ();
    KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
    double[] xValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < xValues.length; i++) {
      if (dataset.getX(0, i).doubleValue() == xValue) {
        plot.setDomainCrosshairValue(xValue);
        plot.setRangeCrosshairValue(dataset.getYValue(0, i));
      }
    }
  }

  private SpectraVisualizerTab buildSpectrumTab(RawDataFile dataFile, Feature peak) {

    Scan scan = peak.getRepresentativeScan();
    IsotopePattern detectedPattern = peak.getIsotopePattern();

    if (scan == null) {
      MZmineCore.getDesktop()
          .displayErrorMessage("Raw data file " + dataFile + " does not contain the given scan.");
      return null;
    }

    SpectraVisualizerTab newTab = new SpectraVisualizerTab(dataFile, scan, true);
    newTab.loadRawData(scan);

    if (peak != null) {
      newTab.loadSinglePeak(peak);
    }

    Range<Double> zoomMzRange = null;

    if (detectedPattern != null) {
      newTab.loadIsotopes(detectedPattern);
      zoomMzRange = detectedPattern.getDataPointMZRange();
    }

    if (zoomMzRange != null) {
      // zoom to the isotope pattern
      newTab.getSpectrumPlot().getXYPlot().getDomainAxis()
          .setRange(zoomMzRange.lowerEndpoint() - 3, zoomMzRange.upperEndpoint() + 3);
      ChartLogicsFX.autoRangeAxis(newTab.getSpectrumPlot());
    }

    return newTab;
  }

  private KendrickMassPlotChart buildKendrickMassPlotChart() {
    //init a dataset
    KendrickMassPlotParameters kendrickMassPlotParameters = new KendrickMassPlotParameters();
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.xAxisValues,
        KendrickPlotDataTypes.M_OVER_Z);
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase,
        "CH2");
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.yAxisValues,
        KendrickPlotDataTypes.KENDRICK_MASS_DEFECT);
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase,
        "H");
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.colorScaleValues,
        KendrickPlotDataTypes.RETENTION_TIME);
    kendrickMassPlotParameters.setParameter(
        KendrickMassPlotParameters.colorScaleCustomKendrickMassBase, "H");
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.bubbleSizeValues,
        KendrickPlotDataTypes.INTENSITY);
    kendrickMassPlotParameters.setParameter(
        KendrickMassPlotParameters.bubbleSizeCustomKendrickMassBase, "H");
    KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset = new KendrickMassPlotXYZDataset(
        kendrickMassPlotParameters, rowsWithLipidID);
    return new KendrickMassPlotChart("Kendrick Mass Plot", "m/z", "KMD (H)", "Retention time",
        kendrickMassPlotXYZDataset);
  }

  public KendrickMassPlotChart getKendrickMassPlotChart() {
    return kendrickMassPlotChart;
  }

  public FeatureTableFX getFeatureTable() {
    return featureTable;
  }

}

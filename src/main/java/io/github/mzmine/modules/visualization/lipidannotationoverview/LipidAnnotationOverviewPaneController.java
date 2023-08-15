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
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SpeciesLevelAnnotation;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotChart;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickPlotDataTypes;
import io.github.mzmine.modules.visualization.lipidannotationoverview.lipidannotationoverviewplots.EquivalentCarbonNumberChart;
import io.github.mzmine.modules.visualization.lipidannotationoverview.lipidannotationoverviewplots.EquivalentCarbonNumberDataset;
import io.github.mzmine.modules.visualization.lipidannotationoverview.lipidannotationoverviewplots.LipidAnnotationSunburstPlot;
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
  public BorderPane eicPane;
  public BorderPane ecnPane;
  public Tab matchtedLipidSpectrumTab;
  public BorderPane msOne;
  public BorderPane matchedMSMS;
  public Tab kendrickPlotTab;
  public BorderPane lipidIDsPane;
  public BorderPane bestLipidIDsPane;

  private FeatureTableFX featureTable;
  private KendrickMassPlotChart kendrickMassPlotChart;
  private EquivalentCarbonNumberChart equivalentCarbonNumberChart;
  private SpectraPlot spectrumPlot;

  private FeatureTableFX internalFeatureTable;

  private List<FeatureListRow> rowsWithLipidID;
  private ObservableList<ModularFeatureListRow> focussedRows;

  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
  private EquivalentCarbonNumberDataset ecnDataset;


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

    buildEcnModelPlot();

    //MS1 plot
    buildMsSpectrum(rows.get(0).getBestFeature());

    //EIC
    buildEic(rows.get(0).getBestFeature());

    //Matched Lipid signals plot
    buildMatchedLipidSpectrum(focussedRows);

    //Lipid ID summary as bar chart
    buildTotalLipidIDSunburstPlot();

  }

  private void buildEcnModelPlot() {
    MSMSLipidTools msmsLipidTools = new MSMSLipidTools();
    int numberOfDBEs = 0;
    if (focussedRows.get(0).getLipidMatches().get(0)
        .getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation molecularAnnotation) {
      numberOfDBEs = msmsLipidTools.getCarbonandDBEFromLipidAnnotaitonString(
          molecularAnnotation.getAnnotation()).getValue();
    } else if (focussedRows.get(0).getLipidMatches().get(0)
        .getLipidAnnotation() instanceof SpeciesLevelAnnotation) {
      numberOfDBEs = ((SpeciesLevelAnnotation) focussedRows.get(0).getLipidMatches().get(0)
          .getLipidAnnotation()).getNumberOfDBEs();
    }
    ecnDataset = new EquivalentCarbonNumberDataset(focussedRows,
        rowsWithLipidID.toArray(new FeatureListRow[0]),
        focussedRows.get(0).getLipidMatches().get(0).getLipidAnnotation().getLipidClass(),
        numberOfDBEs);
    equivalentCarbonNumberChart = new EquivalentCarbonNumberChart("ECN Model", "Retention time",
        "Number of Carbons", ecnDataset);
    ecnPane.setCenter(equivalentCarbonNumberChart);
    addEcnChartListener();
  }

  private void buildEic(ModularFeature bestFeature) {
    List<FeatureListRow> featureListRows = null;
    // if (ecnDataset != null) {
    //   featureListRows = ecnDataset.getLipidsForDBERows();
    // }
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(1,
        bestFeature.getRawDataFile().getDataRTRange(1));

    // mz range
    Range<Double> mzRange = null;
    mzRange = bestFeature.getRawDataPointsMZRange();

    //List<Feature> features = featureListRows.stream().map(FeatureListRow::getBestFeature).toList();
    List<Feature> features = new ArrayList<>();
    features.add(bestFeature);
    // labels
    features.forEach(feature -> labelsMap.put(feature, feature.toString()));

    // get EIC window
    TICVisualizerTab window = new TICVisualizerTab(new RawDataFile[]{bestFeature.getRawDataFile()},
        // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        features,// selected features
        labelsMap); // labels

    // get EIC Plot
    TICPlot ticPlot = window.getTICPlot();
    ticPlot.switchItemLabelsVisible();
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

    eicPane.setCenter(ticAndMobilogram);
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
    addKendrickChartListener();
    kendrickPlotTab.setContent(kendrickMassPlotChart);
  }

  private void addKendrickChartListener() {
    kendrickMassPlotChart.addChartMouseListener(new ChartMouseListenerFX() {

//      Double oldSize = null;
//      Integer index = null;
//      Double highlightSize = null;
//
//      @Override
//      public void chartMouseMoved(ChartMouseEventFX event) {
//        MouseEvent mouseEvent = event.getTrigger();
//        XYPlot plot = kendrickMassPlotChart.getChart().getXYPlot();
//        Rectangle2D plotArea = kendrickMassPlotChart.getRenderingInfo().getPlotInfo().getPlotArea();
//        double xValue = plot.getDomainAxis()
//            .java2DToValue(mouseEvent.getX(), plotArea, plot.getDomainAxisEdge());
//        double yValue = plot.getRangeAxis()
//            .java2DToValue(mouseEvent.getY(), plotArea, plot.getRangeAxisEdge());
//        KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
//        double[] xValues = new double[dataset.getItemCount(0)];
//        if (oldSize != null && index != null) {
//          dataset.setBubbleSize(index, oldSize);
//          oldSize = null;
//          index = null;
//          kendrickMassPlotChart.getChart().getXYPlot().getRendererForDataset(dataset)
//              .setSeriesItemLabelsVisible(0, false);
//        }
//        for (int i = 0; i < xValues.length; i++) {
//          // Calculate threshold values based on the size of the data point
//          double thresholdX = calculateThreshold(plotArea.getWidth(),
//              dataset.getX(0, i).doubleValue());
//          double thresholdY = calculateThreshold(plotArea.getHeight(),
//              dataset.getY(0, i).doubleValue());
//
//          double xDiff = Math.abs(dataset.getX(0, i).doubleValue() - xValue);
//          double yDiff = Math.abs(dataset.getY(0, i).doubleValue() - yValue);
//          if (xDiff <= thresholdX && yDiff <= thresholdY) {
//            System.out.println("X Diff: " + xDiff + "\n" + "y Diff: " + yDiff);
//            System.out.println(
//                "X value mouse  : " + xValue + "\n" + "x Value dataset: " + dataset.getX(0, i)
//                    .doubleValue());
//            System.out.println(
//                "Y value mouse  : " + yValue + "\n" + "y Value dataset: " + dataset.getY(0, i)
//                    .doubleValue());
//            highlightSize = Arrays.stream(dataset.getBubbleSizeValues()).max().getAsDouble();
//            oldSize = dataset.getBubbleSize(0, i);
//            index = i;
//            dataset.setBubbleSize(i, highlightSize);
//            kendrickMassPlotChart.getChart().getXYPlot().getRendererForDataset(dataset)
//                .setSeriesItemLabelsVisible(0, false);
//            break;
//          }
//        }
//      }

      private double calculateThreshold(double plotSize, double dataValue) {
        // Calculate the threshold based on a fraction of the data point size
        double fraction = 0.05; // Adjust this value as needed
        double dataRange = plotSize * fraction;
        return Math.abs(dataRange / dataValue);
      }
//      public void chartMouseMoved(ChartMouseEventFX event) {
//        MouseEvent mouseEvent = event.getTrigger();
//        XYPlot plot = kendrickMassPlotChart.getChart().getXYPlot();
//        Rectangle2D plotBounds = new Rectangle2D.Double(plot.getDomainAxis().getLowerBound(),
//            plot.getRangeAxis().getLowerBound(),
//            plot.getDomainAxis().getUpperBound() - plot.getDomainAxis().getLowerBound(),
//            plot.getRangeAxis().getUpperBound() - plot.getRangeAxis().getLowerBound());
//        double xValue = plot.getDomainAxis()
//            .java2DToValue(mouseEvent.getX(), plotBounds, plot.getDomainAxisEdge());
//        double yValue = plot.getRangeAxis()
//            .java2DToValue(mouseEvent.getY(), plotBounds, plot.getRangeAxisEdge());
//        KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
//        double[] xValues = new double[dataset.getItemCount(0)];
//        if (oldSize != null && index != null) {
//          dataset.setBubbleSize(index, oldSize);
//          oldSize = null;
//          index = null;
//          kendrickMassPlotChart.getChart().getXYPlot().getRendererForDataset(dataset)
//              .setSeriesItemLabelsVisible(0, false);
//        }
//        for (int i = 0; i < xValues.length; i++) {
//          double xDiff = Math.abs(dataset.getX(0, i).doubleValue() - xValue);
//          double yDiff = Math.abs(dataset.getY(0, i).doubleValue() - yValue);
//          System.out.println("X Diff: " + xDiff + "\n" + "y Diff: " + yDiff);
//          System.out.println(
//              "X value plot  : " + xValue + "\n" + "x Value mouse: " + dataset.getX(0, i)
//                  .doubleValue());
//          System.out.println(
//              "Y value plot  : " + yValue + "\n" + "y Value mouse: " + dataset.getY(0, i)
//                  .doubleValue());
//          if (xDiff <= threshold && yDiff <= threshold) {
//            highlightSize = Arrays.stream(dataset.getBubbleSizeValues()).max().getAsDouble();
//            oldSize = dataset.getBubbleSize(0, i);
//            index = i;
//            dataset.setBubbleSize(i, highlightSize);
//            kendrickMassPlotChart.getChart().getXYPlot().getRendererForDataset(dataset)
//                .setSeriesItemLabelsVisible(0, false);
//            break;
//          }
//        }
//      }

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
              break;
            }
          }
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {

      }
    });

  }

  private void addEcnChartListener() {
    equivalentCarbonNumberChart.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        XYPlot plot = equivalentCarbonNumberChart.getChart().getXYPlot();
        double xValue = plot.getDomainCrosshairValue();
        double yValue = plot.getRangeCrosshairValue();
        EquivalentCarbonNumberDataset dataset = (EquivalentCarbonNumberDataset) plot.getDataset();
        double[] xValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < xValues.length; i++) {
          if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY)) && (
              event.getTrigger().getClickCount() == 1)) {
            if (dataset.getX(0, i).doubleValue() == xValue
                && dataset.getY(0, i).doubleValue() == yValue) {
              focussedRows.clear();

              int finalI = i;
              List<FeatureListRow> list = rowsWithLipidID.stream()
                  .filter(featureListRow -> featureListRow instanceof ModularFeatureListRow).filter(
                      featureListRow -> featureListRow.get(LipidMatchListType.class)
                          .contains(dataset.getMatchedLipid(finalI))).toList();
              focussedRows.setAll((ModularFeatureListRow) list.get(0));
              updatePlots();
              break;
            }
          }
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {

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
    updateCrossHair(focussedRows.get(0));
    buildEcnModelPlot();
  }

  private void updateCrossHair(ModularFeatureListRow row) {
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

//    XYPlot ecnPlot = equivalentCarbonNumberChart.getChart().getXYPlot();
//    double ecnxValue = row.getAverageMZ();
//    EquivalentCarbonNumberDataset ecnDataset = (EquivalentCarbonNumberDataset) ecnPlot.getDataset();
//    double[] ecnxValues = new double[ecnDataset.getItemCount(0)];
//    for (int i = 0; i < ecnxValues.length; i++) {
//      if (ecnDataset.getX(0, i).doubleValue() == ecnxValue) {
//        ecnPlot.setDomainCrosshairValue(ecnxValue);
//        ecnPlot.setRangeCrosshairValue(ecnDataset.getYValue(0, i));
//      }
//    }
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

}

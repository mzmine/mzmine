package io.github.mzmine.modules.visualization.lipidannotationoverview;

import com.google.common.collect.Range;
import eu.hansolo.fx.charts.SunburstChart;
import eu.hansolo.fx.charts.SunburstChartBuilder;
import eu.hansolo.fx.charts.data.ChartItem;
import eu.hansolo.fx.charts.data.TreeNode;
import eu.hansolo.fx.charts.tools.TextOrientation;
import eu.hansolo.fx.charts.tools.VisibleData;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotChart;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickPlotDataTypes;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidSpectrumTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.MultiSpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;

public class LipidAnnotationOverviewPaneController {

  public BorderPane featureTablePane;
  public Tab allMsMsTab;
  public Tab matchtedLipidSpectrumTab;
  public BorderPane msOne;
  public BorderPane matchedMSMS;
  public Tab kendrickPlotTab;
  public BorderPane lipidIDsPane;

  private FeatureTableFX featureTable;
  private KendrickMassPlotChart kendrickMassPlotChart;
  private SpectraPlot spectrumPlot;

  private FeatureTableFX internalFeatureTable;

  private List<FeatureListRow> rowsWithLipidID;
  private ObservableList<ModularFeatureListRow> focussedRows;


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

    //Matched Lipid signals plot
    buildMatchedLipidSpectrum(focussedRows);

    //Lipid ID summary as bar chart
    buildTotalLipidIDBarChartPlot();

    // build all MS/MS and EIC plots

    buildAllMsMs(rows);
  }

  private void createInternalTable(final @NotNull ModularFeatureList featureList) {
    FeatureTableTab tempTab = new FeatureTableTab(featureList);
    internalFeatureTable = tempTab.getFeatureTable();
    internalFeatureTable.getNewColumnMap();
    featureTablePane.setCenter(tempTab.getMainPane());
  }

//  void updateWindowToParameterSetValues() {
//    featureTable.updateColumnsVisibilityParameters(
//        param.getParameter(FeatureTableFXParameters.showRowTypeColumns).getValue(),
//        param.getParameter(FeatureTableFXParameters.showFeatureTypeColumns).getValue());
//  }

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
          });
    }
  }

  private void buildTotalLipidIDBarChartPlot() {

    List<MatchedLipid> matchedLipids = new ArrayList<>();
    for (FeatureListRow featureListRow : rowsWithLipidID) {
      if (featureListRow instanceof ModularFeatureListRow) {
        matchedLipids.addAll(featureListRow.get(LipidMatchListType.class));
      }
    }

    TreeNode<ChartItem> tree = buildTreeDataset(matchedLipids);
    SunburstChart sunburstChart = SunburstChartBuilder.create().prefSize(400, 400).tree(tree)
        .textOrientation(TextOrientation.TANGENT).useColorFromParent(false)
        .visibleData(VisibleData.NAME).backgroundColor(Color.WHITE).textColor(Color.WHITE)
        .decimals(1).interactive(true).build();
    lipidIDsPane.setCenter(sunburstChart);
  }

  private TreeNode<ChartItem> buildTreeDataset(List<MatchedLipid> matchedLipids) {
    TreeNode<ChartItem> tree = new TreeNode<>(new ChartItem("Lipids"));

    Map<LipidCategories, Map<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>>> lipidCategoryToMainClassMap = new HashMap<>();

    for (MatchedLipid matchedLipid : matchedLipids) {
      ILipidAnnotation lipidAnnotation = matchedLipid.getLipidAnnotation();
      LipidCategories lipidCategory = lipidAnnotation.getLipidClass().getMainClass()
          .getLipidCategory();
      LipidMainClasses lipidMainClass = lipidAnnotation.getLipidClass().getMainClass();
      ILipidClass lipidClass = lipidAnnotation.getLipidClass();

      lipidCategoryToMainClassMap.computeIfAbsent(lipidCategory, k -> new HashMap<>())
          .computeIfAbsent(lipidMainClass, k -> new HashMap<>())
          .computeIfAbsent(lipidClass, k -> new ArrayList<>()).add(lipidAnnotation);
    }

    int colorIndex = 1;
    for (Entry<LipidCategories, Map<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>>> entry : lipidCategoryToMainClassMap.entrySet()) {
      int categroyValue = getCategoryLipidAnnotationCount(entry);
      Color cateoryColor = MZmineCore.getConfiguration().getDefaultColorPalette().get(colorIndex);
      TreeNode lipidCategoryTreeNode = new TreeNode(
          new ChartItem(categroyValue + "\n" + entry.getKey().getAbbreviation(), categroyValue,
              cateoryColor), tree);
      for (Map.Entry<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>> mainClassEntry : entry.getValue()
          .entrySet()) {
        Color mainClassColor = cateoryColor.desaturate();
        int mainClassValue = getMainClassLipidAnnotationCount(mainClassEntry);
        TreeNode lipidMainClassTreeNode = new TreeNode(
            new ChartItem(mainClassValue + "\n" + mainClassEntry.getKey().getName(), mainClassValue,
                mainClassColor), lipidCategoryTreeNode);
        for (Map.Entry<ILipidClass, List<ILipidAnnotation>> lipidClassEntry : mainClassEntry.getValue()
            .entrySet()) {
          Color lipidClassColor = mainClassColor.desaturate();
          int lipidClassValue = getLipidClassLipidAnnotationCount(lipidClassEntry);
          TreeNode lipidClassTreeNode = new TreeNode(
              new ChartItem(lipidClassValue + "\n" + lipidClassEntry.getKey().getName(),
                  lipidClassValue, lipidClassColor), lipidMainClassTreeNode);
//          for (ILipidAnnotation lipidAnnotation : lipidClassEntry.getValue()) {
//            Color lipidAnnotationColor = lipidClassColor.desaturate();
//            new TreeNode(new ChartItem(lipidAnnotation.getAnnotation(), 1, lipidAnnotationColor),
//                lipidClassTreeNode);
//          }
        }
      }
      colorIndex++;
    }
    return tree;
  }

  private int getLipidClassLipidAnnotationCount(
      Entry<ILipidClass, List<ILipidAnnotation>> lipidClassEntry) {
    return lipidClassEntry.getValue().size();
  }

  private int getMainClassLipidAnnotationCount(
      Entry<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>> mainClassEntry) {
    int totalCount = 0;
    for (Map.Entry<ILipidClass, List<ILipidAnnotation>> classEntry : mainClassEntry.getValue()
        .entrySet()) {
      totalCount += classEntry.getValue().size();
    }
    return totalCount;
  }

  private int getCategoryLipidAnnotationCount(
      Entry<LipidCategories, Map<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>>> entry) {
    int totalCount = 0;
    for (Map.Entry<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>> mainClassEntry : entry.getValue()
        .entrySet()) {
      for (Map.Entry<ILipidClass, List<ILipidAnnotation>> classEntry : mainClassEntry.getValue()
          .entrySet()) {
        totalCount += classEntry.getValue().size();
      }
    }
    return totalCount;
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

  private void buildAllMsMs(List<ModularFeatureListRow> rows) {
    MultiSpectraVisualizerTab multiSpectraVisualizerTab = new MultiSpectraVisualizerTab(
        rows.get(0));
    allMsMsTab.setContent(multiSpectraVisualizerTab.getContent());
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
    buildAllMsMs(focussedRows);
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

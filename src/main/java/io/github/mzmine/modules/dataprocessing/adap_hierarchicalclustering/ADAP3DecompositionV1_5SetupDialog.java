/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering;

import io.github.mzmine.datamodel.features.FeatureList;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;

import io.github.mzmine.modules.dataprocessing.adap_mcr.ChromatogramPeakPair;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.ArrayUtils;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import dulab.adap.common.algorithms.FeatureTools;
import dulab.adap.datamodel.Peak;
import dulab.adap.workflow.TwoStepDecomposition;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class ADAP3DecompositionV1_5SetupDialog extends ParameterSetupDialog {
  private static final Dimension MIN_DIMENSIONS = new Dimension(400, 300);
  private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN, 10);
  private static final DecimalFormat DECIMAL = new DecimalFormat("#.00");

  private static final byte NO_CHANGE = 0;
  private static final byte FIRST_PHASE_CHANGE = 1;
  private static final byte SECOND_PHASE_CHANGE = 2;

  class ComboClustersItem {
    final List<Peak> cluster;
    final double aveRetTime;

    ComboClustersItem(List<Peak> cluster) {
      this.cluster = cluster;

      double sumRetTime = 0.0;
      for (Peak peak : cluster)
        sumRetTime += peak.getRetTime();
      aveRetTime = sumRetTime / cluster.size();
    }

    @Override
    public String toString() {
      return "Cluster at " + DECIMAL.format(aveRetTime) + " min";
    }
  }

  private BorderPane pnlVisible;
  private GridPane pnlLabelsFields;
  private VBox pnlTabs;
  private CheckBox preview;
  private ComboBox<FeatureList> comboPeakList;

  private Button btnRefresh;

  private final ObservableList<ComboClustersItem> comboClustersModel =
      FXCollections.observableArrayList();
  private final ComboBox<ComboClustersItem> comboClusters = new ComboBox<>(comboClustersModel);
  private SimpleScatterPlot retTimeMZPlot;
  private EICPlot retTimeIntensityPlot;

  private Object[] currentValues;

  public ADAP3DecompositionV1_5SetupDialog(boolean valueCheckRequired,
      final ParameterSet parameters, String message) {
    super(valueCheckRequired, parameters, message);

    Parameter[] params = parameters.getParameters();
    int size = params.length;

    currentValues = new Object[size];

    for (int i = 0; i < size; ++i)
      currentValues[i] = params[i].getValue();

    comboPeakList = new ComboBox<>();
    comboPeakList.setOnAction(e -> {
      // -------------------------
      // Retrieve current PeakList
      // -------------------------

      final FeatureList peakList = comboPeakList.getSelectionModel().getSelectedItem();

      final List<Peak> peaks = ADAP3DecompositionV1_5Task.getPeaks(peakList,
          parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.EDGE_TO_HEIGHT_RATIO)
              .getValue(),
          parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.DELTA_TO_HEIGHT_RATIO)
              .getValue());

      // ---------------------------------
      // Calculate retention time clusters
      // ---------------------------------

      List<Double> retTimeValues = new ArrayList<>();
      List<Double> mzValues = new ArrayList<>();
      List<Double> colorValues = new ArrayList<>();

      retTimeCluster(peaks, retTimeValues, mzValues, colorValues);

      final int size2 = retTimeValues.size();

      retTimeMZPlot.updateData(ArrayUtils.toPrimitive(retTimeValues.toArray(new Double[size2])),
          ArrayUtils.toPrimitive(mzValues.toArray(new Double[size2])),
          ArrayUtils.toPrimitive(colorValues.toArray(new Double[size2])));

      // ------------------------
      // Calculate shape clusters
      // ------------------------

      final ComboClustersItem item = comboClusters.getSelectionModel().getSelectedItem();

      if (item != null) {
        final List<List<NavigableMap<Double, Double>>> shapeClusters = new ArrayList<>();
        final List<List<String>> texts = new ArrayList<>();
        final List<Double> colors = new ArrayList<>();

        shapeCluster(item.cluster, shapeClusters, texts, colors);

        retTimeIntensityPlot.updateData(shapeClusters, colors, texts, null);
      }
    });

    retTimeMZPlot = new SimpleScatterPlot("Retention time", "m/z");
    retTimeIntensityPlot = new EICPlot();

    FeatureList[] peakLists = MZmineCore.getDesktop().getSelectedPeakLists();

    // -----------------------------
    // Panel with preview parameters
    // -----------------------------

    preview = new CheckBox("Show preview");
    preview.setOnAction(e -> {
      if (preview.isSelected()) {
        // Set the height of the preview to 200 cells, so it will span
        // the whole vertical length of the dialog (buttons are at row
        // no 100). Also, we set the weight to 10, so the preview
        // component will consume most of the extra available space.
        paramsPane.add(pnlTabs, 3, 0, 1, 100);
        pnlVisible.setCenter(pnlLabelsFields);
        //comboPeakList.getSelectionModel().select(0);

      } else {
        paramsPane.getChildren().remove(pnlTabs);
        pnlVisible.getChildren().remove(pnlLabelsFields);
      }
    });

    comboClusters.setOnAction(e -> {
      // ------------------------
      // Calculate shape clusters
      // ------------------------

      final ComboClustersItem item = comboClusters.getSelectionModel().getSelectedItem();

      if (item != null) {
        final List<List<NavigableMap<Double, Double>>> shapeClusters = new ArrayList<>();
        final List<List<String>> texts = new ArrayList<>();
        final List<Double> colors = new ArrayList<>();

        shapeCluster(item.cluster, shapeClusters, texts, colors);

        retTimeIntensityPlot.updateData(shapeClusters, colors, texts, null);
      }

    });
    // preview.setHorizontalAlignment(SwingConstants.CENTER);

    if (peakLists == null || peakLists.length == 0)
      preview.setDisable(true);
    else
      preview.setDisable(false);

    final BorderPane previewPanel = new BorderPane();
    previewPanel.setTop(new Separator());
    previewPanel.setCenter(preview);
    // previewPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

    // comboPeakList.setFont(COMBO_FONT);
    for (final FeatureList peakList : peakLists)
      if (peakList.getNumberOfRawDataFiles() == 1)
        comboPeakList.getItems().add(peakList);
    // comboPeakList.addActionListener(this);

    // comboClusters.setFont(COMBO_FONT);
    // comboClusters.addActionListener(this);
    comboPeakList.setPrefWidth(200);
    comboClusters.setPrefWidth(200);
    pnlLabelsFields = new GridPane();
    pnlLabelsFields.add(new Label("Feature list"), 0, 0);
    pnlLabelsFields.add(comboPeakList, 0, 1);
    pnlLabelsFields.add(new Label("Cluster list"), 2, 0);
    pnlLabelsFields.add(comboClusters, 2, 1);

    pnlVisible = new BorderPane();
    pnlVisible.setTop(previewPanel);

    // --------------------------------------------------------------------
    // ----- Tabbed panel with plots --------------------------------------
    // --------------------------------------------------------------------

    // pnlTabs = new JTabbedPane();
    //pnlTabs = new HBox();
    // pnlTabs.setLayout(new BoxLayout(pnlTabs, BoxLayout.Y_AXIS));

    retTimeMZPlot.setMinSize(400,300);

    BorderPane pnlPlotRetTimeClusters = new BorderPane();
    pnlPlotRetTimeClusters.setStyle("-fx-background-color: #FFFFFF;");
    pnlPlotRetTimeClusters.setCenter(retTimeMZPlot);
    // GUIUtils.addMarginAndBorder(pnlPlotRetTimeClusters, 10);

    //pnlTabs.getChildren().add();

    retTimeIntensityPlot.setMinSize(400,300);

    BorderPane pnlPlotShapeClusters = new BorderPane();
    pnlPlotShapeClusters.setStyle("-fx-background-color: #FFFFFF;");;
    pnlPlotShapeClusters.setCenter(retTimeIntensityPlot);
    // GUIUtils.addMarginAndBorder(pnlPlotShapeClusters, 10);

    //pnlTabs.getChildren().add();
    pnlTabs = new VBox(pnlPlotRetTimeClusters,pnlPlotShapeClusters);
    pnlTabs.setSpacing(10);
    super.paramsPane.add(pnlVisible, 0, super.getNumberOfParameters() + 3);
  }

  @Override
  public void parametersChanged() {
    super.updateParameterSetFromComponents();

    if (!preview.isSelected())
      return;

    switch (compareParameters(parameterSet.getParameters())) {
      case FIRST_PHASE_CHANGE:
        comboPeakList.getOnAction().handle(new ActionEvent());
//        comboPeakList.getSelectionModel().select(comboPeakList.getSelectionModel().getSelectedIndex());
        break;

      case SECOND_PHASE_CHANGE:
        comboClusters.getOnAction().handle(new ActionEvent());
//        comboClusters.getSelectionModel().select(comboClusters.getSelectionModel().getSelectedIndex());
        break;
    }
  }

  /**
   * Cluster all peaks in PeakList based on retention time
   *
   * @param peaks list od ADAP peaks
   * @param retTimeValues output of retention times
   * @param mzValues output of m/z-values
   * @param colorValues output of colors
   */

  private void retTimeCluster(List<Peak> peaks, List<Double> retTimeValues, List<Double> mzValues,
      List<Double> colorValues) {
    Double minDistance =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_DISTANCE).getValue();
    Integer minSize =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_SIZE).getValue();
    Double minIntensity = parameterSet
        .getParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_INTENSITY).getValue();

    if (minDistance == null || minSize == null || minIntensity == null)
      return;

    List<List<Peak>> retTimeClusters =
        TwoStepDecomposition.getRetTimeClusters(peaks, minDistance, minSize, minIntensity);

    int colorIndex = 0;
    final int numColors = 7;
    final double[] colors = new double[numColors];
    for (int i = 0; i < numColors; ++i)
      colors[i] = (double) i / numColors;

    comboClustersModel.clear();;

    // Disable action listeners
    // ActionListener[] comboListeners = comboClusters.getActionListeners();
    // for (ActionListener l : comboListeners)
    // comboClusters.removeActionListener(l);

    for (List<Peak> cluster : retTimeClusters) {
      for (Peak peak : cluster) {
        retTimeValues.add(peak.getRetTime());
        mzValues.add(peak.getMZ());
        colorValues.add(colors[colorIndex % numColors]);
      }

      ++colorIndex;

      ComboClustersItem newItem = new ComboClustersItem(cluster);

      int i;

      for (i = 0; i < comboClustersModel.size(); ++i) {
        double retTime = comboClustersModel.get(i).aveRetTime;
        if (newItem.aveRetTime < retTime) {
          comboClustersModel.add(i, newItem);
          break;
        }
      }

      if (i == comboClustersModel.size())
        comboClustersModel.add(newItem);
    }

    // Enable action listeners
    // for (ActionListener l : comboListeners)
    // comboClusters.addActionListener(l);
  }

  /**
   * Cluster list of PeakInfo based on the chromatographic shapes
   *
   * @param peaks list of ADAP peaks
   * @param outClusters output of clusters
   * @param outText output of tooltip text
   * @param outColors output of colors
   */

  private void shapeCluster(List<Peak> peaks, List<List<NavigableMap<Double, Double>>> outClusters,
      List<List<String>> outText, List<Double> outColors) {
    NumberFormat numberFormat = NumberFormat.getNumberInstance();

    Double edgeToHeightRatio =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.EDGE_TO_HEIGHT_RATIO).getValue();
    Double deltaToHeightRatio = parameterSet
        .getParameter(ADAP3DecompositionV1_5Parameters.DELTA_TO_HEIGHT_RATIO).getValue();
    Boolean useIsShared =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.USE_ISSHARED).getValue();
    Double shapeSimThreshold =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.SHAPE_SIM_THRESHOLD).getValue();
    Double minModelPeakSharpness =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.MIN_MODEL_SHARPNESS).getValue();
    List<Range<Double>> deprecatedMZValues =
        parameterSet.getParameter(ADAP3DecompositionV1_5Parameters.MZ_VALUES).getValue();

    if (edgeToHeightRatio == null || deltaToHeightRatio == null || useIsShared == null
        || shapeSimThreshold == null || minModelPeakSharpness == null)
      return;

    List<Peak> modelPeakCandidates = TwoStepDecomposition.filterPeaks(peaks, useIsShared,
        edgeToHeightRatio, deltaToHeightRatio, minModelPeakSharpness, new ArrayList<>());

    if (modelPeakCandidates.isEmpty())
      return;

    List<List<Peak>> clusters =
        TwoStepDecomposition.getShapeClusters(modelPeakCandidates, shapeSimThreshold);

    outClusters.clear();
    outText.clear();
    outColors.clear();

    Random rand = new Random();
    rand.setSeed(0);

    int colorIndex = 0;
    final int numColors = 10;
    final double[] colors = new double[numColors];

    for (int i = 0; i < numColors; ++i)
      colors[i] = rand.nextDouble();

    for (List<Peak> cluster : clusters) {
      List<NavigableMap<Double, Double>> c = new ArrayList<>(cluster.size());

      List<String> texts = new ArrayList<>(cluster.size());

      for (Peak peak : cluster) {
        c.add(peak.getChromatogram());
        texts.add(peak.getInfo() + "\nSharpness: "
            + numberFormat.format(FeatureTools.sharpnessYang(peak.getChromatogram())));
      }
      outClusters.add(c);
      outText.add(texts);

      outColors.add(colors[colorIndex % numColors]);
      ++colorIndex;
    }
  }

  private byte compareParameters(Parameter[] newValues) {
    if (currentValues == null) {
      int size = newValues.length;
      currentValues = new Object[size];
      for (int i = 0; i < size; ++i)
        currentValues[i] = newValues[i].getValue();

      return FIRST_PHASE_CHANGE;
    }

    final Set<Integer> firstPhaseIndices = new HashSet<>(Arrays.asList(new Integer[] {1, 2, 3}));

    final Set<Integer> secondPhaseIndices =
        new HashSet<>(Arrays.asList(new Integer[] {4, 5, 6, 7, 8, 10}));

    int size = Math.min(currentValues.length, newValues.length);

    Set<Integer> changedIndices = new HashSet<>();

    for (int i = 0; i < size; ++i) {
      Object oldValue = currentValues[i];
      Object newValue = newValues[i].getValue();

      if (newValue != null && oldValue != null && oldValue.equals(newValue))
        continue;

      changedIndices.add(i);
    }

    byte result = NO_CHANGE;

    if (!Sets.intersection(firstPhaseIndices, changedIndices).isEmpty())
      result = FIRST_PHASE_CHANGE;

    else if (!Sets.intersection(secondPhaseIndices, changedIndices).isEmpty())
      result = SECOND_PHASE_CHANGE;

    for (int i = 0; i < size; ++i)
      currentValues[i] = newValues[i].getValue();

    return result;
  }

}

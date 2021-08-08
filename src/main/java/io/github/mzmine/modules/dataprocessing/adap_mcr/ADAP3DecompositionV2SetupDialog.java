/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.adap_mcr;


import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.RangeUtils;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Sets;
import dulab.adap.datamodel.BetterComponent;
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.workflow.decomposition.ComponentSelector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class ADAP3DecompositionV2SetupDialog extends ParameterSetupDialog {
  /**
   * Minimum dimensions of plots
   */
  private static final Dimension MIN_DIMENSIONS = new Dimension(400, 300);

  /**
   * Font for the preview combo elements
   */
  private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN, 10);

  /**
   * One of three states: > no changes made, > change in the first phase parameters, > change in the
   * second phase parameters
   */
  private enum CHANGE_STATE {
    NONE, FIRST_PHASE, SECOND_PHASE
  }

  /**
   * Elements of the interface
   */
  private BorderPane pnlUIElements;
  private FlowPane pnlComboBoxes;
  private HBox pnlPlots;
  private CheckBox chkPreview;
  private ComboBox<ChromatogramPeakPair> cboPeakLists;
  private ComboBox<RetTimeClusterer.Cluster> cboClusters;
  private Button btnRefresh;
  private SimpleScatterPlot retTimeMZPlot;
  private EICPlot retTimeIntensityPlot;

  /**
   * Current values of the parameters
   */
  private Object[] currentParameters;

  /**
   * Creates an instance of the class and saves the current values of all parameters
   */
  ADAP3DecompositionV2SetupDialog(boolean valueCheckRequired,
      @NotNull final ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    Parameter[] params = parameters.getParameters();
    int size = params.length;

    currentParameters = new Object[size];
    for (int i = 0; i < size; ++i)
      currentParameters[i] = params[i].getValue();

    // -----------------------------
    // Panel with preview UI elements
    // -----------------------------

    // Preview CheckBox
    chkPreview = new CheckBox("Show preview");
    chkPreview.setOnAction(e -> {
      if (chkPreview.isSelected()) {
        // Set the height of the chkPreview to 200 cells, so it will
        // span
        // the whole vertical length of the dialog (buttons are at row
        // no 100). Also, we set the weight to 10, so the chkPreview
        // component will consume most of the extra available space.
        paramsPane.add(pnlPlots, 3, 0);
        pnlUIElements.setCenter(pnlComboBoxes);

        refresh();
      } else {
        paramsPane.getChildren().remove(pnlPlots);
        pnlUIElements.getChildren().remove(pnlComboBoxes);
      }

    });
    // chkPreview.setHorizontalAlignment(SwingConstants.CENTER);
    // chkPreview.setEnabled(true);

    // Preview panel that will contain ComboBoxes
    final BorderPane panel = new BorderPane();
    panel.setTop(new Separator());
    panel.setCenter(chkPreview);
    // panel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
    pnlUIElements = new BorderPane();
    pnlUIElements.setTop(panel);

    // ComboBox for Feature lists
    cboPeakLists = new ComboBox<>();
    // cboPeakLists.setFont(COMBO_FONT);
    for (ChromatogramPeakPair p : ChromatogramPeakPair.fromParameterSet(parameterSet).values())
      cboPeakLists.getItems().add(p);
    cboPeakLists.setOnAction(e -> retTimeCluster());

    btnRefresh = new Button("Refresh");
    btnRefresh.setOnAction(e -> refresh());

    // ComboBox with Clusters
    cboClusters = new ComboBox<>();
    // cboClusters.setFont(COMBO_FONT);
    cboClusters.setOnAction(e -> shapeCluster());
    // cboClusters.addActionListener(this);

    pnlComboBoxes = new FlowPane(new Label("Feature Lists"), cboPeakLists, btnRefresh,
        new Label("Clusters"), cboClusters, new BorderPane());
    pnlComboBoxes.setOrientation(Orientation.VERTICAL);

    // --------------------------------------------------------------------
    // ----- Panel with plots --------------------------------------
    // --------------------------------------------------------------------

    pnlPlots = new HBox();
    // pnlPlots.setLayout(new BoxLayout(pnlPlots, BoxLayout.Y_AXIS));

    // Plot with retention-time clusters
    retTimeMZPlot = new SimpleScatterPlot("Retention time", "m/z");
    // retTimeMZPlot.setMinimumSize(MIN_DIMENSIONS);
    // retTimeMZPlot.setPreferredSize(MIN_DIMENSIONS);

    final BorderPane pnlPlotRetTimeClusters = new BorderPane();
    // pnlPlotRetTimeClusters.setBackground(Color.white);
    pnlPlotRetTimeClusters.setCenter(retTimeMZPlot);
    // GUIUtils.addMarginAndBorder(pnlPlotRetTimeClusters, 10);

    // Plot with chromatograms
    retTimeIntensityPlot = new EICPlot();
    // retTimeIntensityPlot.setMinimumSize(MIN_DIMENSIONS);
    // retTimeIntensityPlot.setPreferredSize(MIN_DIMENSIONS);

    BorderPane pnlPlotShapeClusters = new BorderPane();
    // pnlPlotShapeClusters.setBackground(Color.white);
    pnlPlotShapeClusters.setCenter(retTimeIntensityPlot);
    // GUIUtils.addMarginAndBorder(pnlPlotShapeClusters, 10);

    pnlPlots.getChildren().add(pnlPlotRetTimeClusters);
    pnlPlots.getChildren().add(pnlPlotShapeClusters);

    super.paramsPane.add(pnlUIElements, 0, super.getNumberOfParameters() + 3);
  }



  @Override
  public void parametersChanged() {
    super.updateParameterSetFromComponents();

    if (!chkPreview.isSelected())
      return;

    Cursor cursor = getScene().getCursor();
    getScene().setCursor(Cursor.WAIT);

    switch (compareParameters(parameterSet.getParameters())) {
      case FIRST_PHASE:
        retTimeCluster();
        break;

      case SECOND_PHASE:
        shapeCluster();
        break;
    }

    getScene().setCursor(cursor);
  }

  private void refresh() {
    cboPeakLists.getItems().clear();
    for (ChromatogramPeakPair p : ChromatogramPeakPair.fromParameterSet(parameterSet).values())
      cboPeakLists.getItems().add(p);

    if (cboPeakLists.getItems().size() > 0)
      cboPeakLists.getSelectionModel().select(0);
  }

  /**
   * Cluster all peaks in PeakList based on retention time
   */
  private void retTimeCluster() {
    ChromatogramPeakPair chromatogramPeakPair =
        cboPeakLists.getItems().get(cboPeakLists.getSelectionModel().getSelectedIndex());
    if (chromatogramPeakPair == null)
      return;

    FeatureList chromatogramList = chromatogramPeakPair.chromatograms;
    FeatureList peakList = chromatogramPeakPair.peaks;
    if (chromatogramList == null || peakList == null)
      return;

    Double minDistance =
        parameterSet.getParameter(ADAP3DecompositionV2Parameters.PREF_WINDOW_WIDTH).getValue();
    if (minDistance == null || minDistance <= 0.0)
      return;

    // Convert peakList into ranges
    List<RetTimeClusterer.Interval> ranges =
        peakList.getRows().stream().map(FeatureListRow::getBestFeature)
            .map(p -> new RetTimeClusterer.Interval(RangeUtils.toDoubleRange(p.getRawDataPointsRTRange()), p.getMZ()))
            .collect(Collectors.toList());

    List<BetterPeak> peaks = new ADAP3DecompositionV2Utils().getPeaks(peakList);

    // Form clusters of ranges
    List<RetTimeClusterer.Cluster> retTimeClusters =
        new RetTimeClusterer(minDistance).execute(peaks);

    cboClusters.getItems().clear();
    // cboClusters.removeActionListener(this);
    for (RetTimeClusterer.Cluster cluster : retTimeClusters) {
      int i;

      for (i = 0; i < cboClusters.getItems().size(); ++i) {
        double retTime = cboClusters.getItems().get(i).retTime;
        if (cluster.retTime < retTime) {
          cboClusters.getItems().add(i, cluster);
          break;
        }
      }

      if (i == cboClusters.getItems().size())
        cboClusters.getItems().add(cluster);
    }

    retTimeMZPlot.updateData(retTimeClusters);

    shapeCluster();
  }

  /**
   * Cluster list of PeakInfo based on the chromatographic shapes
   */
  private void shapeCluster() {
    ChromatogramPeakPair chromatogramPeakPair = cboPeakLists.getSelectionModel().getSelectedItem();
    if (chromatogramPeakPair == null)
      return;

    FeatureList chromatogramList = chromatogramPeakPair.chromatograms;
    FeatureList peakList = chromatogramPeakPair.peaks;
    if (chromatogramList == null || peakList == null)
      return;

    final RetTimeClusterer.Cluster cluster = cboClusters.getSelectionModel().getSelectedItem();
    if (cluster == null)
      return;

    Double retTimeTolerance =
        parameterSet.getParameter(ADAP3DecompositionV2Parameters.RET_TIME_TOLERANCE).getValue();
    Boolean adjustApexRetTime =
        parameterSet.getParameter(ADAP3DecompositionV2Parameters.ADJUST_APEX_RET_TIME).getValue();
    Integer minClusterSize =
        parameterSet.getParameter(ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE).getValue();
    if (retTimeTolerance == null || retTimeTolerance <= 0.0 || adjustApexRetTime == null
        || minClusterSize == null || minClusterSize <= 0)
      return;

    List<BetterPeak> chromatograms = new ADAP3DecompositionV2Utils().getPeaks(chromatogramList);

    List<BetterComponent> components = null;
    try {
      components = new ComponentSelector().execute(chromatograms, cluster, retTimeTolerance,
          adjustApexRetTime, minClusterSize);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (components != null)
      retTimeIntensityPlot.updateData(chromatograms, components); // chromatograms
  }

  private CHANGE_STATE compareParameters(Parameter[] newValues) {
    if (currentParameters == null) {
      int size = newValues.length;
      currentParameters = new Object[size];
      for (int i = 0; i < size; ++i)
        currentParameters[i] = newValues[i].getValue();

      return CHANGE_STATE.FIRST_PHASE;
    }

    final Set<Integer> firstPhaseIndices = new HashSet<>(Collections.singleton(2));
    final Set<Integer> secondPhaseIndices = new HashSet<>(Arrays.asList(3, 4, 5));

    int size = Math.min(currentParameters.length, newValues.length);

    Set<Integer> changedIndices = new HashSet<>();

    for (int i = 0; i < size; ++i) {
      Object oldValue = currentParameters[i];
      Object newValue = newValues[i].getValue();

      if (newValue != null && oldValue != null && oldValue.equals(newValue))
        continue;

      changedIndices.add(i);
    }

    CHANGE_STATE result = CHANGE_STATE.NONE;

    if (!Sets.intersection(firstPhaseIndices, changedIndices).isEmpty())
      result = CHANGE_STATE.FIRST_PHASE;

    else if (!Sets.intersection(secondPhaseIndices, changedIndices).isEmpty())
      result = CHANGE_STATE.SECOND_PHASE;

    for (int i = 0; i < size; ++i)
      currentParameters[i] = newValues[i].getValue();

    return result;
  }
}

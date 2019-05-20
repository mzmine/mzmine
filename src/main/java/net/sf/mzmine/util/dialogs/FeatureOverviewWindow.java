/*
 * Copyright 2006-2019 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerWindow;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;

/**
 * Window to show a summary of a feature list row
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FeatureOverviewWindow extends JFrame {

  private Feature feature;
  private RawDataFile[] rawFiles;

  private static final long serialVersionUID = 1L;

  public FeatureOverviewWindow(PeakListRow row) {

    this.feature = row.getBestPeak();
    rawFiles = row.getRawDataFiles();

    setBackground(Color.white);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    JSplitPane splitPaneCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    add(splitPaneCenter);

    // split pane left for plots
    JSplitPane splitPaneLeftPlot = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPaneCenter.add(splitPaneLeftPlot);

    // add Tic plots
    splitPaneLeftPlot.add(addTicPlot(row));

    // add feature data summary
    splitPaneLeftPlot.add(addFeatureDataSummary(row));

    // split pane right
    JSplitPane splitPaneRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPaneCenter.add(splitPaneRight);

    // add spectra MS1
    splitPaneRight.add(addSpectraMS1());

    // add Spectra MS2
    if (feature.getMostIntenseFragmentScanNumber() > 0) {
      splitPaneRight.add(addSpectraMS2());
    } else {
      JPanel noMSMSPanel = new JPanel();
      JLabel noMSMSScansFound = new JLabel("Sorry, no MS/MS scans found!");
      noMSMSScansFound.setFont(new Font("Dialog", Font.BOLD, 16));
      noMSMSScansFound.setForeground(Color.RED);
      noMSMSPanel.add(noMSMSScansFound, SwingConstants.CENTER);
      splitPaneRight.add(noMSMSPanel, SwingConstants.CENTER);
    }
    setVisible(true);
    validate();
    repaint();
    pack();
  }

  private JSplitPane addTicPlot(PeakListRow row) {
    JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    // labels for TIC visualizer
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(rawFiles[0].getDataRTRange(1), 1);

    // mz range
    Range<Double> mzRange = null;
    mzRange = feature.getRawDataPointsMZRange();
    // optimize output by extending the range
    double upper = mzRange.upperEndpoint();
    double lower = mzRange.lowerEndpoint();
    double fiveppm = (upper * 5E-6);
    mzRange = Range.closed(lower - fiveppm, upper + fiveppm);

    // labels
    labelsMap.put(feature, feature.toString());

    TICVisualizerWindow window = new TICVisualizerWindow(rawFiles, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        row.getPeaks(), // selected features
        labelsMap); // labels

    pane.add(window.getTICPlot());
    pane.add(window.getToolBar());
    pane.setResizeWeight(1);
    pane.setDividerSize(1);
    pane.setBorder(BorderFactory.createLineBorder(Color.black));
    return pane;
  }

  private JPanel addFeatureDataSummary(PeakListRow row) {
    JPanel featureDataSummary = new JPanel(new GridLayout(0, 1));
    featureDataSummary.setBackground(Color.WHITE);
    featureDataSummary.add(new Label("Summary of feature: " + row.getID()));
    if (row.getPreferredPeakIdentity() != null)
      featureDataSummary.add(new Label("Identity: " + row.getPreferredPeakIdentity().getName()));
    if (row.getComment() != null)
      featureDataSummary.add(new Label("Comment: " + row.getComment()));
    featureDataSummary.add(new Label("Raw File: " + rawFiles[0].getName()));
    featureDataSummary.add(new Label("Intensity: "
        + MZmineCore.getConfiguration().getIntensityFormat().format(feature.getHeight())));
    featureDataSummary.add(new Label(
        "Area: " + MZmineCore.getConfiguration().getIntensityFormat().format(feature.getArea())));
    featureDataSummary.add(new Label("Charge: " + feature.getCharge()));
    featureDataSummary.add(
        new Label("m/z: " + MZmineCore.getConfiguration().getMZFormat().format(feature.getMZ())));
    featureDataSummary.add(new Label(
        "Retention time: " + MZmineCore.getConfiguration().getRTFormat().format(feature.getRT())));
    featureDataSummary.add(new Label("Asymmetry factor "
        + MZmineCore.getConfiguration().getRTFormat().format(feature.getAsymmetryFactor())));
    featureDataSummary.add(new Label("Tailing Factor factor "
        + MZmineCore.getConfiguration().getRTFormat().format(feature.getTailingFactor())));
    featureDataSummary.add(new Label("Status: " + feature.getFeatureStatus()));
    return featureDataSummary;
  }

  private JSplitPane addSpectraMS1() {
    JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    SpectraVisualizerWindow spectraWindowMS1 = new SpectraVisualizerWindow(rawFiles[0]);
    spectraWindowMS1.loadRawData(rawFiles[0].getScan(feature.getRepresentativeScanNumber()));

    pane.add(spectraWindowMS1.getSpectrumPlot());
    pane.add(spectraWindowMS1.getToolBar());
    pane.setResizeWeight(1);
    pane.setEnabled(false);
    pane.setDividerSize(0);
    return pane;
  }

  private JSplitPane addSpectraMS2() {
    JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    SpectraVisualizerWindow spectraWindowMS2 = new SpectraVisualizerWindow(rawFiles[0]);
    spectraWindowMS2.loadRawData(rawFiles[0].getScan(feature.getMostIntenseFragmentScanNumber()));

    pane.add(spectraWindowMS2.getSpectrumPlot());
    pane.add(spectraWindowMS2.getToolBar());
    pane.setResizeWeight(1);
    pane.setEnabled(false);
    pane.setDividerSize(0);
    return pane;
  }
}

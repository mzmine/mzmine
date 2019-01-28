/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerWindow;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;

/**
 * Window to show a summary of a feature list row
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MultiSpectraVisualizerWindow extends JFrame {

  private RawDataFile[] rawFiles;
  private PeakListRow row;

  private static final long serialVersionUID = 1L;

  public MultiSpectraVisualizerWindow(int[] scanNumbers, PeakListRow row) {

    rawFiles = row.getRawDataFiles();
    this.row = row;

    setBackground(Color.WHITE);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setMinimumSize(new Dimension(800, 600));
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(scanNumbers.length, 0, 0, 25));
    panel.setAutoscrolls(true);
    add(panel);

    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    JPanel contentPane = new JPanel(new BorderLayout());
    contentPane.add(scrollPane);

    for (int scan : scanNumbers) {
      panel.add(addSpectra(scan));
    }

    add(contentPane, BorderLayout.CENTER);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setVisible(true);
    validate();
    repaint();
    pack();
  }


  private JPanel addSpectra(int scan) {
    JPanel panel = new JPanel(new BorderLayout());
    // Split pane for eic plot (top) and spectrum (bottom)
    JSplitPane bottomPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    // Create EIC plot
    // labels for TIC visualizer
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(rawFiles[0].getDataRTRange(1), 1);

    // mz range
    Range<Double> mzRange = null;
    mzRange = row.getBestPeak().getRawDataPointsMZRange();
    // optimize output by extending the range
    double upper = mzRange.upperEndpoint();
    double lower = mzRange.lowerEndpoint();
    double fiveppm = (upper * 5E-6);
    mzRange = Range.closed(lower - fiveppm, upper + fiveppm);

    // labels
    labelsMap.put(row.getBestPeak(), row.getBestPeak().toString());

    TICVisualizerWindow window = new TICVisualizerWindow(rawFiles, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        row.getPeaks(), // selected features
        labelsMap); // labels

    TICPlot ticPlot = window.getTICPlot();
    ticPlot.setPreferredSize(new Dimension(600, 200));
    ticPlot.getChart().removeLegend();

    ValueMarker marker = new ValueMarker(rawFiles[0].getScan(scan).getRetentionTime());
    marker.setPaint(Color.RED);
    marker.setStroke(new BasicStroke(3.0f));

    XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();
    plot.addDomainMarker(marker);
    bottomPane.add(ticPlot);
    bottomPane.setResizeWeight(0.5);
    bottomPane.setEnabled(true);
    bottomPane.setDividerSize(5);
    bottomPane.setDividerLocation(200);

    JSplitPane spectrumPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(rawFiles[0]);
    spectraWindow.loadRawData(rawFiles[0].getScan(scan));
    SpectraPlot spectrumPlot = spectraWindow.getSpectrumPlot();
    spectrumPlot.getChart().removeLegend();
    spectrumPlot.setPreferredSize(new Dimension(600, 400));
    spectrumPane.add(spectrumPlot);
    spectrumPane.add(spectraWindow.getToolBar());
    spectrumPane.setResizeWeight(1);
    spectrumPane.setEnabled(false);
    spectrumPane.setDividerSize(0);
    bottomPane.add(spectrumPane);
    panel.add(bottomPane);
    panel.setBorder(BorderFactory.createLineBorder(Color.black));
    return panel;
  }
}

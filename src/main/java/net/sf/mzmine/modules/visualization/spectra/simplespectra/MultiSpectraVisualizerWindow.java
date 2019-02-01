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

package net.sf.mzmine.modules.visualization.spectra.simplespectra;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
 * Window to show all MS/MS scans of a feature list row
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MultiSpectraVisualizerWindow extends JFrame {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private RawDataFile[] rawFiles;
  private PeakListRow row;
  private RawDataFile activeRaw;

  private static final long serialVersionUID = 1L;
  private JPanel pnGrid;
  private JLabel lbRaw;

  /**
   * Shows best fragmentation scan raw data file first
   * 
   * @param row
   */
  public MultiSpectraVisualizerWindow(PeakListRow row) {
    this(row, row.getBestFragmentation().getDataFile());
  }

  public MultiSpectraVisualizerWindow(PeakListRow row, RawDataFile raw) {
    setBackground(Color.WHITE);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setMinimumSize(new Dimension(800, 600));
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());

    pnGrid = new JPanel();
    // any number of rows
    pnGrid.setLayout(new GridLayout(0, 1, 0, 25));
    pnGrid.setAutoscrolls(true);

    JScrollPane scrollPane = new JScrollPane(pnGrid);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    getContentPane().add(scrollPane, BorderLayout.CENTER);

    JPanel pnMenu = new JPanel();
    FlowLayout fl_pnMenu = (FlowLayout) pnMenu.getLayout();
    fl_pnMenu.setVgap(0);
    fl_pnMenu.setAlignment(FlowLayout.LEFT);
    getContentPane().add(pnMenu, BorderLayout.NORTH);

    JButton nextRaw = new JButton("next");
    nextRaw.addActionListener(e -> nextRaw());
    JButton prevRaw = new JButton("prev");
    prevRaw.addActionListener(e -> prevRaw());
    pnMenu.add(prevRaw);
    pnMenu.add(nextRaw);

    lbRaw = new JLabel();
    pnMenu.add(lbRaw);

    JLabel lbRawTotalWithFragmentation = new JLabel();
    pnMenu.add(lbRaw);

    int n = 0;
    for (Feature f : row.getPeaks()) {
      if (f.getMostIntenseFragmentScanNumber() > 0)
        n++;
    }
    lbRawTotalWithFragmentation.setText("(total raw:" + n + ")");

    // add charts
    setData(row, raw);

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setVisible(true);
    validate();
    repaint();
    pack();
  }


  /**
   * next raw file with peak and MSMS
   */
  private void nextRaw() {
    logger.log(Level.INFO, "All MS/MS scans window: next raw file");
    int n = indexOfRaw(activeRaw);
    while (n + 1 < rawFiles.length) {
      n++;
      setRawFileAndShow(rawFiles[n]);
    }
  }

  /**
   * Previous raw file with peak and MSMS
   */
  private void prevRaw() {
    logger.log(Level.INFO, "All MS/MS scans window: previous raw file");
    int n = indexOfRaw(activeRaw) - 1;
    while (n - 1 >= 0) {
      n--;
      setRawFileAndShow(rawFiles[n]);
    }
  }


  /**
   * Set data and create charts
   * 
   * @param row
   * @param raw
   */
  public void setData(PeakListRow row, RawDataFile raw) {
    rawFiles = row.getRawDataFiles();
    this.row = row;
    setRawFileAndShow(raw);
  }


  /**
   * Set the raw data file and create all chromatograms and MS2 spectra
   * 
   * @param raw
   * @return true if row has peak with MS2 spectrum in RawDataFile raw
   */
  public boolean setRawFileAndShow(RawDataFile raw) {
    Feature peak = row.getPeak(raw);
    // no peak / no ms2 - return false
    if (peak == null || peak.getAllMS2FragmentScanNumbers() == null
        || peak.getAllMS2FragmentScanNumbers().length == 0)
      return false;

    this.activeRaw = raw;
    // clear
    pnGrid.removeAll();

    int[] numbers = peak.getAllMS2FragmentScanNumbers();
    for (int scan : numbers) {
      pnGrid.add(addSpectra(scan));
    }

    int n = indexOfRaw(raw);
    lbRaw.setText(n + ": " + raw.getName());
    logger.log(Level.INFO, "All MS/MS scans window: Added " + numbers.length
        + " spectra of raw file " + n + ": " + raw.getName());
    // show
    pnGrid.revalidate();
    pnGrid.repaint();
    return true;
  }


  private int indexOfRaw(RawDataFile raw) {
    return Arrays.asList(rawFiles).indexOf(raw);
  }


  private JPanel addSpectra(int scan) {
    JPanel panel = new JPanel(new BorderLayout());
    // Split pane for eic plot (top) and spectrum (bottom)
    JSplitPane bottomPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    // Create EIC plot
    // labels for TIC visualizer
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    Feature peak = row.getPeak(activeRaw);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(activeRaw.getDataRTRange(1), 1);

    // mz range
    Range<Double> mzRange = null;
    mzRange = peak.getRawDataPointsMZRange();
    // optimize output by extending the range
    double upper = mzRange.upperEndpoint();
    double lower = mzRange.lowerEndpoint();
    double fiveppm = (upper * 5E-6);
    mzRange = Range.closed(lower - fiveppm, upper + fiveppm);

    // labels
    labelsMap.put(peak, peak.toString());

    // get EIC window
    TICVisualizerWindow window = new TICVisualizerWindow(new RawDataFile[] {activeRaw}, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        new Feature[] {peak}, // selected features
        labelsMap); // labels

    // get EIC Plot
    TICPlot ticPlot = window.getTICPlot();
    ticPlot.setPreferredSize(new Dimension(600, 200));
    ticPlot.getChart().getLegend().setVisible(false);

    // add a retention time Marker to the EIC
    ValueMarker marker = new ValueMarker(activeRaw.getScan(scan).getRetentionTime());
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

    // get MS/MS spectra window
    SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(activeRaw);
    spectraWindow.loadRawData(activeRaw.getScan(scan));

    // get MS/MS spectra plot
    SpectraPlot spectrumPlot = spectraWindow.getSpectrumPlot();
    spectrumPlot.getChart().getLegend().setVisible(false);
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

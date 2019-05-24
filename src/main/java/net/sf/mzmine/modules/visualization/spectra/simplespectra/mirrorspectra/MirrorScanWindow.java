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
package net.sf.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.spectra.multimsms.SpectrumChartFactory;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.DataPointsTag;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

/**
 * Creates a window with a mirror chart to compare to scans
 * 
 * @author Robin Schmid
 */
public class MirrorScanWindow extends JFrame {

  private JPanel contentPane;
  private EChartPanel mirrorSpecrumPlot;

  /**
   * Create the frame.
   */
  public MirrorScanWindow() {
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 800, 800);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);
  }

  public void setScans(String labelA, double precursorMZA, double rtA, DataPoint[] dpsA,
      String labelB, double precursorMZB, double rtB, DataPoint[] dpsB) {
    contentPane.removeAll();
    mirrorSpecrumPlot = SpectrumChartFactory.createMirrorChartPanel(labelA, precursorMZA, rtA, dpsA,
        labelB, precursorMZB, rtB, dpsB, false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }

  /**
   * Set scan and mirror scan and create chart
   * 
   * @param scan
   * @param mirror
   */
  public void setScans(Scan scan, Scan mirror) {
    contentPane.removeAll();
    mirrorSpecrumPlot = SpectrumChartFactory.createMirrorChartPanel(scan, mirror,
        scan.getScanDefinition(), mirror.getScanDefinition(), false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();

  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    contentPane.removeAll();
    mirrorSpecrumPlot =
        SpectrumChartFactory.createMirrorChartPanel(scan, mirror, labelA, labelB, false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }

  /**
   * Based on a data base match to a spectral library
   * 
   * @param row
   * @param db
   */
  public void setScans(SpectralDBPeakIdentity db) {
    Scan scan = db.getQueryScan();
    if (scan == null)
      return;
    // scan a
    double precursorMZA = scan.getPrecursorMZ();
    double rtA = scan.getRetentionTime();
    DataPoint[] dpsA = scan.getDataPoints();

    //
    Double precursorMZB = db.getEntry().getPrecursorMZ();
    Double rtB = (Double) db.getEntry().getField(DBEntryField.RT).orElse(0d);
    DataPoint[] dpsB = db.getEntry().getDataPoints();


    contentPane.removeAll();
    // create without data
    mirrorSpecrumPlot = SpectrumChartFactory.createMirrorChartPanel(
        "Query: " + scan.getScanDefinition(), precursorMZA, rtA, null, "Library: " + db.getName(),
        precursorMZB == null ? 0 : precursorMZB, rtB, null, false, true);
    mirrorSpecrumPlot.setMaximumDrawWidth(4200);
    mirrorSpecrumPlot.setMaximumDrawHeight(2500);
    // add data
    DataPointsTag[] tags =
        new DataPointsTag[] {DataPointsTag.ORIGINAL, DataPointsTag.FILTERED, DataPointsTag.ALIGNED};
    Color[] colors = new Color[] {Color.black, new Color(0xF57C00), new Color(0x388E3C)};
    DataPoint[][] query = new DataPoint[tags.length][];
    DataPoint[][] library = new DataPoint[tags.length][];
    for (int i = 0; i < tags.length; i++) {
      DataPointsTag tag = tags[i];
      query[i] = db.getQueryDataPoints(tag);
      library[i] = db.getLibraryDataPoints(tag);
    }

    // add datasets and renderer
    // set up renderer
    CombinedDomainXYPlot domainPlot =
        (CombinedDomainXYPlot) mirrorSpecrumPlot.getChart().getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    XYPlot queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
    XYPlot libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);

    // add all datapoints to a dataset that are not present in subsequent masslist
    for (int i = 0; i < tags.length; i++) {
      DataPointsTag tag = tags[i];
      PseudoSpectrumDataSet qdata =
          new PseudoSpectrumDataSet(true, "Query " + tag.toRemainderString());
      for (DataPoint dp : query[i]) {
        // not contained in other
        if (notInSubsequentMassList(dp, query, i))
          qdata.addDP(dp.getMZ(), dp.getIntensity(), null);
      }

      PseudoSpectrumDataSet ldata =
          new PseudoSpectrumDataSet(true, "Library " + tag.toRemainderString());
      for (DataPoint dp : library[i]) {
        if (notInSubsequentMassList(dp, library, i))
          ldata.addDP(dp.getMZ(), dp.getIntensity(), null);
      }

      Color color = colors[i];
      PseudoSpectraRenderer renderer = new PseudoSpectraRenderer(color, false);
      PseudoSpectraRenderer renderer2 = new PseudoSpectraRenderer(color, false);

      queryPlot.setDataset(i, qdata);
      queryPlot.setRenderer(i, renderer);

      libraryPlot.setDataset(i, ldata);
      libraryPlot.setRenderer(i, renderer2);
    }

    // add legend
    LegendItem item;
    LegendItemCollection collection = new LegendItemCollection();
    for (int i = 0; i < tags.length; i++) {
      item = new LegendItem(tags[i].toRemainderString(), colors[i]);
      collection.add(item);
    }
    mirrorSpecrumPlot.getChart().removeLegend();
    LegendTitle legend = new LegendTitle(() -> collection);
    legend.setPosition(RectangleEdge.BOTTOM);
    mirrorSpecrumPlot.getChart().addLegend(legend);

    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }



  private boolean notInSubsequentMassList(DataPoint dp, DataPoint[][] query, int current) {
    for (int i = current + 1; i < query.length; i++) {
      for (DataPoint b : query[i]) {
        if (Double.compare(dp.getMZ(), b.getMZ()) == 0
            && Double.compare(dp.getIntensity(), b.getIntensity()) == 0)
          return false;
      }
    }
    return true;
  }

  public EChartPanel getMirrorSpecrumPlot() {
    return mirrorSpecrumPlot;
  }

  public void setMirrorSpecrumPlot(EChartPanel mirrorSpecrumPlot) {
    this.mirrorSpecrumPlot = mirrorSpecrumPlot;
  }

}

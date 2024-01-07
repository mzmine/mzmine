/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
package io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.logging.Logger;
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

/**
 * Creates a window with a mirror chart to compare to scans
 *
 * @author Robin Schmid
 */
public class MirrorScanWindow extends JFrame {

  public static final DataPointsTag[] tags = new DataPointsTag[]{DataPointsTag.ORIGINAL,
      DataPointsTag.FILTERED, DataPointsTag.ALIGNED};
  // for SpectralDBIdentity
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final JPanel contentPane;
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
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorChartPanel(labelA, precursorMZA, rtA, dpsA,
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
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorChartPanel(scan, mirror,
        scan.getScanDefinition(), mirror.getScanDefinition(), false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();

  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    contentPane.removeAll();
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorChartPanel(scan, mirror, labelA, labelB,
        false, true);
    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }

  /**
   * Based on a data base match to a spectral library
   *
   * @param db
   */
  public void setScans(SpectralDBAnnotation db) {
    Scan scan = db.getQueryScan();
    if (scan == null) {
      return;
    }

    // get highest data intensity to calc relative intensity
    double mostIntenseQuery = Arrays.stream(db.getQueryDataPoints(DataPointsTag.ORIGINAL))
        .mapToDouble(DataPoint::getIntensity).max().orElse(0d);
    double mostIntenseDB = Arrays.stream(db.getLibraryDataPoints(DataPointsTag.ORIGINAL))
        .mapToDouble(DataPoint::getIntensity).max().orElse(0d);

    if (mostIntenseDB == 0d) {
      logger.warning(
          "This data set has no original data points in the library spectrum (development error)");
    }
    if (mostIntenseQuery == 0d) {
      logger.warning(
          "This data set has no original data points in the query spectrum (development error)");
    }
    if (mostIntenseDB == 0d || mostIntenseQuery == 0d) {
      return;
    }

    // get colors for vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    // colors for the different DataPointsTags:
    final Color[] colors = new Color[]{Color.black, // black = filtered
        palette.getNegativeColorAWT(), // unaligned
        palette.getPositiveColorAWT()// aligned
    };

    // scan a
    double precursorMZA =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    double rtA = scan.getRetentionTime();

    Double precursorMZB = db.getEntry().getPrecursorMZ();
    Float rtB = (Float) db.getEntry().getField(DBEntryField.RT).orElse(0f);

    contentPane.removeAll();
    // create without data
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorChartPanel(
        "Query: " + scan.getScanDefinition(), precursorMZA, rtA, null,
        "Library: " + db.getCompoundName(), precursorMZB == null ? 0 : precursorMZB, rtB, null,
        false, true);
    mirrorSpecrumPlot.setMaximumDrawWidth(4200);
    mirrorSpecrumPlot.setMaximumDrawHeight(2500);
    // add data
    DataPoint[][] query = new DataPoint[tags.length][];
    DataPoint[][] library = new DataPoint[tags.length][];
    for (int i = 0; i < tags.length; i++) {
      DataPointsTag tag = tags[i];
      query[i] = db.getQueryDataPoints(tag);
      library[i] = db.getLibraryDataPoints(tag);
    }

    // add datasets and renderer
    // set up renderer
    CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) mirrorSpecrumPlot.getChart()
        .getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    XYPlot queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
    XYPlot libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);

    // add all datapoints to a dataset that are not present in subsequent
    // masslist
    for (int i = 0; i < tags.length; i++) {
      DataPointsTag tag = tags[i];
      PseudoSpectrumDataSet qdata = new PseudoSpectrumDataSet(true,
          "Query " + tag.toRemainderString());
      for (DataPoint dp : query[i]) {
        // not contained in other
        if (notInSubsequentMassList(dp, query, i) && mostIntenseQuery > 0) {
          qdata.addDP(dp.getMZ(), dp.getIntensity() / mostIntenseQuery * 100d, null);
        }
      }

      PseudoSpectrumDataSet ldata = new PseudoSpectrumDataSet(true,
          "Library " + tag.toRemainderString());
      for (DataPoint dp : library[i]) {
        if (notInSubsequentMassList(dp, library, i) && mostIntenseDB > 0) {
          ldata.addDP(dp.getMZ(), dp.getIntensity() / mostIntenseDB * 100d, null);
        }
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

    // set y axis title
    queryPlot.getRangeAxis().setLabel("rel. intensity [%] (query)");
    libraryPlot.getRangeAxis().setLabel("rel. intensity [%] (library)");

    contentPane.add(mirrorSpecrumPlot, BorderLayout.CENTER);
    contentPane.revalidate();
    contentPane.repaint();
  }

  private boolean notInSubsequentMassList(DataPoint dp, DataPoint[][] query, int current) {
    for (int i = current + 1; i < query.length; i++) {
      for (DataPoint b : query[i]) {
        if (Double.compare(dp.getMZ(), b.getMZ()) == 0
            && Double.compare(dp.getIntensity(), b.getIntensity()) == 0) {
          return false;
        }
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

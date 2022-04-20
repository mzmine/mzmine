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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.multimsms.SpectrumChartFactory;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;

public class MirrorChartFactory {

  public static final DataPointsTag[] tags = new DataPointsTag[]{DataPointsTag.ORIGINAL,
      DataPointsTag.FILTERED, DataPointsTag.ALIGNED};
  public static final String LIBRARY_MATCH_USER_DATA = "Library match";
  private static final Logger logger = Logger.getLogger(MirrorChartFactory.class.getName());

  /**
   * Creates a mirror chart from
   *
   * @param db
   * @return
   */
  public static EChartViewer createMirrorPlotFromSpectralDBPeakIdentity(
      SpectralDBAnnotation db) {

    Scan scan = db.getQueryScan();
    if (scan == null) {
      return null;
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
      return null;
    }

    // get colors for vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    // colors for the different DataPointsTags:
    final Color[] colors = new Color[]{Color.black, // black = filtered
        palette.getNegativeColorAWT(), // unaligned
        palette.getPositiveColorAWT() // aligned
    };

    // scan a
    double precursorMZA =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    double rtA = scan.getRetentionTime();

    Double precursorMZB = db.getEntry().getPrecursorMZ();
    Double rtB = (Double) db.getEntry().getField(DBEntryField.RT).orElse(0d);

    // create without data
    EChartViewer mirrorSpecrumPlot = createMirrorChartViewer("Query: " + scan.getScanDefinition(),
        precursorMZA, rtA, null, "Library: " + db.getCompoundName(),
        precursorMZB == null ? 0 : precursorMZB, rtB, null, false, true);
    // mirrorSpecrumPlot.setMaximumDrawWidth(4200); // TODO?
    // mirrorSpecrumPlot.setMaximumDrawHeight(2500);

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
    LegendTitle legend = createLibraryMatchingLegend(domainPlot);
    mirrorSpecrumPlot.getChart().addLegend(legend);
    mirrorSpecrumPlot.setUserData(LIBRARY_MATCH_USER_DATA);

    // set y axis title
    queryPlot.getRangeAxis().setLabel("rel. intensity [%] (query)");
    libraryPlot.getRangeAxis().setLabel("rel. intensity [%] (library)");
    domainPlot.getDomainAxis().setLabel("m/z");

    queryPlot.setDomainGridlinesVisible(false);
    queryPlot.setDomainMinorGridlinesVisible(false);
    libraryPlot.setDomainGridlinesVisible(false);
    libraryPlot.setDomainMinorGridlinesVisible(false);
    queryPlot.setRangeGridlinesVisible(false);
    queryPlot.setRangeMinorGridlinesVisible(false);
    libraryPlot.setRangeGridlinesVisible(false);
    libraryPlot.setRangeMinorGridlinesVisible(false);

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(mirrorSpecrumPlot.getChart());

    return mirrorSpecrumPlot;
  }

  /**
   * Creates a legend for
   *
   * @param mirrorSpectrumPlot
   * @return
   */
  public static LegendTitle createLibraryMatchingLegend(CombinedDomainXYPlot mirrorSpectrumPlot) {
    // get colors for vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    // colors for the different DataPointsTags:
    final Color[] colors = new Color[]{Color.black, // black = filtered
        palette.getNegativeColorAWT(), // unaligned
        palette.getPositiveColorAWT() // aligned
    };

    LegendItem item;
    LegendItemCollection collection = new LegendItemCollection();

    for (int i = 0; i < tags.length; i++) {
      item = new LegendItem(tags[i].toRemainderString(), colors[i]);
      collection.add(item);
    }
    mirrorSpectrumPlot.getChart().removeLegend();
    LegendTitle legend = new LegendTitle(() -> collection);
    legend.setPosition(RectangleEdge.BOTTOM);

    return legend;
  }

  private static boolean notInSubsequentMassList(DataPoint dp, DataPoint[][] query, int current) {
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


  // ---- From old SpectrumChartFactory
  public static Scan getMSMSScan(FeatureListRow row, RawDataFile raw, boolean alwaysShowBest,
      boolean useBestForMissingRaw) {
    Scan scan = null;
    if (alwaysShowBest || raw == null) {
      scan = row.getMostIntenseFragmentScan();
    } else if (raw != null) {
      Feature peak = row.getFeature(raw);
      if (peak != null) {
        scan = peak.getMostIntenseFragmentScan();
      }
    }
    if (scan == null && useBestForMissingRaw) {
      scan = row.getMostIntenseFragmentScan();
    }
    return scan;
  }

  public static PseudoSpectrumDataSet createMSMSDataSet(Scan scan, String label) {
    if (scan != null) {
      return createMSMSDataSet(
          scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d,
          scan.getRetentionTime(), ScanUtils.extractDataPoints(scan), label);
    } else {
      return null;
    }
  }

  public static PseudoSpectrumDataSet createMSMSDataSet(double precursorMZ, double rt,
      DataPoint[] dps, String label) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (label == null) {
      label = "";
    } else if (!label.isEmpty()) {
      label = " (" + label + ")";
    }
    // data
    PseudoSpectrumDataSet series = new PseudoSpectrumDataSet(true, MessageFormat
        .format("MSMS for m/z={0} RT={1}{2}", mzForm.format(precursorMZ), rtForm.format(rt),
            label));
    // for each row
    for (DataPoint dp : dps) {
      series.addDP(dp.getMZ(), dp.getIntensity(), null);
    }
    return series;
  }

  /**
   * Two scans as a mirror comparison
   *
   * @param scan
   * @param mirror gets reflected by *-1
   * @return
   */
  public static PseudoSpectrumDataSet createMirrorDataSet(Scan scan, Scan mirror) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (scan != null && mirror != null) {
      double scanPrecursor =
          scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
      double mirrorPrecursor =
          mirror.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;

      String label1 = MessageFormat.format("MSMS for m/z={0} RT={1}", mzForm.format(scanPrecursor),
          rtForm.format(scan.getRetentionTime()));
      String label2 = MessageFormat
          .format("MSMS for m/z={0} RT={1}", mzForm.format(mirrorPrecursor),
              rtForm.format(mirror.getRetentionTime()));
      // data
      PseudoSpectrumDataSet data = new PseudoSpectrumDataSet(true, label1, label2);
      // for each row
      for (DataPoint dp : scan) {
        data.addDP(0, dp.getMZ(), dp.getIntensity(), null);
      }
      for (DataPoint dp : mirror) {
        data.addDP(1, dp.getMZ(), -dp.getIntensity(), null);
      }

      return data;
    } else {
      return null;
    }
  }

  /**
   * Also adds the label gen
   *
   * @param showTitle
   * @param showLegend
   * @return
   */
  @Nullable
  public static EChartPanel createMirrorChartPanel(Scan scan, Scan mirror, String labelA,
      String labelB, boolean showTitle, boolean showLegend) {
    if (scan == null || mirror == null) {
      return null;
    }

    double scanPrecursor =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    double mirrorPrecursor =
        mirror.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;

    return new EChartPanel(createMirrorChart(labelA, scanPrecursor, scan.getRetentionTime(),
        ScanUtils.extractDataPoints(scan), labelB, mirrorPrecursor, mirror.getRetentionTime(),
        ScanUtils.extractDataPoints(mirror), showTitle, showLegend));
  }

  public static EChartPanel createMirrorChartPanel(String labelA, double precursorMZA, double rtA,
      DataPoint[] dpsA, String labelB, double precursorMZB, double rtB, DataPoint[] dpsB,
      boolean showTitle, boolean showLegend) {
    return new EChartPanel(
        createMirrorChart(labelA, precursorMZA, rtA, dpsA, labelB, precursorMZB, rtB, dpsB,
            showTitle, showLegend));
  }

  public static EChartViewer createMirrorChartViewer(MassSpectrum scan, MassSpectrum mirror,
      String labelA, String labelB, boolean showTitle, boolean showLegend) {
    if (scan == null || mirror == null) {
      return null;
    }

    return new EChartViewer(createMirrorChart(labelA, ScanUtils.extractDataPoints(scan), labelB,
        ScanUtils.extractDataPoints(mirror), showTitle, showLegend));
  }

  public static EChartViewer createMirrorChartViewer(Scan scan, Scan mirror, String labelA,
      String labelB, boolean showTitle, boolean showLegend) {
    if (scan == null || mirror == null) {
      return null;
    }
    double scanPrecursor =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    double mirrorPrecursor =
        mirror.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;

    return new EChartViewer(createMirrorChart(labelA, scanPrecursor, scan.getRetentionTime(),
        ScanUtils.extractDataPoints(scan), labelB, mirrorPrecursor, mirror.getRetentionTime(),
        ScanUtils.extractDataPoints(mirror), showTitle, showLegend));
  }

  public static EChartViewer createMirrorChartViewer(String labelA, double precursorMZA, double rtA,
      DataPoint[] dpsA, String labelB, double precursorMZB, double rtB, DataPoint[] dpsB,
      boolean showTitle, boolean showLegend) {
    return new EChartViewer(
        createMirrorChart(labelA, precursorMZA, rtA, dpsA, labelB, precursorMZB, rtB, dpsB,
            showTitle, showLegend));
  }

  private static JFreeChart createMirrorChart(String labelA, DataPoint[] dpsA, String labelB,
      DataPoint[] dpsB, boolean showTitle, boolean showLegend) {
    return createMirrorChart(labelA, -1, -1, dpsA, labelB, -1, -1, dpsB, showTitle, showLegend);
  }

  private static JFreeChart createMirrorChart(String labelA, double precursorMZA, double rtA,
      DataPoint[] dpsA, String labelB, double precursorMZB, double rtB, DataPoint[] dpsB,
      boolean showTitle, boolean showLegend) {
    PseudoSpectrumDataSet data =
        dpsA == null ? null : createMSMSDataSet(precursorMZA, rtA, dpsA, labelA);
    PseudoSpectrumDataSet dataMirror =
        dpsB == null ? null : createMSMSDataSet(precursorMZB, rtB, dpsB, labelB);

    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = new DecimalFormat("0.#");

    // set the X axis (retention time) properties
    NumberAxis xAxis = new NumberAxis("m/z");
    xAxis.setNumberFormatOverride(mzForm);
    xAxis.setUpperMargin(0.05);
    xAxis.setLowerMargin(0.05);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setMinorTickCount(5);

    PseudoSpectraRenderer renderer1 = new PseudoSpectraRenderer(Color.BLACK, false);
    PseudoSpectraRenderer renderer2 = new PseudoSpectraRenderer(Color.BLACK, false);

    // create subplot 1...
    final NumberAxis rangeAxis1 = new NumberAxis(labelA != null ? labelA : "rel. intensity [%]");
    final XYPlot subplot1 = new XYPlot(data, null, rangeAxis1, renderer1);
    subplot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    rangeAxis1.setNumberFormatOverride(intensityFormat);
    rangeAxis1.setAutoRangeIncludesZero(true);
    rangeAxis1.setAutoRangeStickyZero(true);
    rangeAxis1.addChangeListener(event -> {
      if (rangeAxis1.getLowerBound() > 0) {
        rangeAxis1.setRange(0, rangeAxis1.getUpperBound());
      }
    });

    // create subplot 2...
    final NumberAxis rangeAxis2 = new NumberAxis(labelB != null ? labelB : "rel. intensity [%]");
    rangeAxis2.setNumberFormatOverride(intensityFormat);
    rangeAxis2.setAutoRangeIncludesZero(true);
    rangeAxis2.setAutoRangeStickyZero(true);
    rangeAxis2.setInverted(true);
    final XYPlot subplot2 = new XYPlot(dataMirror, null, rangeAxis2, renderer2);
    subplot2.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
    rangeAxis2.addChangeListener(event -> {
      if (rangeAxis2.getLowerBound() > 0) {
        rangeAxis2.setRange(0, rangeAxis2.getUpperBound());
      }
    });

    // parent plot...
    final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(xAxis);
    plot.setGap(0);

    // add the subplots...
    plot.add(subplot1, 1);
    plot.add(subplot2, 1);
    plot.setOrientation(PlotOrientation.VERTICAL);

    // set the plot properties
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    // return a new chart containing the overlaid plot...
    JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    chart.setBackgroundPaint(Color.white);
    chart.getTitle().setVisible(false);

    chart.getXYPlot().setRangeZeroBaselineVisible(true);
    chart.getTitle().setVisible(showTitle);
    chart.getLegend().setVisible(showLegend);

    XYPlot queryPlot = (XYPlot) plot.getSubplots().get(0);
    XYPlot libraryPlot = (XYPlot) plot.getSubplots().get(1);

    queryPlot.setDomainGridlinesVisible(false);
    queryPlot.setDomainMinorGridlinesVisible(false);
    libraryPlot.setDomainGridlinesVisible(false);
    libraryPlot.setDomainMinorGridlinesVisible(false);
    queryPlot.setRangeGridlinesVisible(false);
    queryPlot.setRangeMinorGridlinesVisible(false);
    libraryPlot.setRangeGridlinesVisible(false);
    libraryPlot.setRangeMinorGridlinesVisible(false);

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(plot.getChart());

    return chart;
  }


  /**
   * Also adds the label gen
   *
   * @param row
   * @param raw
   * @param showTitle
   * @param showLegend
   * @param useBestForMissingRaw
   * @param alwaysShowBest
   * @return
   */
  /*public static EChartPanel createMSMSChartPanel(FeatureListRow row, RawDataFile raw,
      boolean showTitle, boolean showLegend, boolean alwaysShowBest, boolean useBestForMissingRaw) {
    Scan scan = getMSMSScan(row, raw, alwaysShowBest, useBestForMissingRaw);
    if (scan == null) {
      return null;
    }
    return SpectrumChartFactory.createScanChartPanel(scan, showTitle, showLegend);
  }*/
  public static EChartViewer createMSMSChartViewer(FeatureListRow row, RawDataFile raw,
      boolean showTitle, boolean showLegend, boolean alwaysShowBest, boolean useBestForMissingRaw) {
    Scan scan = getMSMSScan(row, raw, alwaysShowBest, useBestForMissingRaw);
    if (scan == null) {
      return null;
    }
    return SpectrumChartFactory.createScanChartViewer(scan, showTitle, showLegend);
  }
}

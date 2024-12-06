/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util;

import static java.util.Objects.requireNonNullElse;

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
import io.github.mzmine.modules.visualization.spectra.spectra_stack.SpectrumChartFactory;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraItemLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.awt.BasicStroke;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
import org.jfree.chart.plot.ValueMarker;
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
  public static EChartViewer createMirrorPlotFromSpectralDBPeakIdentity(SpectralDBAnnotation db) {

    Scan scan = db.getQueryScan();
    if (scan == null || db.getEntry().getDataPoints() == null) {
      EChartViewer mirrorSpecrumPlot = createMirrorChartViewer("Query: " + db.getCompoundName(), 0d,
          0, null, "Library: " + db.getDatabase(), 0d, 0, null, true, true);
      mirrorSpecrumPlot.setUserData(LIBRARY_MATCH_USER_DATA);
      EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
      theme.apply(mirrorSpecrumPlot.getChart());

      return mirrorSpecrumPlot;
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
    final Color[] colors = new Color[]{palette.getNeutralColorAWT(), // grey = filtered
        palette.getNegativeColorAWT(), // unaligned
        palette.getPositiveColorAWT() // aligned
    };

    // scan a
    double precursorMZA =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    double rtA = scan.getRetentionTime();

    Double precursorMZB = db.getEntry().getPrecursorMZ();
    Double rtB = db.getEntry().getField(DBEntryField.RT)
        .map(v -> v instanceof Number n ? n.doubleValue() : 0d).orElse(0d);

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
      PseudoSpectrumDataSet qdata = new PseudoSpectrumDataSet(true, tag.toRemainderString());
      for (DataPoint dp : query[i]) {
        // not contained in other
        if (notInSubsequentMassList(dp, query, i) && mostIntenseQuery > 0) {
          qdata.addDP(dp.getMZ(), dp.getIntensity() / mostIntenseQuery * 100d, null);
        }
      }

      PseudoSpectrumDataSet ldata = new PseudoSpectrumDataSet(true, tag.toRemainderString());
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
      renderer2.setDefaultSeriesVisibleInLegend(false, false);
    }

    // add legend
    LegendTitle legend = createLibraryMatchingLegend(tags, colors, domainPlot);
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
    mirrorSpecrumPlot.getChart().getLegend().setVisible(true);

    return mirrorSpecrumPlot;
  }

  public static EChartViewer createMirrorPlotFromAligned(MZTolerance mzTol, boolean modified,
      DataPoint[] dpa, @Nullable Double precursorMZA, DataPoint[] dpb,
      @Nullable Double precursorMZB) {
    List<DataPoint[]> aligned;

    if (modified && precursorMZA != null && precursorMZB != null) {
      aligned = ScanAlignment.alignModAware(mzTol, dpa, dpb, precursorMZA, precursorMZB);
    } else {
      aligned = ScanAlignment.align(mzTol, dpa, dpb);
    }
    return createMirrorPlotFromAligned(mzTol, modified, aligned.toArray(DataPoint[][]::new),
        precursorMZA, precursorMZB);
  }

  public static EChartViewer createMirrorPlotFromAligned(MZTolerance mzTol, boolean modified,
      DataPoint[][] aligned, @Nullable Double precursorMZA, @Nullable Double precursorMZB) {

    final DataPointsTag[] tags = new DataPointsTag[]{DataPointsTag.UNALIGNED,
        DataPointsTag.ALIGNED_MODIFIED, DataPointsTag.ALIGNED};

    // get highest data intensity to calc relative intensity
    double mostIntenseQuery = Arrays.stream(aligned).map(dps -> dps[0]).filter(Objects::nonNull)
        .mapToDouble(DataPoint::getIntensity).max().orElse(0d);
    double mostIntenseDB = Arrays.stream(aligned).map(dps -> dps[1]).filter(Objects::nonNull)
        .mapToDouble(DataPoint::getIntensity).max().orElse(0d);

    if (mostIntenseDB == 0d) {
      logger.warning(
          "This data set has no original data points in the library spectrum (development error)");
      return null;
    }
    if (mostIntenseQuery == 0d) {
      logger.warning(
          "This data set has no original data points in the query spectrum (development error)");
      return null;
    }

    // get colors for vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    // colors for the different DataPointsTags:
    final Color[] colors = new Color[]{palette.getNeutralColorAWT(), // unaligned
        palette.getNegativeColorAWT(), // modified
        palette.getPositiveColorAWT() // aligned
    };

    // create without data
    EChartViewer mirrorSpecrumPlot = createMirrorChartViewer(
        "Top: " + requireNonNullElse(precursorMZA, 0), precursorMZA, -1, null,
        "Bottom: " + requireNonNullElse(precursorMZB, 0), precursorMZB, -1, null, false, true);

    // add datasets and renderer
    // set up renderer
    CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) mirrorSpecrumPlot.getChart()
        .getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    XYPlot queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
    XYPlot libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);

    var labelGenerator = new PseudoSpectraItemLabelGenerator(mirrorSpecrumPlot);
    // add all datapoints to a dataset that are not present in subsequent
    // masslist
    for (int i = 0; i < tags.length; i++) {
      DataPointsTag tag = tags[i];
      PseudoSpectrumDataSet qdata = new PseudoSpectrumDataSet(true, tag.toRemainderString());
      PseudoSpectrumDataSet ldata = new PseudoSpectrumDataSet(true, tag.toRemainderString());

      if (i == 0) {
        // unmatched
        for (DataPoint[] dps : aligned) {
          if (dps[0] == null || dps[1] == null) {
            if (dps[0] != null) {
              qdata.addDP(dps[0].getMZ(), dps[0].getIntensity() / mostIntenseQuery * 100d, null);
            }
            if (dps[1] != null) {
              ldata.addDP(dps[1].getMZ(), dps[1].getIntensity() / mostIntenseDB * 100d, null);
            }
          }
        }
      } else if (i == 1) {
        // modified
        for (DataPoint[] dps : aligned) {
          if (dps[0] != null && dps[1] != null && !mzTol.checkWithinTolerance(dps[0].getMZ(),
              dps[1].getMZ())) {
            qdata.addDP(dps[0].getMZ(), dps[0].getIntensity() / mostIntenseQuery * 100d, null);
            ldata.addDP(dps[1].getMZ(), dps[1].getIntensity() / mostIntenseDB * 100d, null);
          }
        }
      } else if (i == 2) {
        // matched
        for (DataPoint[] dps : aligned) {
          if (dps[0] != null && dps[1] != null && mzTol.checkWithinTolerance(dps[0].getMZ(),
              dps[1].getMZ())) {
            qdata.addDP(dps[0].getMZ(), dps[0].getIntensity() / mostIntenseQuery * 100d, null);
            ldata.addDP(dps[1].getMZ(), dps[1].getIntensity() / mostIntenseDB * 100d, null);
          }
        }
      }

      Color color = colors[i];
      PseudoSpectraRenderer renderer = new PseudoSpectraRenderer(color, false);
      PseudoSpectraRenderer renderer2 = new PseudoSpectraRenderer(color, false);

      queryPlot.setDataset(i, qdata);
      queryPlot.setRenderer(i, renderer);

      libraryPlot.setDataset(i, ldata);
      libraryPlot.setRenderer(i, renderer2);

      renderer.setSeriesItemLabelGenerator(0, labelGenerator);
      renderer2.setSeriesItemLabelGenerator(0, labelGenerator);
      renderer.setDefaultItemLabelsVisible(true);
      renderer2.setDefaultItemLabelsVisible(true);

      renderer2.setDefaultSeriesVisibleInLegend(false, false);
    }

    // add legend
    LegendTitle legend = createLibraryMatchingLegend(tags, colors, domainPlot);
    mirrorSpecrumPlot.getChart().addLegend(legend);
    mirrorSpecrumPlot.setUserData(LIBRARY_MATCH_USER_DATA);

    // set y axis title
    queryPlot.getRangeAxis().setLabel("rel. intensity [%]");
    libraryPlot.getRangeAxis().setLabel("rel. intensity [%]");
    domainPlot.getDomainAxis().setLabel("m/z");

    queryPlot.getRangeAxis().setUpperMargin(0.12);
    libraryPlot.getRangeAxis().setUpperMargin(0.12);

    queryPlot.setDomainGridlinesVisible(false);
    queryPlot.setDomainMinorGridlinesVisible(false);
    libraryPlot.setDomainGridlinesVisible(false);
    libraryPlot.setDomainMinorGridlinesVisible(false);
    queryPlot.setRangeGridlinesVisible(false);
    queryPlot.setRangeMinorGridlinesVisible(false);
    libraryPlot.setRangeGridlinesVisible(false);
    libraryPlot.setRangeMinorGridlinesVisible(false);

    final DecimalFormat intensityFormat = new DecimalFormat("0.0");
    ((NumberAxis) queryPlot.getRangeAxis()).setNumberFormatOverride(intensityFormat);
    ((NumberAxis) libraryPlot.getRangeAxis()).setNumberFormatOverride(intensityFormat);

    if (precursorMZA != null && precursorMZA > 0 && precursorMZB != null && precursorMZB > 0) {
      queryPlot.addDomainMarker(createPrecursorMarker(precursorMZA, Color.GRAY, 0.5f));
      libraryPlot.addDomainMarker(createPrecursorMarker(precursorMZB, Color.GRAY, 0.5f));
    }

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(mirrorSpecrumPlot.getChart());
    mirrorSpecrumPlot.getChart().getLegend().setVisible(true);

    return mirrorSpecrumPlot;
  }

  private static ValueMarker createPrecursorMarker(double precursorMz, Color color, float alpha) {
    final ValueMarker marker = new ValueMarker(precursorMz);
    marker.setStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7}, 0f));
    marker.setPaint(color);
    marker.setAlpha(alpha);
    return marker;
  }

  /**
   * Creates a legend for
   *
   * @param mirrorSpectrumPlot
   * @return
   */
  public static LegendTitle createLibraryMatchingLegend(DataPointsTag[] tags, Color[] colors,
      CombinedDomainXYPlot mirrorSpectrumPlot) {
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
      scan = row.getAllFragmentScans().stream().findFirst().orElse(null);
    } else if (raw != null) {
      Feature peak = row.getFeature(raw);
      if (peak != null) {
        scan = peak.getMostIntenseFragmentScan();
      }
    }
    if (scan == null && useBestForMissingRaw) {
      scan = row.getAllFragmentScans().stream().findFirst().orElse(null);
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

  public static PseudoSpectrumDataSet createMSMSDataSet(@Nullable Double precursorMZ, double rt,
      DataPoint[] dps, String label) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (label == null) {
      label = "";
    } else if (!label.isEmpty()) {
      label = " (" + label + ")";
    }
    // data
    final String seriesName;
    if (precursorMZ != null) {
      seriesName = MessageFormat.format("Fragment spectrum for m/z={0} RT={1}{2}",
          mzForm.format(precursorMZ), rtForm.format(rt), label);
    } else {
      seriesName = MessageFormat.format("Fragment spectrum for RT={0}{1}", rtForm.format(rt),
          label);
    }
    PseudoSpectrumDataSet series = new PseudoSpectrumDataSet(true, seriesName);
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
      String label2 = MessageFormat.format("MSMS for m/z={0} RT={1}",
          mzForm.format(mirrorPrecursor), rtForm.format(mirror.getRetentionTime()));
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

  public static EChartViewer createNeutralLossMirrorChartViewer(Scan scan, Scan mirror,
      String labelA, String labelB, boolean showTitle, boolean showLegend) {
    if (scan == null || mirror == null) {
      return null;
    }
    double scanPrecursor =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    double mirrorPrecursor =
        mirror.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;

    return new EChartViewer(createMirrorChart(labelA, scanPrecursor, scan.getRetentionTime(),
        ScanUtils.getNeutralLossSpectrum(ScanUtils.extractDataPoints(scan), scanPrecursor), labelB,
        mirrorPrecursor, mirror.getRetentionTime(),
        ScanUtils.getNeutralLossSpectrum(ScanUtils.extractDataPoints(mirror), mirrorPrecursor),
        showTitle, showLegend));
  }

  public static EChartViewer createMirrorChartViewer(String labelA, @Nullable Double precursorMZA,
      double rtA, DataPoint[] dpsA, String labelB, @Nullable Double precursorMZB, double rtB,
      DataPoint[] dpsB, boolean showTitle, boolean showLegend) {
    return new EChartViewer(
        createMirrorChart(labelA, precursorMZA, rtA, dpsA, labelB, precursorMZB, rtB, dpsB,
            showTitle, showLegend));
  }

  private static JFreeChart createMirrorChart(String labelA, DataPoint[] dpsA, String labelB,
      DataPoint[] dpsB, boolean showTitle, boolean showLegend) {
    return createMirrorChart(labelA, -1d, -1, dpsA, labelB, -1d, -1, dpsB, showTitle, showLegend);
  }

  private static JFreeChart createMirrorChart(String labelA, @Nullable Double precursorMZA,
      double rtA, DataPoint[] dpsA, String labelB, @Nullable Double precursorMZB, double rtB,
      DataPoint[] dpsB, boolean showTitle, boolean showLegend) {
    PseudoSpectrumDataSet data =
        dpsA == null ? null : createMSMSDataSet(precursorMZA, rtA, dpsA, labelA);
    PseudoSpectrumDataSet dataMirror =
        dpsB == null ? null : createMSMSDataSet(precursorMZB, rtB, dpsB, labelB);

    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

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

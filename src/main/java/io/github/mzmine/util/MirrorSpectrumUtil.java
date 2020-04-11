/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.multimsms.SpectrumChartFactory;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;
import java.awt.Color;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;

public class MirrorSpectrumUtil {

  private static final Logger logger = Logger.getLogger(MirrorSpectrumUtil.class.getName());

  public static final DataPointsTag[] tags =
      new DataPointsTag[]{DataPointsTag.ORIGINAL, DataPointsTag.FILTERED, DataPointsTag.ALIGNED};

  public static EChartViewer createPlotFromSpectralDBPeakIdentity(SpectralDBPeakIdentity db) {

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
    double precursorMZA = scan.getPrecursorMZ();
    double rtA = scan.getRetentionTime();

    Double precursorMZB = db.getEntry().getPrecursorMZ();
    Double rtB = (Double) db.getEntry().getField(DBEntryField.RT).orElse(0d);

    // create without data
    EChartViewer mirrorSpecrumPlot = SpectrumChartFactory.createMirrorChartViewer(
        "Query: " + scan.getScanDefinition(), precursorMZA, rtA, null, "Library: " + db.getName(),
        precursorMZB == null ? 0 : precursorMZB, rtB, null, false, true);
//    mirrorSpecrumPlot.setMaximumDrawWidth(4200); // TODO?
//    mirrorSpecrumPlot.setMaximumDrawHeight(2500);

//     add data
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

    // add all datapoints to a dataset that are not present in subsequent
    // masslist
    for (int i = 0; i < tags.length; i++) {
      DataPointsTag tag = tags[i];
      PseudoSpectrumDataSet qdata =
          new PseudoSpectrumDataSet(true, "Query " + tag.toRemainderString());
      for (DataPoint dp : query[i]) {
        // not contained in other
        if (notInSubsequentMassList(dp, query, i) && mostIntenseQuery > 0) {
          qdata.addDP(dp.getMZ(), dp.getIntensity() / mostIntenseQuery * 100d, null);
        }
      }

      PseudoSpectrumDataSet ldata =
          new PseudoSpectrumDataSet(true, "Library " + tag.toRemainderString());
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
    LegendTitle legend = createLegend(domainPlot);
    mirrorSpecrumPlot.getChart().addLegend(legend);

    // set y axis title
    queryPlot.getRangeAxis().setLabel("rel. intensity [%] (query)");
    libraryPlot.getRangeAxis().setLabel("rel. intensity [%] (library)");

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(mirrorSpecrumPlot.getChart());

    return mirrorSpecrumPlot;
  }

  public static LegendTitle createLegend(CombinedDomainXYPlot mirrorSpectrumPlot) {

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
}

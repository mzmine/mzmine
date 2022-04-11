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

package io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Creates a window with a mirror chart to compare to scans
 *
 * @author Robin Schmid & Steffen Heuckeroth
 */
public class MirrorScanWindowFX extends Stage {

  // for SpectralDBIdentity

  public static final DataPointsTag[] tags =
      new DataPointsTag[]{DataPointsTag.ORIGINAL, DataPointsTag.FILTERED, DataPointsTag.ALIGNED};

  private BorderPane contentPane;
  private EChartViewer mirrorSpecrumPlot;

  /**
   * Create the frame.
   */
  public MirrorScanWindowFX() {
    contentPane = new BorderPane();
    contentPane.setStyle("-fx-border-width: 5;");
    contentPane.setPrefSize(800, 800);

    this.setScene(new Scene(contentPane));
  }

  public void setScans(String labelA, double precursorMZA, double rtA, DataPoint[] dpsA,
      String labelB, double precursorMZB, double rtB, DataPoint[] dpsB) {
    contentPane.getChildren().removeAll();
    mirrorSpecrumPlot = MirrorChartFactory
        .createMirrorChartViewer(labelA, precursorMZA, rtA, dpsA,
            labelB, precursorMZB, rtB, dpsB, false, true);
    contentPane.setCenter(mirrorSpecrumPlot);
  }

  /**
   * Set scan and mirror scan and create chart
   *
   * @param scan
   * @param mirror
   */
  public void setScans(Scan scan, Scan mirror) {
    contentPane.getChildren().removeAll();
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorChartViewer(scan, mirror,
        scan.getScanDefinition(), mirror.getScanDefinition(), false, true);
    contentPane.setCenter(mirrorSpecrumPlot);

  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    contentPane.getChildren().removeAll();
    mirrorSpecrumPlot =
        MirrorChartFactory.createMirrorChartViewer(scan, mirror, labelA, labelB, false, true);
    contentPane.setCenter(mirrorSpecrumPlot);
  }

  /**
   * Based on a data base match to a spectral library
   *
   * @param db
   */
  public void setScans(SpectralDBFeatureIdentity db) {
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromSpectralDBPeakIdentity(db);
    contentPane.setCenter(mirrorSpecrumPlot);
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

  public EChartViewer getMirrorSpecrumPlot() {
    return mirrorSpecrumPlot;
  }

  public void setMirrorSpecrumPlot(EChartViewer mirrorSpecrumPlot) {
    this.mirrorSpecrumPlot = mirrorSpecrumPlot;
  }

}

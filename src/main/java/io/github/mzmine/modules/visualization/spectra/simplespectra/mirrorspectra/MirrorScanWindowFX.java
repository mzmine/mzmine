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
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.Arrays;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Creates a window with a mirror chart to compare to scans
 *
 * @author Robin Schmid & Steffen Heuckeroth
 */
public class MirrorScanWindowFX extends Stage {

  // for SpectralDBIdentity

  public static final DataPointsTag[] tags = new DataPointsTag[]{DataPointsTag.ORIGINAL,
      DataPointsTag.FILTERED, DataPointsTag.ALIGNED};

  private final BorderPane contentPane;
  private final BorderPane pnMirror;
  private final BorderPane neutralLossMirror;
  private final GridPane center;
  private final Label lbMirrorStats;
  private final Label lbNeutralLossStats;
  private final Label lbMirrorModifiedStats;
  private EChartViewer mirrorSpecrumPlot;
  private EChartViewer neutralLossMirrorSpecrumPlot;

  /**
   * Create the frame.
   */
  public MirrorScanWindowFX() {

    contentPane = new BorderPane();
    contentPane.setStyle("-fx-border-width: 5;");
    contentPane.setPrefSize(800, 800);

    center = new GridPane();
    final ColumnConstraints col = new ColumnConstraints();
    col.setHgrow(Priority.ALWAYS);
    col.setFillWidth(true);
    center.getColumnConstraints().add(col);

    final RowConstraints row1 = new RowConstraints();
    row1.setVgrow(Priority.ALWAYS);
    row1.setFillHeight(true);
    final RowConstraints row2 = new RowConstraints();
    row2.setVgrow(Priority.ALWAYS);
    row2.setFillHeight(true);
    center.getRowConstraints().addAll(row1, row2);

    pnMirror = new BorderPane();
    neutralLossMirror = new BorderPane();
    center.add(pnMirror, 0, 0);
    center.add(neutralLossMirror, 0, 1);

    // labels
    lbMirrorStats = new Label("");
    lbMirrorModifiedStats = new Label("");
    pnMirror.setTop(new VBox(lbMirrorStats, lbMirrorModifiedStats));

    lbNeutralLossStats = new Label("");
    neutralLossMirror.setTop(lbNeutralLossStats);

    contentPane.setCenter(center);

    this.setScene(new Scene(contentPane));
  }

  public void setScans(double precursorMZA, DataPoint[] dpsA, double precursorMZB,
      DataPoint[] dpsB) {
    pnMirror.getChildren().removeAll();
    neutralLossMirror.getChildren().removeAll();

    final ParameterSet params = MZmineCore.getConfiguration()
        .getModuleParameters(MirrorScanModule.class);
    final MZTolerance mzTol = params.getValue(MirrorScanParameters.mzTol);

    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromAligned(mzTol, true, dpsA,
        precursorMZA, dpsB, precursorMZB);
    pnMirror.setCenter(mirrorSpecrumPlot);

    if (precursorMZA > 0 && precursorMZB > 0) {
      neutralLossMirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromAligned(mzTol, false,
          ScanUtils.getNeutralLossSpectrum(dpsA, precursorMZA), precursorMZA,
          ScanUtils.getNeutralLossSpectrum(dpsB, precursorMZB), precursorMZB);
      neutralLossMirror.setCenter(neutralLossMirrorSpecrumPlot);

      //
      calcSpectralSimilarity(dpsA, precursorMZA, dpsB, precursorMZB);
    }
  }

  private void calcSpectralSimilarity(DataPoint[] dpsA, double precursorMZA, DataPoint[] dpsB,
      double precursorMZB) {
    final ParameterSet params = MZmineCore.getConfiguration()
        .getModuleParameters(MirrorScanModule.class);
    final MZTolerance mzTol = params.getValue(MirrorScanParameters.mzTol);

    // needs to be sorted
    Arrays.sort(dpsA, DataPointSorter.DEFAULT_INTENSITY);
    Arrays.sort(dpsB, DataPointSorter.DEFAULT_INTENSITY);
    SpectralSimilarity cosine = MS2SimilarityTask.createMS2Sim(mzTol, dpsA, dpsB, 2);

    if (cosine != null) {
      lbMirrorStats.setText(String.format(
          "  cosine=%1.3f; matched signals=%d; explained intensity top=%1.3f; explained intensity bottom=%1.3f; matched signals top=%1.3f; matched signals bottom=%1.3f",
          cosine.cosine(), cosine.overlap(), cosine.explainedIntensityA(),
          cosine.explainedIntensityB(), cosine.overlap() / (double) cosine.sizeA(),
          cosine.overlap() / (double) cosine.sizeB()));
    } else {
      lbMirrorStats.setText("");
    }

    //modified cosine
    cosine = MS2SimilarityTask.createMS2SimModificationAware(mzTol, dpsA, dpsB, 2,
        MS2SimilarityTask.SIZE_OVERLAP, precursorMZA, precursorMZB);
    if (cosine != null) {
      lbMirrorModifiedStats.setText(String.format(
          "modified=%1.3f; matched signals=%d; explained intensity top=%1.3f; explained intensity bottom=%1.3f; matched signals top=%1.3f; matched signals bottom=%1.3f",
          cosine.cosine(), cosine.overlap(), cosine.explainedIntensityA(),
          cosine.explainedIntensityB(), cosine.overlap() / (double) cosine.sizeA(),
          cosine.overlap() / (double) cosine.sizeB()));
    } else {
      lbMirrorModifiedStats.setText("");
    }

    // neutral loss
    final DataPoint[] nlA = ScanUtils.getNeutralLossSpectrum(dpsA, precursorMZA);
    final DataPoint[] nlB = ScanUtils.getNeutralLossSpectrum(dpsB, precursorMZB);
    Arrays.sort(nlA, DataPointSorter.DEFAULT_INTENSITY);
    Arrays.sort(nlB, DataPointSorter.DEFAULT_INTENSITY);

    cosine = MS2SimilarityTask.createMS2Sim(mzTol, nlA, nlB, 2);
    if (cosine != null) {
      lbNeutralLossStats.setText(String.format(
          "cosine=%1.3f; matched signals=%d; explained intensity top=%1.3f; explained intensity bottom=%1.3f; matched signals top=%1.3f; matched signals bottom=%1.3f",
          cosine.cosine(), cosine.overlap(), cosine.explainedIntensityA(),
          cosine.explainedIntensityB(), cosine.overlap() / (double) cosine.sizeA(),
          cosine.overlap() / (double) cosine.sizeB()));
    } else {
      lbNeutralLossStats.setText("");
    }
  }


  /**
   * Set scan and mirror scan and create chart
   *
   * @param scan
   * @param mirror
   */
  public void setScans(Scan scan, Scan mirror) {
    setScans(scan.getPrecursorMz(), ScanUtils.extractDataPoints(scan.getMassList()),
        mirror.getPrecursorMz(), ScanUtils.extractDataPoints(mirror.getMassList()));
  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    setScans(scan, mirror);
  }

  /**
   * Based on a data base match to a spectral library
   *
   * @param db
   */
  public void setScans(SpectralDBFeatureIdentity db) {
    pnMirror.getChildren().clear();
    neutralLossMirror.getChildren().removeAll();
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromSpectralDBPeakIdentity(db);
    pnMirror.setCenter(mirrorSpecrumPlot);
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

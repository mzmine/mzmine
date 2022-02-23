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
import io.github.mzmine.gui.framework.FormattedTableCell;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.CosinePairContributions;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityTask;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.SignalAlignmentAnnotation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.components.ColorPickerTableCell;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */

public class MirrorScanWindowController {

  public static final DataPointsTag[] tags = new DataPointsTag[]{DataPointsTag.ORIGINAL,
      DataPointsTag.FILTERED, DataPointsTag.ALIGNED};

  // components
  @FXML
  private BorderPane pnMirror;
  @FXML
  private TableView<TableData> tableMirror;
  @FXML
  private BorderPane pnNLMirror;
  @FXML
  private TableView<TableData> tableNLMIrror;
  @FXML
  private Label lbMirrorStats;
  @FXML
  private Label lbNeutralLossStats;
  @FXML
  private Label lbMirrorModifiedStats;


  @FXML
  private TableColumn<TableData, Double> colMzTop;

  @FXML
  private TableColumn<TableData, Double> colIntensityTop;

  @FXML
  private TableColumn<TableData, Double> colMzBottom;

  @FXML
  private TableColumn<TableData, Double> colIntensityBottom;

  @FXML
  private TableColumn<TableData, SignalAlignmentAnnotation> colMatch;
  @FXML
  private TableColumn<TableData, Color> colMatchColor;

  @FXML
  private TableColumn<TableData, Double> colContribution;

  // data
  private EChartViewer mirrorSpecrumPlot;
  private EChartViewer neutralLossMirrorSpecrumPlot;

  @FXML
  public void initialize() {
    final MZmineConfiguration config = MZmineCore.getConfiguration();

    colMzTop.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().mzA()));
    colIntensityTop.setCellValueFactory(
        row -> new SimpleObjectProperty<>(row.getValue().intensityA()));
    colMzBottom.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().mzB()));
    colIntensityBottom.setCellValueFactory(
        row -> new SimpleObjectProperty<>(row.getValue().intensityB()));
    colMatch.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().match()));
    colMatchColor.setCellValueFactory(
        row -> new SimpleObjectProperty<>(getColor(row.getValue().match())));
    colContribution.setCellValueFactory(
        row -> new SimpleDoubleProperty(row.getValue().contribution()).asObject());

    colMatchColor.setCellFactory(ColorPickerTableCell::new);
    colMzTop.setCellFactory(col -> new FormattedTableCell<>(config.getMZFormat()));
    colMzBottom.setCellFactory(col -> new FormattedTableCell<>(config.getMZFormat()));
    colIntensityTop.setCellFactory(col -> new FormattedTableCell<>(config.getIntensityFormat()));
    colIntensityBottom.setCellFactory(col -> new FormattedTableCell<>(config.getIntensityFormat()));
    colContribution.setCellFactory(col -> new FormattedTableCell<>(config.getScoreFormat()));

  }

  private Color getColor(SignalAlignmentAnnotation match) {
    return switch (match) {
      case MATCH -> MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColor();
      case MODIFIED -> MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColor();
      case NONE, FILTERED -> MZmineCore.getConfiguration().getDefaultColorPalette()
          .getNeutralColor();
    };
  }

  public void setScans(double precursorMZA, DataPoint[] dpsA, double precursorMZB,
      DataPoint[] dpsB) {
    pnMirror.getChildren().removeAll();
    pnNLMirror.getChildren().removeAll();

    final MZTolerance mzTol = getMzTolerance();

    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromAligned(mzTol, true, dpsA,
        precursorMZA, dpsB, precursorMZB);
    pnMirror.setCenter(mirrorSpecrumPlot);

    if (precursorMZA > 0 && precursorMZB > 0) {
      neutralLossMirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromAligned(mzTol, false,
          ScanUtils.getNeutralLossSpectrum(dpsA, precursorMZA), precursorMZA,
          ScanUtils.getNeutralLossSpectrum(dpsB, precursorMZB), precursorMZB);
      pnNLMirror.setCenter(neutralLossMirrorSpecrumPlot);

      //
      calcSpectralSimilarity(dpsA, precursorMZA, dpsB, precursorMZB);
    }
  }

  private MZTolerance getMzTolerance() {
    final ParameterSet params = MZmineCore.getConfiguration()
        .getModuleParameters(MirrorScanModule.class);
    return params.getValue(MirrorScanParameters.mzTol);
  }

  private void calcSpectralSimilarity(DataPoint[] dpsA, double precursorMZA, DataPoint[] dpsB,
      double precursorMZB) {
    final MZTolerance mzTol = getMzTolerance();

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

      // get contributions of all data points
      final CosinePairContributions contributions = MS2SimilarityTask.calculateModifiedCosineSimilarityContributions(
          mzTol, dpsA, dpsB, precursorMZA, precursorMZB);

      if (contributions != null) {
        List<TableData> data = new ArrayList<>(contributions.size());
        for (int i = 0; i < contributions.size(); i++) {
          final DataPoint[] pair = contributions.pairs().get(i);
          Double mza = pair[0] != null ? pair[0].getMZ() : null;
          Double intensitya = pair[0] != null ? pair[0].getIntensity() : null;
          Double mzb = pair[1] != null ? pair[1].getMZ() : null;
          Double intensityb = pair[1] != null ? pair[1].getIntensity() : null;

          data.add(new TableData(mza, intensitya, mzb, intensityb, contributions.match()[i],
              contributions.contributions()[i]));
        }

        tableMirror.getItems().addAll(data);
        colContribution.setSortType(SortType.DESCENDING);
      }
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
    pnNLMirror.getChildren().removeAll();
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromSpectralDBPeakIdentity(db);
    pnMirror.setCenter(mirrorSpecrumPlot);
  }

}

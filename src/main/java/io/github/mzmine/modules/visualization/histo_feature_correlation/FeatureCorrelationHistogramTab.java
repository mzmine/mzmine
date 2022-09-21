/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.histo_feature_correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramData;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramPanel;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

/**
 * Histogram of Pearson correlation values for grouped features Enhanced version. Use arrows to jump
 * to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FeatureCorrelationHistogramTab extends MZmineTab {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  private final ModularFeatureList flist;
  protected HistogramPanel histoUnidentified;
  protected HistogramPanel histoIonIdentities;

  // parameters
  private final HistogramData dataUnidentified;
  private final HistogramData dataIdentified;

  public FeatureCorrelationHistogramTab(ModularFeatureList flist,
      DoubleArrayList valuesUnidentified, DoubleArrayList valuesIdentified, String title,
      String xLabel, double binWidth) {
    super(title, true, false);

    this.flist = flist;
    dataUnidentified = new HistogramData(valuesUnidentified.toDoubleArray());
    dataIdentified = new HistogramData(valuesIdentified.toDoubleArray());

    mainPane = new BorderPane();
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histoUnidentified = new HistogramPanel(xLabel + " (unidentified)", dataUnidentified, binWidth);

    histoIonIdentities = new HistogramPanel(xLabel + " (connected ion identities)", dataIdentified,
        binWidth);

    setContent(mainPane);

    GridPane gridPane = new GridPane();
    gridPane.add(histoUnidentified, 0, 0);
    gridPane.add(histoIonIdentities, 1, 0);
    GridPane.setVgrow(histoUnidentified, Priority.ALWAYS);
    GridPane.setVgrow(histoIonIdentities, Priority.ALWAYS);
    GridPane.setHgrow(histoUnidentified, Priority.ALWAYS);
    GridPane.setHgrow(histoIonIdentities, Priority.ALWAYS);
    mainPane.setCenter(gridPane);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return List.of(flist);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}

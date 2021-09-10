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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.histo_feature_correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.mzhistogram.chart.HistogramData;
import io.github.mzmine.modules.visualization.mzhistogram.chart.HistogramPanel;
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
  protected HistogramPanel histo;
  protected HistogramPanel histoDeltaNeutralMass;

  // parameters
  private final HistogramData dataUnidentified;
  private final HistogramData dataIdentified;

  public FeatureCorrelationHistogramTab(ModularFeatureList flist,
      DoubleArrayList valuesUnidentified,
      DoubleArrayList valuesIdentified, String title, String xLabel, double binWidth) {
    super(title, true, false);

    this.flist = flist;
    dataUnidentified = new HistogramData(valuesUnidentified.toDoubleArray());
    dataIdentified = new HistogramData(valuesIdentified.toDoubleArray());

    mainPane = new BorderPane();
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histo = new HistogramPanel(xLabel + " (unidentified)", dataUnidentified, binWidth);

    histoDeltaNeutralMass = new HistogramPanel(xLabel + " (connected ion identities)",
        dataIdentified,
        binWidth);

    setContent(mainPane);

    GridPane gridPane = new GridPane();
    gridPane.add(histo, 0, 0);
    gridPane.add(histoDeltaNeutralMass, 1, 0);
    GridPane.setVgrow(histo, Priority.ALWAYS);
    GridPane.setVgrow(histoDeltaNeutralMass, Priority.ALWAYS);
    GridPane.setHgrow(histo, Priority.ALWAYS);
    GridPane.setHgrow(histoDeltaNeutralMass, Priority.ALWAYS);
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
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists) {

  }
}

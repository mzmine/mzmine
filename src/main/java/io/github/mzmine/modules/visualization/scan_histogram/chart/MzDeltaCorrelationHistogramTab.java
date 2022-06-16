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

package io.github.mzmine.modules.visualization.scan_histogram.chart;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.scan_histogram.ScanHistogramParameters;
import io.github.mzmine.parameters.ParameterSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;

/**
 * Enhanced version. Use arrows to jump to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MzDeltaCorrelationHistogramTab extends MZmineTab {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  private final ModularFeatureList flist;
  protected HistogramPanel histo;
  protected HistogramPanel histoDeltaNeutralMass;

  // scan counter
  private int processedScans, totalScans;

  // parameters
  private final double binWidth;

  private final HistogramData data;
  private final HistogramData dataDeltaNeutralMass;

  public MzDeltaCorrelationHistogramTab(ModularFeatureList flist, DoubleArrayList deltaMZList,
      DoubleArrayList deltaMZToNeutralMassList, String title, String xLabel,
      ParameterSet parameters) {
    super(title, true, false);

    this.flist = flist;
    binWidth = parameters.getParameter(ScanHistogramParameters.binWidth).getValue();
    data = new HistogramData(deltaMZList.toDoubleArray());
    dataDeltaNeutralMass = new HistogramData(deltaMZToNeutralMassList.toDoubleArray());

    mainPane = new BorderPane();
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histo = new HistogramPanel(xLabel, data, binWidth);

    histoDeltaNeutralMass = new HistogramPanel(xLabel + " (neutral mass)", dataDeltaNeutralMass,
        binWidth);

    //setMinWidth(1050);
    //setMinHeight(700);
    //setScene(mainScene);
    setContent(mainPane);

    GridPane gridPane = new GridPane();
    gridPane.add(histo, 0, 0);
    gridPane.add(histoDeltaNeutralMass, 1, 0);
    GridPane.setVgrow(histo, Priority.ALWAYS);
    GridPane.setVgrow(histoDeltaNeutralMass, Priority.ALWAYS);
    GridPane.setHgrow(histo, Priority.ALWAYS);
    GridPane.setHgrow(histoDeltaNeutralMass, Priority.ALWAYS);
    mainPane.setCenter(gridPane);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);
  }

  private XYPlot getXYPlot(HistogramPanel histo) {
    ChartViewer chart = histo.getChartPanel();
    if (chart != null) {
      return chart.getChart().getXYPlot();
    } else {
      return null;
    }
  }

  public int getTotalScans() {
    return totalScans;
  }

  public int getProcessedScans() {
    return processedScans;
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

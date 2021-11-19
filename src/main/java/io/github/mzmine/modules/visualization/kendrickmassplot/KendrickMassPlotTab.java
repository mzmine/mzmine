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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class KendrickMassPlotTab extends MZmineTab {
  private final KendrickMassPlotAnchorPaneController controller;

  public KendrickMassPlotTab(ParameterSet parameters, EChartViewer chartViewer) {
    super("Kendrick Mass Plot", true, false);

    AnchorPane root = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("KendrickMassPlotAnchorPane.fxml")));
    try {
      root = loader.load();
      logger.finest("Root element of Kendrick Mass Plot tab has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Get controller
    controller = loader.getController();
    controller.initialize(parameters);
    BorderPane plotPane = controller.getPlotPane();
    plotPane.setCenter(chartViewer);

    setContent(root);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return controller.getFeatureList().getRawDataFiles();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return
        new ArrayList<>(Collections.singletonList((ModularFeatureList)controller.getFeatureList()));
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

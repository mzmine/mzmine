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

package io.github.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class Fx3DVisualizerTab extends MZmineTab {
  private final Fx3DBorderPaneController controller;

  public Fx3DVisualizerTab(RawDataFile dataFiles[], ScanSelection scanSel, Range<Float> rtRange, Range<Double> mzRange,
      int rtRes, int mzRes, List<Feature> featureSelList) {
    super("3D Visualizer", true, false);

    FXMLLoader loader = new FXMLLoader(getClass().getResource("Fx3DBorderPane.fxml"));
    BorderPane borderPane = null;
    try {
      borderPane = loader.load();
      logger.finest("3D Visualizer tab has been successfully loaded from the FXML loader.");
    } catch (Exception e) {
      e.printStackTrace();
    }

    controller = loader.getController();
    controller.setScanSelection(scanSel);
    controller.setRtAndMzResolutions(rtRes, mzRes);
    controller.setRtAndMzValues(rtRange, mzRange);
    for (RawDataFile dataFile : dataFiles) {
      MZmineCore.getTaskController().addTask(
          new Fx3DSamplingTask(dataFile, scanSel, mzRange, rtRes, mzRes, controller),
          TaskPriority.HIGH);

    }
    controller.addFeatureSelections(featureSelList);

    setContent(borderPane);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return (List<RawDataFile>)(List<?>)(controller.getVisualizedFiles());
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    controller.updateVisualizedFiles(rawDataFiles);
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists) {

  }
}

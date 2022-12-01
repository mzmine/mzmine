/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.visualization.image.ImageVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImagingPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ImagingUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.SortableFeatureComboBox;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.jfree.chart.annotations.XYPointerAnnotation;

public class TimsTOFImageMsMsDialog extends ParameterSetupDialogWithPreview {

  private final ImagingPlot imageChart = new ImagingPlot(
      MZmineCore.getConfiguration().getModuleParameters(ImageVisualizerModule.class));
  private SimsefImagingSchedulerTask currentTask;

  private PauseTransition delay = new PauseTransition(Duration.millis(500));

  public TimsTOFImageMsMsDialog(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  public TimsTOFImageMsMsDialog(boolean valueCheckRequired, ParameterSet parameters,
      String message) {
    super(valueCheckRequired, parameters, message);

    SortableFeatureComboBox fBox = new SortableFeatureComboBox();

    ComboBox<FeatureList> cmbFlist = new ComboBox<>();
    final ObservableList<FeatureList> flists = FXCollections.observableArrayList(
        MZmineCore.getProject().getCurrentFeatureLists().stream().filter(
            fl -> fl.getNumberOfRawDataFiles() == 1 && fl.getRawDataFile(
                0) instanceof IMSImagingRawDataFile).toList());

    cmbFlist.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.getFeatureBox().setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
          }
        }));

    cmbFlist.setItems(flists);
    if (!flists.isEmpty()) {
      cmbFlist.getSelectionModel().selectFirst();
    }

    fBox.getFeatureBox().getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            featureChanged(newValue);
          }
        }));

    delay.setOnFinished(e -> {
      updateParameterSetFromComponents();
      if (currentTask != null) {
        currentTask.cancel();
      }

      final List<String> errors = new ArrayList<>();
      if (!parameters.checkParameterValues(errors)) {
        String msg =
            "Please check parameter values:\n" + errors.stream().collect(Collectors.joining("\n"));
        MZmineCore.getDesktop().displayErrorMessage(msg);
        return;
      }

      final ParameterSet param = parameters.cloneParameterSet();
      param.setParameter(SimsefImagingSchedulerParameters.flists,
          new FeatureListsSelection((ModularFeatureList) cmbFlist.getValue()));

      currentTask = new SimsefImagingSchedulerTask(null, Instant.now(), param,
          MZmineCore.getProject(), true, true);
      MZmineCore.getTaskController().addTask(currentTask);

      currentTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
        logger.finest("Preview task status changed: " + newStatus);
        if (newStatus == TaskStatus.FINISHED && task == currentTask) {
          imageChart.getChart()
              .applyWithNotifyChanges(false, () -> updateMarkers(fBox.getFeatureBox().getValue()));
        }
      });
    });

    previewWrapperPane.setCenter(imageChart);
    VBox vbox = new VBox(cmbFlist, fBox);
    previewWrapperPane.setBottom(vbox);
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();

    if (cbShowPreview.isSelected()) {
      delay.playFromStart();
    }
  }

  private void featureChanged(Feature newFeature) {
    imageChart.getChart().applyWithNotifyChanges(false, () -> {
      imageChart.getChart().removeAllDatasets();
      imageChart.getChart().getChart().getXYPlot().clearAnnotations();

      imageChart.setData(newFeature);
      if (currentTask != null && currentTask.isFinished()) {
        imageChart.getChart().applyWithNotifyChanges(true, () -> updateMarkers(newFeature));
      } else if (currentTask == null) {
        delay.playFromStart();
      }
    });
  }

  /**
   * Updates MSMS markers, does not notify the plot for changes.
   *
   * @param newFeature The feature.
   */
  private void updateMarkers(Feature newFeature) {
    if (currentTask == null) {
      delay.playFromStart();
      return;
    }

    if (newFeature == null) {
      return;
    }

    final Map<Feature, List<ImagingSpot>> map = currentTask.getFeatureSpotMap();

    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette()
        .clone();

    final Font markerFont = MZmineCore.getConfiguration().getDefaultChartTheme().getRegularFont();

    imageChart.getChart().getXYPlot().clearAnnotations();

    final List<ImagingSpot> imagingSpots = map.get(newFeature);
    for (ImagingSpot imagingSpot : imagingSpots) {
      final MaldiSpotInfo info = imagingSpot.spotInfo();
      final double[] ms2Coord = ImagingUtils.transformCoordinates(info,
          (ImagingRawDataFile) newFeature.getRawDataFile());
      final Map<Float, Color> ceColor = new HashMap<>();

      XYPointerAnnotation msMsMarker = new XYPointerAnnotation(
          String.format("%.0f eV", imagingSpot.getCollisionEnergy()), ms2Coord[0], ms2Coord[1],
          315);
      final BasicStroke stroke = new BasicStroke(3.0f);
      final Color clr = ceColor.computeIfAbsent(imagingSpot.getCollisionEnergy().floatValue(),
          energy -> palette.getNextColorAWT());
      msMsMarker.setArrowPaint(clr);
      msMsMarker.setArrowWidth(3d);
      msMsMarker.setArrowStroke(stroke);
      msMsMarker.setLabelOffset(10);
      msMsMarker.setFont(markerFont);

      imageChart.getChart().getXYPlot().addAnnotation(msMsMarker, false);
    }
  }
}

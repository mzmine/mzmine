/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.CalculateDatasetsTask;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.PlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;

/**
 * @author https://github.com/SteffenHeu
 */
public class MobilityMzRegionExtractionSetupDialog extends ParameterSetupDialogWithPreview {

  private final SimpleXYZScatterPlot heatmap;
  private final RegionSelectionWrapper<SimpleXYZScatterPlot> wrapper;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final NumberFormat ccsFormat;
  private final UnitFormat unitFormat;
  private final ComboBox<FeatureList> comboBox;

  public MobilityMzRegionExtractionSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  public MobilityMzRegionExtractionSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      TextFlow message) {
    super(valueCheckRequired, parameters, message);
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    ccsFormat = MZmineCore.getConfiguration().getCCSFormat();

    heatmap = new SimpleXYZScatterPlot<>();
    heatmap.setDomainAxisLabel("m/z");
    heatmap.setDomainAxisNumberFormatOverride(mzFormat);
    heatmap.setRangeAxisLabel("Mobility");
    heatmap.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmap.setLegendAxisLabel(unitFormat.format("Intensity", "a.u."));
    heatmap.setLegendNumberFormatOverride(intensityFormat);
    heatmap.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmap.getXYPlot().setDomainCrosshairPaint(Color.LIGHT_GRAY);
    heatmap.getXYPlot().setRangeCrosshairPaint(Color.LIGHT_GRAY);

    wrapper = new RegionSelectionWrapper<>(heatmap);

    previewWrapperPane.setCenter(wrapper);

    FlowPane fp = new FlowPane(new Label("Feature list "));
    fp.setHgap(5);

    var featureLists = FXCollections.observableArrayList(
        ProjectService.getProjectManager().getCurrentProject().getCurrentFeatureLists());
    comboBox = new ComboBox<>(featureLists);
    comboBox.valueProperty().addListener(((observable, oldValue, newValue) -> parametersChanged()));

    wrapper.getFinishedRegionSelectionListeners()
        .addListener((ListChangeListener<RegionSelectionListener>) c -> {
          parameters.getParameter(MobilityMzRegionExtractionParameters.regions)
              .setValue(wrapper.getFinishedRegionsAsListOfPointLists());
        });

    fp.getChildren().add(comboBox);
    fp.setAlignment(Pos.TOP_CENTER);
    previewWrapperPane.setBottom(fp);
  }

  @Override
  protected void parametersChanged() {
    updateParameterSetFromComponents();

    List<? extends FeatureListRow> features = comboBox.getValue().getRows();
    PlotType pt = parameterSet.getParameter(MobilityMzRegionExtractionParameters.ccsOrMobility)
        .getValue();
    if (pt == PlotType.MOBILITY) {
      heatmap.setRangeAxisLabel("Mobility");
      heatmap.setRangeAxisNumberFormatOverride(mobilityFormat);
    } else {
      heatmap.setRangeAxisLabel(unitFormat.format("CCS", "A^2"));
      heatmap.setRangeAxisNumberFormatOverride(ccsFormat);
    }

    heatmap.removeAllDatasets();
    CalculateDatasetsTask calc = new CalculateDatasetsTask(
        (Collection<ModularFeatureListRow>) features, pt, false);
    MZmineCore.getTaskController().addTask(calc);
    calc.addTaskStatusListener((task, newStatus, oldStatus) -> {
      if (newStatus == TaskStatus.FINISHED) {
        Platform.runLater(() -> {
          var datasetsRenderers = calc.getDatasetsRenderers();
          heatmap.addDatasetsAndRenderers(datasetsRenderers);
          heatmap.setLegendPaintScale(calc.getPaintScale());
        });
      }
    });
  }
}

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

package io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleXYLabelGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.text.NumberFormat;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class MobilityScanMergerSetupDialog extends ParameterSetupDialogWithPreview {

  private final SimpleXYChart<MassSpectrumProvider> plot;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;
  private final SimpleParameterSet parameterSet;
  private final ComboBox<Frame> frameComboBox;

  public MobilityScanMergerSetupDialog(boolean valueCheckRequired, SimpleParameterSet parameterSet) {
    super(valueCheckRequired, parameterSet);
    this.parameterSet = parameterSet;

    plot = new SimpleXYChart<>();
    plot.setDomainAxisLabel("m/z");
    plot.setRangeAxisLabel("Intensity");

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    previewWrapperPane.setCenter(plot);

    final GridPane controlPane = new GridPane();
    previewWrapperPane.setBottom(controlPane);
    controlPane.setPadding(new Insets(5));
    controlPane.setHgap(5);
    controlPane.setVgap(5);

    RawDataFile[] files = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    ComboBox<RawDataFile> fileComboBox = new ComboBox<>(FXCollections.observableArrayList(files));
    frameComboBox = new ComboBox<>();

    fileComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue instanceof IMSRawDataFile) {
        frameComboBox
            .setItems(FXCollections.observableArrayList(((IMSRawDataFile) newValue).getFrames()));
      }
    });

    frameComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        parametersChanged();
      }
    });

    controlPane.add(new Label("Raw data file"), 0, 0);
    controlPane.add(fileComboBox, 1, 0);
    controlPane.add(new Label("Frame"), 0, 1);
    controlPane.add(frameComboBox, 1, 1);
  }

  public MobilityScanMergerSetupDialog(SimpleParameterSet parameterSet) {
    this(true, parameterSet);
  }

  @Override
  protected void parametersChanged() {
    if (frameComboBox.getValue() == null) {
      return;
    }

    updateParameterSetFromComponents();
    final double noiseLevel = parameterSet.getParameter(MobilityScanMergerParameters.noiseLevel)
        .getValue();
    IntensityMergingType intensityMergingType = parameterSet.getParameter(
        MobilityScanMergerParameters.mergingType).getValue();
    MZTolerance mzTolerance = parameterSet.getParameter(MobilityScanMergerParameters.mzTolerance)
        .getValue();
    Weighting weighting = parameterSet.getParameter(MobilityScanMergerParameters.weightingType)
        .getValue();

    if (intensityMergingType == null || mzTolerance == null || frameComboBox.getValue() == null) {
      return;
    }

    double[][] merged;
    try {
      merged = SpectraMerging.calculatedMergedMzsAndIntensities(
          frameComboBox.getValue().getMobilityScans().stream().map(MobilityScan::getMassList)
              .toList(), mzTolerance, intensityMergingType,
          new CenterFunction(CenterMeasure.AVG, weighting), null, noiseLevel, null);
    } catch (NullPointerException e) {
      MZmineCore.getDesktop().displayErrorMessage(
          "No mass list present in " + frameComboBox.getValue().getDataFile().getName()
              + ".\nPlease run mass detection first.");
      return;
    }

    ColoredXYBarRenderer coloredXYBarRenderer = new ColoredXYBarRenderer(false);
    coloredXYBarRenderer.setDefaultItemLabelGenerator(new SimpleXYLabelGenerator(plot));

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    plot.removeAllDatasets();
    plot.addDataset(new ColoredXYDataset(new MassSpectrumProvider(merged[0], merged[1], "Preview")),
        coloredXYBarRenderer);
    theme.apply(plot.getChart());
  }
}


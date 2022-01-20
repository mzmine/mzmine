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
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
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
    double noiseLevel = parameterSet.getParameter(MobilityScanMergerParameters.noiseLevel)
        .getValue();
    MergingType mergingType = parameterSet.getParameter(MobilityScanMergerParameters.mergingType)
        .getValue();
    MZTolerance mzTolerance = parameterSet.getParameter(MobilityScanMergerParameters.mzTolerance)
        .getValue();
    Weighting weighting = parameterSet.getParameter(MobilityScanMergerParameters.weightingType)
        .getValue();

    if (mergingType == null || mzTolerance == null || frameComboBox.getValue() == null) {
      return;
    }

    double[][] merged;
    try {
      merged = SpectraMerging.calculatedMergedMzsAndIntensities(
          frameComboBox.getValue().getMobilityScans().stream().map(MobilityScan::getMassList)
              .toList(), mzTolerance, mergingType, new CenterFunction(CenterMeasure.AVG, weighting),
          noiseLevel, null);
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


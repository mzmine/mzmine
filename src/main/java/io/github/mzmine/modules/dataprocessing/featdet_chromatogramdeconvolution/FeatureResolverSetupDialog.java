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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.graphicalnodes.provider.MsTimeSeriesXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class FeatureResolverSetupDialog extends ParameterSetupDialogWithPreview {

  protected final SimpleXYChart<MsTimeSeriesXYProvider> previewChart;
  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;

  protected ComboBox<ModularFeatureList> flistBox;
  protected ComboBox<ModularFeature> fBox;

  public FeatureResolverSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters, String message) {
    super(valueCheckRequired, parameters, message);
    uf = MZmineCore.getConfiguration().getUnitFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    previewChart = new SimpleXYChart<>(uf.format("Retention time", "min"),
        uf.format("Intensity", "cps"));
    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ObservableList<ModularFeatureList> flists = (ObservableList<ModularFeatureList>)
        (ObservableList<? extends FeatureList>) MZmineCore.getProjectManager().getCurrentProject()
            .getFeatureLists();

    fBox = new ComboBox<>();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if(newValue!=null)
            fBox.setItems((ObservableList<ModularFeature>)(ObservableList<? extends Feature>) newValue.getFeatures(newValue.getRawDataFile(0)));
          else
            fBox.setItems(FXCollections.emptyObservableList());
        }));
    fBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> onSelectedFeatureChanged(newValue)));

    GridPane pnControls = new GridPane();
    pnControls.add(new Label("Feature list"), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature"), 0, 1);
    pnControls.add(fBox, 1, 1);
    previewWrapperPane.setBottom(pnControls);
    previewWrapperPane.setCenter(previewChart);
  }

  protected void onSelectedFeatureChanged(ModularFeature newValue) {
    if(newValue == null) {
      return;
    }
    previewChart.removeAllDatasets();
    Platform.runLater(() -> previewChart
        .addDataset(new FastColoredXYDataset(new MsTimeSeriesXYProvider(newValue))));
    ResolvedPeak[] resolved = resolveFeature(newValue);
    if (resolved.length == 0) {
      return;
    }
    int resolvedFeatureCounter = 0;
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    for (ResolvedPeak rp : resolved) {
      FastColoredXYDataset ds = new FastColoredXYDataset(rp);
      ds.setColor(FxColorUtil.fxColorToAWT(palette.get(resolvedFeatureCounter++)));
      Platform.runLater(() -> previewChart.addDataset(ds, new ColoredXYShapeRenderer()));
    }
  }

  protected ResolvedPeak[] resolveFeature(ModularFeature feature) {
    FeatureResolver resolver = ((GeneralResolverParameters) parameterSet).getResolver();
    if (fBox.getValue() == null) {
      return null;
    }
    CenterFunction cf = new CenterFunction(CenterMeasure.MEDIAN, Weighting.logger10, 0, 4);
    try {
      RSessionWrapper rWrapper = null;
      if (resolver.getRequiresR()) {
        // Check R availability, by trying to open the
        // connection.
        String[] reqPackages = resolver.getRequiredRPackages();
        String[] reqPackagesVersions = resolver.getRequiredRPackagesVersions();
        String callerFeatureName = resolver.getName();
        REngineType rEngineType = parameterSet.getParameter(GeneralResolverParameters.RENGINE_TYPE)
            .getValue();
        rWrapper = new RSessionWrapper(rEngineType, callerFeatureName, reqPackages,
            reqPackagesVersions);
        rWrapper.open();
      }
      ResolvedPeak[] resolvedFeatures = resolver
          .resolvePeaks(fBox.getValue(), parameterSet, rWrapper, cf, 0, 0);
      if (rWrapper != null) {
        rWrapper.close(false);
      }
      return resolvedFeatures;
    } catch (RSessionWrapperException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE, "Feature deconvolution error", e);
    }
    return null;
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updateParameterSetFromComponents();
    List<String> errors = new ArrayList<>();
    if(parameterSet.checkParameterValues(errors)) {
      onSelectedFeatureChanged(fBox.getValue());
    }
  }

  @Override
  public void setOnPreviewShown(Runnable onPreviewShown) {
    super.setOnPreviewShown(onPreviewShown);
  }

}

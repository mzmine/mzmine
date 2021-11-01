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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.util.FeatureUtils;
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
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class FeatureResolverSetupDialog extends ParameterSetupDialogWithPreview {

  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChart;
  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;
  protected ComboBox<ModularFeatureList> flistBox;
  protected ComboBox<ModularFeature> fBox;
  protected ColoredXYShapeRenderer shapeRenderer = new ColoredXYShapeRenderer();
  protected BinningMobilogramDataAccess mobilogramBinning;
  protected Resolver resolver;

  public FeatureResolverSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      String message) {
    super(valueCheckRequired, parameters, message);
    uf = MZmineCore.getConfiguration().getUnitFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    previewChart = new SimpleXYChart<>(uf.format("Retention time", "min"),
        uf.format("Intensity", "a.u."));
    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ObservableList<ModularFeatureList> flists = (ObservableList<ModularFeatureList>) (ObservableList<? extends FeatureList>) MZmineCore
        .getProjectManager().getCurrentProject().getFeatureLists();

    fBox = new ComboBox<>();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.setItems(FXCollections
                .observableArrayList(newValue.getFeatures(newValue.getRawDataFile(0))));
          } else {
            fBox.setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(ModularFeature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object);
      }

      @Override
      public ModularFeature fromString(String string) {
        return null;
      }
    });
    fBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> onSelectedFeatureChanged(newValue)));

    GridPane pnControls = new GridPane();
    pnControls.add(new Label("Feature list"), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature"), 0, 1);
    pnControls.add(fBox, 1, 1);
    previewWrapperPane.setBottom(pnControls);
    previewWrapperPane.setCenter(previewChart);
    shapeRenderer.setDefaultItemLabelPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getItemLabelPaint());
  }

  protected void onSelectedFeatureChanged(ModularFeature newValue) {
    if (newValue == null) {
      return;
    }
    previewChart.removeAllDatasets();

    ResolvingDimension dimension = ResolvingDimension.RETENTION_TIME;
    try {
      // not all resolvers are capable of resolving rt and mobility dimension. In that case, the
      // parameter has not been added to the parameter set.
      dimension = parameterSet.getParameter(GeneralResolverParameters.dimension).getValue();
    } catch (IllegalArgumentException e) {
      // this one can go silent
    }
    // add preview depending on which dimension is selected.
    if (dimension == ResolvingDimension.RETENTION_TIME) {
      MZmineCore.runLater(() -> {
        previewChart.addDataset(new ColoredXYDataset(new IonTimeSeriesToXYProvider(newValue)));
        previewChart.setDomainAxisLabel(uf.format("Retention time", "min"));
        previewChart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
      });
    } else if (dimension == ResolvingDimension.MOBILITY && newValue
        .getFeatureData() instanceof IonMobilogramTimeSeries) {
      IonMobilogramTimeSeries data = (IonMobilogramTimeSeries) newValue.getFeatureData();
      MZmineCore.runLater(() -> {
        previewChart.addDataset(new ColoredXYDataset(
            new SummedMobilogramXYProvider(data.getSummedMobilogram(),
                new SimpleObjectProperty<>(newValue.getRawDataFile().getColor()), "")));
        IMSRawDataFile file = (IMSRawDataFile) newValue.getRawDataFile();
        previewChart.setDomainAxisLabel(
            uf.format(file.getMobilityType().getAxisLabel(), file.getMobilityType().getUnit()));
        previewChart
            .setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
      });
    } else {
      MZmineCore.getDesktop().displayErrorMessage(
          "Cannot resolve for mobility in a dataset that has no mobility dimension.");
      return;
    }

    int resolvedFeatureCounter = 0;
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();

    if (resolver == null) {
      resolver = ((GeneralResolverParameters) parameterSet)
          .getResolver(parameterSet, flistBox.getValue());
    }
    if (resolver != null) {

      if (newValue.getFeatureList() instanceof ModularFeatureList flist) {
        if (dimension == ResolvingDimension.RETENTION_TIME) {
          try {
            // we can't use FeatureDataAccess to select a specific feature, so we need to remap manually.
            final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(IonTimeSeriesUtils.remapRtAxis(newValue.getFeatureData(),
                flistBox.getValue().getSeletedScans(newValue.getRawDataFile())), null);

            for (IonTimeSeries<? extends Scan> series : resolved) {
              ColoredXYDataset ds = new ColoredXYDataset(new IonTimeSeriesToXYProvider(series, "",
                  new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++))));
              MZmineCore.runLater(() -> previewChart.addDataset(ds, shapeRenderer));
            }
          } catch (UnsupportedOperationException e) {
            MZmineCore.getDesktop().displayErrorMessage(e.getMessage());
          }
        } else {
          // for mobility dimension we don't need to remap RT
          try {
            final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(newValue.getFeatureData(), null);
            for (IonTimeSeries<? extends Scan> series : resolved) {
              ColoredXYDataset ds = new ColoredXYDataset(new SummedMobilogramXYProvider(((IonMobilogramTimeSeries) series).getSummedMobilogram(),
                  new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++)), ""));
              MZmineCore.runLater(() -> previewChart.addDataset(ds, shapeRenderer));
            }
          } catch (UnsupportedOperationException e) {
            MZmineCore.getDesktop().displayErrorMessage(e.getMessage());
          }
        }
      }
    } else {
      ResolvedPeak[] resolved = resolveFeature(newValue);
      if (resolved.length == 0) {
        return;
      }
      for (ResolvedPeak rp : resolved) {
        ColoredXYDataset ds = new ColoredXYDataset(rp);
        ds.setColor(FxColorUtil.fxColorToAWT(palette.get(resolvedFeatureCounter++)));
        MZmineCore.runLater(() -> previewChart.addDataset(ds, shapeRenderer));
      }
    }
  }

  @Deprecated
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
          .resolvePeaks(feature, parameterSet, rWrapper, cf, 0, 0);
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

    if (flistBox.getValue() != null) {
      resolver = ((GeneralResolverParameters) parameterSet)
          .getResolver(parameterSet, flistBox.getValue());
    }

    List<String> errors = new ArrayList<>();
    if (parameterSet.checkParameterValues(errors)) {
      onSelectedFeatureChanged(fBox.getValue());
    }
  }

  @Override
  public void setOnPreviewShown(Runnable onPreviewShown) {
    super.setOnPreviewShown(onPreviewShown);
  }

}

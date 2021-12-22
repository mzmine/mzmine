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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
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
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.util.StringConverter;

public class FeatureResolverSetupDialog extends ParameterSetupDialogWithPreview {

  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChart;
  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChartBadFeature;
  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;
  protected final NumberFormat mobilityFormat;
  protected ComboBox<FeatureList> flistBox;
  protected ComboBox<ModularFeature> fBox;
  protected ComboBox<ModularFeature> fBoxBadFeature;
  protected ColoredXYShapeRenderer shapeRenderer = new ColoredXYShapeRenderer();
  protected BinningMobilogramDataAccess mobilogramBinning;
  protected Resolver resolver;

  public FeatureResolverSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      String message) {
    super(valueCheckRequired, parameters, message);
    uf = MZmineCore.getConfiguration().getUnitFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();

    previewChart = new SimpleXYChart<>("Please select a good EIC",
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));
    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);

    previewChartBadFeature = new SimpleXYChart<>("Please select a noisy EIC",
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));
    previewChartBadFeature.setDomainAxisNumberFormatOverride(rtFormat);
    previewChartBadFeature.setRangeAxisNumberFormatOverride(intensityFormat);

    ObservableList<FeatureList> flists = FXCollections.observableList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    fBox = new ComboBox<>();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
            fBoxBadFeature.setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
            fBox.setValue(findGoodEIC(
                (List<ModularFeatureListRow>) (List<? extends FeatureListRow>) newValue.getRows()));
            fBoxBadFeature.setValue(findBadFeature(
                (List<ModularFeatureListRow>) (List<? extends FeatureListRow>) newValue.getRows()));
          } else {
            fBox.setItems(FXCollections.emptyObservableList());
            fBoxBadFeature.setItems(FXCollections.emptyObservableList());
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
    fBox.getSelectionModel().selectedItemProperty().addListener(
        ((observable, oldValue, newValue) -> onSelectedFeatureChanged(previewChart, newValue)));

    fBoxBadFeature = new ComboBox<>();
    fBoxBadFeature.setConverter(new StringConverter<>() {
      @Override
      public String toString(ModularFeature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object) + " (height / area = " + String.format("%.3f",
            object.getHeight() / object.getArea()) + ")";
      }

      @Override
      public ModularFeature fromString(String string) {
        return null;
      }
    });
    fBoxBadFeature.getSelectionModel().selectedItemProperty().addListener(
        ((observable, oldValue, newValue) -> onSelectedFeatureChanged(previewChartBadFeature,
            newValue)));

    final BorderPane pnBadFeaturePreview = new BorderPane();
    previewChartBadFeature.setMinHeight(200);
    pnBadFeaturePreview.setCenter(previewChartBadFeature);
    pnBadFeaturePreview.setBottom(new HBox(new Label("Feature "), fBoxBadFeature));

    final BorderPane pnFeaturePreview = new BorderPane();
    previewChart.setMinHeight(200);
    GridPane pnControls = new GridPane();
    pnControls.add(new Label("Feature list "), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature "), 0, 1);
    pnControls.add(fBox, 1, 1);
    pnFeaturePreview.setCenter(previewChart);
    pnFeaturePreview.setBottom(pnControls);

    GridPane preview = new GridPane();
    preview.add(pnBadFeaturePreview, 0, 0, 2, 1);
    preview.add(pnFeaturePreview, 0, 1, 2, 1);
    preview.getRowConstraints()
        .add(new RowConstraints(200, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
    preview.getRowConstraints()
        .add(new RowConstraints(200, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
    preview.getColumnConstraints()
        .add(new ColumnConstraints(200, -1, -1, Priority.ALWAYS, HPos.LEFT, true));
    previewWrapperPane.setCenter(preview);

    shapeRenderer.setDefaultItemLabelPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getItemLabelPaint());
  }

  protected void onSelectedFeatureChanged(SimpleXYChart<IonTimeSeriesToXYProvider> chart,
      ModularFeature newValue) {
    if (newValue == null) {
      return;
    }
    chart.removeAllDatasets();

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
        chart.addDataset(new ColoredXYDataset(new IonTimeSeriesToXYProvider(newValue)));
        chart.setDomainAxisLabel(uf.format("Retention time", "min"));
        chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
      });
    } else if (dimension == ResolvingDimension.MOBILITY
               && newValue.getFeatureData() instanceof IonMobilogramTimeSeries) {
      IonMobilogramTimeSeries data = (IonMobilogramTimeSeries) newValue.getFeatureData();
      MZmineCore.runLater(() -> {
        chart.addDataset(new ColoredXYDataset(
            new SummedMobilogramXYProvider(data.getSummedMobilogram(),
                new SimpleObjectProperty<>(newValue.getRawDataFile().getColor()), "")));
        IMSRawDataFile file = (IMSRawDataFile) newValue.getRawDataFile();
        chart.setDomainAxisLabel(
            uf.format(file.getMobilityType().getAxisLabel(), file.getMobilityType().getUnit()));
        chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getMobilityFormat());
      });
    } else {
      MZmineCore.getDesktop().displayErrorMessage(
          "Cannot resolve for mobility in a dataset that has no mobility dimension.");
      return;
    }

    int resolvedFeatureCounter = 0;
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();

    if (resolver == null) {
      resolver = ((GeneralResolverParameters) parameterSet).getResolver(parameterSet,
          (ModularFeatureList) flistBox.getValue());
    }
    if (resolver != null) {

      if (newValue.getFeatureList() instanceof ModularFeatureList flist) {
        if (dimension == ResolvingDimension.RETENTION_TIME) {
          // we can't use FeatureDataAccess to select a specific feature, so we need to remap manually.
          final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(
              IonTimeSeriesUtils.remapRtAxis(newValue.getFeatureData(),
                  flistBox.getValue().getSeletedScans(newValue.getRawDataFile())), null);

          for (IonTimeSeries<? extends Scan> series : resolved) {
            ColoredXYDataset ds = new ColoredXYDataset(new IonTimeSeriesToXYProvider(series,
                rtFormat.format(series.getSpectra().get(0).getRetentionTime()) + " - "
                + rtFormat.format(
                    series.getSpectra().get(series.getNumberOfValues() - 1).getRetentionTime())
                + " min", new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++))));
            MZmineCore.runLater(() -> chart.addDataset(ds, shapeRenderer));
          }
        } else {
          // for mobility dimension we don't need to remap RT
          final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(
              newValue.getFeatureData(), null);
          for (IonTimeSeries<? extends Scan> series : resolved) {
            final SummedIntensityMobilitySeries mobilogram = ((IonMobilogramTimeSeries) series).getSummedMobilogram();
            ColoredXYDataset ds = new ColoredXYDataset(new SummedMobilogramXYProvider(mobilogram,
                new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++)),
                mobilityFormat.format(mobilogram.getMobility(0)) + " - " + mobilityFormat.format(
                    mobilogram.getMobility(mobilogram.getNumberOfValues() - 1)) + " "
                + ((Frame) series.getSpectrum(0)).getMobilityType().getUnit()));
            MZmineCore.runLater(() -> chart.addDataset(ds, shapeRenderer));
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
        MZmineCore.runLater(() -> chart.addDataset(ds, shapeRenderer));
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
      ResolvedPeak[] resolvedFeatures = resolver.resolvePeaks(feature, parameterSet, rWrapper, cf,
          0, 0);
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
      resolver = ((GeneralResolverParameters) parameterSet).getResolver(parameterSet,
          (ModularFeatureList) flistBox.getValue());
    }

    List<String> errors = new ArrayList<>();
    if (parameterSet.checkParameterValues(errors)) {
      onSelectedFeatureChanged(previewChart, fBox.getValue());
      onSelectedFeatureChanged(previewChartBadFeature, fBoxBadFeature.getValue());
    }
  }

  @Override
  public void setOnPreviewShown(Runnable onPreviewShown) {
    super.setOnPreviewShown(onPreviewShown);
  }


  private ModularFeature findBadFeature(List<ModularFeatureListRow> rows) {
    final List<ModularFeatureListRow> sortedByArea = rows.stream()
        .sorted((r1, r2) -> Double.compare(r2.getAverageArea(), r1.getAverageArea())).toList();
    final List<ModularFeatureListRow> top20 = new ArrayList<>(
        sortedByArea.subList(0, Math.min(sortedByArea.size() - 1, 20)));

    // we a looking for a feature with a low height to area ratio -> big area but low height could
    // be a noisy chromatogram
    top20.sort(Comparator.comparingDouble(r -> r.getAverageHeight() / r.getAverageArea()));
    return top20.get(0).getBestFeature();
  }

  private ModularFeature findGoodEIC(List<ModularFeatureListRow> rows) {
    final List<ModularFeatureListRow> sortedByArea = rows.stream()
        .sorted((r1, r2) -> Double.compare(r2.getAverageArea(), r1.getAverageArea())).toList();
    final List<ModularFeatureListRow> top30 = new ArrayList<>(
        sortedByArea.subList(0, Math.min(sortedByArea.size() - 1, 30)));

    top30.sort(Comparator.comparingDouble(ModularFeatureListRow::getAverageHeight));
    return top30.get(top30.size() - 1).getBestFeature();
  }
}

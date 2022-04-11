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
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.SortableFeatureComboBox;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import javafx.util.StringConverter;

public class FeatureResolverSetupDialog extends ParameterSetupDialogWithPreview {

  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChart;
  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChartBadFeature;
  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;
  protected final NumberFormat mobilityFormat;
  private final PauseTransition delayedUpdateListener;
  protected ComboBox<FeatureList> flistBox;
  protected SortableFeatureComboBox fBox;
  protected SortableFeatureComboBox fBoxBadFeature;
  protected BinningMobilogramDataAccess mobilogramBinning;
  protected Resolver resolver;
  private final Map<SimpleXYChart<IonTimeSeriesToXYProvider>, AbstractTask> updateTasksMap = new HashMap<>();

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

    ObservableList<FeatureList> flists = FXCollections.observableArrayList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    fBox = new SortableFeatureComboBox();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.getFeatureBox().setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
            fBoxBadFeature.getFeatureBox().setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
            fBox.getFeatureBox().setValue(findGoodEIC(
                (List<ModularFeatureListRow>) (List<? extends FeatureListRow>) newValue.getRows()));
            fBoxBadFeature.getFeatureBox().setValue(findBadFeature(
                (List<ModularFeatureListRow>) (List<? extends FeatureListRow>) newValue.getRows()));
          } else {
            fBox.getFeatureBox().setItems(FXCollections.emptyObservableList());
            fBoxBadFeature.getFeatureBox().setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.getFeatureBox().setConverter(new StringConverter<>() {
      @Override
      public String toString(Feature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object);
      }

      @Override
      public Feature fromString(String string) {
        return null;
      }
    });
    fBox.getFeatureBox().getSelectionModel().selectedItemProperty().addListener(
        ((observable, oldValue, newValue) -> onSelectedFeatureChanged(previewChart, newValue)));

    fBoxBadFeature = new SortableFeatureComboBox();
    fBoxBadFeature.getFeatureBox().setConverter(new StringConverter<>() {
      @Override
      public String toString(Feature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object) + " (height / area = " + String.format("%.3f",
            object.getHeight() / object.getArea()) + ")";
      }

      @Override
      public Feature fromString(String string) {
        return null;
      }
    });
    fBoxBadFeature.getFeatureBox().getSelectionModel().selectedItemProperty().addListener(
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

    // add pause to delay response to parameter changes
    delayedUpdateListener = new PauseTransition(Duration.seconds(0.5));
    delayedUpdateListener.setOnFinished(event -> updateWithCurrentParameters());
  }

  protected void onSelectedFeatureChanged(SimpleXYChart<IonTimeSeriesToXYProvider> chart,
      Feature newValue) {
    if (newValue == null) {
      return;
    }
    // cancel old
    AbstractTask oldTask = updateTasksMap.get(chart);
    if (oldTask != null) {
      oldTask.cancel();
    }

    // do all of this and only update the chart once finished
    final AbstractTask updateTask = new UpdateTask(chart, newValue);
    updateTasksMap.put(chart, updateTask);
    MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH);
  }

  @Deprecated
  protected ResolvedPeak[] resolveFeature(Feature feature) {
    FeatureResolver resolver = ((GeneralResolverParameters) parameterSet).getResolver();
    if (fBox.getFeatureBox().getValue() == null) {
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
    // add a delay to accumulate changes then call updateWithCurrentParameters
    delayedUpdateListener.playFromStart();
  }

  private void updateWithCurrentParameters() {
    updateParameterSetFromComponents();

    if (flistBox.getValue() != null) {
      resolver = ((GeneralResolverParameters) parameterSet).getResolver(parameterSet,
          (ModularFeatureList) flistBox.getValue());
    }

    List<String> errors = new ArrayList<>();
    if (parameterSet.checkParameterValues(errors)) {
      onSelectedFeatureChanged(previewChart, fBox.getFeatureBox().getValue());
      onSelectedFeatureChanged(previewChartBadFeature, fBoxBadFeature.getFeatureBox().getValue());
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

  private class UpdateTask extends AbstractTask {

    private final SimpleXYChart chart;
    private final Feature newValue;

    UpdateTask(SimpleXYChart chart, Feature newValue) {
      super(null, Instant.now());

      this.chart = chart;
      this.newValue = newValue;
    }

    @Override
    public String getTaskDescription() {
      return "Updating resolver preview with " + FeatureUtils.featureToString(newValue);
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }

    @Override
    public void run() {
      setStatus(TaskStatus.PROCESSING);
      try {
        chart.applyWithNotifyChanges(false, true, () -> {
          logger.finest("Updating feature resolving preview");
          chart.removeAllDatasets();
          if (isCanceled()) {
            return;
          }

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
            chart.addDataset(new ColoredXYDataset(new IonTimeSeriesToXYProvider(newValue),
                RunOption.THIS_THREAD));
            chart.setDomainAxisLabel(uf.format("Retention time", "min"));
            chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
          } else if (dimension == ResolvingDimension.MOBILITY
              && newValue.getFeatureData() instanceof IonMobilogramTimeSeries) {
            IonMobilogramTimeSeries data = (IonMobilogramTimeSeries) newValue.getFeatureData();
            chart.addDataset(new ColoredXYDataset(
                new SummedMobilogramXYProvider(data.getSummedMobilogram(),
                    new SimpleObjectProperty<>(newValue.getRawDataFile().getColor()), ""),
                RunOption.THIS_THREAD));
            IMSRawDataFile file = (IMSRawDataFile) newValue.getRawDataFile();
            chart.setDomainAxisLabel(
                uf.format(file.getMobilityType().getAxisLabel(), file.getMobilityType().getUnit()));
            chart.setDomainAxisNumberFormatOverride(
                MZmineCore.getConfiguration().getMobilityFormat());
          } else {
            MZmineCore.getDesktop().displayErrorMessage(
                "Cannot resolve for mobility in a dataset that has no mobility dimension.");
            return;
          }
          if (isCanceled()) {
            return;
          }

          int resolvedFeatureCounter = 0;
          SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();

          if (resolver == null || (flistBox.getValue() != null
              && resolver.getRawDataFile() != flistBox.getValue().getRawDataFile(0))) {
            resolver = ((GeneralResolverParameters) parameterSet).getResolver(parameterSet,
                (ModularFeatureList) flistBox.getValue());
          }
          if (resolver != null) {

            if (newValue.getFeatureList() instanceof ModularFeatureList) {
              if (dimension == ResolvingDimension.RETENTION_TIME) {
                // we can't use FeatureDataAccess to select a specific feature, so we need to remap manually.
                final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(
                    IonTimeSeriesUtils.remapRtAxis(newValue.getFeatureData(),
                        flistBox.getValue().getSeletedScans(newValue.getRawDataFile())), null);

                for (IonTimeSeries<? extends Scan> series : resolved) {
                  if (isCanceled()) {
                    return;
                  }
                  ColoredXYDataset ds = new ColoredXYDataset(new IonTimeSeriesToXYProvider(series,
                      rtFormat.format(series.getSpectra().get(0).getRetentionTime()) + " - "
                          + rtFormat.format(series.getSpectra().get(series.getNumberOfValues() - 1)
                          .getRetentionTime()) + " min",
                      new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++))),
                      RunOption.THIS_THREAD);
                  chart.addDataset(ds, new ColoredXYShapeRenderer());
                }
              } else {
                // for mobility dimension we don't need to remap RT
                final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(
                    newValue.getFeatureData(), null);
                for (IonTimeSeries<? extends Scan> series : resolved) {
                  if (isCanceled()) {
                    return;
                  }
                  final SummedIntensityMobilitySeries mobilogram = ((IonMobilogramTimeSeries) series).getSummedMobilogram();
                  ColoredXYDataset ds = new ColoredXYDataset(
                      new SummedMobilogramXYProvider(mobilogram,
                          new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++)),
                          mobilityFormat.format(mobilogram.getMobility(0)) + " - "
                              + mobilityFormat.format(
                              mobilogram.getMobility(mobilogram.getNumberOfValues() - 1)) + " "
                              + ((Frame) series.getSpectrum(0)).getMobilityType().getUnit()),
                      RunOption.THIS_THREAD);
                  chart.addDataset(ds, new ColoredXYShapeRenderer());
                }
              }
            }
          } else {
            ResolvedPeak[] resolved = resolveFeature(newValue);
            if (resolved.length == 0) {
              return;
            }
            for (ResolvedPeak rp : resolved) {
              if (isCanceled()) {
                return;
              }
              ColoredXYDataset ds = new ColoredXYDataset(rp, RunOption.THIS_THREAD);
              ds.setColor(FxColorUtil.fxColorToAWT(palette.get(resolvedFeatureCounter++)));
              chart.addDataset(ds, new ColoredXYShapeRenderer());
            }
          }
        });
      } catch (Exception ex) {
        logger.log(Level.FINER,
            "Error during resolver preview update. This is no issue if the old task was stopped and a new was started.",
            ex);
      }
      setStatus(TaskStatus.FINISHED);
    }
  }
}

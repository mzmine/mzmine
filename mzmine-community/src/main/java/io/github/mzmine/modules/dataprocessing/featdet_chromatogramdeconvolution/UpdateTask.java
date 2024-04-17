/*
 * Copyright 2006-2022 The MZmine Development Team
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
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.EmptyParameterSetupDialogBase;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javafx.beans.property.SimpleObjectProperty;

class UpdateTask extends AbstractTask {

  private final FeatureResolverSetupDialog featureResolverSetupDialog;
  private final SimpleXYChart chart;
  private final Feature feature;
  private final Resolver resolver;
  private final FeatureResolver legacyResolver;
  private ResolvingDimension dimension;

  UpdateTask(FeatureResolverSetupDialog featureResolverSetupDialog, SimpleXYChart chart,
      Feature feature, Resolver resolver, FeatureResolver legacyResolver) {
    super(null, Instant.now());
    this.featureResolverSetupDialog = featureResolverSetupDialog;

    this.chart = chart;
    this.feature = feature;
    this.resolver = resolver;
    this.legacyResolver = legacyResolver;

    dimension = ResolvingDimension.RETENTION_TIME;
    try {
      // not all resolvers are capable of resolving rt and mobility dimension. In that case, the
      // parameter has not been added to the parameter set.
      dimension = featureResolverSetupDialog.parameterSet.getParameter(
          GeneralResolverParameters.dimension).getValue();
    } catch (IllegalArgumentException e) {
      // this one can go silent
    }
  }

  @Override
  public String getTaskDescription() {
    return "Updating resolver preview with " + FeatureUtils.featureToString(feature);
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final ColoredXYDataset rtFeatureDataset = new ColoredXYDataset(
        new IonTimeSeriesToXYProvider(feature), RunOption.THIS_THREAD);

    final ColoredXYDataset imsMobilogramDataset;

    IonMobilogramTimeSeries data = (IonMobilogramTimeSeries) feature.getFeatureData();
    imsMobilogramDataset = new ColoredXYDataset(
        new SummedMobilogramXYProvider(data.getSummedMobilogram(),
            new SimpleObjectProperty<>(feature.getRawDataFile().getColor()), ""),
        RunOption.THIS_THREAD);

    try {
      chart.applyWithNotifyChanges(false, true, () -> {
        EmptyParameterSetupDialogBase.logger.finest("Updating feature resolving preview");
        chart.removeAllDatasets();
        if (isCanceled()) {
          return;
        }

        // add preview depending on which dimension is selected.
        if (dimension == ResolvingDimension.RETENTION_TIME) {
          chart.addDataset(rtFeatureDataset);
          chart.setDomainAxisLabel(featureResolverSetupDialog.uf.format("Retention time", "min"));
          chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
        } else if (isValidImsDataset()) {
          chart.addDataset(imsMobilogramDataset);
          IMSRawDataFile file = (IMSRawDataFile) feature.getRawDataFile();
          chart.setDomainAxisLabel(
              featureResolverSetupDialog.uf.format(file.getMobilityType().getAxisLabel(),
                  file.getMobilityType().getUnit()));
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

        if (featureResolverSetupDialog.resolver == null || (
            featureResolverSetupDialog.flistBox.getValue() != null
                && featureResolverSetupDialog.resolver.getRawDataFile()
                != featureResolverSetupDialog.flistBox.getValue().getRawDataFile(0))) {
          featureResolverSetupDialog.resolver = ((GeneralResolverParameters) featureResolverSetupDialog.parameterSet).getResolver(
              featureResolverSetupDialog.parameterSet,
              (ModularFeatureList) featureResolverSetupDialog.flistBox.getValue());
        }
        if (featureResolverSetupDialog.resolver != null) {

          if (feature.getFeatureList() instanceof ModularFeatureList) {
            List<ColoredXYDataset> datasets = switch (dimension) {
              case RETENTION_TIME -> resolveInRtDimension();
              case null, default -> resolveInMobilityDimension();
            };

            datasets.forEach(ds -> chart.addDataset(ds, new ColoredAreaShapeRenderer()));
          }
        } else {
          ResolvedPeak[] resolved = featureResolverSetupDialog.resolveFeature(feature);
          if (resolved.length == 0) {
            return;
          }
          for (ResolvedPeak rp : resolved) {
            if (isCanceled()) {
              return;
            }
            ColoredXYDataset ds = new ColoredXYDataset(rp, RunOption.THIS_THREAD);
            ds.setColor(FxColorUtil.fxColorToAWT(palette.get(resolvedFeatureCounter++)));
            chart.addDataset(ds, new ColoredAreaShapeRenderer());
          }
        }
      });
    } catch (Exception ex) {
      EmptyParameterSetupDialogBase.logger.log(Level.FINER,
          "Error during resolver preview update. This is no issue if the old task was stopped and a new was started.",
          ex);
    }
    setStatus(TaskStatus.FINISHED);
  }

  private List<ColoredXYDataset> resolveInMobilityDimension() {
    final List<ColoredXYDataset> datasets = new ArrayList<>();
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    int resolvedFeatureCounter = 0;
    // for mobility dimension we don't need to remap RT
    final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(
        feature.getFeatureData(), null);
    for (IonTimeSeries<? extends Scan> series : resolved) {
      final SummedIntensityMobilitySeries mobilogram = ((IonMobilogramTimeSeries) series).getSummedMobilogram();
      ColoredXYDataset ds = new ColoredXYDataset(
          new SummedMobilogramXYProvider(mobilogram,
              new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++)),
              featureResolverSetupDialog.mobilityFormat.format(mobilogram.getMobility(0))
                  + " - " + featureResolverSetupDialog.mobilityFormat.format(
                  mobilogram.getMobility(mobilogram.getNumberOfValues() - 1)) + " "
                  + ((Frame) series.getSpectrum(0)).getMobilityType().getUnit()),
          RunOption.THIS_THREAD);
      datasets.add(ds);
    }
    return datasets;
  }

  private List<ColoredXYDataset> resolveInRtDimension() {
    final List<ColoredXYDataset> datasets = new ArrayList<>();
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    int resolvedFeatureCounter = 0;

    // we can't use FeatureDataAccess to select a specific feature, so we need to remap manually.
    final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(
        IonTimeSeriesUtils.remapRtAxis(feature.getFeatureData(),
            featureResolverSetupDialog.flistBox.getValue()
                .getSeletedScans(feature.getRawDataFile())), null);

    for (IonTimeSeries<? extends Scan> series : resolved) {
      ColoredXYDataset ds = new ColoredXYDataset(new IonTimeSeriesToXYProvider(series,
          featureResolverSetupDialog.rtFormat.format(series.getSpectra().get(0).getRetentionTime())
              + " - " + featureResolverSetupDialog.rtFormat.format(
              series.getSpectra().get(series.getNumberOfValues() - 1).getRetentionTime()) + " min",
          new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++))),
          RunOption.THIS_THREAD);
      datasets.add(ds);
    }
    return datasets;
  }

  private boolean isValidImsDataset() {
    return dimension == ResolvingDimension.MOBILITY
        && feature.getFeatureData() instanceof IonMobilogramTimeSeries;
  }
}

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
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;

class ResolverPreviewUpdateTask extends FxUpdateTask<Object> {

  private static Logger logger = Logger.getLogger(ResolverPreviewUpdateTask.class.getName());

  private final SimpleXYChart<?> chart;
  private final Feature feature;
  private final ModularFeatureList flist;
  @org.jetbrains.annotations.NotNull
  private final GeneralResolverParameters parameters;
  private Resolver resolver;
  private ResolvingDimension dimension;

  private final NumberFormats formats = ConfigService.getConfiguration().getGuiFormats();
  private ColoredXYDataset unresolvedDataset;
  private List<ColoredXYDataset> resolvedFeatures;

  public ResolverPreviewUpdateTask(String chartName, SimpleXYChart<?> chart, Feature feature,
      ModularFeatureList flist, GeneralResolverParameters parameters) {
    super("Updating %s".formatted(chartName), Instant.now());
    this.chart = chart;
    this.feature = feature;
    this.flist = flist;
    this.parameters = parameters;

    dimension = ResolvingDimension.RETENTION_TIME;
    try {
      // not all resolvers are capable of resolving rt and mobility dimension. In that case, the
      // parameter has not been added to the parameter set.
      dimension = parameters.getParameter(GeneralResolverParameters.dimension).getValue();
    } catch (IllegalArgumentException e) {
      // this one can go silent
    }
  }

  @Override
  public String getTaskDescription() {
    return "Updating resolver preview with %s".formatted(FeatureUtils.featureToString(feature));
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  protected void process() {

    if (feature == null || flist == null) {
      logger.warning("Resolver is null");
      return;
    }

    if (!isValidDataset()) {
      final String msg = "Cannot resolve for mobility in a feature that has no mobility dimension.";
      logger.warning(msg);
      DesktopService.getDesktop().displayErrorMessage(msg);
      setStatus(TaskStatus.ERROR);
      return;
    }

    try {
      unresolvedDataset = createMainDataset();

      final List<String> errors = new ArrayList<>();
      if (parameters.checkParameterValues(errors, true)) {
        this.resolver = parameters.getResolver(parameters, flist);
        if (resolver != null) {
          resolvedFeatures = switch (dimension) {
            case RETENTION_TIME -> resolveInRtDimension();
            case MOBILITY -> resolveInMobilityDimension();
          };
        }
      } else {
        logger.info(errors.toString());
      }


    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "Error in feature resolving preview.", e);
    }
  }

  @Override
  protected void updateGuiModel() {

    chart.applyWithNotifyChanges(false, true, () -> {
      logger.finest("Updating feature resolving preview");
      chart.removeAllDatasets();
      if (isCanceled()) {
        return;
      }

      // add preview depending on which dimension is selected.
      if (dimension == ResolvingDimension.RETENTION_TIME) {
        chart.setDomainAxisLabel(formats.unit("Retention time", "min"));
        chart.setDomainAxisNumberFormatOverride(formats.rtFormat());
      } else if (isValidDataset()) {
        final IMSRawDataFile file = (IMSRawDataFile) feature.getRawDataFile();
        chart.setDomainAxisLabel(
            formats.unit(file.getMobilityType().getAxisLabel(), file.getMobilityType().getUnit()));
        chart.setDomainAxisNumberFormatOverride(formats.mobilityFormat());
      }

      if (unresolvedDataset != null) {
        chart.addDataset(unresolvedDataset);
      }
      if (resolvedFeatures != null) {
        resolvedFeatures.forEach(ds -> chart.addDataset(ds, new ColoredAreaShapeRenderer()));
      }
    });
  }

  private List<ColoredXYDataset> resolveInMobilityDimension() {
    final List<ColoredXYDataset> datasets = new ArrayList<>();
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    int resolvedFeatureCounter = 0;
    // for mobility dimension we don't need to remap RT
    final List<IonTimeSeries<? extends Scan>> resolved = resolver.resolve(feature.getFeatureData(),
        null);
    for (IonTimeSeries<? extends Scan> series : resolved) {
      final SummedIntensityMobilitySeries mobilogram = ((IonMobilogramTimeSeries) series).getSummedMobilogram();
      final String name = "%s - %s %s".formatted(formats.mobility(mobilogram.getMobility(0)),
          formats.mobility(mobilogram.getMobility(mobilogram.getNumberOfValues() - 1)),
          ((Frame) series.getSpectrum(0)).getMobilityType().getUnit());
      ColoredXYDataset ds = new ColoredXYDataset(new SummedMobilogramXYProvider(mobilogram,
          new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++)), name),
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
            feature.getFeatureList().getSeletedScans(feature.getRawDataFile())), null);

    for (IonTimeSeries<? extends Scan> series : resolved) {
      final String seriesKey =
          formats.rt(series.getSpectra().get(0).getRetentionTime()) + " - " + formats.rt(
              series.getSpectra().get(series.getNumberOfValues() - 1).getRetentionTime()) + " min";
      ColoredXYDataset ds = new ColoredXYDataset(new IonTimeSeriesToXYProvider(series, seriesKey,
          new SimpleObjectProperty<>(palette.get(resolvedFeatureCounter++))),
          RunOption.THIS_THREAD);
      datasets.add(ds);
    }
    return datasets;
  }

  /**
   * checks if the config is valid. dimension must be RT or mobility and feature must contain
   * mobility data.
   */
  private boolean isValidDataset() {
    return dimension == ResolvingDimension.RETENTION_TIME || (
        dimension == ResolvingDimension.MOBILITY
            && feature.getFeatureData() instanceof IonMobilogramTimeSeries);
  }

  public ColoredXYDataset createMainDataset() {
    if (dimension == ResolvingDimension.RETENTION_TIME) {
      return new ColoredXYDataset(new IonTimeSeriesToXYProvider(feature), RunOption.THIS_THREAD);
    } else if (dimension == ResolvingDimension.MOBILITY
        && feature.getFeatureData() instanceof IonMobilogramTimeSeries data) {
      return new ColoredXYDataset(new SummedMobilogramXYProvider(data.getSummedMobilogram(),
          new SimpleObjectProperty<>(feature.getRawDataFile().getColor()), ""),
          RunOption.THIS_THREAD);
    }
    return null;
  }
}

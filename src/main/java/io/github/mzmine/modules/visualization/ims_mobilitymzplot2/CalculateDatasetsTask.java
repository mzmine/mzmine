/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.ims_mobilitymzplot2;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.FeaturesToCCSMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.FeaturesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYSmallBlockRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.PlotType;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.renderer.PaintScale;

public class CalculateDatasetsTask extends AbstractTask {

  private final Collection<ModularFeature> features;
  private final PlotType plotType;
  private double progress;
  private double minZ = Double.MAX_VALUE;
  private double maxZ = Double.MIN_VALUE;
  private PaintScaleColorStyle defaultPaintScaleColorStyle;
  private PaintScaleBoundStyle defaultPaintScaleBoundStyle;
  private PaintScale paintScale;
  private String description;
  private Map<FastColoredXYZDataset, ColoredXYSmallBlockRenderer> datasetsRenderers;

  public CalculateDatasetsTask(Collection<ModularFeature> features,
      PlotType plotType) {
    super(null); // no new data stored -> null
    this.features = features;
    this.plotType = plotType;
    description = "IMS Feature Visualizer: Waiting";

    defaultPaintScaleColorStyle = PaintScaleColorStyle.RAINBOW;
    defaultPaintScaleBoundStyle = PaintScaleBoundStyle.LOWER_AND_UPPER_BOUND;
    progress = 0d;
  }

  public Map<FastColoredXYZDataset, ColoredXYSmallBlockRenderer> getDatasetsRenderers() {
    return datasetsRenderers;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final FastColoredXYZDataset featureDataSet;

    if (plotType == PlotType.CCS) {
      if (features instanceof List) {
        featureDataSet = new FastColoredXYZDataset(
            new FeaturesToCCSMzHeatmapProvider((List<ModularFeature>) features));
      } else {
        featureDataSet = new FastColoredXYZDataset(
            new FeaturesToCCSMzHeatmapProvider(
                new ArrayList<>(features)));
      }
      for (ModularFeature f : features) {
        float height = f.getHeight();
        if (height < minZ) {
          minZ = height;
        }
        if (height > maxZ) {
          maxZ = height;
        }
      }
    } else {
      if (features instanceof List) {
        featureDataSet = new FastColoredXYZDataset(
            new FeaturesToMobilityMzHeatmapProvider((List<ModularFeature>) features));
      } else {
        featureDataSet = new FastColoredXYZDataset(
            new FeaturesToMobilityMzHeatmapProvider(
                new ArrayList<>(features)));
      }
      for (ModularFeature f : features) {
        float height = f.getHeight();
        if (height < minZ) {
          minZ = height;
        }
        if (height > maxZ) {
          maxZ = height;
        }
      }
    }

    description = "IMS Feature Visualizer: Creating paint scale.";

    Map<Integer, Double> percentile = Quantiles.percentiles().indexes(5, 95)
        .compute(features.stream().mapToDouble(Feature::getHeight).toArray());
    paintScale = makePaintScale(percentile.get(5), percentile.get(95));

    List<FastColoredXYZDataset> datasets = new ArrayList<>();
    datasets.add(featureDataSet);
    datasetsRenderers = new LinkedHashMap<>();
    for (FastColoredXYZDataset dataset : datasets) {
      description = "IMS Feature Visualizer: Creating renderer " + datasetsRenderers.size()
          + "/" + datasets.size();
      ColoredXYSmallBlockRenderer newRenderer = new ColoredXYSmallBlockRenderer();
      SimpleToolTipGenerator tt = new SimpleToolTipGenerator();
      newRenderer.setBlockHeight(dataset.getBoxHeight());
      newRenderer.setBlockWidth(dataset.getBoxWidth());
      newRenderer.setPaintScale(paintScale);
      newRenderer.setDefaultToolTipGenerator(tt);
      datasetsRenderers.put(dataset, newRenderer);

      if (isCanceled()) {
        return;
      }
    }

    description = "IMS Feature Visualizer: Finished";
    setStatus(TaskStatus.FINISHED);
  }

  private PaintScale makePaintScale(double min, double max) {
    if (min >= max) {
      min = 0;
      max = 1;
    }
    paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.SQRT, Range.closed(min, max));

    return paintScale;
  }

  public PaintScale getPaintScale() {
    return paintScale;
  }
}

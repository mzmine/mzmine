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

package io.github.mzmine.modules.visualization.ims_mobilitymzplot;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.RowToCCSMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.RowToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYSmallBlockRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZPieRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYZDataset;

public class CalculateDatasetsTask extends AbstractTask {

  private final Collection<ModularFeatureListRow> rows;
  private final PlotType plotType;
  private final boolean useMobilograms;
  private double progress;
  private double minZ = Double.MAX_VALUE;
  private double maxZ = Double.MIN_VALUE;
  private PaintScale paintScale;
  private String description;
  private Map<XYZDataset, XYItemRenderer> datasetsRenderers;

  public CalculateDatasetsTask(Collection<ModularFeatureListRow> rows,
      PlotType plotType, boolean useMobilograms) {
    super(null, Instant.now()); // no new data stored -> null, date is irrelevant (not used in batch)
    this.rows = rows;
    this.plotType = plotType;
    this.useMobilograms = useMobilograms;
    description = "IMS Feature Visualizer: Waiting";
    datasetsRenderers = new HashMap<>();

    progress = 0d;
  }

  public Map<XYZDataset, XYItemRenderer> getDatasetsRenderers() {
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

    if (plotType == PlotType.MOBILITY && useMobilograms) {
      var result = calculateMobilogramDatasets();
      if (result != null) {
        datasetsRenderers.putAll(result);
      }
    } else if (plotType == PlotType.MOBILITY && !useMobilograms) {
      final ColoredXYZPieDataset<IMSRawDataFile> featureDataSet = new ColoredXYZPieDataset<>(
          new RowToMobilityMzHeatmapProvider(rows));
      ColoredXYZPieRenderer renderer = new ColoredXYZPieRenderer();
      renderer.setDefaultToolTipGenerator(new SimpleToolTipGenerator());
      datasetsRenderers.put(featureDataSet, renderer);
    } else if (plotType == PlotType.CCS) {
      final ColoredXYZPieDataset<IMSRawDataFile> featureDataSet = new ColoredXYZPieDataset<>(
          new RowToCCSMzHeatmapProvider(rows));
      ColoredXYZPieRenderer renderer = new ColoredXYZPieRenderer();
      renderer.setDefaultToolTipGenerator(new SimpleToolTipGenerator());
      datasetsRenderers.put(featureDataSet, renderer);
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

  @Nullable
  private Map<ColoredXYZDataset, ColoredXYSmallBlockRenderer> calculateMobilogramDatasets() {

    final List<ModularFeature> features = rows.stream()
        .<ModularFeature>mapMulti((row, c) -> {
          for (Feature feature : row.getFeatures()) {
            if (feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
              c.accept((ModularFeature) feature);
            }
          }
        }).toList();

    final Map<ColoredXYZDataset, ColoredXYSmallBlockRenderer> results = new HashMap<>();

    for (ModularFeature feature : features) {
      description =
          "IMS Feature Visualizer: Calculating dataset " + results.size() + "/" + features
              .size();

      final ColoredXYZDataset dataset = new ColoredXYZDataset(
          new SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider(feature),
          RunOption.THIS_THREAD);

      final SummedIntensityMobilitySeries mobilogram = ((IonMobilogramTimeSeries) feature
          .getFeatureData()).getSummedMobilogram();
     final Range<Float> intensityRange = FeatureDataUtils.getIntensityRange(mobilogram);

      if (intensityRange.lowerEndpoint().doubleValue() < minZ) {
        minZ = intensityRange.lowerEndpoint().doubleValue();
      }
      if (intensityRange.upperEndpoint().doubleValue() > maxZ) {
        maxZ = intensityRange.upperEndpoint().doubleValue();
      }
      results.put(dataset, null);
      progress = results.size() / (double) features.size();

      if (isCanceled()) {
        return null;
      }
    }

    description = "IMS Feature Visualizer: Creating paint scale.";

    Map<Integer, Double> percentile = Quantiles.percentiles().indexes(5, 95)
        .compute(features.stream().mapToDouble(Feature::getHeight).toArray());
    paintScale = makePaintScale(percentile.get(5), percentile.get(95));

    for (ColoredXYZDataset dataset : results.keySet()) {
      description = "IMS Feature Visualizer: Creating renderer " + results.size()
          + "/" + results.size();
      ColoredXYSmallBlockRenderer newRenderer = new ColoredXYSmallBlockRenderer();
      SimpleToolTipGenerator tt = new SimpleToolTipGenerator();
      newRenderer.setBlockHeight(dataset.getBoxHeight());
      newRenderer.setBlockWidth(dataset.getBoxWidth());
      newRenderer.setUseDatasetPaintScale(false);
      newRenderer.setPaintScale(paintScale);
      newRenderer.setDefaultToolTipGenerator(tt);
      results.put(dataset, newRenderer);

      if (isCanceled()) {
        return null;
      }
    }
    return results;
  }
}

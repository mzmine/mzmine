package io.github.mzmine.modules.visualization.imsfeaturevisualizer;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.FeaturesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYSmallBlockRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.ButtonType;
import org.jfree.chart.renderer.PaintScale;

public class CalculateDatasetsTask extends AbstractTask {

  final Collection<ModularFeature> features;
  private double progress;
  private double minZ = Double.MAX_VALUE;
  private double maxZ = Double.MIN_VALUE;
  private PaintScaleColorStyle defaultPaintScaleColorStyle;
  private PaintScaleBoundStyle defaultPaintScaleBoundStyle;
  private PaintScale paintScale;
  private String description;
  private Map<FastColoredXYZDataset, ColoredXYSmallBlockRenderer> datasetsRenderers;

  public CalculateDatasetsTask(Collection<ModularFeature> features) {
    this.features = features;
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

    final Collection<FastColoredXYZDataset> datasets = new ArrayList<>();

    boolean useMobilograms = true;
    if (features.size() > 2000) {
      ButtonType btnType = MZmineCore.getDesktop()
          .displayConfirmation("You selected " + features.size()
                  + " to visualize. This might take a long time or crash MZmine.\nWould you like to "
                  + "visualize points instead of mobilograms for features?", ButtonType.YES,
              ButtonType.NO);
      if (btnType == ButtonType.YES) {
        useMobilograms = false;
      }
    }

    if (useMobilograms) {
      for (ModularFeature feature : features) {
        description =
            "IMS Feature Visualizer: Calculating dataset " + datasets.size() + "/" + features
                .size();

        FastColoredXYZDataset dataset = new FastColoredXYZDataset(
            new SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider(feature));

        SummedIntensityMobilitySeries mobilogram = ((IonMobilogramTimeSeries) feature
            .getFeatureData()).getSummedMobilogram();
        Range<Float> intensityRange = FeatureDataUtils.getIntensityRange(mobilogram);

        if (intensityRange.lowerEndpoint().doubleValue() < minZ) {
          minZ = intensityRange.lowerEndpoint().doubleValue();
        }
        if (intensityRange.upperEndpoint().doubleValue() > maxZ) {
          maxZ = intensityRange.upperEndpoint().doubleValue();
        }
        datasets.add(dataset);
        progress = datasets.size() / (double) features.size();

        if (isCanceled()) {
          return;
        }
      }
    } else {
      if (features instanceof List) {
        FastColoredXYZDataset dataset = new FastColoredXYZDataset(
            new FeaturesToMobilityMzHeatmapProvider((List<ModularFeature>) features));
        datasets.add(dataset);
      } else {
        FastColoredXYZDataset dataset = new FastColoredXYZDataset(
            new FeaturesToMobilityMzHeatmapProvider(
                new ArrayList<>(features)));
        datasets.add(dataset);
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
//    paintScale = makePaintScale(minZ - minZ * 0.1, maxZ);

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
        .toPaintScale(PaintScaleTransform.LOG2, Range.closed(min, max));

    return paintScale;
  }

  public PaintScale getPaintScale() {
    return paintScale;
  }
}

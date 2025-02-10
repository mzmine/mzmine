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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PaintScaleProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Provides a {@link io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset} with a
 * {@link IonMobilogramTimeSeries}. The underlying data is used, no calculations are needed. Domain
 * axis = retention time, range axis = mobility, z axis = time.
 *
 * @author https://github.com/SteffenHeu
 */
public class IonMobilogramTimeSeriesToRtMobilityHeatmapProvider implements PlotXYZDataProvider,
    PaintScaleProvider, MassSpectrumProvider<MobilityScan> {

  @NotNull
  private IonMobilogramTimeSeries data;
  private final String seriesKey;
  private final javafx.scene.paint.Color color;
  private final boolean isUseSingleColorPaintScale;
  int numValues = 0;
  private final double progress;
  private PaintScale paintScale = null;
  private final List<IonMobilitySeries> mobilograms;

  public IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(final ModularFeature f) {
    if (!(f.getFeatureData() instanceof IonMobilogramTimeSeries)) {
      throw new IllegalArgumentException("Cannot create IMS heatmap for non-IMS feature");
    }
    data = (IonMobilogramTimeSeries) f.getFeatureData();
    mobilograms = new ArrayList<>();

    seriesKey = FeatureUtils.featureToString(f);
    color = f.getRawDataFile().getColor();
    isUseSingleColorPaintScale = false;
    progress = 1d;
  }

  /**
   * @param data                     The data to plot.
   * @param seriesKey                The series key.
   * @param color                    A color which will be used if useSingleColorPaintScale is
   *                                 true.
   * @param useSingleColorPaintScale If true, a paint scale will be generated from the passed
   *                                 color.
   */
  public IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(final IonMobilogramTimeSeries data,
      final String seriesKey, final javafx.scene.paint.Color color,
      final boolean useSingleColorPaintScale) {
    this.data = data;
    this.seriesKey = seriesKey;
    this.color = color;
    this.isUseSingleColorPaintScale = useSingleColorPaintScale;
    progress = 1d;
    mobilograms = new ArrayList<>();
  }

  @Override
  public Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(color);
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return color;
  }

  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return paintScale;
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {

    mobilograms.addAll(data.getMobilograms());

    numValues = 0;
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < mobilograms.size(); i++) {
      numValues += mobilograms.get(i).getNumberOfValues();
      for (int j = 0; j < mobilograms.get(i).getNumberOfValues(); j++) {
        max = Math.max(mobilograms.get(i).getIntensity(j), max);
      }
    }
    if (isUseSingleColorPaintScale) {
      javafx.scene.paint.Color base = javafx.scene.paint.Color.BLACK;
//          MZmineCore.getConfiguration().isDarkMode() ? javafx.scene.paint.Color.BLACK
//              : javafx.scene.paint.Color.WHITE;
      paintScale = new SimpleColorPalette(base, color).toPaintScale(PaintScaleTransform.LINEAR,
          Range.closed(1d, max));
    }
  }

  @Override
  public double getDomainValue(int index) {
    for (IonMobilitySeries mobilitySeries : mobilograms) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getSpectrum(index).getRetentionTime();
      }
    }
    return 0;
  }

  @Override
  public double getRangeValue(int index) {
    for (IonMobilitySeries mobilitySeries : mobilograms) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getMobility(index);
      }
    }
    return 0;
  }

  @Override
  public int getValueCount() {
    return numValues;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return progress;
  }

  @Override
  public double getZValue(int index) {
    for (IonMobilitySeries mobilitySeries : mobilograms) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getIntensity(index);
      }
    }
    return 0;
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return null;
  }

  @Nullable
  @Override
  public MobilityScan getSpectrum(int index) {
    for (IonMobilitySeries mobilitySeries : mobilograms) {
      if (index >= mobilitySeries.getNumberOfValues()) {
        index -= mobilitySeries.getNumberOfValues();
      } else {
        return mobilitySeries.getSpectrum(index);
      }
    }
    return null;
  }
}

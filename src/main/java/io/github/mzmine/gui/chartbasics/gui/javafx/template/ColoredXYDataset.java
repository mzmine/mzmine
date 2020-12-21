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

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.ColorProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.DomainValueProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.LabelTextProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.RangeValueProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.SeriesKeyProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Intended for values that don't have to be calculated or have already been. Otherwise it might
 * crash the gui.
 */
public class ColoredXYDataset extends AbstractXYDataset implements ColorProvider {

  private final int seriesCount = 1;
  private final ColorProvider colorProvider;
  private final DomainValueProvider domainValueProvider;
  private final RangeValueProvider rangeValueProvider;
  private final SeriesKeyProvider<Comparable<?>> seriesKeyProvider;
  private final LabelTextProvider labelTextProvider;
  private final int itemCount;
  private Color color;
  private javafx.scene.paint.Color colorfx;

  private List<Double> domainValues;
  private List<Double> rangeValues;

  private Double minRangeValue;

  public ColoredXYDataset(DomainValueProvider domainValueProvider,
      RangeValueProvider rangeValueProvider,
      SeriesKeyProvider<Comparable<?>> seriesKeyProvider, ColorProvider colorProvider) {

    if (domainValueProvider.getValueCount() != rangeValueProvider.getValueCount()) {
      throw new IllegalArgumentException(
          "Number of domain values does not match number of range values.");

    }
    this.itemCount = domainValueProvider.getValueCount();

    this.colorProvider = colorProvider;
    this.domainValueProvider = domainValueProvider;
    this.rangeValueProvider = rangeValueProvider;
    this.seriesKeyProvider = seriesKeyProvider;
    this.labelTextProvider = null;

    this.color = colorProvider.getAWTColor();
    this.colorfx = colorProvider.getFXColor();

    minRangeValue = Double.MAX_VALUE;

    MZmineCore.getTaskController().addTask(new ValueComputing(() -> {
      compute();
      return 1;
    }));
  }

  public ColoredXYDataset(PlotDatasetProvider datasetProvider) {
    this(datasetProvider, datasetProvider, datasetProvider, datasetProvider);
  }

  @Override
  public Color getAWTColor() {
    return color;
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return colorfx;
  }

  @Override
  public int getSeriesCount() {
    return seriesCount;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return seriesKeyProvider.getSeriesKey();
  }

  @Override
  public int getItemCount(int series) {
    return itemCount;
  }

  @Override
  public Number getX(int series, int item) {
    return domainValues.get(item);
  }

  @Override
  public Number getY(int series, int item) {
    return rangeValues.get(item);
  }

  public List<Double> getXValues() {
    return Collections.unmodifiableList(domainValues);
  }

  public List<Double> getYValues() {
    return Collections.unmodifiableList(rangeValues);
  }

  public int getValueIndex(final double domainValue, final double rangeValue) {
    // todo binary search somehow here
    for (int i = 0; i < itemCount; i++) {
      if (Double.compare(domainValue, getX(0, i).doubleValue()) == 0
          && Double.compare(rangeValue, getY(0, i).doubleValue()) == 0) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Note: does not return the original color provider but <b>this</b>dataset.
   *
   * @return
   */
  public ColorProvider getColorProvider() {
    return this;
  }

  public DomainValueProvider getDomainValueProvider() {
    return domainValueProvider;
  }

  public RangeValueProvider getRangeValueProvider() {
    return rangeValueProvider;
  }

  public SeriesKeyProvider<Comparable<?>> getSeriesKeyProvider() {
    return seriesKeyProvider;
  }

  @Nullable
  public String getLabel(final int itemIndex) {
    if(itemIndex > getItemCount(1)) {
      return null;
    }
    if (labelTextProvider != null) {
      return labelTextProvider.getLabel(itemIndex);
    }
    return String.valueOf(getYValue(1, itemIndex));
  }

  public void compute() {

    domainValues = domainValueProvider.getDomainValues();
    rangeValues = rangeValueProvider.getRangeValues();

    for (Double rangeValue : rangeValues) {
      if (rangeValue.doubleValue() < minRangeValue.doubleValue()) {
        minRangeValue = rangeValue;
      }
    }

  }

  public Double getMinimumRangeValue() {
    return minRangeValue;
  }

  private static class ValueComputing extends AbstractTask {

    final Supplier<Integer> computationMethod;

    public ValueComputing(Supplier<Integer> computationMethod) {
      this.computationMethod = computationMethod;
      setStatus(TaskStatus.WAITING);
    }

    @Override
    public String getTaskDescription() {
      return "Processing values for dataset.";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }

    @Override
    public void run() {
      setStatus(TaskStatus.PROCESSING);
      computationMethod.get();
      setStatus(TaskStatus.FINISHED);
    }

  }

}

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

import com.google.common.collect.Lists;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.ColorProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.DomainValueProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.RangeValueProvider;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.SeriesKeyProvider;
import java.awt.Color;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Intended for values that don't have to be calculated or have already been. Otherwise it might
 * crash the gui.
 */
public class ColoredXYDataset extends AbstractXYDataset implements ColorProvider {

  private final int seriesCount = 1;
  private final ColorProvider colorProvider;
  private final DomainValueProvider<Number> domainValueProvider;
  private final RangeValueProvider<Number> rangeValueProvider;
  private final SeriesKeyProvider<Comparable<?>> seriesKeyProvider;
  private int itemCount;

  public ColoredXYDataset(DomainValueProvider<Number> domainValueProvider,
      RangeValueProvider<Number> rangeValueProvider,
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
  }

  public ColoredXYDataset(PlotDatasetProvider datasetProvider) {
    this(datasetProvider, datasetProvider, datasetProvider, datasetProvider);
  }

  @Override
  public Color getAWTColor() {
    return colorProvider.getAWTColor();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return colorProvider.getFXColor();
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
    return domainValueProvider.getDomainValue(item);
  }

  @Override
  public Number getY(int series, int item) {
    return rangeValueProvider.getRangeValue(item);
  }

  public int getIndex(final double domainValue, final double rangeValue) {
    return 0;
    // todo binary search somehow here
  }
}

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

package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import javax.annotation.Nonnull;

/**
 * Used to plot XYZ datasets in a scatterplot-type of plot. Used to display spatial distribution in
 * imaging and ion mobility heatmaps.
 *
 * @author https://github.com/SteffenHeu
 */
public class FastColoredXYZDataset extends ColoredXYZDataset {

  public FastColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider) {
    this(dataProvider, true);
  }

  public FastColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale) {
    this(dataProvider, useAlphaInPaintscale, FALLBACK_PS_STYLE, FALLBACK_PS_BOUND);
  }

  public FastColoredXYZDataset(@Nonnull PlotXYZDataProvider dataProvider,
      final boolean useAlphaInPaintscale, PaintScaleColorStyle paintScaleColorStyle,
      PaintScaleBoundStyle paintScaleBoundStyle) {
    super(dataProvider, false, paintScaleColorStyle, paintScaleBoundStyle, false);
    run();
  }

}

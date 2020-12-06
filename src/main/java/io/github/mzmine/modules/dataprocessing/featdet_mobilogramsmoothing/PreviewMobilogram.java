/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.SimpleMobilogram;
import java.awt.Color;

public class PreviewMobilogram extends SimpleMobilogram {

  public PreviewMobilogram(Mobilogram mobilogram) {
    super(mobilogram.getMobilityType(), mobilogram.getRawDataFile());

    mobilogram.getDataPoints().forEach(this::addDataPoint);
    calc();
  }

  @Override
  public Color getAWTColor() {
    return MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColor();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return "smoothed";
  }
}

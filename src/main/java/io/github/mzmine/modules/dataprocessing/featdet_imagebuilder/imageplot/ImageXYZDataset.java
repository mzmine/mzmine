/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot;

import org.jfree.data.xy.AbstractXYZDataset;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageXYZDataset extends AbstractXYZDataset {

  private Double[] xValues;
  private Double[] yValues;
  private Double[] zValues;
  private String seriesKey;

  public ImageXYZDataset(Double[] xValues, Double[] yValues, Double[] zValues, String seriesKey) {
    super();
    this.xValues = xValues;
    this.yValues = yValues;
    this.zValues = zValues;
    this.seriesKey = seriesKey;
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Number getZ(int series, int item) {
    return zValues[item];
  }

  @Override
  public int getItemCount(int series) {
    return xValues.length;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  @Override
  public Comparable<String> getSeriesKey(int series) {
    return seriesKey;
  }
}

package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot;

import org.jfree.data.xy.AbstractXYZDataset;

public class ImageXYZDataset extends AbstractXYZDataset {

  private Integer[] xValues;
  private Integer[] yValues;
  private Double[] zValues;
  private String seriesKey;

  public ImageXYZDataset(Integer[] xValues, Integer[] yValues, Double[] zValues, String seriesKey) {
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

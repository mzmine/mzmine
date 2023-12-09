package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import org.jfree.data.xy.XYZDataset;

public interface XYZBubbleDataset extends XYZDataset {

  public Number getBubbleSize(int series, int item);


  public double getBubbleSizeValue(int series, int item);

  public double[] getBubbleSizeValues();

}





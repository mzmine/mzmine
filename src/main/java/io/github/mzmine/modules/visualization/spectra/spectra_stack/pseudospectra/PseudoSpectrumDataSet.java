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
package io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra;

import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class PseudoSpectrumDataSet extends XYSeriesCollection {

  private static final long serialVersionUID = 1L;

  private Map<XYDataItem, String> annotation;

  public PseudoSpectrumDataSet(boolean autoSort, Comparable... keys) {
    super();
    for (Comparable key : keys) {
      addSeries(new XYSeries(key, autoSort));
    }
  }

  public void addDP(double x, double y, String ann) {
    addDP(0, x, y, ann);
  }

  public void addDP(int series, double x, double y, String ann) {
    if (series >= getSeriesCount()) {
      throw new OutOfRangeException(series, 0, getSeriesCount());
    }

    XYDataItem dp = new XYDataItem(x, y);
    getSeries(series).add(dp);
    if (ann != null) {
      addAnnotation(dp, ann);
    }
  }

  /**
   * Add annotation
   *
   * @param dp
   * @param ann
   */
  public void addAnnotation(XYDataItem dp, String ann) {
    if (annotation == null) {
      this.annotation = new HashMap<>();
    }
    annotation.put(dp, ann);
  }

  public String getAnnotation(int series, int item) {
    if (annotation == null) {
      return null;
    }
    XYDataItem itemDataPoint = getSeries(series).getDataItem(item);
    for (XYDataItem key : annotation.keySet()) {
      if (Math.abs(key.getXValue() - itemDataPoint.getXValue()) < 0.0001) {
        return annotation.get(key);
      }
    }
    return null;
  }

  public void addIdentity(MZTolerance mzTolerance, AbstractMSMSIdentity ann) {
    if (ann instanceof AbstractMSMSDataPointIdentity) {
      addDPIdentity(mzTolerance, (AbstractMSMSDataPointIdentity) ann);
    }
    // TODO add diff identity
  }

  private void addDPIdentity(MZTolerance mzTolerance, AbstractMSMSDataPointIdentity ann) {
    for (int s = 0; s < getSeriesCount(); s++) {
      XYSeries series = getSeries(s);
      for (int i = 0; i < series.getItemCount(); i++) {
        XYDataItem dp = series.getDataItem(i);
        if (mzTolerance.checkWithinTolerance(dp.getXValue(), ann.getMZ())) {
          addAnnotation(dp, ann.getName());
        }
      }
    }
  }

  @Override
  public Number getX(int series, int item) {
    return getSeries(series).getX(item);
  }

  @Override
  public Number getY(int series, int item) {
    return getSeries(series).getY(item);
  }

  @Override
  public Number getEndX(int series, int item) {
    return getX(series, item);
  }

  @Override
  public double getEndXValue(int series, int item) {
    return getXValue(series, item);
  }

  @Override
  public Number getEndY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getEndYValue(int series, int item) {
    return getYValue(series, item);
  }

  @Override
  public Number getStartX(int series, int item) {
    return getX(series, item);
  }

  @Override
  public double getStartXValue(int series, int item) {
    return getXValue(series, item);
  }

  @Override
  public Number getStartY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getStartYValue(int series, int item) {
    return getYValue(series, item);
  }
}

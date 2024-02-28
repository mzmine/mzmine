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



/*
 * Created by Owen Myers (Oweenm@gmail.com)
 */


package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;


/**
 * DataPoint implementation extended with scan number
 */
public class ExpandedDataPoint implements DataPoint {

  private Scan scan = null;
  private double mz, intensity;

  /**
   */
  public ExpandedDataPoint(double mz, double intensity, Scan scan) {

    this.scan = scan;
    this.mz = mz;
    this.intensity = intensity;

  }

  /**
   * Constructor which copies the data from another DataPoint
   */
  public ExpandedDataPoint(DataPoint dp) {
    this.mz = dp.getMZ();
    this.intensity = dp.getIntensity();
  }

  /**
   * Constructor which copies the data from another DataPoint and takes the scan number
   */
  public ExpandedDataPoint(DataPoint dp, Scan scanNumIn) {
    this.mz = dp.getMZ();
    this.intensity = dp.getIntensity();
    this.scan = scanNumIn;
  }

  public ExpandedDataPoint() {
    this.mz = 0.0;
    this.intensity = 0.0;
    this.scan = null;
  }

  @Override
  public double getIntensity() {
    return intensity;
  }

  @Override
  public double getMZ() {
    return mz;
  }

  public Scan getScan() {
    return scan;
  }
}

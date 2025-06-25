/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datasets;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Spectra visualizer data set for scan data points
 */
public class ScanDataSet extends AbstractXYDataset implements IntervalXYDataset, RelativeOption {

  private static final long serialVersionUID = 1L;

  // For comparing small differences.
  private static final double EPSILON = 0.0000001;

  private final String label;
  private final Scan scan;
  private final Map<Integer, String> annotation = new Hashtable<>();
  private final Map<Double, String> mzAnnotationMap = new Hashtable<>();
  private final double maxIntensity;
  private boolean normalize;

  /*
   * Save a local copy of m/z and intensity values, because accessing the scan every time may cause
   * reloading the data from HDD
   */
  // private DataPoint dataPoints[];

  public ScanDataSet(Scan scan) {
    this(scan, false);
  }

  public ScanDataSet(Scan scan, boolean normalize) {
    this("Scan #" + scan.getScanNumber(), scan, normalize);
  }

  public ScanDataSet(String label, Scan scan) {
    this(label, scan, false);
  }

  public ScanDataSet(String label, Scan scan, boolean normalize) {
    // this.dataPoints = scan.getDataPoints();
    this.scan = scan;
    this.label = label;
    this.normalize = normalize;
    maxIntensity = Objects.requireNonNullElse(scan.getBasePeakIntensity(), 0d);

    /*
     * This optimalization is disabled, because it crashes on scans with no datapoints. Also, it
     * distorts the view of the raw data - user would see something different from the actual
     * content of the raw data file.
     *
     * // remove all extra zeros List<DataPoint> dp = new ArrayList<>(); dp.add(dataPoints[0]); for
     * (int i = 1; i < dataPoints.length - 1; i++) { // previous , this and next are zero --> do not
     * add this data point if (Double.compare(dataPoints[i - 1].getIntensity(), 0d) != 0 ||
     * Double.compare(dataPoints[i].getIntensity(), 0d) != 0 || Double.compare(dataPoints[i +
     * 1].getIntensity(), 0d) != 0) { dp.add(dataPoints[i]); } } dp.add(dataPoints[dataPoints.length
     * - 1]); this.dataPoints = dp.toArray(new DataPoint[0]);
     */
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return label;
  }

  @Override
  public int getItemCount(int series) {
    return scan.getNumberOfDataPoints();
  }

  @Override
  public Number getX(int series, int item) {
    return scan.getMzValue(item);
  }

  @Override
  public Number getY(int series, int item) {
    return normalize ? scan.getIntensityValue(item) / maxIntensity * 100d
        : scan.getIntensityValue(item);
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

  public int getIndex(final double mz, final double intensity) {
    for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
      if (Math.abs(mz - scan.getMzValue(i)) < EPSILON
          && Math.abs(intensity - scan.getIntensityValue(i)) < EPSILON) {
        return i;
      }
    }
    return -1;
  }

  /**
   * This function finds highest data point intensity in given m/z range. It is important for
   * normalizing isotope patterns.
   */
  public double getHighestIntensity(Range<Double> mzRange) {
    return ScanUtils.findBasePeak(scan, mzRange).getIntensity();
  }

  public Scan getScan() {
    return scan;
  }

  public void addAnnotation(Map<Integer, String> annotation) {
    this.annotation.putAll(annotation);
  }

  /**
   * Add annotations for m/z values
   *
   * @param annotation m/z value and annotation map
   */
  public void addMzAnnotation(Map<DataPoint, String> annotation) {
    if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      annotation.entrySet().stream()
          .forEach(e -> mzAnnotationMap.put(e.getKey().getMZ(), e.getValue()));
    } else {
      // annotations are almost never displayed in profile data because the mz from the annotation
      // comes from the mass list, which is centered in the peak and not the local maximum.
      annotation.entrySet().stream().forEach(e -> {
        final double mz = e.getKey().getMZ();
        final int index = scan.binarySearch(mz, DefaultTo.CLOSEST_VALUE);
        if (scan.getMzValue(index) - mz < 0.01d) {
          mzAnnotationMap.put(scan.getMzValue(index), e.getValue());
        }
      });
    }
  }


  public String getAnnotation(int item) {
    String ann = mzAnnotationMap.get(getXValue(0, item));
    String ann2 = annotation.get(item);
    if (ann != null && ann2 != null) {
      return ann + " " + ann2;
    }
    if (ann2 != null) {
      return ann2;
    }
    if (ann != null) {
      return ann;
    }
    return null;
  }

  @Override
  public void setRelative(boolean relative) {
    normalize = relative;
  }
}

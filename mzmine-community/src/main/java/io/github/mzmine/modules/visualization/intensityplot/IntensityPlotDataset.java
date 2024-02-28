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

package io.github.mzmine.modules.visualization.intensityplot;

import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.MathUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * This class implements 2 kinds of data sets - CategoryDataset and XYDataset CategoryDataset is
 * used if X axis is a raw data file or string parameter XYDataset is used if X axis is a number
 * parameter
 */
class IntensityPlotDataset extends AbstractDataset implements StatisticalCategoryDataset,
    IntervalXYDataset {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private Object xAxisValueSource;
  private YAxisValueSource yAxisValueSource;
  private Comparable<?> xValues[];
  private RawDataFile[] selectedFiles;
  private FeatureListRow[] selectedRows;

  @SuppressWarnings("rawtypes")
  IntensityPlotDataset(ParameterSet parameters) {

    FeatureList featureList = parameters.getParameter(IntensityPlotParameters.featureList)
        .getValue().getMatchingFeatureLists()[0];
    this.xAxisValueSource = parameters.getParameter(IntensityPlotParameters.xAxisValueSource)
        .getValue();
    this.yAxisValueSource = parameters.getParameter(IntensityPlotParameters.yAxisValueSource)
        .getValue();

    this.selectedFiles = parameters.getParameter(IntensityPlotParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();

    this.selectedRows = parameters.getParameter(IntensityPlotParameters.selectedRows)
        .getMatchingRows(featureList);

    if (xAxisValueSource instanceof ParameterWrapper) {
      MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
      UserParameter xAxisParameter = ((ParameterWrapper) xAxisValueSource).getParameter();
      LinkedHashSet<Comparable> parameterValues = new LinkedHashSet<>();
      for (RawDataFile file : selectedFiles) {
        Object value = project.getParameterValue(xAxisParameter, file);
        parameterValues.add((Comparable<?>) value);
      }
      xValues = parameterValues.toArray(new Comparable[0]);

      // if we have a numerical axis, we don't want the values to be
      // sorted by the data file order, but rather numerically
      if (xAxisParameter instanceof DoubleParameter) {
        Arrays.sort(xValues);
      }
    }

    if (xAxisValueSource == IntensityPlotParameters.rawDataFilesOption) {
      xValues = new String[selectedFiles.length];
      for (int i = 0; i < selectedFiles.length; i++) {
        xValues[i] = selectedFiles[i].getName();
      }
    }
  }

  Feature[] getFeatures(int row, int column) {
    return getFeatures(xValues[column], selectedRows[row]);
  }

  Feature[] getFeatures(Comparable<?> xValue, FeatureListRow row) {
    RawDataFile[] files = getFiles(xValue);
    Feature[] features = new Feature[files.length];
    for (int i = 0; i < files.length; i++) {
      features[i] = row.getFeature(files[i]);
    }
    return features;
  }

  RawDataFile[] getFiles(int column) {
    return getFiles(xValues[column]);
  }

  RawDataFile[] getFiles(Comparable<?> xValue) {
    if (xAxisValueSource instanceof String) {
      RawDataFile columnFile = selectedFiles[getColumnIndex(xValue)];
      return new RawDataFile[]{columnFile};
    }
    if (xAxisValueSource instanceof ParameterWrapper) {
      HashSet<RawDataFile> files = new HashSet<>();
      UserParameter<?, ?> xAxisParameter = ((ParameterWrapper) xAxisValueSource).getParameter();
      MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
      for (RawDataFile file : selectedFiles) {
        Object fileValue = project.getParameterValue(xAxisParameter, file);
        if (fileValue == null) {
          continue;
        }
        if (fileValue.equals(xValue)) {
          files.add(file);
        }
      }
      return files.toArray(new RawDataFile[0]);
    }
    return null;
  }

  public Number getMeanValue(int row, int column) {
    Feature[] features = getFeatures(xValues[column], selectedRows[row]);
    HashSet<Double> values = new HashSet<>();
    for (Feature feature : features) {
      if (feature == null) {
        continue;
      }
      if (yAxisValueSource == YAxisValueSource.HEIGHT) {
        values.add((double) feature.getHeight());
      }
      if (yAxisValueSource == YAxisValueSource.AREA) {
        values.add((double) feature.getArea());
      }
      if (yAxisValueSource == YAxisValueSource.RT) {
        values.add((double) feature.getRT());
      }
    }
    double[] doubleValues = Doubles.toArray(values);
    if (doubleValues.length == 0) {
      return 0;
    }
    return MathUtils.calcAvg(doubleValues);
  }

  public Number getMeanValue(Comparable rowKey, Comparable columnKey) {
    throw (new UnsupportedOperationException("Unsupported"));
  }

  public Number getStdDevValue(int row, int column) {
    Feature[] features = getFeatures(xValues[column], selectedRows[row]);

    // if we have only 1 peak, there is no standard deviation
    if (features.length == 1) {
      return 0;
    }

    HashSet<Double> values = new HashSet<>();
    for (int i = 0; i < features.length; i++) {
      if (features[i] == null) {
        continue;
      }
      if (yAxisValueSource == YAxisValueSource.HEIGHT) {
        values.add((double) features[i].getHeight());
      }
      if (yAxisValueSource == YAxisValueSource.AREA) {
        values.add((double) features[i].getArea());
      }
      if (yAxisValueSource == YAxisValueSource.RT) {
        values.add((double) features[i].getRT());
      }
    }
    double[] doubleValues = Doubles.toArray(values);
    return MathUtils.calcStd(doubleValues);
  }

  @SuppressWarnings("rawtypes")
  public Number getStdDevValue(Comparable rowKey, Comparable columnKey) {
    throw (new UnsupportedOperationException("Unsupported"));
  }

  @SuppressWarnings("rawtypes")
  public int getColumnIndex(Comparable column) {
    for (int i = 0; i < selectedFiles.length; i++) {
      if (selectedFiles[i].getName().equals(column)) {
        return i;
      }
    }
    return -1;
  }

  public Comparable<?> getColumnKey(int column) {
    return xValues[column];
  }

  @SuppressWarnings("rawtypes")
  public List getColumnKeys() {
    return Arrays.asList(xValues);
  }

  @SuppressWarnings("rawtypes")
  public int getRowIndex(Comparable row) {
    for (int i = 0; i < selectedRows.length; i++) {
      if (selectedRows[i].toString().equals(row)) {
        return i;
      }
    }
    return -1;
  }

  public Comparable<?> getRowKey(int row) {
    return selectedRows[row].toString();
  }

  @SuppressWarnings("rawtypes")
  public List getRowKeys() {
    return Arrays.asList(selectedRows);
  }

  @SuppressWarnings("rawtypes")
  public Number getValue(Comparable rowKey, Comparable columnKey) {
    return getMeanValue(rowKey, columnKey);

  }

  public int getColumnCount() {
    return xValues.length;
  }

  public int getRowCount() {
    return selectedRows.length;
  }

  public Number getValue(int row, int column) {
    return getMeanValue(row, column);
  }

  public Number getEndX(int row, int column) {
    return getEndXValue(row, column);
  }

  public double getEndXValue(int row, int column) {
    return ((Number) xValues[column]).doubleValue();

  }

  public Number getEndY(int row, int column) {
    return getEndYValue(row, column);
  }

  public double getEndYValue(int row, int column) {
    return getMeanValue(row, column).doubleValue() + getStdDevValue(row, column).doubleValue();
  }

  public Number getStartX(int row, int column) {
    return getEndXValue(row, column);
  }

  public double getStartXValue(int row, int column) {
    return getEndXValue(row, column);
  }

  public Number getStartY(int row, int column) {
    return getStartYValue(row, column);
  }

  public double getStartYValue(int row, int column) {
    return getMeanValue(row, column).doubleValue() - getStdDevValue(row, column).doubleValue();
  }

  public DomainOrder getDomainOrder() {
    return DomainOrder.ASCENDING;
  }

  public int getItemCount(int series) {
    return xValues.length;
  }

  public Number getX(int series, int item) {
    return getStartX(series, item);
  }

  public double getXValue(int series, int item) {
    return getStartX(series, item).doubleValue();
  }

  public Number getY(int series, int item) {
    return getMeanValue(series, item);
  }

  public double getYValue(int series, int item) {
    return getMeanValue(series, item).doubleValue();
  }

  public int getSeriesCount() {
    return selectedRows.length;
  }

  public Comparable<?> getSeriesKey(int series) {
    return getRowKey(series);
  }

  @SuppressWarnings("rawtypes")
  public int indexOf(Comparable value) {
    return getRowIndex(value);
  }

}

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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.significance.ttest.StudentTTest;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class VolcanoDatasetProvider extends SimpleXYProvider implements
    XYItemObjectProvider<RowSignificanceTestResult>, PlotXYZDataProvider {

  private final StudentTTest<?> test;
  private final List<RowSignificanceTestResult> results;

  private final AbundanceMeasure abundanceMeasure;

  public VolcanoDatasetProvider(StudentTTest<?> test, List<RowSignificanceTestResult> results,
      Color color, String key, AbundanceMeasure abundanceMeasure) {
    super(key, color, new DecimalFormat("0.0"), new DecimalFormat("0.0"));
    this.test = test;
    this.results = results;
    this.abundanceMeasure = abundanceMeasure;
  }

  @Override
  public @Nullable String getLabel(int index) {
    return null;
  }

  @Override
  public @Nullable String getToolTipText(int index) {
    RowSignificanceTestResult result = results.get(index);
    final FeatureAnnotation bestAnnotation = FeatureUtils.getBestFeatureAnnotation(result.row());
    String name = result.row().toString();
    if (bestAnnotation != null) {
      name += ", " + bestAnnotation.getCompoundName();
    }
    return String.format("""
        %s
        Fold change: %.3f
        p-Value: %.3f""", name, Math.pow(2, getDomainValue(index)), result.pValue());
  }

  @Override
  public RowSignificanceTestResult getItemObject(int item) {
    return results.get(item);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    double[] minusLog10PValue = new double[results.size()];
    final List<RawDataFile> groupAFiles = test.getGroupAFiles();
    final List<RawDataFile> groupBFiles = test.getGroupBFiles();

    for (int i = 0; i < results.size(); i++) {
      minusLog10PValue[i] = -Math.log10(results.get(i).pValue());
    }
    double[] log2FoldChange = StatisticUtils.calculateLog2FoldChange(results, groupAFiles,
        groupBFiles, abundanceMeasure);

    setxValues(log2FoldChange);
    setyValues(minusLog10PValue);
  }

  @Override
  public @Nullable PaintScale getPaintScale() {
    return null;
  }

  /**
   * @return The row id, mainly used for the export of the chart data.
   */
  @Override
  public double getZValue(int index) {
    return results.get(index).row().getID();
  }

  @Override
  public @Nullable Double getBoxHeight() {
    return null;
  }

  @Override
  public @Nullable Double getBoxWidth() {
    return null;
  }
}

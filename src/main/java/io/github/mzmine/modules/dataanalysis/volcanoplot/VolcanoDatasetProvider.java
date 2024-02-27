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
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.significance.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.significance.ttest.StudentTTest;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VolcanoDatasetProvider implements PlotXYDataProvider,
    XYItemObjectProvider<RowSignificanceTestResult> {

  private final StudentTTest<?> test;
  private final List<RowSignificanceTestResult> results;
  private final Color color;

  private final String key;
  private final AbundanceMeasure abundanceMeasure;
  private double[] log2FoldChange;
  private double[] minusLog10PValue;

  public VolcanoDatasetProvider(StudentTTest<?> test, List<RowSignificanceTestResult> results,
      Color color, String key, AbundanceMeasure abundanceMeasure) {
    this.test = test;
    this.results = results;
    this.color = color;
    this.key = key;
    this.abundanceMeasure = abundanceMeasure;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return color;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(color);
  }

  @Override
  public @Nullable String getLabel(int index) {
    return null;
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey() {
    return key;
  }

  @Override
  public @Nullable String getToolTipText(int itemIndex) {
    return results.get(itemIndex).row().toString();
  }

  @Override
  public RowSignificanceTestResult getItemObject(int item) {
    return results.get(item);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    minusLog10PValue = new double[results.size()];
    final List<RawDataFile> groupAFiles = test.getGroupAFiles();
    final List<RawDataFile> groupBFiles = test.getGroupBFiles();

    for (int i = 0; i < results.size(); i++) {
      minusLog10PValue[i] = -Math.log10(results.get(i).pValue());
    }
    log2FoldChange = StatisticUtils.calculateLog2FoldChange(results, groupAFiles, groupBFiles,
        abundanceMeasure);
  }

  @Override
  public double getDomainValue(int index) {
    return log2FoldChange[index];
  }

  @Override
  public double getRangeValue(int index) {
    return minusLog10PValue[index];
  }

  @Override
  public int getValueCount() {
    return results.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 0;
  }

}

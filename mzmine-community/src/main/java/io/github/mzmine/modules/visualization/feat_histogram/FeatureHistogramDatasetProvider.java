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

package io.github.mzmine.modules.visualization.feat_histogram;

import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import java.awt.Color;
import java.text.DecimalFormat;
import org.jetbrains.annotations.Nullable;

public class FeatureHistogramDatasetProvider extends SimpleXYProvider {

  private final NumberType dataType;

  public FeatureHistogramDatasetProvider(Color color, String key, NumberType dataType) {
    super(key, color, dataType.getFormat(), new DecimalFormat("0"));
    this.dataType = dataType;
  }

  @Override
  public @Nullable String getLabel(int index) {
    return null;
  }

  @Override
  public @Nullable String getToolTipText(int index) {
    String name = dataType.getUniqueID();
    return name;
  }

//  @Override
//  public void computeValues(Property<TaskStatus> status) {
//    double[] minusLog10PValue = new double[results.size()];
//    final List<RawDataFile> groupAFiles = test.getGroupAFiles();
//    final List<RawDataFile> groupBFiles = test.getGroupBFiles();
//
//    for (int i = 0; i < results.size(); i++) {
//      minusLog10PValue[i] = -Math.log10(results.get(i).pValue());
//    }
//    double[] log2FoldChange = StatisticUtils.calculateLog2FoldChange(results, groupAFiles,
//        groupBFiles, dataType);
//
//    setxValues(log2FoldChange);
//    setyValues(minusLog10PValue);
//  }

}

/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ZLegendCategoryProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.collections.SortOrder;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.util.Map;
import javafx.beans.property.Property;
import org.apache.commons.math3.linear.RealMatrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;

public class PCALoadingsProvider extends SimpleXYProvider implements PlotXYZDataProvider,
    ZLegendCategoryProvider, XYItemObjectProvider<FeatureListRow> {

  private final PCARowsResult result;
  private final int loadingsIndexY;
  private final int loadingsIndexX;

  private int[] zCategories;
  private int numberOfCategories;
  private LookupPaintScale paintScale;
  private String[] legendNames;

  /**
   * @param loadingsIndexX index of the principal component used for domain axis, subtract 1 from
   *                       the number since the pc matrix starts at 0.
   * @param loadingsIndexY index of the principal component used for range axis, subtract 1 from the
   *                       number since the pc matrix starts at 0.
   */
  public PCALoadingsProvider(PCARowsResult result, String seriesKey, Color awt, int loadingsIndexX,
      int loadingsIndexY) {
    super(seriesKey, awt);
    this.result = result;
    this.loadingsIndexX = loadingsIndexX;
    this.loadingsIndexY = loadingsIndexY;
  }

  public PCALoadingsProvider(PCARowsResult result, String seriesKey, Color awt) {
    this(result, seriesKey, awt, 0, 1);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    final PCAResult pcaResult = result.pcaResult();

    final RealMatrix loadingsMatrix = pcaResult.getLoadingsMatrix();

    final Map<FeatureListRow, DataType<?>> bestRowAnnotationType = CompoundAnnotationUtils.mapBestAnnotationTypesByPriority(
        result.rows(), true);

    // only create order of actually existing annotation types + missing type
    // ASCENDING will put MissingType first so that it is always black
    Map<DataType<?>, Integer> typesInOrder = CompoundAnnotationUtils.rankUniqueAnnotationTypes(
        bestRowAnnotationType.values(), SortOrder.ASCENDING);
    numberOfCategories = typesInOrder.size();

    double[] domainData = new double[loadingsMatrix.getColumnDimension()];
    double[] rangeData = new double[loadingsMatrix.getColumnDimension()];
    zCategories = new int[loadingsMatrix.getColumnDimension()];
    assert result.rows().size() == loadingsMatrix.getColumnDimension();

    for (int i = 0; i < loadingsMatrix.getColumnDimension(); i++) {
      domainData[i] = loadingsMatrix.getEntry(loadingsIndexX, i);
      rangeData[i] = loadingsMatrix.getEntry(loadingsIndexY, i);
      // find annotation type or missing type
      FeatureListRow row = result.rows().get(i);
      final DataType<?> bestTypeWithValue = bestRowAnnotationType.get(row);
      zCategories[i] = typesInOrder.get(bestTypeWithValue);
    }

    paintScale = new LookupPaintScale(0, numberOfCategories, Color.BLACK);
    final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    for (int i = 0; i < numberOfCategories; i++) {
      paintScale.add(i, colors.getAWT(i));
    }

    // LinkedHashMap is sorted
    legendNames = typesInOrder.keySet().stream()
        .map(type -> type instanceof MissingValueType _ ? "Unknown" : type.getHeaderString())
        .toArray(String[]::new);

    setxValues(domainData);
    setyValues(rangeData);
  }

  @Override
  public @Nullable PaintScale getPaintScale() {
    return paintScale;
  }

  @Override
  public double getZValue(int index) {
    return zCategories[index];
  }

  @Override
  public @Nullable Double getBoxHeight() {
    return 5d;
  }

  @Override
  public @Nullable Double getBoxWidth() {
    return 5d;
  }


  @Override
  public int getNumberOfLegendCategories() {
    return numberOfCategories;
  }

  @Override
  public @NotNull String getLegendCategoryLabel(int category) {
    return legendNames[category];
  }

  @Override
  public @NotNull Paint getLegendCategoryItemColor(int category) {
    return paintScale.getPaint(category);
  }

  @Override
  public @NotNull Shape getLegendCategoryShape(int category) {
    return JFreeChartUtils.defaultShape();
  }

  @Override
  public FeatureListRow getItemObject(int item) {
    return result.rows().get(item);
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return getItemObject(itemIndex).toString();
  }
}

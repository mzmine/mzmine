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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ZCategoryProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataUtils;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColoredMetadataGroup;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.Property;
import org.apache.commons.math3.linear.RealMatrix;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;

public class PCAScoresProvider extends SimpleXYProvider implements PlotXYZDataProvider,
    ZCategoryProvider, XYItemObjectProvider<RawDataFile> {

  private final PCARowsResult result;
  private final int pcX;
  private final int pcY;
  private final MetadataColumn<?> groupingColumn;
  private double[] zData;
  private LookupPaintScale paintScale;
  private int numberOfCategories;
  private String[] groupNames;
  private Color[] groupColors;
  // raw data file values will be sorted by metadata
  // for the groups to appear in order in the legend - the order may be different from results.files order
  // access index of raw data file index in pca results
  private final Map<RawDataFile, Integer> pcaResultIndex = new HashMap<>();
  // index of raw data file in dataset
  private final Map<Integer, RawDataFile> dataFileIndex = new HashMap<>();

  /**
   * @param pcX index of the principal component used for domain axis, subtract 1 from the number
   *            since the pc matrix starts at 0.
   * @param pcY index of the principal component used for range axis, subtract 1 from the number
   *            since the pc matrix starts at 0.
   */
  public PCAScoresProvider(PCARowsResult result, String seriesKey, Color awt, int pcX, int pcY,
      MetadataColumn<?> groupingColumn) {
    super(seriesKey, awt);
    this.result = result;
    this.pcX = pcX;
    this.pcY = pcY;
    this.groupingColumn = groupingColumn;
    for (int i = 0; i < result.files().size(); i++) {
      pcaResultIndex.put(result.files().get(i), i);
    }
  }

  public PCAScoresProvider(PCARowsResult result, String seriesKey, Color awt) {
    this(result, seriesKey, awt, 0, 1, null);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    // group and assign default color
    // already sorted by name
    // null may be first element if present
    final List<ColoredMetadataGroup> groups = ColorByMetadataUtils.colorByColumn(groupingColumn,
        result.files());

    assert result.files().size() == groups.stream().mapToInt(ColoredMetadataGroup::size).sum();

    setFileDataIndexes(groups);

    setXYZScores(groups);

    // too many numeric groups - create gradient
    if (groups.size() > 10 && groupingColumn.hasNaturalOrder()) {
      createNumericGradientPaintScale(groups);
    } else {
      createDistinctCategoriesPaintScale(groups);
    }
  }

  /**
   * Distinct colors for each group
   *
   * @param groups
   */
  private void createDistinctCategoriesPaintScale(List<ColoredMetadataGroup> groups) {
    // if no column selected all will be treated as null
    numberOfCategories = groups.size();
    groupNames = new String[numberOfCategories];
    groupColors = new Color[numberOfCategories];

    // create paintscale from groups
    paintScale = ColorByMetadataUtils.createPaintScale(groups);

    // null values are first group if present and they have the lowest value
    // doubleValues are already sorted ascending
    for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
      final ColoredMetadataGroup group = groups.get(groupIndex);
      // this needs to match the order in the legend - therefore the data files need to be sorted by groupedFiles
      groupNames[groupIndex] = group.valueString(); // N/A or real value
      groupColors[groupIndex] = group.colorAWT();
      paintScale.add(group.doubleValue(), group.colorAWT());
    }
  }

  /**
   * Gradient paintscale with numeric values
   *
   * @param groups
   */
  private void createNumericGradientPaintScale(List<ColoredMetadataGroup> groups) {

  }

  private void setFileDataIndexes(List<ColoredMetadataGroup> groups) {
    int dataIndex = 0;
    for (ColoredMetadataGroup group : groups) {
      for (RawDataFile file : group.files()) {
        dataFileIndex.put(dataIndex, file);
        dataIndex++;
      }
    }
  }

  private void setXYZScores(List<ColoredMetadataGroup> groups) {
    final PCAResult pcaResult = result.pcaResult();
    final RealMatrix scores = pcaResult.projectDataToScores(pcX, pcY);

    // actual xyz data
    zData = new double[scores.getRowDimension()];
    double[] domainData = new double[scores.getRowDimension()];
    double[] rangeData = new double[scores.getRowDimension()];

    int dp = 0;
    for (final ColoredMetadataGroup group : groups) {
      for (RawDataFile file : group.files()) {
        // set data
        final int resultIndex = pcaResultIndex.get(file);
        domainData[dp] = scores.getEntry(resultIndex, 0);
        rangeData[dp] = scores.getEntry(resultIndex, 1);
        // use numeric value for double or date column and use id index for string values
        // NaN for missing values
        zData[dp] = group.doubleValue();
        dp++;
      }
    }

    setxValues(domainData);
    setyValues(rangeData);
  }


  @Override
  public @Nullable PaintScale getPaintScale() {
    return paintScale;
  }

  public void setPaintScale(LookupPaintScale paintScale) {
    this.paintScale = paintScale;
  }

  @Override
  public double getZValue(int index) {
    return zData[index];
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
  public String getToolTipText(int itemIndex) {
    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    final RawDataFile file = getItemObject(itemIndex);
    final Object value = metadata.getValue(groupingColumn, file);

    return """
        File: %s
        Group: %s
        """.formatted(file.getName(), value);
  }

  @Override
  public int getNumberOfCategories() {
    return numberOfCategories;
  }

  @Override
  public String getLegendLabel(int category) {
    return groupNames[category] != null ? groupNames[category] : "unnamed group";
  }

  @Override
  public Paint getLegendItemColor(int category) {
    return groupColors[category] != null ? groupColors[category] : Color.black;
  }

  @Override
  public RawDataFile getItemObject(int item) {
    return dataFileIndex.get(item);
  }
}

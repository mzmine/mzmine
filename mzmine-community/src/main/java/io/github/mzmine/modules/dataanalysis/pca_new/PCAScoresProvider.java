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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ZCategoryProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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
  private int[] zData;
  private LookupPaintScale paintScale;
  private Map<RawDataFile, Integer> fileGroupMap;
  private int numberOfCategories;
  private String[] groupNames;

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
  }

  public PCAScoresProvider(PCARowsResult result, String seriesKey, Color awt) {
    this(result, seriesKey, awt, 0, 1, null);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    final PCAResult pcaResult = result.pcaResult();

    final RealMatrix scores = pcaResult.projectDataToScores(pcX, pcY);

    final List<RawDataFile> files = result.files();
    final Map<?, List<RawDataFile>> groupedFiles = MZmineCore.getProjectMetadata()
        .groupFilesByColumn(groupingColumn);

    numberOfCategories = Math.max(groupedFiles.size(), 1);
    groupNames = new String[Math.max(groupedFiles.size(), 1)];

    AtomicInteger counter = new AtomicInteger(0);
    fileGroupMap = new HashMap<>();
    groupedFiles.forEach((groupKey, value) -> {
      value.forEach(file -> {
        fileGroupMap.put(file, counter.get());
      });
      groupNames[counter.get()] = groupKey.toString();
      counter.getAndIncrement();
    });

    double[] domainData = new double[scores.getRowDimension()];
    double[] rangeData = new double[scores.getRowDimension()];
    zData = new int[scores.getRowDimension()];
    assert files.size() == scores.getRowDimension();
    for (int i = 0; i < scores.getRowDimension(); i++) {
      domainData[i] = scores.getEntry(i, 0);
      rangeData[i] = scores.getEntry(i, 1);
      final RawDataFile file = files.get(i);
      zData[i] = Objects.requireNonNullElse(fileGroupMap.get(file), 0);
    }

    setxValues(domainData);
    setyValues(rangeData);

    final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    final Color defaultColor = colors.getPositiveColorAWT();

    paintScale = new LookupPaintScale(0, numberOfCategories, defaultColor);
    colors.resetColorCounter();
    for (int i = 0; i < groupedFiles.size(); i++) {
      paintScale.add(i, colors.getAWT(i));
    }
  }

  @Override
  public @Nullable PaintScale getPaintScale() {
    return paintScale;
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
    final RawDataFile file = result.files().get(itemIndex);
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
  public RawDataFile getItemObject(int item) {
    return result.files().get(item);
  }
}

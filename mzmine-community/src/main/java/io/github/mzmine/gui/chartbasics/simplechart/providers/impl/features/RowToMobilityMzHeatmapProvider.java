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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RowToMobilityMzHeatmapProvider implements PieXYZDataProvider<IMSRawDataFile> {

  private final String seriesKey;
  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final NumberFormat mobilityFormat;
  private final List<ModularFeatureListRow> rows;
  private final double[] summedValues;
  private final IMSRawDataFile[] files;

  private double maxValue = Double.NEGATIVE_INFINITY;
  private double minValue = Double.POSITIVE_INFINITY;
  private final double maxDiameter = 30d;
  private final double minDiameter = 10d;
  private double deltaDiameter = 1d;
  private double deltaValue = 1d;

  public RowToMobilityMzHeatmapProvider(@NotNull final Collection<ModularFeatureListRow> f) {
    // copy the list, so we don't run into problems in case the flist is modified
    rows = new ArrayList<>(f);
    seriesKey = (f.isEmpty()) ? "No features found" : rows.get(0).getFeatureList().getName();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    summedValues = new double[rows.size()];
    // to set first to remove duplicates
    files = rows.stream().flatMap(row -> row.getRawDataFiles().stream())
        .filter(file -> file instanceof IMSRawDataFile).distinct().toArray(IMSRawDataFile[]::new);
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return Color.BLACK;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.BLACK;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    ModularFeatureListRow f = rows.get(index);
    final String sb =
        "m/z:" + mzFormat.format(f.getAverageMZ()) + "\n" + "Mobility: " + mobilityFormat.format(
            f.getAverageMobility());
    return sb;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    ModularFeatureListRow f = rows.get(itemIndex);

    final String sb =
        "m/z:" + mzFormat.format(f.getMZRange().lowerEndpoint()) + " - " + mzFormat.format(
            f.getMZRange().upperEndpoint()) + "\n" + "Height: " + intensityFormat.format(
            f.getAverageHeight()) + "\n" + "Retention time" + ": " + rtFormat.format(
            f.getAverageRT()) + " min\n" + "Mobility: " + mobilityFormat.format(
            f.getAverageMobility());
    return sb;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    for (int i = 0; i < rows.size(); i++) {
      final ModularFeatureListRow row = rows.get(i);
      for (final IMSRawDataFile file : files) {
        final ModularFeature feature = row.getFeature(file);
        if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
          summedValues[i] += feature.getHeight();
        }
        minValue = Math.min(summedValues[i], minValue);
        maxValue = Math.max(summedValues[i], maxValue);

        if (status.getValue() == TaskStatus.CANCELED) {
          return;
        }
      }
    }
    deltaDiameter = maxDiameter - minDiameter;
    deltaValue = maxValue - minValue;
  }

  @Override
  public double getDomainValue(int index) {
    return rows.get(index).getAverageMZ();
  }

  @Override
  public double getRangeValue(int index) {
    return Objects.requireNonNullElse(rows.get(index).getAverageMobility(), 0f).doubleValue();
  }

  @Override
  public int getValueCount() {
    return rows.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public IMSRawDataFile[] getSliceIdentifiers() {
    return files;
  }

  @Override
  public double getZValue(int index) {
    return summedValues[index];
  }

  @Override
  public double getZValue(int series, int item) {
    final ModularFeatureListRow row = rows.get(item);
    final ModularFeature f = row.getFeature(files[series]);
    if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN) {
      return f.getHeight();
    }
    return 0d;
  }

  @NotNull
  @Override
  public Color getSliceColor(int series) {
    return files[series].getColorAWT();
  }

  @Override
  public double getPieDiameter(int index) {
    return (getZValue(index) - minValue) / deltaValue * deltaDiameter + minDiameter;
  }

  @Override
  public String getLabelForSeries(int series) {
    return files[series].getName();
  }

  @Nullable
  public List<ModularFeatureListRow> getSourceRows() {
    return rows;
  }
}

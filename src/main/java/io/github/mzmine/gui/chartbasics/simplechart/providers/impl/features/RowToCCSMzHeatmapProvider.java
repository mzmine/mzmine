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
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RowToCCSMzHeatmapProvider implements
    PieXYZDataProvider<IMSRawDataFile> {

  private final String seriesKey;
  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final NumberFormat ccsFormat;
  private final List<ModularFeatureListRow> rows;
  private final double pieDiameter = 7d;
  private final double[] summedValues;
  private final IMSRawDataFile[] files;

  private double maxValue = Double.NEGATIVE_INFINITY;
  private double minValue = Double.POSITIVE_INFINITY;
  private double maxDiameter = 30d;
  private double minDiameter = 10d;
  private double deltaDiameter = 1d;
  private double deltaValue = 1d;

  public RowToCCSMzHeatmapProvider(@NotNull final Collection<ModularFeatureListRow> f) {
    // copy the list, so we don't run into problems in case the flist is modified
    rows = new ArrayList<>(f);
    seriesKey = (f.isEmpty()) ? "No features found" : rows.get(0).getFeatureList().getName();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    ccsFormat = MZmineCore.getConfiguration().getCCSFormat();
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
    StringBuilder sb = new StringBuilder();
    sb.append("m/z:");
    sb.append(mzFormat.format(f.getAverageMZ()));
    sb.append("\n");
    sb.append("CCS: ");
    sb.append(ccsFormat.format(f.getAverageCCS()));
    return sb.toString();
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

    StringBuilder sb = new StringBuilder();
    sb.append("m/z:");
    sb.append(mzFormat.format(f.getMZRange().lowerEndpoint()));
    sb.append(" - ");
    sb.append(mzFormat.format(f.getMZRange().upperEndpoint()));
    sb.append("\n");
    sb.append("Height: ");
    sb.append(intensityFormat.format(f.getAverageHeight()));
    sb.append("\n");
    sb.append("Retention time");
    sb.append(": ");
    sb.append(rtFormat.format(f.getAverageRT()));
    sb.append(" min\n");
    sb.append("CCS: ");
    sb.append(ccsFormat.format(f.getAverageCCS()));
    return sb.toString();
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    for (int i = 0; i < rows.size(); i++) {
      final ModularFeatureListRow row = rows.get(i);
      for(final IMSRawDataFile file : files) {
        final ModularFeature feature = row.getFeature(file);
        if(feature == null) {
          continue;
        }
        if(feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
          summedValues[i] += feature.getHeight();
        }
        minValue = Math.min(summedValues[i], minValue);
        maxValue = Math.max(summedValues[i], maxValue);

        if(status.get() == TaskStatus.CANCELED) {
          return;
        }
      }
    }
    deltaDiameter = maxDiameter - minDiameter;
    deltaValue = maxValue - minValue;
  }

  @Override
  public double getDomainValue(int index) {
    return rows.get(index).getAverageMZ() * rows.get(index).getRowCharge();
  }

  @Override
  public double getRangeValue(int index) {
    return Objects.requireNonNullElse(rows.get(index).getAverageCCS(), 0f).doubleValue();
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
    return (getZValue(index) - minValue)/deltaValue * deltaDiameter + minDiameter;
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

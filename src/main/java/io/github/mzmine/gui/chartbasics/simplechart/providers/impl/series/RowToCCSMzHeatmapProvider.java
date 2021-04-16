/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class RowToCCSMzHeatmapProvider implements PlotXYZDataProvider {

  private final String seriesKey;
  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final NumberFormat ccsFormat;
  private final UnitFormat unitFormat;
  private final List<ModularFeatureListRow> rows;
  private double boxWidth;
  private double boxHeight;

  public RowToCCSMzHeatmapProvider(@Nonnull final Collection<ModularFeatureListRow> f) {
    rows = f.stream().filter(row -> row.getAverageCCS() != null).collect(Collectors.toList());
    seriesKey = (rows.isEmpty()) ? "No features found" : rows.get(0).getFeatureList().getName();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    ccsFormat = MZmineCore.getConfiguration().getCCSFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
  }

  @Nonnull
  @Override
  public Color getAWTColor() {
    return Color.BLACK;
  }

  @Nonnull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.BLACK;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return null;
  }

  @Nonnull
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
    sb.append(unitFormat.format("Retention time", "min"));
    sb.append(": ");
    sb.append(rtFormat.format(f.getAverageRT()));
    sb.append("\n");
    sb.append("CCS: ");
    sb.append(ccsFormat.format(f.getAverageCCS()));
    sb.append("\nHeight: ");
    sb.append(intensityFormat.format(f.getHeight()));
    return sb.toString();
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    boxWidth = 0.01;
    boxHeight = 4d;
  }

  @Override
  public double getDomainValue(int index) {
    ModularFeatureListRow f = rows.get(index);
    return f.getAverageMZ() * f.getRowCharge();
  }

  @Override
  public double getRangeValue(int index) {
    return rows.get(index).getAverageCCS();
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
  public double getZValue(int index) {
    return rows.get(index).getHeight();
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return boxHeight;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return boxWidth;
  }

  @Nullable
  public List<ModularFeatureListRow> getSourceRows() {
    return rows;
  }

}

/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScanBPCProvider implements PlotXYDataProvider, MassSpectrumProvider<Scan> {

  private final List<Scan> scans;
  private final javafx.scene.paint.Color color;
  private final String seriesKey;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;

  public ScanBPCProvider(final List<Scan> scans) {
    this.scans = scans;
    if (!scans.isEmpty()) {
      this.color = scans.get(0).getDataFile().getColor();
    } else {
      color = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();
    }
    seriesKey = "Base peak chromatogram (" + scans.get(0).getDataFile().getName() + ")";

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(color);
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return color;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return mzFormat.format(scans.get(index).getBasePeakMz());
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return ScanUtils.scanToString(scans.get(itemIndex), true);
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    // nothing to compute, everything is in ram already.
  }

  @Override
  public double getDomainValue(int index) {
    return scans.get(index).getRetentionTime();
  }

  @Override
  public double getRangeValue(int index) {
    return Objects.requireNonNullElse(scans.get(index).getBasePeakIntensity(), 0d);
  }

  @Override
  public int getValueCount() {
    return scans.size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public Scan getSpectrum(int index) {
    if (index > scans.size()) {
      return null;
    }
    return scans.get(index);
  }
}

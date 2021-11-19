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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SingleSpectrumProvider implements PlotXYDataProvider {

  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

  private final MassSpectrum spectrum;
  private final String seriesKey;
  private final Color color;

  public SingleSpectrumProvider(MassSpectrum spectrum, String seriesKey, Color color) {
    this.spectrum = spectrum;
    this.seriesKey = seriesKey;
    this.color = color;
  }

  public SingleSpectrumProvider(Scan scan) {
    this(scan, ScanUtils.scanToString(scan, false), scan.getDataFile().getColor());
  }

  @NotNull
  @Override
  public java.awt.Color getAWTColor() {
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
    return mzFormat.format(spectrum.getMzValue(index));
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return "m/z: " + mzFormat
        .format(spectrum.getMzValue(itemIndex)) + "\nIntensity: " + intensityFormat
        .format(spectrum.getIntensityValue(itemIndex));
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

  }

  @Override
  public double getDomainValue(int index) {
    return spectrum.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return spectrum.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return spectrum.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }
}

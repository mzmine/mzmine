/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScanBPCProvider implements PlotXYDataProvider, MassSpectrumProvider<Scan> {

  private final List<Scan> scans;
  private final boolean normalizeToOne;
  private final javafx.scene.paint.Color color;
  private final String seriesKey;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private double normalizationFactor = 1;

  public ScanBPCProvider(final List<Scan> scans) {
    this(scans, false);
  }

  public ScanBPCProvider(final List<Scan> scans, final boolean normalizeToOne) {
    this.scans = scans;
    this.normalizeToOne = normalizeToOne;
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
  public void computeValues(Property<TaskStatus> status) {
    normalizationFactor = 1 / scans.stream()
        .mapToDouble(scan -> Objects.requireNonNullElse(scan.getBasePeakIntensity(), 0d)).max()
        .orElse(1d);
  }

  @Override
  public double getDomainValue(int index) {
    return scans.get(index).getRetentionTime();
  }

  @Override
  public double getRangeValue(int index) {
    return Objects.requireNonNullElse(scans.get(index).getBasePeakIntensity(), 0d)
        * normalizationFactor;
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

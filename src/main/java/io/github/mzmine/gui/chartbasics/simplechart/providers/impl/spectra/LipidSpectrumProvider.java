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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidSpectrumProvider implements PlotXYDataProvider {

  private final String seriesKey;
  private final MassSpectrum spectrum;
  private final List<LipidFragment> matchedFragments;
  private final Color color;

  public LipidSpectrumProvider(List<LipidFragment> matchedFragments, MassSpectrum spectrum,
      String seriesKey, Color color) {
    this.matchedFragments = matchedFragments;
    this.spectrum = spectrum;
    this.seriesKey = seriesKey;
    this.color = color;
  }

  public LipidSpectrumProvider(List<LipidFragment> matchedFragments, double[] mzs,
      double[] intensities, String seriesKey) {
    this(matchedFragments, mzs, intensities, seriesKey,
        MZmineCore.getConfiguration().isDarkMode() ? Color.lightGray : Color.black);
  }

  public LipidSpectrumProvider(List<LipidFragment> matchedFragments, double[] mzs,
      double[] intensities, String seriesKey, Color color) {
    this.matchedFragments = matchedFragments;
    this.color = color;
    this.spectrum = new MassSpectrum() {
      @Override
      public int getNumberOfDataPoints() {
        return mzs.length;
      }

      @Override
      public MassSpectrumType getSpectrumType() {
        if (spectrum != null) {
          return spectrum.getSpectrumType();
        }
        return null;
      }

      @Override
      public double[] getMzValues(@NotNull double[] dst) {
        return new double[0]; // Local implementation only so this does not matter
      }

      @Override
      public double[] getIntensityValues(@NotNull double[] dst) {
        return new double[0]; // Local implementation only so this does not matter
      }

      @Override
      public double getMzValue(int index) {
        return mzs[index];
      }

      @Override
      public double getIntensityValue(int index) {
        return intensities[index];
      }

      @Nullable
      @Override
      public Double getBasePeakMz() {
        return null;
      }

      @Nullable
      @Override
      public Double getBasePeakIntensity() {
        return null;
      }

      @Nullable
      @Override
      public Integer getBasePeakIndex() {
        return null;
      }

      @Nullable
      @Override
      public Range<Double> getDataPointMZRange() {
        return null;
      }

      @Nullable
      @Override
      public Double getTIC() {
        return null;
      }

      @NotNull
      @Override
      public Iterator<DataPoint> iterator() {
        return null;
      }

    };

    this.seriesKey = seriesKey;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return color;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(color);
  }


  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
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

  @Override
  public @org.jetbrains.annotations.Nullable String getLabel(int index) {
    return null;
  }

  @Override
  public @org.jetbrains.annotations.Nullable String getToolTipText(int itemIndex) {
    return null;
  }
}

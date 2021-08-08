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

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.beans.property.SimpleObjectProperty;

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

      @Override
      public Stream<DataPoint> stream() {
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

  @Nullable
  @Override
  public String getLabel(int index) {
    if (matchedFragments != null) {
      return buildFragmentAnnotation(matchedFragments.get(index));
    } else {
      return null;
    }
  }

  private String buildFragmentAnnotation(LipidFragment lipidFragment) {
    if (lipidFragment.getLipidFragmentInformationLevelType()
        .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
      StringBuilder sb = new StringBuilder();
      sb.append(lipidFragment.getLipidChainType() + " " + lipidFragment.getChainLength() + ":"
          + lipidFragment.getNumberOfDBEs());
      System.out.println(sb.toString());
      return sb.toString();
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(lipidFragment.getRuleType());
      return sb.toString();
    }
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    if (matchedFragments != null) {
      return buildFragmentAnnotation(matchedFragments.get(itemIndex));
    } else {
      return null;
    }
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

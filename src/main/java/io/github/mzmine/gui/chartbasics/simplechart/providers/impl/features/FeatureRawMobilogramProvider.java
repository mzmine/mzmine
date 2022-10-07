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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.IonMobilityUtils.MobilogramType;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Goes to the raw data and builds a mobilogram for the m/z and mobility range of the feature.
 * Useful for comparing the processed to the actual raw data.
 */
public class FeatureRawMobilogramProvider implements PlotXYDataProvider {

  private final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final ModularFeature f;
  private final IonMobilogramTimeSeries featureData;
  private SummedIntensityMobilitySeries rawMobilogram;
  private final Range<Double> mzRange;
  private double percentage = 0d;

  public FeatureRawMobilogramProvider(@NotNull final ModularFeature f,
      @NotNull final Range<Double> mzRange) {
    this.f = f;
    this.mzRange = mzRange;
    if (f.getFeatureData() instanceof IonMobilogramTimeSeries) {
      featureData = (IonMobilogramTimeSeries) f.getFeatureData();
    } else {
      throw new IllegalArgumentException("Feature does not have mobility data.");
    }
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return f.getRawDataFile().getColorAWT();
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return f.getRawDataFile().getColor();
  }

  @Override
  public @Nullable String getLabel(int index) {
    return mobilityFormat.format(rawMobilogram.getMobility(index));
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey() {
    return FeatureUtils.featureToString(f) + " (raw)";
  }

  @Override
  public @Nullable String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    final List<Frame> frames = featureData.getSpectra();
    final List<IonMobilitySeries> mobilograms = new ArrayList<>();
    for (int i = 0; i < frames.size(); i++) {
      percentage = i / ((double) frames.size() - 1);

      final Frame frame = frames.get(i);
      mobilograms.add(IonMobilityUtils
          .buildMobilogramForMzRange(frame, mzRange, MobilogramType.BASE_PEAK, null));
      if (status.get() == TaskStatus.CANCELED) {
        return;
      }
    }

    final int binningWith = BinningMobilogramDataAccess
        .getPreviousBinningWith((ModularFeatureList) f.getFeatureList(), f.getMobilityUnit());
    var binner = new BinningMobilogramDataAccess((IMSRawDataFile) f.getRawDataFile(), binningWith);
    binner.setMobilogram(mobilograms);
    rawMobilogram = binner.toSummedMobilogram(null);
    percentage = 1d;
  }

  @Override
  public double getDomainValue(int index) {
    return rawMobilogram.getMobility(index);
  }

  @Override
  public double getRangeValue(int index) {
    return rawMobilogram.getIntensity(index);
  }

  @Override
  public int getValueCount() {
    return rawMobilogram.getNumberOfValues();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return percentage;
  }
}

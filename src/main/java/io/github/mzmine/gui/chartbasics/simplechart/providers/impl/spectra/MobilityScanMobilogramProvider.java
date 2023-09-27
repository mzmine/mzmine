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
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.IonMobilityUtils.MobilogramType;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilityScanMobilogramProvider implements PlotXYDataProvider {

  private final NumberFormats format = MZmineCore.getConfiguration().getGuiFormats();

  @NotNull
  private final MobilogramType type;
  @NotNull
  private final List<MobilityScan> scans;
  private final boolean normalize;
  private final javafx.scene.paint.Color colorFx;
  private final Color colorAwt;
  private final String key;
  private final Range<Double> mzRange;
  private int processed = 0;
  private int totalFrames = 1;
  private SummedIntensityMobilitySeries data;

  /**
   * Creates a mobilogram from the mobility scans.
   *
   * @param type  BPM or TIM
   * @param scans the mobility scans.
   */
  public MobilityScanMobilogramProvider(@NotNull MobilogramType type, List<MobilityScan> scans,
      boolean normalize) {
    this(type, scans, Range.closed(Double.MIN_VALUE, Double.MAX_VALUE), normalize);
  }

  public MobilityScanMobilogramProvider(@NotNull MobilogramType type, List<MobilityScan> scans,
      @NotNull Range<Double> mzRange, boolean normalize) {
    this.type = type;
    this.scans = scans;
    this.normalize = normalize;
    if (scans.isEmpty()) {
      colorFx = javafx.scene.paint.Color.TRANSPARENT;
      colorAwt = FxColorUtil.fxColorToAWT(colorFx);
    } else {
      colorFx = scans.get(0).getDataFile().getColor();
      colorAwt = scans.get(0).getDataFile().getColorAWT();
    }
    double min = scans.stream().mapToDouble(MobilityScan::getMobility).min().orElse(0);
    double max = scans.stream().mapToDouble(MobilityScan::getMobility).max().orElse(0);
    key = type.shortString() + "M " + format.mobility(min) + " - " + format.mobility(max);

    this.mzRange = mzRange;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return colorAwt;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return colorFx;
  }

  @Override
  public @Nullable String getLabel(int index) {
    return null;
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey() {
    return key;
  }

  @Override
  public @Nullable String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {

    if (scans.isEmpty()) {
      throw new IllegalStateException("No scans to build mobilogram.");
    }

    final Map<Frame, List<MobilityScan>> frameScanMap = scans.stream()
        .collect(Collectors.groupingBy(MobilityScan::getFrame));
    processed = 0;
    totalFrames = frameScanMap.size();

    List<IonMobilitySeries> mobilograms = new ArrayList<>();
    for (List<MobilityScan> s : frameScanMap.values()) {
      final List<MobilityScan> mobilityScans = new ArrayList<>(s);
      mobilityScans.sort(Comparator.comparingInt(MobilityScan::getScanNumber));

      final IonMobilitySeries mobilogram = IonMobilityUtils.buildMobilogramForMzRange(mobilityScans,
          mzRange, type, null);
      mobilograms.add(mobilogram);
      processed++;
    }

    final Frame frame = frameScanMap.keySet().stream().findAny().get();
    final IMSRawDataFile dataFile = (IMSRawDataFile) frame.getDataFile();

    var access = new BinningMobilogramDataAccess(dataFile,
        BinningMobilogramDataAccess.getRecommendedBinWidth(dataFile));
    access.setMobilogram(mobilograms);
    data = access.toSummedMobilogram(null);

    if (normalize) {
      data = IonMobilityUtils.normalizeMobilogram(data, null);
    }
  }

  @Override
  public double getDomainValue(int index) {
    return data.getMobility(index);
  }

  @Override
  public double getRangeValue(int index) {
    return data.getIntensity(index);
  }

  @Override
  public int getValueCount() {
    return data.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return processed / (double) totalFrames;
  }
}

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

package io.github.mzmine.modules.visualization.rawdataoverviewims.threads;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.rawdataoverviewims.IMSRawDataOverviewPane;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.IonMobilityUtils.MobilogramType;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

public class BuildSelectedRanges implements Runnable {

  private final Range<Double> mzRange;
  private final Set<Frame> frames;
  private final IMSRawDataOverviewPane pane;
  private final IMSRawDataFile file;
  private final ScanSelection scanSelection;
  private final Float rtWidth;
  private final BinningMobilogramDataAccess binning;

  public BuildSelectedRanges(@NotNull Range<Double> mzRange, @NotNull Set<Frame> frames,
      @NotNull IMSRawDataFile file, @NotNull ScanSelection scanSelection,
      @NotNull IMSRawDataOverviewPane pane, Float rtWidth,
      final BinningMobilogramDataAccess binning) {
    this.mzRange = mzRange;
    this.frames = frames;
    this.pane = pane;
    this.file = file;
    this.scanSelection = scanSelection;
    this.rtWidth = rtWidth;
    this.binning = binning;
  }

  @Override
  public void run() {
    Frame frame = frames.stream().findAny().orElse(null);
    if(frame == null) {
      return;
    }

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    final String seriesKey =
        "m/z " + mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat
            .format(mzRange.upperEndpoint());
    ColoredXYDataset dataset = null;

    List<IonMobilitySeries> mobilograms = new ArrayList<>();
    for (Frame f : frames) {
      final IonMobilitySeries mobilogram = IonMobilityUtils
          .buildMobilogramForMzRange(f, mzRange, MobilogramType.TIC, null);
      mobilograms.add(mobilogram);
    }

    binning.setMobilogram(mobilograms);
    final SummedIntensityMobilitySeries summed = binning.toSummedMobilogram(null);
    SummedMobilogramXYProvider provider = new SummedMobilogramXYProvider(summed,
        new SimpleObjectProperty<>(FxColorUtil.awtColorToFX(color)), seriesKey, true);
    dataset = new ColoredXYDataset(provider, RunOption.THIS_THREAD);

    ScanSelection scanSel = new ScanSelection(scanSelection.getScanNumberRange(),
        scanSelection.getBaseFilteringInteger(),
        Range.closed(frame.getRetentionTime() - rtWidth / 2,
            frame.getRetentionTime() + rtWidth / 2), scanSelection.getScanMobilityRange(),
        scanSelection.getPolarity(), scanSelection.getSpectrumType(),
        scanSelection.getMsLevel(), scanSelection.getScanDefinition());
    TICDataSet ticDataSet = new TICDataSet(file, scanSel.getMatchingScans(file),
        mzRange, null);
    ticDataSet.setCustomSeriesKey(seriesKey);
    final ColoredXYDataset finalDataset = dataset;
    MZmineCore.runLater(() -> pane.setSelectedRangesToChart(finalDataset, ticDataSet, color));
  }
}

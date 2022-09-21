/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.injection_time;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemScanProvider;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;

/**
 * This dataset filters the injection data for the MS level and plots the lowest intensity against
 * 1/injection time
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
class InjectTimeDataProvider extends SimpleXYProvider implements XYItemScanProvider {

  private final double[] mobilities;
  private final boolean useMobilities;
  private final Scan[] scans;
  private NumberFormat mobilityFormat;

  public InjectTimeDataProvider(int msLevel, Color awt, List<InjectData> data) {
    super("MS" + msLevel, awt, null, null, MZmineCore.getConfiguration().getRTFormat(),
        MZmineCore.getConfiguration().getIntensityFormat());
    List<InjectData> filtered = data.stream().filter(d -> d.msLevel() == msLevel)
        .sorted(Comparator.comparingDouble(InjectData::injectTime).reversed()).toList();
    setxValues(filtered.stream().mapToDouble(v -> 1.0 / v.injectTime()).toArray());
    setyValues(filtered.stream().mapToDouble(InjectData::lowestIntensity).toArray());

    mobilities = filtered.stream().mapToDouble(InjectData::mobility).toArray();
    scans = filtered.stream().map(InjectData::scan).toArray(Scan[]::new);
    useMobilities = mobilities[0] > 0;
  }

  @Override
  public String getLabel(int index) {
    if (useMobilities) {
      mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
      return mobilityFormat.format(mobilities[index]);
    }

    return super.getLabel(index);
  }

  @Override
  public String getToolTipText(int itemIndex) {
    if (useMobilities) {
      mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
      return mobilityFormat.format(mobilities[itemIndex]);
    }

    return super.getToolTipText(itemIndex);
  }

  public Scan getScan(int item) {
    return item >= 0 && item < scans.length ? scans[item] : null;
  }
}

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

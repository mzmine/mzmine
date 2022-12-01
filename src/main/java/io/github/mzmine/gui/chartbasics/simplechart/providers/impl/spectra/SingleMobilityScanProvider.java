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

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Used to plot a single {@link MobilityScan} in a {@link  io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart}.
 *
 * @author https://github.com/SteffenHeu
 */
public class SingleMobilityScanProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final MobilityScan scan;
  private double finishedPercentage;

  public SingleMobilityScanProvider(MobilityScan scan) {
    this.scan = scan;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    finishedPercentage = 0d;
  }

  @Override
  public Color getAWTColor() {
    return scan.getFrame().getDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return scan.getFrame().getDataFile().getColor();
  }

  @Override
  public String getLabel(int index) {
    return mzFormat.format(scan.getMzValue(index));
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return "Frame #" + scan.getFrame().getFrameId() + " Mobility scan #"
        + scan.getMobilityScanNumber();
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "Frame #" + scan.getFrame().getFrameId() + "\nMobility scan #"
        + scan.getMobilityScanNumber() + "\nMobility: " + mobilityFormat.format(scan.getMobility())
        + " " + scan.getMobilityType().getUnit() + "\nm/z: "
        + mzFormat.format(scan.getMzValue(itemIndex)) + "\nIntensity: "
        + intensityFormat.format(scan.getIntensityValue(itemIndex));

  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    finishedPercentage = 1.d;
  }

  @Override
  public double getDomainValue(int index) {
    return scan.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return scan.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return scan.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }
}

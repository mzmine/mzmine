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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Used to plot a 2d intensity vs m/z spectrum in a {@link io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart}.
 *
 * @author https://github.com/SteffenHeu
 */
public class FrameSummedSpectrumProvider implements PlotXYDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;

  private double finishedPercentage;

  public FrameSummedSpectrumProvider(Frame frame) {
    this.frame = frame;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    finishedPercentage = 0d;
  }

  @Override
  public String getLabel(int index) {
    return mzFormat.format((frame.getMzValue(index)));
  }

  @Override
  public Color getAWTColor() {
    return frame.getDataFile().getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return frame.getDataFile().getColor();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return frame.getDataFile().getName() + " - Frame " + frame.getFrameId() + " "
        + rtFormat.format(frame.getRetentionTime()) + " min";
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return "Frame #" + frame.getFrameId() + "RT " + frame.getRetentionTime() + "\nm/z "
        + mzFormat.format(frame.getMzValue(itemIndex)) + "\nIntensity "
        + intensityFormat.format(frame.getIntensityValue(itemIndex));
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
//    dataPoints = ScanUtils.extractDataPoints(frame);
  }

  @Override
  public double getDomainValue(int index) {
    return frame.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return frame.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return frame.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1;
  }
}

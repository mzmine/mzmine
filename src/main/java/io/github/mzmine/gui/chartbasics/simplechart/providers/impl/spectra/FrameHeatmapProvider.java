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
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MathUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;
import smile.math.DoubleArrayList;

/**
 * Used to plot a Frame in a
 * {@link io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot}. Domain axis = m/z,
 * range axis = mobility and z = intensity. Usage of a thresholded  {@link CachedFrame} is
 * encouraged to increase responsiveness of the GUI.
 *
 * @author https://github.com/SteffenHeu
 */
public class FrameHeatmapProvider implements PlotXYZDataProvider {

  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Frame frame;

  private final DoubleArrayList domainValues;
  private final DoubleArrayList rangeValues;
  private final DoubleArrayList zValues;
  private final List<MobilityScan> mobilityScanAtValueIndex;

  protected PaintScale paintScale;
  private double finishedPercentage;

  public FrameHeatmapProvider(Frame frame) {
    this.frame = frame;
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    domainValues = new DoubleArrayList();
    rangeValues = new DoubleArrayList();
    zValues = new DoubleArrayList();
    mobilityScanAtValueIndex = new ArrayList<>();
    finishedPercentage = 0d;
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
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return paintScale;
  }

  @Override
  public Comparable<?> getSeriesKey() {

    return frame.getScanDefinition();
//    return frame.getDataFile().getName() + " - Frame " + frame.getFrameId() + " " + rtFormat.format(
//        frame.getRetentionTime()) + " min";
  }

  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    double numScans = frame.getNumberOfMobilityScans();
    int finishedScans = 0;
    for (MobilityScan mobilityScan : frame.getSortedMobilityScans()) {
      for (int i = 0; i < mobilityScan.getNumberOfDataPoints(); i++) {
        rangeValues.add(mobilityScan.getMobility());
        domainValues.add(mobilityScan.getMzValue(i));
        zValues.add(mobilityScan.getIntensityValue(i));
        mobilityScanAtValueIndex.add(mobilityScan);
      }
      finishedScans++;
      finishedPercentage = finishedScans / numScans;
    }

    final double[] quantiles = MathUtils.calcQuantile(zValues.toArray(), new double[]{0.50, 0.98});
    paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, Range.closed(quantiles[0], quantiles[1]));
  }

  public MobilityScan getMobilityScanAtValueIndex(int index) {
    return mobilityScanAtValueIndex.get(index);
  }

  @Override
  public double getDomainValue(int index) {
    return domainValues.get(index);
  }

  @Override
  public double getRangeValue(int index) {
    return rangeValues.get(index);
  }

  @Override
  public int getValueCount() {
    return domainValues.size();
  }

  @Override
  public double getZValue(int index) {
    return zValues.get(index);
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return null;
  }
}

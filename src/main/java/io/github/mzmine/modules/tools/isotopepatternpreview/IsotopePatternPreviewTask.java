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

package io.github.mzmine.modules.tools.isotopepatternpreview;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.validation.constraints.NotNull;
import org.jfree.data.xy.XYDataset;

public class IsotopePatternPreviewTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IsotopePatternPreviewTask.class.getName());
  private static final double _2SQRT_2LN2 = 2 * Math.sqrt(2 * Math.log(2));
  private final boolean applyFit;
  SimpleIsotopePattern pattern;
  IsotopePatternPreviewDialog dialog;
  private String message;
  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType polarity;
  private String formula;
  private boolean displayResult;

  public IsotopePatternPreviewTask(String formula, double minIntensity, double mergeWidth,
      int charge, PolarityType polarity, boolean applyFit, IsotopePatternPreviewDialog dialog) {
    super(null, Instant.now());
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    this.applyFit = applyFit;
    this.dialog = dialog;
    setStatus(TaskStatus.WAITING);
    pattern = null;
    displayResult = true;
    message = "Calculating isotope pattern " + formula + ".";
  }

  public void initialise(String formula, double minIntensity, double mergeWidth, int charge,
      PolarityType polarity) {
    message = "Wating for parameters";
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    pattern = null;
    displayResult = true;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    assert mergeWidth > 0d;

    message = "Calculating isotope pattern " + formula + ".";
    boolean keepComposition = FormulaUtils.getFormulaSize(formula) < 5E3;
    pattern = (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(formula,
        minIntensity, mergeWidth, charge, polarity, keepComposition);
    if (pattern == null) {
      logger.warning("Isotope pattern could not be calculated.");
      setStatus(TaskStatus.FINISHED);
      return;
    }
    logger.finest("Pattern " + pattern.getDescription() + " calculated.");

    if (displayResult) {
      updateWindow();
    }
    setStatus(TaskStatus.FINISHED);
  }

  public void updateWindow() {
    if (pattern == null) {
      return;
    }

    final XYDataset fit =
        applyFit ? gaussianIsotopePatternFit(pattern, pattern.getBasePeakMz() / mergeWidth) : null;
    //
    Platform.runLater(() -> {
      if (displayResult) {
        dialog.taskFinishedUpdate(this, pattern, fit);
      }
    });
  }

  public synchronized void setDisplayResult(boolean val) {
    this.displayResult = val;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  /**
   * f(x) = a * e^( -(x-b)/(2(c^2)) )
   */
  public XYDataset gaussianIsotopePatternFit(@NotNull final IsotopePattern pattern,
      final double resolution) {

    final Double basePeakMz = pattern.getBasePeakMz();
    if (basePeakMz == null) {
      return null;
    }

    final double fwhm = basePeakMz / resolution;
    final double stepSize = fwhm / 10d;
    final double mzOffset = 1d;

    final Range<Double> mzRange = pattern.getDataPointMZRange();
    final int numPoints = (int) (
        ((mzRange.upperEndpoint() + mzOffset) - (mzRange.lowerEndpoint() - mzOffset)) / stepSize);

    TDoubleArrayList tempMzs = new TDoubleArrayList();

    for (int i = 0; i < numPoints; i++) {
      double mz = mzRange.lowerEndpoint() - mzOffset + i * stepSize;
      for (int j = 0; j < pattern.getNumberOfDataPoints(); j++) {
        // no need to calc otherwise
        if (Math.abs(mz - pattern.getMzValue(j)) < (5 * fwhm)) {
          tempMzs.add(mz);
        }
      }
    }

    final double[] mzs = tempMzs.toArray();
    final double c = fwhm / _2SQRT_2LN2;
    final double[] intensities = new double[mzs.length];
    Arrays.fill(intensities, 0d);

    double highest = 1d;
    for (int i = 0; i < mzs.length; i++) {
      final double gridMz = mzs[i];

      for (int j = 0; j < pattern.getNumberOfDataPoints(); j++) {
        final double isotopeMz = pattern.getMzValue(j);
        final double isotopeIntensity = pattern.getIntensityValue(j);

        final double gauss = getGaussianValue(gridMz, isotopeMz, c);
        intensities[i] += isotopeIntensity * gauss;
      }

      highest = Math.max(highest, intensities[i]);
    }

    final double basePeakIntensity = Objects.requireNonNullElse(pattern.getBasePeakIntensity(), 1d);
    final DataPoint[] dataPoints = new DataPoint[mzs.length];
    for (int i = 0; i < dataPoints.length; i++) {
      intensities[i] = intensities[i] / highest * basePeakIntensity;
      dataPoints[i] = new SimpleDataPoint(mzs[i], intensities[i]);
    }
    return new ColoredXYDataset(
        new AnyXYProvider(MZmineCore.getConfiguration().getDefaultColorPalette().getAWT(1),
            "Gaussian fit; Resolution = " + (int) (resolution), dataPoints.length,
            i -> dataPoints[i].getMZ(), i -> dataPoints[i].getIntensity()));
  }

  /**
   * f(x) = 1 * e^( -(x-b)/(2(c^2)) )
   *
   * @param x the x value.
   * @param b the center of the curve.
   * @param c the sigma of the curve.
   * @return The f(x) value for x.
   */
  public double getGaussianValue(final double x, final double b, final double c) {
    return Math.exp(-Math.pow((x - b), 2) / (2 * (Math.pow(c, 2))));
  }
}

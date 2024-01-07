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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.anyelementdeisotoper;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.IsotopePatternUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 *
 * Currently in development. TODO: - handle undetected, low abundant isotopic peaks - selection to
 * use lowest mass/most abundant peak as reference
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPAnyElementIsotopeGrouperTask extends DataPointProcessingTask {

  private static Logger logger = Logger.getLogger(DPPAnyElementIsotopeGrouperTask.class.getName());

  // peaks counter
  private int processedSteps = 0, totalSteps = 1;

  private NumberFormat format;

  // parameter values
  private MZTolerance mzTolerance;
  private String elements;
  private boolean autoRemove;
  private double mergeWidth;
  private final double minAbundance = 0.01;
  private Range<Double> mzrange;
  private int maxCharge;

  public DPPAnyElementIsotopeGrouperTask(MassSpectrum spectrum, SpectraPlot plot,
      ParameterSet parameterSet, DataPointProcessingController controller,
      TaskStatusListener listener) {
    super(spectrum, plot, parameterSet, controller, listener);

    // Get parameter values for easier use
    mzTolerance =
        parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.mzTolerance).getValue();
    elements = parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.element).getValue();
    autoRemove =
        parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.autoRemove).getValue();
    mzrange = parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.mzRange).getValue();
    maxCharge =
        parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.maximumCharge).getValue();
    setDisplayResults(
        parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.displayResults).getValue());

    Color c = FxColorUtil.fxColorToAWT(
        parameterSet.getParameter(DPPAnyElementIsotopeGrouperParameters.datasetColor).getValue());
    setColor(c);

    format = MZmineCore.getConfiguration().getMZFormat();
  }

  @Override
  public void run() {
    if (!checkParameterSet() || !checkValues()) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    // check formula
    if (elements == null || elements.equals("") || !FormulaUtils.checkMolecularFormula(elements)) {
      setErrorMessage("Invalid element parameter in " + getTaskDescription());
      setStatus(TaskStatus.ERROR);
      logger.warning("Invalid element parameter in " + getTaskDescription());
      return;
    }

    if (!FormulaUtils.checkMolecularFormula(elements)) {
      setStatus(TaskStatus.ERROR);
      logger.warning(
          "Data point/Spectra processing: Invalid element parameter in " + getTaskDescription());
    }

    if (getDataPoints().getNumberOfDataPoints() == 0) {
      logger.info("Data point/Spectra processing: 0 data points were passed to "
          + getTaskDescription() + " Please check the parameters.");
      setStatus(TaskStatus.CANCELED);
      return;
    }

    /*
     * if (!(getDataPoints() instanceof ProcessedDataPoint[])) { logger.warning(
     * "Data point/Spectra processing: The data points passed to Isotope Grouper were not an instance of processed data points."
     * + " Make sure to run mass detection first."); setStatus(TaskStatus.CANCELED); return; }
     */

    setStatus(TaskStatus.PROCESSING);

    SimpleIsotopePattern[] elementPattern = getIsotopePatterns(elements, mergeWidth, minAbundance);

    ProcessedDataPoint[] originalDataPoints = {}; // (ProcessedDataPoint[]) getDataPoints();

    totalSteps = originalDataPoints.length * 2 + 1;

    // one loop for every element
    for (SimpleIsotopePattern pattern : elementPattern) {

      // one loop for every datapoint
      // we want to check all the isotopes for every datapoint before we
      // delete anything. this will
      // take a long time, but should give the most reliable results.
      // we search by ascending mz
      for (int i_dp = 0; i_dp < originalDataPoints.length; i_dp++) {

        // dp is the peak we are currently searching an isotope pattern
        // for
        ProcessedDataPoint dp = originalDataPoints[i_dp];

        if (!mzrange.contains(dp.getMZ()))
          continue;

        IsotopePatternUtils.findIsotopicPeaks(dp, originalDataPoints, mzTolerance, pattern, mzrange,
            maxCharge);

        processedSteps++;
      }

      if (isCanceled())
        return;
    }

    for (int x = 0; x < originalDataPoints.length; x++) {
      ProcessedDataPoint dp = originalDataPoints[x];
      if (!mzrange.contains(dp.getMZ()))
        continue;
      if (isCanceled())
        return;
      IsotopePatternUtils.mergeIsotopicPeakResults(dp);
    }

    for (int x = 0; x < originalDataPoints.length; x++) {
      ProcessedDataPoint dp = originalDataPoints[x];
      if (!mzrange.contains(dp.getMZ()))
        continue;
      if (isCanceled())
        return;
      IsotopePatternUtils.convertIsotopicPeakResultsToPattern(dp, false);
    }

    List<ProcessedDataPoint> results = new ArrayList<>();

    for (ProcessedDataPoint dp : originalDataPoints) {
      if (autoRemove) {
        if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN))
          results.add(dp);
      } else
        results.add(dp);
    }

    setResults(results.toArray(new ProcessedDataPoint[0]));
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * Returns an array of isotope patterns for the given string. Every element gets its own isotope
   * pattern.
   *
   * @param elements String of element symbols
   * @param mergeWidth
   * @param minAbundance
   * @return
   */
  public static SimpleIsotopePattern[] getIsotopePatterns(String elements, double mergeWidth,
      double minAbundance) {
    SilentChemObjectBuilder builder =
        (SilentChemObjectBuilder) SilentChemObjectBuilder.getInstance();
    IMolecularFormula form =
        MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(elements, builder);

    SimpleIsotopePattern[] isotopePatterns = new SimpleIsotopePattern[form.getIsotopeCount()];

    int i = 0;
    // create a isotope pattern for every element
    for (IIsotope element : form.isotopes()) {
      isotopePatterns[i] = (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(
          element.getSymbol(), minAbundance, mergeWidth, 1, PolarityType.NEUTRAL, true);
      i++;
    }
    // also, we want to keep track of the isotope composition, to do that
    // cleanly, we remove the
    // lightest isotope description

    SimpleIsotopePattern[] cleanedPatterns = new SimpleIsotopePattern[form.getIsotopeCount()];

    i = 0;
    for (SimpleIsotopePattern p : isotopePatterns) {
      String[] composition = p.getIsotopeCompositions();
      composition[0] = "";
      cleanedPatterns[i] = new SimpleIsotopePattern(ScanUtils.extractDataPoints(p), p.getCharge(),
          p.getStatus(), p.getDescription(), composition);
      i++;
    }

    return cleanedPatterns;
  }

  private String dataPointsToString(DataPoint[] dp) {
    String str = "";
    for (DataPoint p : dp)
      str += "(m/z = " + format.format(p.getMZ()) + "), ";
    return str;
  }

  @Override
  public double getFinishedPercentage() {
    return processedSteps / totalSteps;
  }

  @Override
  public void displayResults() {
    if (displayResults || getController().isLastTaskRunning()) {
      // getTargetPlot().addDataSet(compressIsotopeDataSets(getResults()),
      // Color.GREEN, false);
      int i = 0;
      for (ProcessedDataPoint result : getResults())
        if (result.resultTypeExists(ResultType.ISOTOPEPATTERN))
          i++;
      if (i == 0)
        i = 1;

      List<Color> clr = generateRainbowColors(i);
      int j = 0;
      for (ProcessedDataPoint result : getResults())
        if (result.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
          getTargetPlot().addDataSet(new IsotopesDataSet(
                  (IsotopePattern) result.getFirstResultByType(ResultType.ISOTOPEPATTERN).getValue()),
              clr.get(j), false, true);
          j++;
        }
      // getTargetPlot().addDataSet(new DPPResultsDataSet("Isotopes (" +
      // getResults().length + ")",
      // getResults()), color, false);
    }
  }

  public static List<Color> generateRainbowColors(int num) {
    List<Color> clr = new ArrayList<>(num);
    if (num == 0) {
      clr.add(getColor(1));
      return clr;
    }

    for (int j = 0; j < num; j++) {
      clr.add(getColor((double) j / (double) num));
    }
    Collections.shuffle(clr);
    return clr;
  }

  public static Color getColor(double power) {
    double H = power; // Hue (note 0.4 = Green, see huge chart below)
    double S = 0.9; // Saturation
    double B = 0.9; // Brightness
    return Color.getHSBColor((float) H, (float) S, (float) B);
  }
}

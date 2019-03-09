/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.deisotoper;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopeCompositionResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.IsotopePatternUtils;

public class DPPIsotopeGrouperTask extends DataPointProcessingTask {

  private static Logger logger = Logger.getLogger(DPPIsotopeGrouperTask.class.getName());

  // peaks counter
  private int processedSteps = 0, totalSteps = 1;

  private static NumberFormat format;

  // parameter values
  private MZTolerance mzTolerance;
  private boolean monotonicShape;
  private int maximumCharge;
  private String elements;
  private boolean autoRemove;
  private double mergeWidth;
  private final double minAbundance = 0.01;
  private Range<Double> mzrange;
  private boolean displayResults;
  private int maxCharge;

  public DPPIsotopeGrouperTask(DataPoint[] dataPoints, SpectraPlot plot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, plot, parameterSet, controller, listener);

    // Get parameter values for easier use
    mzTolerance = parameterSet.getParameter(DPPIsotopeGrouperParameters.mzTolerance).getValue();
    monotonicShape =
        parameterSet.getParameter(DPPIsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge = parameterSet.getParameter(DPPIsotopeGrouperParameters.maximumCharge).getValue();
    elements = parameterSet.getParameter(DPPIsotopeGrouperParameters.element).getValue();
    autoRemove = parameterSet.getParameter(DPPIsotopeGrouperParameters.autoRemove).getValue();
    mzrange = parameterSet.getParameter(DPPIsotopeGrouperParameters.mzRange).getValue();
    maxCharge = parameterSet.getParameter(DPPIsotopeGrouperParameters.maximumCharge).getValue();
    displayResults =
        parameterSet.getParameter(DPPIsotopeGrouperParameters.displayResults).getValue();

    format = MZmineCore.getConfiguration().getMZFormat();
  }


  @Override
  public void run() {
    if (!(getDataPoints() instanceof ProcessedDataPoint[])) {
      logger.warning(
          "The data points passed to Isotope Grouper were not an instance of processed data points."
              + " Make sure to run mass detection first.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // check formula
    if (elements == null || elements.equals("") || !FormulaUtils.checkMolecularFormula(elements)) {
      setErrorMessage("Invalid element parameter in " + this.getClass().getName());
      setStatus(TaskStatus.ERROR);
      logger.warning("Invalid element parameter in " + this.getClass().getName());
      return;
    }

    ExtendedIsotopePattern[] elementPattern =
        getIsotopePatterns(elements, mergeWidth, minAbundance);


    ProcessedDataPoint[] originalDataPoints = (ProcessedDataPoint[]) getDataPoints();

    totalSteps = originalDataPoints.length * 2 + 1;

    // one loop for every element
    for (ExtendedIsotopePattern pattern : elementPattern) {

      // one loop for every datapoint
      // we want to check all the isotopes for every datapoint before we delete anything. this will
      // take a long time, but should give the most reliable results.
      // we search by ascending mz
      for (int i_dp = 0; i_dp < originalDataPoints.length; i_dp++) {

        // dp is the peak we are currently searching an isotope pattern for
        ProcessedDataPoint dp = originalDataPoints[i_dp];

        if (!mzrange.contains(dp.getMZ()))
          continue;

        IsotopePatternUtils.findIsotopePatterns(dp, originalDataPoints, mzTolerance, pattern,
            mzrange, maxCharge);

        processedSteps++;
      }
    }


    // List<ProcessedDataPoint> results =
    IsotopePatternUtils.mergeDetectedPatterns(originalDataPoints, maxCharge);
    List<ProcessedDataPoint> results = new ArrayList<>();
    
    for(ProcessedDataPoint dp : originalDataPoints) {
      if(dp.resultTypeExists(ResultType.ISOTOPEPATTERN))
        results.add(dp);
    }
    
    // now we looped through all dataPoints and link the found isotope patterns together
    // we start from the back so we can just accumulate them by merging the linked the
    // peaks/patterns
    setResults(results.toArray(new ProcessedDataPoint[0]));
    setStatus(TaskStatus.FINISHED);

  }

  public static IsotopePattern checkOverlappingIsotopes(IsotopePattern pattern, IIsotope[] isotopes,
      double mergeWidth, double minAbundance) {
    DataPoint[] dp = pattern.getDataPoints();
    double basemz = dp[0].getMZ();
    List<DataPoint> newPeaks = new ArrayList<DataPoint>();

    double isotopeBaseMass = 0d;
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() > minAbundance) {
        isotopeBaseMass = isotope.getExactMass();
        logger.info("isotopeBaseMass of " + isotope.getSymbol() + " = " + isotopeBaseMass);
        break;
      }
    }


    // loop all new isotopes
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() < minAbundance)
        continue;
      // the difference added by the heavier isotope peak
      double possiblemzdiff = isotope.getExactMass() - isotopeBaseMass;
      if (possiblemzdiff < 0.000001)
        continue;
      boolean add = true;
      for (DataPoint patternDataPoint : dp) {
        // here check for every peak in the pattern, if a new peak would overlap
        // if it overlaps good, we dont need to add a new peak

        int i = 1;
        do {
          if (Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff) <= mergeWidth) {
            // TODO: maybe we should do a average of the masses? i can'T say if it makes sense,
            // since
            // we're just looking for isotope mass differences and dont look at the total
            // composition,
            // so we dont know the intensity ratios
            logger.info("possible overlap found: " + i + " * pattern dp = "
                + patternDataPoint.getMZ() + "\toverlaps with " + isotope.getMassNumber()
                + isotope.getSymbol() + " (" + (isotopeBaseMass - isotope.getExactMass())
                + ")\tdiff: " + Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff));
            add = false;
          }
          i++;
          // logger.info("do");
        } while (patternDataPoint.getMZ() * i <= possiblemzdiff + mergeWidth
            && patternDataPoint.getMZ() != 0.0);
      }

      if (add)
        newPeaks.add(new SimpleDataPoint(possiblemzdiff, 1));
    }

    // now add all new mzs to the isotopePattern
    // DataPoint[] newDataPoints = new SimpleDataPoint[dp.length + newPeaks.size()];
    for (DataPoint p : dp) {
      newPeaks.add(p);
    }
    newPeaks.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return new SimpleIsotopePattern(newPeaks.toArray(new DataPoint[0]),
        IsotopePatternStatus.PREDICTED, "");
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
  public static ExtendedIsotopePattern[] getIsotopePatterns(String elements, double mergeWidth,
      double minAbundance) {
    SilentChemObjectBuilder builder =
        (SilentChemObjectBuilder) SilentChemObjectBuilder.getInstance();
    IMolecularFormula form =
        MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(elements, builder);

    ExtendedIsotopePattern[] isotopePatterns = new ExtendedIsotopePattern[form.getIsotopeCount()];

    int i = 0;
    // create a isotope pattern for every element
    for (IIsotope element : form.isotopes()) {
      isotopePatterns[i] =
          (ExtendedIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(
              element.getSymbol(), minAbundance, mergeWidth, 1, PolarityType.NEUTRAL, true);
      i++;
    }
    // also, we want to keep track of the isotope composition, to do that cleanly, we remove the
    // lightest isotope description

    ExtendedIsotopePattern[] cleanedPatterns = new ExtendedIsotopePattern[form.getIsotopeCount()];

    i = 0;
    for (ExtendedIsotopePattern p : isotopePatterns) {
      String[] composition = p.getIsotopeCompositions();
      composition[0] = "";
      cleanedPatterns[i] = new ExtendedIsotopePattern(p.getDataPoints(), p.getStatus(),
          p.getDescription(), composition);
      i++;
    }

    return cleanedPatterns;
  }

  private static String dataPointsToString(DataPoint[] dp) {
    String str = "";
    for (DataPoint p : dp)
      str += "(m/z = " + format.format(p.getMZ()) + "), ";
    return str;
  }

  @Override
  public String getTaskDescription() {
    return "Isotope grouping for Scan #"
        + getTargetPlot().getMainScanDataSet().getScan().getScanNumber();
  }

  @Override
  public double getFinishedPercentage() {
    return processedSteps / totalSteps;
  }


  @Override
  public void displayResults() {
    if (displayResults || getController().isLastTaskRunning()) {
      // getTargetPlot().addDataSet(compressIsotopeDataSets(getResults()), Color.GREEN, false);
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
              clr.get(j), false);
          j++;
        }
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

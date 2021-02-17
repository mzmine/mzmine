/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration;

import java.text.NumberFormat;
import java.util.TreeMap;
import com.google.common.collect.Range;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.combonested.NestedComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;

public class MassCalibrationParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public enum MassPeakMatchingChoice {
    STANDARDS_LIST("Standard Calibrant Library (SCL)"), //
    UNIVERSAL_CALIBRANTS("Universal Calibrant Library (UCL)");

    private final String name;

    MassPeakMatchingChoice(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final FileNameParameter standardsList =
      new FileNameParameter("Standard Calibrant Library file",
          "File with a list of standard calibrants (ionic formula and retention time)"
              + " expected to appear in the dataset",
          FileSelectionType.OPEN, false);

  public static final MZToleranceParameter mzToleranceSCL =
      new MZToleranceParameter("m/z tolerance",
          "Max difference between actual mz peaks and standard calibrants to consider a match,"
              + " max of m/z and ppm is used",
          0.001, 5, true);

  public static final RTToleranceParameter retentionTimeTolerance = new RTToleranceParameter(
      "Retention time tolerance",
      "Max retention time difference between mass peaks and standard calibrants to consider a match.");

  public static final MZToleranceParameter mzToleranceUCL =
      new MZToleranceParameter("m/z tolerance ",
          "Max difference between actual mz peaks and universal calibrants to consider a match,"
              + " max of m/z and ppm is used",
          0.001, 5, true);

  public static final TreeMap<String, String> ionizationModeChoices = new TreeMap<>() {
    {
      put("Universal calibrants from Keller et al., Anal Chim Acta 2008, positive mode",
          "universal_calibrants_1_positive_mode.csv");
      put("Universal calibrants from Keller et al., Anal Chim Acta 2008, negative mode",
          "universal_calibrants_1_negative_mode.csv");
      put("Universal calibrants from Hawkes et al., Limnol Oceanogr Methods 2020, positive mode",
          "universal_calibrants_2_positive_mode.csv");
      put("Universal calibrants from Hawkes et al., Limnol Oceanogr Methods 2020, negative mode",
          "universal_calibrants_2_negative_mode.csv");
      put("Positive mode merged", "universal_calibrants_merged_positive_mode.csv");
      put("Negative mode merged", "universal_calibrants_merged_negative_mode.csv");
    }
  };

  public static final ComboParameter<String> ionizationMode =
      new ComboParameter<String>("Ionization mode",
          "Ionization mode for which to use an appropriate universal calibrants list",
          ionizationModeChoices.keySet().toArray(new String[0]));

  public static final TreeMap<String, ParameterSet> massPeakMatchingChoices = new TreeMap<>() {
    {
      put(MassPeakMatchingChoice.STANDARDS_LIST.toString(), new SimpleParameterSet(
          new Parameter[] {standardsList, mzToleranceSCL, retentionTimeTolerance}));
      put(MassPeakMatchingChoice.UNIVERSAL_CALIBRANTS.toString(),
          new SimpleParameterSet(new Parameter[] {ionizationMode, mzToleranceUCL}));
    }
  };

  public static final NestedComboParameter referenceLibrary =
      new NestedComboParameter("Reference library of ions",
          "Method used to match mass peaks from the dataset with reference values",
          massPeakMatchingChoices, MassPeakMatchingChoice.STANDARDS_LIST.toString(), true, 250);

  public static final DoubleParameter intensityThreshold = new DoubleParameter(
      "Intensity threshold",
      "Intensity threshold of m/z peaks to use for matching. This parameter is used to facilitate"
          + " noise filtering. Only mass peaks with intensity equal or above the threshold will be used for"
          + " matching, use 0 to allow all.",
      NumberFormat.getNumberInstance(), 0.0, 0.0, Double.POSITIVE_INFINITY);

  public static final BooleanParameter duplicateErrorFilter =
      new BooleanParameter("Duplicate error filter",
          "If checked, the distribution of errors will be filtered to remove duplicates");

  public enum RangeExtractionChoice {
    RANGE_METHOD("High-density range of errors"), //
    PERCENTILE_RANGE("Percentile range of errors");

    private final String name;

    RangeExtractionChoice(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final DoubleParameter errorRangeSize = new DoubleParameter(
      "Primary high-density range of errors size",
      "The maximum length of the range that contains the most errors. The module searches for a range"
          + " of error values that is up to this size and contains the most errors, this way a high-density error"
          + " range can be established. The range size is the difference between upper and lower endpoint"
          + " of the range, both are values of PPM errors of m/z ratio. See help for more details.",
      NumberFormat.getNumberInstance(), 2.0, 0.0, Double.POSITIVE_INFINITY);

  public static final DoubleParameter errorRangeTolerance = new DoubleParameter(
      "Error range tolerance",
      "Error range tolerance is the max distance allowed between errors to be included in the same range."
          + " This is used when extending the most populated error range, if next closest error is within that"
          + " tolerance, the range is extended to contain it. The process is repeated until no new errors can be"
          + " included in that range. The tolerance is the absolute difference between PPM errors of m/z ratio."
          + " See help for more details.",
      NumberFormat.getNumberInstance(), 0.4, 0.0, Double.POSITIVE_INFINITY);

  public static final DoubleRangeParameter percentileRange =
      new DoubleRangeParameter("Percentile range",
          "The percentile range used for extraction of errors.", NumberFormat.getNumberInstance(),
          true, false, Range.closed(25.0, 75.0), Range.closed(0.0, 100.0));

  public static final TreeMap<String, ParameterSet> rangeExtractionChoices = new TreeMap<>() {
    {
      put(RangeExtractionChoice.RANGE_METHOD.toString(),
          new SimpleParameterSet(new Parameter[] {errorRangeSize, errorRangeTolerance}));
      put(RangeExtractionChoice.PERCENTILE_RANGE.toString(),
          new SimpleParameterSet(new Parameter[] {percentileRange}));
    }
  };

  public static final NestedComboParameter rangeExtractionMethod =
      new NestedComboParameter("Overall mass bias estimation",
          "Method used to extract range of errors considered substantial to the bias estimation of"
              + " mass peaks m/z measurement",
          rangeExtractionChoices, RangeExtractionChoice.RANGE_METHOD.toString(), true, 250);

  public enum BiasEstimationChoice {
    ARITHMETIC_MEAN("Arithmetic mean"), //
    KNN_REGRESSION("KNN regression"), //
    OLS_REGRESSION("OLS regression");

    private final String name;

    BiasEstimationChoice(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final DoubleParameter nearestNeighborsPercentage = new DoubleParameter(
      "Nearest neighbors percentage",
      "Simple KNN regression involves finding K closest neighbors for a given input"
          + " and then using these data points to predict the output. It is used to find PPM errors"
          + " corresponding to m/z peaks in the neighbour of the mass peak that is going to be"
          + " shifted/calibrated. The arithmetic mean of the PPM errors from the distribution that are"
          + " neighbors is used to estimate PPM error of the shifted mass peak. This parameter sets the"
          + " percentage of m/z errors in the distribution that are used as neighbors.",
      NumberFormat.getNumberInstance(), 10.0, 0.0, 100.0);

  public static final IntegerParameter polynomialDegree =
      new IntegerParameter("Polynomial degree",
          "The degree of the polynomial feature used in OLS regression. Use 0 just for the constant"
              + " component, 1 for linear, 2 for quadratic and so on.",
          1, true, 0, Integer.MAX_VALUE);

  public static final BooleanParameter exponentialFeature =
      new BooleanParameter("Exponential feature",
          "Check this to include exponential feature exp(x) in the OLS regression.", false);

  public static final BooleanParameter logarithmicFeature =
      new BooleanParameter("Logarithmic feature",
          "Check this to include logarithmic feature ln(x) in the OLS regression.", false);

  public static final TreeMap<String, ParameterSet> biasEstimationChoices = new TreeMap<>() {
    {
      put(BiasEstimationChoice.ARITHMETIC_MEAN.toString(),
          new SimpleParameterSet(new Parameter[] {}));
      put(BiasEstimationChoice.KNN_REGRESSION.toString(),
          new SimpleParameterSet(new Parameter[] {nearestNeighborsPercentage}));
      put(BiasEstimationChoice.OLS_REGRESSION.toString(), new SimpleParameterSet(
          new Parameter[] {polynomialDegree, exponentialFeature, logarithmicFeature}));
    }
  };

  public static final NestedComboParameter biasEstimationMethod = new NestedComboParameter(
      "Mass calibration method",
      "To estimate mass measurement bias more accurately, we can model the trend exhibited by the"
          + " error size vs m/z value relation obtained by matching the mass peaks. With the estimation model"
          + " we can shift/calibrate the mass peaks at different particular m/z values more accurately."
          + " Select the bias estimation method, a global arithmetic mean estimate, or a regression method"
          + " for error size vs m/z value. Please see the help file for more details.",
      biasEstimationChoices, BiasEstimationChoice.ARITHMETIC_MEAN.toString(), true, 250);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to mass list name as a suffix", "calibrated");

  public MassCalibrationParameters() {
    super(new Parameter[] {dataFiles, intensityThreshold, duplicateErrorFilter,
        referenceLibrary, rangeExtractionMethod, biasEstimationMethod, suffix});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    MassCalibrationSetupDialog dialog = new MassCalibrationSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}

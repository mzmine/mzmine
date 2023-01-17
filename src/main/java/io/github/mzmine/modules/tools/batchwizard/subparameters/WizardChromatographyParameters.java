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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ChromatographyDefaults;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.*;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

import java.text.NumberFormat;

public class WizardChromatographyParameters extends SimpleParameterSet {
  /**************************************************ADAP CHROMATOGRAMBUILDER***************************************************************/
  public static final DoubleParameter minHighestPoint = new DoubleParameter("Min highest intensity",
          "Points below this intensity will not be considered in starting a new chromatogram",
          MZmineCore.getConfiguration().getIntensityFormat());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
          "Scan to scan accuracy (m/z)", """
      m/z tolerance of the same compound between two scans.
      This does not describe the deviation of the accurate mass (measured) from the exact mass (calculated),
      but the fluctuation of the accurate between two scans.""", 0.002, 10);

  public static final IntegerParameter minimumScanSpan = new IntegerParameter(
          "Min group size in # of scans", """
      Minimum scan span over which some feature in the chromatogram must have (continuous) points above the noise level
      to be recognized as a chromatogram.
      The optimal value depends on the chromatography system setup. The best way to set this parameter
      is by studying the raw data and determining what is the typical time span of chromatographic features.""",
          5, true, 1, null);

  /**************************************************ADAP RESOLVER***************************************************************/
  public static final DoubleParameter SN_THRESHOLD = new DoubleParameter("S/N threshold",
          "Signal to noise ratio threshold", NumberFormat.getNumberInstance(), 10.0, 0.0, null);

  public static final DoubleRangeParameter RT_FOR_CWT_SCALES_DURATION = new DoubleRangeParameter(
          "RT wavelet range",
          "Upper and lower bounds of retention times to be used for setting the wavelet scales. Choose a range that that simmilar to the range of peak widths expected to be found from the data.",
          MZmineCore.getConfiguration().getRTFormat(), true, true, Range.closed(0.001, 0.1));

  /**************************************************MULTICURVE RESOLUTION***************************************************************/
  public static final DoubleParameter PREF_WINDOW_WIDTH = new DoubleParameter(
          "Deconvolution window width (min)", "Preferred width of deconvolution windows (in minutes).",
          NumberFormat.getNumberInstance(), 0.2);

  public static final DoubleParameter RET_TIME_TOLERANCE = new DoubleParameter(
          "Retention time tolerance (min)",
          "Retention time tolerance value (between 0 and 1) is used for determine the number of components"
                  + " in a window. The larger tolerance, the smaller components are determined.",
          NumberFormat.getNumberInstance(), 0.05, 0.0, Double.MAX_VALUE);

  public static final IntegerParameter MIN_CLUSTER_SIZE = new IntegerParameter(
          "Minimum Number of Peaks", "Minimum number of peaks that can form a component", 1);

  /**************************************************ADAP ALIGNER***************************************************************/
  public static final DoubleParameter SAMPLE_COUNT_RATIO = new DoubleParameter(
          "Min confidence (between 0 and 1)",
          "A fraction of the total number of samples. An aligned feature must be detected at "
                  + "least in several samples.\nThis parameter determines the minimum number of samples where a "
                  + "feature must be detected.",
          NumberFormat.getInstance(), 0.7, 0.0, 1.0);

  public static final RTToleranceParameter RET_TIME_RANGE = new RTToleranceParameter();

  public static final MZToleranceParameter MZ_RANGE = new MZToleranceParameter();

  public static final DoubleParameter SCORE_TOLERANCE = new DoubleParameter(
          "Score threshold (between 0 and 1)",
          "The minimum value of the similarity function required for features to be aligned together.",
          NumberFormat.getInstance(), 0.75, 0.0, 1.0);

  //others
  public static final RTToleranceParameter approximateChromatographicFWHM = new RTToleranceParameter(
      "Approximate feature FWHM",
      "The approximate feature width (chromatograpic peak width) in retention time (full-width-at-half-maximum, FWHM). ");

  public static final RTToleranceParameter intraSampleRTTolerance = new RTToleranceParameter(
      "Intra-sample RT tolerance",
      "Retention time tolerance for multiple signals of the same compound in the same "
          + "sample.\nUsed to detect isotopes or multimers/adducts of the same compound.");

  public static final RTToleranceParameter interSampleRTTolerance = new RTToleranceParameter(
      "Inter-sample RT tolerance",
      "Retention time tolerance for the same compound in different samples.\n"
          + "Used to align multiple measurements of the same sample or a batch run.");

  public static final IntegerParameter minNumberOfDataPoints = new IntegerParameter(
      "Min # of data points",
      "Minimum number of data points as used in chromatogram building and feature resolving.", 4, 1,
      Integer.MAX_VALUE);

  public static final RTRangeParameter cropRtRange = new RTRangeParameter("Crop retention time",
      "Crops the RT range of chromatograms. Used to exclude time before the flow time\n"
          + "and after the separation, where in many runs cleaning and re-equilibration starts.",
      true, Range.closed(0.5, 30d));

  public static final IntegerParameter maximumIsomersInChromatogram = new IntegerParameter(
      "Max peaks in chromatogram",
      "An estimate maximum number of peaks in a chromatogram (number of same m/z features).\n"
          + "Used to estimate the chromatographic threshold to filter noisy chromatograms.", 10, 1,
      Integer.MAX_VALUE);

  public static final BooleanParameter stableIonizationAcrossSamples = new BooleanParameter(
      "Stable ionization across samples", """
      Only check if the ionization conditions are stable across all samples.
      Uncheck for varying salt content or variations in ionization conditions.
      Used in feature grouping.""", true);

  public static final BooleanParameter smoothing = new BooleanParameter("Smoothing",
      "Apply smoothing in the retention time dimension, usually only needed if the peak shapes are spiky.",
      true);

  /**
   * Keep track of the workflow used: GC or LC
   */
  public static final HiddenParameter<ChromatographyWorkflow> workflow = new HiddenParameter<>(
      new ComboParameter<>("Workflow", "defines the workflow used by the batch wizard",
          ChromatographyWorkflow.values(), ChromatographyWorkflow.LC));

  /**
   * the UI element shown on top to signal the workflow used. Presets May be changed and then saved
   * to user presets as parameter files.
   */
  public static final HiddenParameter<ChromatographyDefaults> wizardPart = new HiddenParameter<>(
      new ComboParameter<>("Wizard part", "Defines the wizard part used",
          ChromatographyDefaults.values(), ChromatographyDefaults.UHPLC));

  /**
   * the part category of presets - is used in all wizard parameter classes
   */
  public static final WizardPartParameter wizardPartCategory = new WizardPartParameter(
      WizardPart.CHROMATOGRAPHY);

  public WizardChromatographyParameters() {
    super(new Parameter[]{
        // hidden
        workflow, wizardPart, wizardPartCategory,
        // actual parameters
            smoothing, stableIonizationAcrossSamples, cropRtRange, maximumIsomersInChromatogram,
        minNumberOfDataPoints, approximateChromatographicFWHM, intraSampleRTTolerance,
        interSampleRTTolerance,minHighestPoint, mzTolerance, minimumScanSpan, SN_THRESHOLD, RT_FOR_CWT_SCALES_DURATION, PREF_WINDOW_WIDTH,
            RET_TIME_TOLERANCE, MIN_CLUSTER_SIZE, SAMPLE_COUNT_RATIO, RET_TIME_RANGE, MZ_RANGE, SCORE_TOLERANCE});
  }

  public WizardChromatographyParameters(final ChromatographyWorkflow workflowPreset,
      final boolean stableIonization, final int maxIsomersInSample, final int minDataPoints,
      final Range<Double> cropRt, final RTTolerance fwhm, final RTTolerance intraSampleTolerance,
      final RTTolerance interSampleTolerance) {
    this();
    setParameter(workflow, workflowPreset);
    setParameter(stableIonizationAcrossSamples, stableIonization);
    setParameter(maximumIsomersInChromatogram, maxIsomersInSample);
    setParameter(minNumberOfDataPoints, minDataPoints);
    // defaults - others override those values
    setParameter(cropRtRange, cropRt);
    setParameter(approximateChromatographicFWHM, fwhm);
    setParameter(intraSampleRTTolerance, intraSampleTolerance);
    setParameter(interSampleRTTolerance, interSampleTolerance);
  }

  public WizardChromatographyParameters(final ChromatographyWorkflow workflowPreset,
                                         final Double minHighest, final MZTolerance mzToleranceValue,
                                         final Integer minScanSpan, final Double snThreshold,
                                         final Range<Double> rtforCWT, final Double windowWidth,
                                         final Double rtTolderance, final Integer minClusterSize,
                                         final Double sampleCountRatio, final RTTolerance rtRange,
                                         final MZTolerance mzRange, final Double scoreTolerance
                                         ) {
    this();

    setParameter(workflow, workflowPreset);

    //adap chromatogram builder
    setParameter(minHighestPoint, minHighest);
    setParameter(mzTolerance, mzToleranceValue);
    setParameter(minimumScanSpan, minScanSpan);

    //adap resolver
    setParameter(SN_THRESHOLD, snThreshold);
    setParameter(RT_FOR_CWT_SCALES_DURATION, rtforCWT);

    //multicurve
    setParameter(PREF_WINDOW_WIDTH, windowWidth);
    setParameter(RET_TIME_TOLERANCE, rtTolderance);
    setParameter(MIN_CLUSTER_SIZE, minClusterSize);

    //adap aligner
    setParameter(SAMPLE_COUNT_RATIO, sampleCountRatio);
    setParameter(RET_TIME_RANGE, rtRange);
    setParameter(MZ_RANGE, mzRange);
    setParameter(SCORE_TOLERANCE, scoreTolerance);


  }

  /**
   * Create parameters from defaults
   *
   * @param defaults defines default values
   */
  public static WizardChromatographyParameters create(final ChromatographyDefaults defaults) {
    // override defaults
    WizardChromatographyParameters params = switch (defaults) {
      case HPLC -> new WizardChromatographyParameters(ChromatographyWorkflow.LC, true, 15, 4,
          Range.closed(0.5, 60d), new RTTolerance(0.1f, Unit.MINUTES),
          new RTTolerance(0.08f, Unit.MINUTES), new RTTolerance(0.4f, Unit.MINUTES));
      case UHPLC -> new WizardChromatographyParameters(ChromatographyWorkflow.LC, true, 15, 4,
          Range.closed(0.3, 30d), new RTTolerance(0.05f, Unit.MINUTES),
          new RTTolerance(0.04f, Unit.MINUTES), new RTTolerance(0.1f, Unit.MINUTES));
      case HILIC -> new WizardChromatographyParameters(ChromatographyWorkflow.LC, true, 15, 5,
          Range.closed(0.3, 30d), new RTTolerance(0.05f, Unit.MINUTES),
          new RTTolerance(3, Unit.SECONDS), new RTTolerance(3, Unit.SECONDS));
      case GC_CI -> new WizardChromatographyParameters(ChromatographyWorkflow.LC, true, 30, 6,
          Range.closed(0.3, 30d), new RTTolerance(0.05f, Unit.MINUTES),
          new RTTolerance(0.04f, Unit.MINUTES), new RTTolerance(0.1f, Unit.MINUTES));
      // different workflow for GC-EI

      //TODO: set default values for params
      case GC_EI -> new WizardChromatographyParameters(ChromatographyWorkflow.GC,
              1000.0, new MZTolerance(0.05, 10),
              3, 5.0, Range.closed(0.001, 0.06),
              0.2, 0.02,1, 0.5,
              new RTTolerance(0.1f, Unit.MINUTES),
              new MZTolerance(0.05, 10),0.5);
//      case GC_EI -> new WizardChromatographyParameters(ChromatographyWorkflow.GC);
    };
    params.setParameter(wizardPart, ChromatographyDefaults.UHPLC);
    params.setParameter(wizardPartCategory, WizardPart.CHROMATOGRAPHY);
    return params;
  }

  public enum ChromatographyWorkflow {
    LC, GC
  }
}

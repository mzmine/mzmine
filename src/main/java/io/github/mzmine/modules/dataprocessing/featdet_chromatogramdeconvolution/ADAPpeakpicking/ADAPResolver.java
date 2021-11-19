/*
 * Copyright 2006-2015 The du-lab Development Team
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
/*
 * author Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import static dulab.adap.workflow.Deconvolution.DeconvoluteSignal;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters.COEF_AREA_THRESHOLD;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters.MIN_FEAT_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters.PEAK_DURATION;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters.RT_FOR_CWT_SCALES_DURATION;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters.SN_ESTIMATORS;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters.SN_THRESHOLD;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.WaveletCoefficientsSNParameters.ABS_WAV_COEFFS;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.WaveletCoefficientsSNParameters.HALF_WAVELET_WINDOW;

import com.google.common.collect.Range;
import dulab.adap.datamodel.PeakInfo;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Use XCMS findPeaks.centWave to identify peaks.
 */
public class ADAPResolver extends AbstractResolver {

  private final ParameterSet parameters;
  double[] xBuffer;
  double[] yBuffer;

  protected ADAPResolver(@NotNull ParameterSet parameters, @NotNull ModularFeatureList flist) {
    super(parameters, flist);
    this.parameters = parameters;
  }

  @Override
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return AdapResolverModule.class;
  }

  // Logger.
  private static final Logger logger = Logger.getLogger(ADAPResolver.class.getName());

  // Name.
  private static final String NAME = "Wavelets (ADAP)";

  // Minutes <-> seconds.
  private static final double SECONDS_PER_MINUTE = 60.0;

  @Override
  @NotNull
  public List<Range<Double>> resolve(@NotNull final double[] retentionTimes,
      @NotNull final double[] intensities) {

    assert retentionTimes.length == intensities.length;
    final int scanCount = retentionTimes.length;

    final Range<Double> peakDuration = parameters.getParameter(PEAK_DURATION).getValue();

    final MZmineProcessingStep<SNEstimatorChoice> signalNoiseEstimator = parameters
        .getParameter(SN_ESTIMATORS).getValue();
    String SNCode = signalNoiseEstimator.getModule().getSNCode();

    double signalNoiseWindowMult = -1.0;
    boolean absWavCoeffs = false;

    Map<String, Object> informationSN = new HashMap<String, Object>();
    if (SNCode == "Wavelet Coefficient Estimator") {
      informationSN.put("code", "Wavelet Coefficient Estimator");
      signalNoiseWindowMult = signalNoiseEstimator.getParameterSet()
          .getParameter(HALF_WAVELET_WINDOW).getValue();
      absWavCoeffs = signalNoiseEstimator.getParameterSet().getParameter(ABS_WAV_COEFFS).getValue();
      informationSN.put("multiplier", signalNoiseWindowMult);
      informationSN.put("absolutewavecoeffs", absWavCoeffs);
    }

    if (SNCode == "Intensity Window Estimator") {
      informationSN.put("code", "Intensity Window Estimator");
    }

    // get the average rt spacing
    double rtSum = 0.0;
    for (int i = 0; i < retentionTimes.length - 1; i++) {
      rtSum += retentionTimes[i + 1] - retentionTimes[i];
    }
    double avgRTInterval = rtSum / (retentionTimes.length - 1);

    // Change the lower and upper bounds for the wavelet scales from retention times to number of
    // scans.
    Range<Double> rtRangeForCWTScales = parameters.getParameter(RT_FOR_CWT_SCALES_DURATION)
        .getValue();
    final double rtLow = rtRangeForCWTScales.lowerEndpoint();
    final double rtHigh = rtRangeForCWTScales.upperEndpoint();
    final int numScansRTLow = Math.max((int) Math.round(rtLow / avgRTInterval), 1);
    final int numScansRTHigh = Math
        .min((int) Math.round(rtHigh / avgRTInterval), retentionTimes.length);

    final List<PeakInfo> ADAPPeaks = DeconvoluteSignal(retentionTimes, intensities, 0d,
        parameters.getParameter(SN_THRESHOLD).getValue(),
        parameters.getParameter(MIN_FEAT_HEIGHT).getValue(), peakDuration,
        parameters.getParameter(COEF_AREA_THRESHOLD).getValue(), numScansRTLow, numScansRTHigh,
        informationSN);

    if (ADAPPeaks == null) {
      return Collections.emptyList();
    }

    // The old way could detect the same peak more than once if the wavlet scales were too large.
    // If the left bounds were the same and there was a null point before the right bounds it
    // would make the same peak twice. To avoid the above see if the peak duration range is met
    // before going into the loop

    final List<Range<Double>> ranges = new ArrayList<>();
    for (int i = 0; i < ADAPPeaks.size(); i++) {
      final PeakInfo curPeak = ADAPPeaks.get(i);

      ranges.add(Range.closed(curPeak.retTimeStart, curPeak.retTimeEnd));
    }
    return ranges;
  }
}

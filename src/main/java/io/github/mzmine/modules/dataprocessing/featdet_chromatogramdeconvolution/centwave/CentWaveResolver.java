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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave.CentWaveResolverParameters.INTEGRATION_METHOD;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave.CentWaveResolverParameters.PEAK_DURATION;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave.CentWaveResolverParameters.PEAK_SCALES;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave.CentWaveResolverParameters.SN_THRESHOLD;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave.CentWaveResolverParameters.PeakIntegrationMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Use XCMS findPeaks.centWave to identify peaks.
 */
public class CentWaveResolver implements FeatureResolver {

  // Logger.
  private static final Logger logger = Logger.getLogger(CentWaveResolver.class.getName());

  // Name.
  private static final String NAME = "Wavelets (XCMS)";

  // Minutes <-> seconds.
  private static final double SECONDS_PER_MINUTE = 60.0;

  // Required minimum version of XCMS.
  private static final String XCMS_VERSION = "1.33.2";

  @NotNull
  @Override
  public String getName() {

    return NAME;
  }

  @NotNull
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {

    return CentWaveResolverParameters.class;
  }

  @Override
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return CentWaveResolverModule.class;
  }

  @Override
  public boolean getRequiresR() {
    return true;
  }

  @Override
  public String[] getRequiredRPackages() {
    return new String[] {"xcms"};
  }

  @Override
  public String[] getRequiredRPackagesVersions() {
    return new String[] {XCMS_VERSION};
  }

  @Override
  public REngineType getREngineType(final ParameterSet parameters) {
    return parameters.getParameter(CentWaveResolverParameters.RENGINE_TYPE).getValue();
  }

  @Override
  public ResolvedPeak[] resolvePeaks(final Feature chromatogram, final ParameterSet parameters,
      RSessionWrapper rSession, CenterFunction mzCenterFunction, double msmsRange,
      float rTRangeMSMS) throws RSessionWrapperException {

    List<Scan> scanNumbers = chromatogram.getScanNumbers();
    final int scanCount = scanNumbers.size();
    double retentionTimes[] = new double[scanCount];
    double intensities[] = new double[scanCount];
    RawDataFile dataFile = chromatogram.getRawDataFile();
    for (int i = 0; i < scanCount; i++) {
      final Scan scanNum = scanNumbers.get(i);
      retentionTimes[i] = scanNum.getRetentionTime();
      DataPoint dp = chromatogram.getDataPointAtIndex(i);
      if (dp != null)
        intensities[i] = dp.getIntensity();
      else
        intensities[i] = 0.0;
    }

    // Call findPeaks.centWave.
    double[][] peakMatrix = null;

    peakMatrix = centWave(rSession, retentionTimes, intensities, chromatogram.getMZ(),
        parameters.getParameter(SN_THRESHOLD).getValue(),
        parameters.getParameter(PEAK_SCALES).getValue(),
        parameters.getParameter(INTEGRATION_METHOD).getValue());

    final List<ResolvedPeak> resolvedPeaks;
    if (peakMatrix == null) {

      resolvedPeaks = new ArrayList<ResolvedPeak>(0);

    } else {

      logger.finest("Processing peak matrix...");

      final Range<Double> peakDuration = parameters.getParameter(PEAK_DURATION).getValue();

      // Process peak matrix.
      resolvedPeaks = new ArrayList<ResolvedPeak>(peakMatrix.length);

      for (final double[] peakRow : peakMatrix) {

        // Get peak start and end.
        final int peakLeft = findRTIndex(retentionTimes, peakRow[4]);
        final int peakRight = findRTIndex(retentionTimes, peakRow[5]);

        // Partition into sections bounded by null data points, creating
        // a peak for each.
        for (int start = peakLeft; start < peakRight; start++) {

          if (chromatogram.getDataPointAtIndex(start) != null) {

            int end = start;

            while (end < peakRight && chromatogram.getDataPointAtIndex(end + 1) != null) {

              end++;
            }

            if ((end > start)
                && (peakDuration.contains(retentionTimes[end] - retentionTimes[start]))) {

              resolvedPeaks.add(new ResolvedPeak(chromatogram, start, end, mzCenterFunction,
                  msmsRange, rTRangeMSMS));
            }

            start = end;
          }
        }
      }
    }

    return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
  }

  private static int findRTIndex(final double[] rtMinutes, final double rtSec) {

    final int i = Arrays.binarySearch(rtMinutes, rtSec / SECONDS_PER_MINUTE);
    return i >= 0 ? i : -i - 2;
  }

  /**
   * Do peak picking using xcms::findPeaks.centWave.
   * 
   * @param scanTime retention times (for each scan).
   * @param intensity intensity values (for each scan).
   * @param mz fixed m/z value for EIC.
   * @param snrThreshold signal:noise ratio threshold.
   * @param peakWidth peak width range.
   * @param integrationMethod integration method.
   * @return a matrix with a row for each detected peak.
   * @throws RSessionWrapperException
   */
  private static double[][] centWave(RSessionWrapper rSession, final double[] scanTime,
      final double[] intensity, final double mz, final double snrThreshold,
      final Range<Double> peakWidth, final PeakIntegrationMethod integrationMethod)
      throws RSessionWrapperException {

    logger.finest("Detecting peaks.");

    final double[][] peaks;

    // Set vectors.
    rSession.assign("scantime", scanTime);
    rSession.assign("intensity", intensity);

    // Initialize.
    rSession.eval("mz <- " + mz);
    rSession.eval("numPoints <- length(intensity)");

    // Construct xcmsRaw object
    rSession.eval("xRaw <- new(\"xcmsRaw\")");
    rSession.eval("xRaw@tic <- intensity");
    rSession.eval("xRaw@scantime <- scantime * " + SECONDS_PER_MINUTE);
    rSession.eval("xRaw@scanindex <- 0:(numPoints-1)");
    rSession.eval("xRaw@env$mz <- rep(mz, numPoints)");
    rSession.eval("xRaw@env$intensity <- intensity");

    // Construct ROIs.
    rSession.eval("ROIs <- list()");
    int roi = 1;
    for (int start = 0; start < intensity.length; start++) {

      // Found non-zero section.
      if (intensity[start] > 0.0) {

        // Look for end.
        int end = start + 1;
        while (end < intensity.length && intensity[end] > 0.0) {

          end++;
        }

        // Add ROI to list.
        rSession.eval("ROIs[[" + roi + "]] <- list('scmin'=" + (start + 1) + ", 'scmax'=" + end
            + ", 'mzmin'=mz, 'mzmax'=mz)");

        // Next ROI.
        start = end;
        roi++;

      }
    }

    // Do peak picking.
    rSession.eval("mtx <- findPeaks.centWave(xRaw, ppm=0, mzdiff=0, verbose=TRUE" + ", peakwidth=c("
        + peakWidth.lowerEndpoint() * SECONDS_PER_MINUTE + ", "
        + peakWidth.upperEndpoint() * SECONDS_PER_MINUTE + ')' + ", snthresh=" + snrThreshold
        + ", integrate=" + integrationMethod.getIndex() + ", ROI.list=ROIs)");

    // Get rid of 'NA' values potentially found in the resulting matrix
    rSession.eval("mtx[is.na(mtx)] <- " + RSessionWrapper.NA_DOUBLE); // +
                                                                      // "0");//

    final Object centWave = roi <= 1 ? null : (double[][]) rSession.collect("mtx", false);

    // Done: Refresh R code stack
    rSession.clearCode();

    peaks = (centWave == null) ? null : (double[][]) centWave;

    return peaks;
  }

}

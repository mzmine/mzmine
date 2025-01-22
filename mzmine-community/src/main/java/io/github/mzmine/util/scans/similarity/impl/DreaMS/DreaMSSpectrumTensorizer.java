/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.scans.similarity.impl.DreaMS;


import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Bins spectra and returns two tensors, one for metadata and one for fragments
 */
public class DreaMSSpectrumTensorizer {

  private static final Logger logger = Logger.getLogger(DreaMSSpectrumTensorizer.class.getName());
  private final DreaMSSettings settings;

  public DreaMSSpectrumTensorizer(DreaMSSettings settings) {
    this.settings = settings;
  }

  /**
   * Converts a mass spectrum into a matrix of size (n + 1) x 2. Each row represents a pair of m/z
   * and intensity values of a signal present in the spectrum (hence 2 columns). The matrix consists
   * of:
   * <p>
   * 1. An artificial signal in the first row representing the precursor ion. - Its m/z value is set
   * to the MS1 isolated precursor m/z provided as `precMz`. - Its intensity is constant and set to
   * 1.1 to distinguish it from other n signals.
   * <p>
   * 2. The remaining rows correspond to the n most intense peaks from the spectrum. - n =
   * `DreaMSSettings.nHighestPeaks`. - The intensities of these peaks are normalized by dividing
   * each by the maximum intensity of the n most intense peaks (i.e., relative intensities). - The
   * m/z values left unchanged.
   *
   * @param spectrum the mass spectrum to be tensorized. Must not be null.
   * @param precMz   the precursor m/z value of the mass spectrum. Must not be null.
   * @return a float matrix of size (n + 1) x 2, where the first column represents m/z values and
   * the second column represents normalized intensities.
   */
  public float[][] tensorizeFragments(@NotNull MassSpectrum spectrum, @NotNull Double precMz) {

    int nHighestPeaks = settings.nHighestPeaks();
    double precIntensity = settings.precIntensity(); // Is always constant (1.1 by default; not that precMz is not)

    // Initialize the result array with nHighestPeaks + 1 rows and 2 columns
    float[][] result = new float[nHighestPeaks + 1][2];

    // Set the first row: [precMz, precIntensity]
    result[0][0] = precMz.floatValue();
    result[0][1] = (float) precIntensity;

    // Collect all peaks into a list of pairs (m/z, intensity)
    List<double[]> peaks = new ArrayList<>();
    int totalPeaks = spectrum.getNumberOfDataPoints();
    if (totalPeaks == 0) {
      return result;
    }

    for (int i = 0; i < totalPeaks; i++) {
      double mz = spectrum.getMzValue(i);
      double intensity = spectrum.getIntensityValue(i);
      peaks.add(new double[]{mz, intensity});
    }

    // Sort peaks by intensity in descending order
    peaks.sort((a, b) -> Double.compare(b[1], a[1])); // Sort by intensity (index 1)

    // Select the top nHighestPeaks peaks
    List<double[]> topPeaks = peaks.subList(0, Math.min(nHighestPeaks, peaks.size()));

    // Get base peak intensity
    float maxIntensity = (float) topPeaks.getFirst()[1];

    // Sort the selected peaks by m/z in ascending order
    topPeaks.sort(Comparator.comparingDouble(a -> a[0])); // Sort by m/z (index 0)

    // Add the selected peaks to the result array
    for (int i = 0; i < topPeaks.size(); i++) {
      double[] peak = topPeaks.get(i);

      result[i + 1][0] = (float) peak[0];  // m/z
      result[i + 1][1] =
          (float) peak[1] / maxIntensity;  // Intensity (division makes it a relative intensity)
    }

    // If there are fewer peaks than nHighestPeaks, the remaining rows are already zero-filled
    return result;
  }

  /**
   * Tensorizes a list of mass spectra. Only works on scans or {@link SpectralLibraryEntry} with
   * precursor mz.
   *
   * @param scans the mass spectra to be tensorized. Must not be null.
   * @return an array of float matrices of size num. signals x 2, where the first column of each
   * matrix represents m/z values and the second column represents normalized intensities. See
   * {@link #tensorizeFragments(MassSpectrum, Double)} for the details.
   */
  public float[][][] tensorizeSpectra(@NotNull List<? extends MassSpectrum> scans) {
    int originalSize = scans.size();

    // Filter scans to only include those with precursor m/z
    scans = scans.stream().filter(scan -> ScanUtils.getPrecursorMz(scan) != null).toList();

    if (originalSize > scans.size()) {
      logger.info(
          "List contained spectra without precursor m/z; those were filtered out. Remaining: %d; Total: %d".formatted(
              scans.size(), originalSize));
    }

    // Initialize the result as a 3D float array
    float[][][] tensorizedFragments = new float[scans.size()][][];

    int index = 0;
    for (MassSpectrum spectrum : scans) {
      // Extract mass list if scan; otherwise use fragments directly
      MassSpectrum fragments = spectrum;
      if (spectrum instanceof Scan scan) {
        fragments = scan.getMassList();
        if (fragments == null) {
          throw new MissingMassListException(scan);
        }
      }

      Double precMz = ScanUtils.getPrecursorMz(spectrum);

      // Tensorize the fragments for the current spectrum
      tensorizedFragments[index++] = tensorizeFragments(fragments, precMz);
    }

    return tensorizedFragments;
  }

}